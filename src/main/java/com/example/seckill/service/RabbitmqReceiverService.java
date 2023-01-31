package com.example.seckill.service;

import com.alibaba.fastjson.JSONObject;
import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.MailDto;
import com.example.seckill.dto.Result;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.pojo.SuccessKilled;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 秒杀成功异步发送邮件-接收消息
     */
//    @RabbitListener(queues = "sendMailQueue",containerFactory = "singleListenerContainer")
    @RabbitListener(queues = "sendMailQueue")
    public void consumeEmailMsg(SuccessKilled info, @Headers Map<String,Object> headers, Channel channel) throws Exception {
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
            //出现异常则手动确认NACK
            channel.basicNack((Long) headers.get(AmqpHeaders.DELIVERY_TAG),false, true);
        }

        /**
         * deliveryTag:该消息的index
         * multiple：是否批量.true:将一次性ack所有小于deliveryTag的消息
         */
        channel.basicAck((Long) headers.get(AmqpHeaders.DELIVERY_TAG),false);
    }

    /**
     * redis预减库存成功异步下单
     */
    @RabbitListener(queues = "order_queue")
//    @RabbitListener(queues = "order_queue", containerFactory = "singleListenerContainer")
    public SeckillExecution consumeOrderMsg(byte[] msg, @Headers Map<String, Object> headers, Channel channel) throws Exception {

        //byte数组转化为Java对象
        Map<String, Object> info = new HashMap<>();
        if(msg!=null) {
            String tmp=new String((byte[]) msg,"UTF-8");
            info= JSONObject.parseObject(tmp, HashMap.class);
        }

        logger.info("redis预减库存成功异步下单-接收消息:{}",info);

        int seckillId = (int) info.get("seckillId");
        String userPhone = (String) info.get("userPhone");
        SeckillExecution seckillExecution;
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
                seckillExecution = new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
            } else {
                //MySQL操作失败，回增redis
                String seckillKey = seckillId + "" + "stock:";
                String orderKey = seckillId + "" + userPhone;
                redisTemplate.opsForValue().increment(seckillKey);
                redisTemplate.delete(orderKey);
                seckillExecution = new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error("redis预减库存成功异步下单-接收消息-发生异常：",e.fillInStackTrace());
            //出现异常则手动确认NACK
            channel.basicNack((Long) headers.get(AmqpHeaders.DELIVERY_TAG),false, true);
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }

        channel.basicAck((Long) headers.get(AmqpHeaders.DELIVERY_TAG),false);
        return seckillExecution;
    }


    /**
     * 秒杀成功进入支付-监听者
     */
    @RabbitListener(queues = "nopay_dead_queue")
//    @RabbitListener(queues = "nopay_dead_queue", containerFactory = "singleListenerContainer")
    public void consumePayMsgListener(SuccessKilled info) {
        logger.info("秒杀成功进入支付-监听者：{}", info);
        try {
            //TODO：判断是不是“未支付”,这里需要重新查询一遍，传过来未支付，有可能TTL内订单状态有变动
            SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(info.getSeckillId(), info.getUserPhone());
            if(successKilled != null && successKilled.getStatus() == 0) {
                //TODO: 删除mysql中的订单, 恢复数据库的库存 --mysql的事务,失败了应该出队之后重新入队
                int payRes = successKilledMapper.deleteOrder(successKilled);
                int incrRes = seckillMapper.incrCount(successKilled.getSeckillId());

                //TODO:删除redis中的订单、恢复redis中的库存 --应该用lua脚本原子化
                String orderKey = successKilled.getSeckillId() + "" + successKilled.getUserPhone();
                String seckillKey = successKilled.getSeckillId() + "" + "stock:";
                redisTemplate.delete(orderKey);
                redisTemplate.opsForValue().increment(seckillKey);
            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付-监听者-发生异常：{}", e.fillInStackTrace());
        }
    }

    /**
     * 用户支付/取消订单(这里只实现取消)
     */
//    @RabbitListener(queues = "deal_queue", containerFactory = "singleListenerContainer")
    @RabbitListener(queues = "deal_queue")
    public Result<String> consumeDealMsg(SuccessKilled info) {
        logger.info("用户支付/取消订单-接收消息：{}", info);
        Result<String> res = new Result<>(false, "订单状态异常,取消失败！");;
        try {
            //TODO：判断是不是“未支付”,这里需要重新查询一遍，传过来未支付，有可能TTL内订单状态有变动
            SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(info.getSeckillId(), info.getUserPhone());
            if(successKilled != null && successKilled.getStatus() == 0) {
                //TODO: 取消订单
                int cancelRes = successKilledMapper.cancel(successKilled);
                if(cancelRes == 0) {
                    res = new Result<>(false, "取消失败,请重试");
                } else {
                    res = new Result<>(true, "取消成功！");
                }
            } else {
                res = new Result<>(false, "订单状态异常,取消失败！");
            }
        } catch (Exception e) {
            logger.error("用户支付/取消订单-发生异常：{}", e.fillInStackTrace());
        }
        return res;
    }
}
