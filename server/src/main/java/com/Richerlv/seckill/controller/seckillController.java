package com.Richerlv.seckill.controller;

import com.Richerlv.seckill.dto.Exposer;
import com.Richerlv.seckill.dto.SeckillExecution;
import com.Richerlv.seckill.enums.SeckillStateEnum;
import com.Richerlv.seckill.exception.RepeatKillException;
import com.Richerlv.seckill.exception.SeckillCloseException;
import com.Richerlv.seckill.pojo.Seckill;
import com.Richerlv.seckill.reponse.Result;
import com.Richerlv.seckill.service.CaptchasService;
import com.Richerlv.seckill.service.RedisService;
import com.Richerlv.seckill.service.SeckillService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author: Richerlv
 * @date: 2023/1/4 12:51
 * @description: 秒杀controller
 *
 * 接口风格：Restful --> /模块/资源/{标识}/集合
 */

@Controller
@RequestMapping("/seckill")
public class seckillController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillService seckillService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private CaptchasService captchasService;
    @Resource
    private RedisTemplate redisTemplate;

    private ExecutorService executorService;

    /**
     * 获取商品列表
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(Model model) {
        List<Seckill> seckillList = seckillService.getAll();
        model.addAttribute("seckillList", seckillList);
        return "list";
    }

    /**
     * 获取商品详情
     */
    @RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Integer seckillId, Model model) {
        //如果seckillId为空，重定向到list
        if(seckillId == null) {
            return "redirect:/seckill/list";
        }

        Seckill seckillDetail = seckillService.getSeckillById(seckillId);
        //如果seckill为空，转发到list(这里只是为了联系一下转发和重定向，没有别的深意)
        if(seckillDetail == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckillDetail);
        return "detail";
    }

    /**
     * 校验验证码
     */
    @RequestMapping(value = "/{seckillId}/{code}/verify", method = RequestMethod.GET)
    @ResponseBody
    public Result<Object> verifyCode(@PathVariable("code") Integer code,
                                     @PathVariable("seckillId") Integer seckillId,
                                     @CookieValue(value = "killPhone", required = false) String killPhone) {
        Boolean res = captchasService.verifyCode(seckillId, killPhone, code);
        if(res == true) {
            return new Result<>(true, "verify pass!");
        } else {
            return new Result<>(false, "verify failed!");
        }
    }

    /**
     * 暴露秒杀地址
     */
    @RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public Result<Exposer> exposer(@PathVariable("seckillId") Integer seckillId) {
        Result<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new Result<>(true, exposer);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = new Result<Exposer>(false, e.getMessage());
        }
        return result;
    }

    /**
     * 秒杀
     * 1000 * 10
     *
     * 优化sql前QPS： 226
     * 使用存储过程后的QPS：417
     * 使用redis后的QPS：589
     * lua脚本: 719
     */
    @RequestMapping(value = "/{seckillId}/{md5}/execution", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public Result<SeckillExecution> execute(@PathVariable("seckillId") Integer seckillId,
                                            @CookieValue(value = "killPhone", required = false) String killPhone,
                                            @PathVariable("md5") String md5) {
        if(killPhone == null) {
            return new Result<>(false, "未注册");
        }

        Result<SeckillExecution> result;
        try {
            //简单接口限流 - 计数器  线程不安全
            //分布式锁，解决线程不安全
//            Boolean res = redisTemplate.opsForValue().setIfAbsent("lock", 1);
//            if(res) {
//                //设置过期时间
//                redisTemplate.expire("lock", 1, TimeUnit.MINUTES);
//                try {
//                    String key = "limit";
//                    Integer count = (Integer) redisTemplate.opsForValue().get(key);
//                    if(count == null) {
//                        redisTemplate.opsForValue().set(key, 1, 100, TimeUnit.MINUTES);
//                    } else if(count < 50) {
//                        redisTemplate.opsForValue().increment(key);
//                    } else {
//                        System.out.println("被限流了");
//                        return null;
//                    }
//                    //redis优化
//                    SeckillExecution seckillExecution = seckillService.executeV4(seckillId, killPhone, md5);
//                    result = new Result<>(true, seckillExecution);
//                    return result;
//                } finally {
//                    redisTemplate.delete("lock");
//                }
//            }
//            return null;

//            /**
//             * 限流操作原子化
//             */
//            DefaultRedisScript redisScript = new DefaultRedisScript();
//            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/limit.lua")));
//            redisScript.setResultType(Long.class);
//            List<Object> list = new ArrayList<>();
//            list.add(50);
//            list.add(600000);
//            // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
//            Long res = (Long)redisTemplate.execute(redisScript, list);
//            if(res == 0L) {
//                SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
//                return new Result<>(true, execution);
//            }

            SeckillExecution seckillExecution = seckillService.executeV4(seckillId, killPhone, md5);
            result = new Result<>(true, seckillExecution);
            return result;
        } catch (RepeatKillException e1) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new Result<>(true, execution);
        } catch (SeckillCloseException e2) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new Result<>(true, execution);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new Result<>(true, execution);
        }
    }

    /**
     * 秒杀 -- 线程池泄洪
     */
    @RequestMapping(value = "/{seckillId}/{md5}/executionV2", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public Result<SeckillExecution> executeV2(@PathVariable("seckillId") Integer seckillId,
                                            @CookieValue(value = "killPhone", required = false) String killPhone,
                                            @PathVariable("md5") String md5) {
        if(killPhone == null) {
            return new Result<>(false, "未注册");
        }

        Result<SeckillExecution> result;
        try {
            Future<Result> future = executorService.submit(new Callable<Result>() {
                @Override
                public Result<SeckillExecution> call() throws Exception {
                    SeckillExecution seckillExecution = seckillService.executeV4(seckillId, killPhone, md5);
                    return new Result<>(true, seckillExecution);
                }
            });
            result = future.get();
            return result;
        } catch (RepeatKillException e1) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            return new Result<>(true, execution);
        } catch (SeckillCloseException e2) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            return new Result<>(true, execution);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            return new Result<>(true, execution);
        }
    }

    /**
     * 获取系统当前时间
     *
     * 保证前端时间与服务器时间一致
     * 直接获取前段时间会与服务器时间有偏差
     */
    @RequestMapping(value = "/time/now", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> time() {
        Date now = new Date();
        return new Result<Long>(true, now.getTime());
    }

    /**
     * 系统初始化时：将商品信息加载到redis中
     */
    @PostConstruct
    public void preheat() {
        List<Seckill> seckillList = seckillService.getAll();
        for(int i = 0; i < seckillList.size(); i ++) {
            redisService.putSeckill(seckillList.get(i));
            redisService.set(seckillList.get(i).getSeckillId() + "" + "stock:", seckillList.get(i).getNumber());
        }
    }

    /**
     * 用户支付/取消订单
     */
    @RequestMapping(value = "/{seckillId}/deal", method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public Result<String> dealOrder(@PathVariable("seckillId") Integer seckillId,
                                    @CookieValue(value = "killPhone", required = false) String killPhone) {
        return seckillService.dealOrder(seckillId, killPhone);
    }

    /**
     * 线程池初始化
     */
    @PostConstruct
    public void init() {
        executorService = new ThreadPoolExecutor(80, 80, 0,
                TimeUnit.MINUTES, new LinkedBlockingDeque<>(1024),
                new ThreadPoolExecutor.AbortPolicy());
    }

}
