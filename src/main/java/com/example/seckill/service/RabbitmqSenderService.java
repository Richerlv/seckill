package com.example.seckill.service;

import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.pojo.SuccessKilled;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange("sendMailExchange");
                    rabbitTemplate.setRoutingKey("sendMailRoutingKey");

                    //将info当消息发送至队列
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = new MessageProperties();
                            //消息持久化, 避免在异常情况(重启，关闭，宕机)下消息系统中数据的丢失。
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            //这样消费者就可以以相同类型接收消息
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, SuccessKilled.class);
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
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange("pay_exchange");
                    rabbitTemplate.setRoutingKey("pay_routingkey");

                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = new MessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, SuccessKilled.class);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付通知消息-发生异常：{}", e.fillInStackTrace());
        }
    }

}


