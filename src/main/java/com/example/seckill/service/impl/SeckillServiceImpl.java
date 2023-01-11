package com.example.seckill.service.impl;

import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.Exposer;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.exception.RepeatKillException;
import com.example.seckill.exception.SeckillCloseException;
import com.example.seckill.exception.SeckillException;
import com.example.seckill.pojo.Seckill;
import com.example.seckill.pojo.SuccessKilled;
import com.example.seckill.service.RabbitmqSenderService;
import com.example.seckill.service.RedisService;
import com.example.seckill.service.SeckillService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.*;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:20
 * @description:
 */

@Service
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SeckillMapper seckillMapper;

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RabbitmqSenderService rabbitmqSenderService;

    @Resource
    private RedisTemplate redisTemplate;

    //md5盐值字符串，用于混淆md5
    private final String salt = "A46~4`23fjka@$#T05sdfh;asd4d6sg^*&!";

    @Override
    public List<Seckill> getAll() {

        //要是秒杀商品很多，这里要改
        return seckillMapper.getAll(0, 100);
    }

    @Override
    public Seckill getSeckillById(int seckillId) {
        return seckillMapper.getSeckillById(seckillId);
    }

    /**
     * 秒杀地址暴露接口
     *
     * 逻辑：
     * 没有查到商品：返回false，seckillId
     * 查到商品：
     * 比较系统当前时间和秒杀时间
     * 未开始：返回false，当前时间，秒杀开始、结束时间，seckillId
     * 已经开始：返回true，md5，seckillId
     * 已结束：返回false，当前时间，秒杀开始、结束时间，seckillId
     *
     * 优化：redis缓存热点数据
     *
     * @param seckillId
     * @return
     */
    @Override
    public Exposer exportSeckillUrl(int seckillId) {

        //获取当前系统时间
        Date nowTime = new Date();
        //获取秒杀商品(先查Redis，再查MySQL)
        Seckill seckill = redisService.getById(seckillId);
        if(seckill == null) {
            //没查到，去查MySQL
            seckill = seckillMapper.getSeckillById(seckillId);
            //存到redis
            if(seckill != null) {
                redisService.putSeckill(seckill);
            }
        }

        //没有获取到商品
        if(seckill == null) {
            return new Exposer(false, seckillId);
        }

        //比较并返回结果
        //未开始 & 已结束
        if(nowTime.getTime() < seckill.getStart().getTime() || nowTime.getTime() > seckill.getEnd().getTime()) {
            return new Exposer(false,
                    seckillId,
                    nowTime.getTime(),
                    seckill.getStart().getTime(),
                    seckill.getEnd().getTime());
        } else {
            //转化特定字符串的过程，不可逆
            String md5 = getMD5(seckillId);
            return new Exposer(true, seckillId, md5);
        }

    }

    private String getMD5(int seckillId) {
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 秒杀操作接口
     * 逻辑：
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    @Transactional
    public SeckillExecution executeSeckill(int seckillId, String userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        //验证md5
        String md5Verify = getMD5(seckillId);
        if(md5 == null || !md5Verify.equals(md5)) {
            //验证失败
            throw new SeckillException("Seckill Data Rewrite");
        }

        Date nowTime = new Date();

        //采用“下单减库存”的流程
        try {
            //下单
            int insertRes = successKilledMapper.insertSuccessKilled(seckillId, userPhone, nowTime);
            if(insertRes <= 0) {
                //插入失败，代表主键重复，重复抢购了
                throw new RepeatKillException("Seckill repeat!");
            } else {
                //扣减库存
                int dcreRes = seckillMapper.decrCount(seckillId, nowTime);
                if(dcreRes <= 0) {
                    //库存扣减失败，代表秒杀结束
                    throw new SeckillCloseException("Seckill closed!");
                } else {
                    //秒杀成功
                    SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (RepeatKillException e1) {
            throw e1;
        } catch (SeckillCloseException e2) {
            throw e2;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            /**
             * 1.除了上面两个，其他所有异常（包括编译期异常）都视为系统内部异常
             * 2.将编译器异常转化为运行期异常
             * 3.Spring事务管理会对所有运行期异常做Rollback
             */
            throw new SeckillException("Seckill system inner error: " + e.getMessage());
        }

    }

    /**
     * 存储过程
     * QPS:300
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeProcedure(int seckillId, String userPhone, String md5) {
        //验证md5
        String md5Verify = getMD5(seckillId);
        if (md5 == null || !md5Verify.equals(md5)) {
            //验证失败
            throw new SeckillException("Seckill Data Rewrite");
        }
        Date nowTime = new Date();

        Map<String, Object> params = new HashMap<>();
        params.put("seckillId", seckillId);
        params.put("userPhone", userPhone);
        params.put("nowTime", nowTime);
        params.put("result", null);
        try {
            //执行存储过程
            seckillMapper.killByProcedure(params);
            int result = (int) params.get("result");
            if (result == 1) {
                SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);

//                //秒杀成功：发邮件
//                rabbitmqSenderService.killSuccessSendMail(seckillId, userPhone);
//                //秒杀成功：支付
//                rabbitmqSenderService.killSuccessToPay(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }


    /**
     * 使用redis优化秒杀逻辑  - redis的超卖没解决
     *
     * 10000 个线程， 5000个会被redis查库存筛掉
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeV3(int seckillId, String userPhone, String md5) {

        //验证md5
        String md5Verify = getMD5(seckillId);
        if(md5 == null || !md5Verify.equals(md5)) {
            //验证失败
            throw new SeckillException("Seckill Data Rewrite");
        }
        Date nowTime = new Date();

        Map<String, Object> params = new HashMap<>();
        params.put("seckillId", seckillId);
        params.put("userPhone", userPhone);
        params.put("nowTime", nowTime);
        params.put("result", null);
        try {
            //查商品
            Seckill seckill = redisService.getById(seckillId);
            //商品不为空
            if(seckill != null) {
                //TODO:判断是否重复抢购
                String orderKey = seckillId + "" + userPhone;
                if(redisService.get(orderKey) == null) {
                    //TODO:判断库存 > 0?
                    String seckillKey = seckill.getSeckillId() + "" + "stock:";
                    if((Integer)redisTemplate.opsForValue().get(seckillKey) > 0) {
                        //TODO:减库存
                        redisTemplate.opsForValue().decrement(seckillKey);
                        //TODO: redis中下订单 ps:这里的value不重要
                        Boolean res = redisTemplate.opsForValue().setIfAbsent(orderKey, orderKey);
                        if(res) {
                            //TODO:访问数据库
                            //执行存储过程
                            seckillMapper.killByProcedure(params);
                            int result = (int) params.get("result");
                            if (result == 1) {
                                SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                            } else {
                                System.out.println(111);
                                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
                            }
                        } else {
                            System.out.println(222);
                            redisTemplate.opsForValue().increment(seckillKey);
                            return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
                        }
                    } else {
                        System.out.println(333);
                        return new SeckillExecution(seckillId, SeckillStateEnum.END);
                    }
                } else {
                    System.out.println(444);
                    return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
                }
            } else {
                System.out.println(555);
                return new SeckillExecution(seckillId, SeckillStateEnum.END);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }

    /**
     * redis + lua解决超卖：还有bug
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeV4(int seckillId, String userPhone, String md5) {

        //验证md5
        String md5Verify = getMD5(seckillId);
        if(md5 == null || !md5Verify.equals(md5)) {
            //验证失败
            throw new SeckillException("Seckill Data Rewrite");
        }
        Date nowTime = new Date();
        Map<String, Object> params = new HashMap<>();
        params.put("seckillId", seckillId);
        params.put("userPhone", userPhone);
        params.put("nowTime", nowTime);
        params.put("result", null);
        try {

            String seckillKey = seckillId + "" + "stock:";
            String orderKey = seckillId + "" + userPhone;

            // 执行 lua 脚本
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            // 指定 lua 脚本
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/seckill.lua")));
            // 指定返回类型
            redisScript.setResultType(Long.class);
            List<Object> list = new ArrayList<>();
            list.add(orderKey);
            list.add(seckillKey);
            // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
            Long res = (Long)redisTemplate.execute(redisScript, list, orderKey);
            System.out.println("res = " + res);
            if(res == 1) {
                //TODO:访问数据库
                //执行存储过程
                seckillMapper.killByProcedure(params);
                int result = (int) params.get("result");
                if (result == 1) {
                    SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                } else {
                    System.out.println(111);
                    return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
                }
            } else {
                System.out.println(222);
                return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }

    }

    /**
     * redis预减库存 + rabbitmq异步下单
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeV5(int seckillId, String userPhone, String md5) {

        //验证md5
        String md5Verify = getMD5(seckillId);
        if(md5 == null || !md5Verify.equals(md5)) {
            //验证失败
            throw new SeckillException("Seckill Data Rewrite");
        }

        try {
            //查商品
            Seckill seckill = redisService.getById(seckillId);
            //商品不为空
            if(seckill != null) {
                //TODO:判断是否重复抢购
                String orderKey = seckillId + "" + userPhone;
                if(redisService.get(orderKey) == null) {
                    //TODO:判断库存 > 0?
                    String seckillKey = seckill.getSeckillId() + "" + "stock:";
                    if((Integer)redisTemplate.opsForValue().get(seckillKey) > 0) {
                        //TODO:减库存
                        redisTemplate.opsForValue().decrement(seckillKey);
                        //TODO: redis中下订单 ps:这里的value不重要
                        Boolean res = redisTemplate.opsForValue().setIfAbsent(orderKey, orderKey);
                        if(res) {
                            //TODO:异步下单
                            return rabbitmqSenderService.killSuccessToOrder(seckillId, userPhone);
                        } else {
                            System.out.println("impl 222");
                            redisTemplate.opsForValue().increment(seckillKey);
                            return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
                        }
                    } else {
                        System.out.println("impl 333");
                        return new SeckillExecution(seckillId, SeckillStateEnum.END);
                    }
                } else {
                    System.out.println("impl  444");
                    return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
                }
            } else {
                System.out.println("impl 555");
                return new SeckillExecution(seckillId, SeckillStateEnum.END);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
