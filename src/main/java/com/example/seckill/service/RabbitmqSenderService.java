package com.example.seckill.service;

import com.alibaba.fastjson.JSON;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.Result;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.pojo.SuccessKilled;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author: Richerlv
 * @date: 2023/1/7 20:12
 * @description:rabbitmq生产者
 */

@Service
public class RabbitmqSenderService {

    private Logger logger = LoggerFactory.getLogger(RabbitmqSenderService.class);

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SuccessKilledMapper successKilledMapper;

    /**
     * 秒杀成功异步发送邮件通知消息
     */
    public void killSuccessSendMail(Integer seckillId, String userPhone) {
        logger.info("秒杀成功异步发送邮件通知消息-准备发送消息：{}", seckillId);
        try {
            if(seckillId != null && userPhone != null) {
                //查询订单
                SuccessKilled info = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                if(info != null) {
                    //如果订单存在, 发消息
                    rabbitTemplate.setExchange("sendMailExchange");
                    rabbitTemplate.setRoutingKey("sendMailRoutingKey");

                    //将info当消息发送至队列
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = new MessageProperties();
                            //消息持久化, 避免在异常情况(重启，关闭，宕机)下消息系统中数据的丢失。
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功异步发送邮件通知消息-发生异常：{}", seckillId, e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功进入支付通知消息
     */
    public void killSuccessToPay(Integer seckillId, String userPhone) {
        logger.info("秒杀成功进入支付通知消息-准备发送消息：{}", seckillId);
        try {
            //参数校验
            if(seckillId != null && userPhone != null) {
                //查订单
                SuccessKilled info = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                if(info != null) {
                    //发消息
                    rabbitTemplate.setExchange("pay_exchange");
                    rabbitTemplate.setRoutingKey("pay_routingkey");

                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = new MessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付通知消息-发生异常：{}", e.fillInStackTrace());
        }
    }

    /**
     * redis预减库存成功异步下单-通知消息
     */
    public SeckillExecution killSuccessToOrder(Integer seckillId, String userPhone) {
        logger.info("redis预减库存成功异步下单-通知消息:{}", seckillId);
        try {
            //参数校验
            if(seckillId != null && userPhone != null) {
                //通知消费者下单
                Map<String, Object> info = new HashMap<>();
                info.put("seckillId", seckillId);
                info.put("userPhone", userPhone);

                rabbitTemplate.setExchange("order_exchange");
                rabbitTemplate.setRoutingKey("order_routingkey");
                //看CorrelationData源码
                CorrelationData correlationData = new CorrelationData();
                //看ReturnedMessage源码
                correlationData.setReturned(
                        new ReturnedMessage(new Message(JSON.toJSONString(info).getBytes()),
                                000,
                                "这是Richerlv自制的ReturnedMessage",
                                "order_exchange",
                                "order_routingkey"));

                SeckillExecution seckillExecution = (SeckillExecution) rabbitTemplate.convertSendAndReceive(JSON.toJSONString(info).getBytes(), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        MessageProperties messageProperties = new MessageProperties();
                        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }
                }, correlationData);
                return seckillExecution;
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.DATA_REWRITE);
            }
        } catch (Exception e) {
            logger.error("redis预减库存成功异步下单-出现异常:{}", e.fillInStackTrace());
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }

    /**
     * 用户支付/取消订单-通知消息
     */
    public Result<String> toDeal(Integer seckillId, String userPhone) {
        logger.info("用户支付/取消订单-准备发送消息：{}", seckillId);
        try {
            //参数校验
            if(seckillId != null && userPhone != null) {
                //查订单
                SuccessKilled info = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
                if(info != null) {
                    //发消息
                    rabbitTemplate.setExchange("deal_exchange");
                    rabbitTemplate.setRoutingKey("deal_routingkey");

                    Result<String> res = (Result<String>) rabbitTemplate.convertSendAndReceive(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = new MessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        }
                    });
                    return res;
                }

            } else {
                return new Result<>(false, "此订单不存在！");
            }
        } catch (Exception e) {
            logger.error("用户支付/取消订单-发生异常：{}", e.fillInStackTrace());
        }
        return null;
    }

}


