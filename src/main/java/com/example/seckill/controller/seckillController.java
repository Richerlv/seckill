package com.example.seckill.controller;

import com.example.seckill.dto.Exposer;
import com.example.seckill.dto.Result;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.exception.RepeatKillException;
import com.example.seckill.exception.SeckillCloseException;
import com.example.seckill.pojo.Seckill;
import com.example.seckill.service.CaptchasService;
import com.example.seckill.service.RedisService;
import com.example.seckill.service.SeckillService;
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
            //优化后:调用存储过程
//            SeckillExecution seckillExecution = seckillService.executeProcedure(seckillId, killPhone, md5);

            //优化前:
//            SeckillExecution seckillExecution = seckillService.executeSeckill(seckillId, killPhone, md5);

            //redis优化
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



}
