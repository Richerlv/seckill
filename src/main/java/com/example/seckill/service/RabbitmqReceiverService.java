package com.example.seckill.service;

import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.MailDto;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.pojo.SuccessKilled;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: Richerlv
 * @date: 2023/1/7 20:12
 * @description:
 */

@Service
public class RabbitmqReceiverService {

    private Logger logger = LoggerFactory.getLogger(RabbitmqSenderService.class);

    volatile static int i = 0;

    @Autowired
    private MailService mailService;

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    @Autowired
    private SeckillMapper seckillMapper;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 秒杀成功异步发送邮件-接收消息
     */
    @RabbitListener(queues = "sendMailQueue",containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(SuccessKilled info) {
        logger.info("秒杀异步邮件通知-接收消息:{}",info);
        try {
            //TODO:发邮件
            MailDto dto = new MailDto();
            dto.setReceiver(new String[]{info.getEmail()});
            dto.setSubject("亲爱的顾客您好！恭喜您秒杀成功！");
            dto.setContent("您刚才参与了\"" + info.getSeckill().getName() + "\"活动： 恭喜您秒杀成功，请于2分钟内支付，超时订单会失效哦！");

            mailService.sendSimpleEmail(dto);
        } catch (Exception e) {
            logger.error("秒杀异步邮件通知-接收消息-发生异常:{}",e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功进入支付-消费消息
     */
    @RabbitListener(queues = "pay_queue", containerFactory = "multiListenerContainer")
    public void consumePayMsg(SuccessKilled info) {
        logger.info("秒杀成功进入支付-接收消息：{}", info);
        try {
            while(true) {

            }
//            //TODO：判断是不是“未支付”
//            if(info.getStatus() == 0) {
//                //TODO: 支付
//                int payRes = successKilledMapper.pay(info);
//                if(payRes > 0) {
//                    logger.info("{}：支付成功", info.getUserPhone());
//                } else {
//                    logger.error("{}：支付失败", info.getUserPhone());
//                }
//            } else {
//                logger.error("{}：订单状态异常", info);
//            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付-接收消息-发生异常：{}", e.fillInStackTrace());
        }
    }

    /**
     * redis预减库存成功异步下单
     */
    @RabbitListener(queues = "order_queue", containerFactory = "singleListenerContainer")
    public SeckillExecution consumeOrderMsg(HashMap<String, Object> info) {
        logger.info("redis预减库存成功异步下单-接收消息:{}",info);
        int seckillId = (int) info.get("seckillId");
        String userPhone = (String) info.get("userPhone");
        try {
            //TODO:访问数据库
            Date nowTime = new Date();
            Map<String, Object> params = new HashMap<>();
            params.put("seckillId", seckillId);
            params.put("userPhone", userPhone);
            params.put("nowTime", nowTime);
            params.put("result", null);

            seckillMapper.killByProcedure(params);
            int result = (int) params.get("result");
            if (result == 1) {
                SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error("redis预减库存成功异步下单-接收消息-发生异常：",e.fillInStackTrace());
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }


    /**
     * 秒杀成功进入支付-监听者
     */
    @RabbitListener(queues = "nopay_dead_queue", containerFactory = "singleListenerContainer")
    public void consumePayMsgListener(SuccessKilled info) {
        logger.info("秒杀成功进入支付-监听者：{}", info);
        try {
            //TODO：判断是不是“未支付”
            if(info.getStatus() == 0) {
                //TODO: 删除mysql中的订单, 恢复数据库的库存
                int payRes = successKilledMapper.deleteOrder(info);
                int incrRes = seckillMapper.incrCount(info.getSeckillId());

                //TODO:删除redis中的订单、恢复redis中的库存
                String orderKey = info.getSeckillId() + "" + info.getUserPhone();
                String seckillKey = info.getSeckillId() + "" + "stock:";
                redisTemplate.delete(orderKey);
                redisTemplate.opsForValue().increment(seckillKey);
            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付-监听者-发生异常：{}", e.fillInStackTrace());
        }
    }
}
