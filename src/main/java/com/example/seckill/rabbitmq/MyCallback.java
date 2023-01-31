package com.example.seckill.rabbitmq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * @author: Richerlv
 * @date: 2023/1/30 21:50
 * @description:
 */

@Component
public class MyCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private Logger logger = LoggerFactory.getLogger(MyCallback.class);

    @Resource
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     *
     * @param correlationData 对象内部有id （消息的唯一性）和Message
     * @param ack 消息投递到exchange 的状态，true表示成功
     * @param cause 表示投递失败的原因。 （若ack为false，则cause不为null；若ack是true，则cause为null）
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            logger.error("confirm==>发送到broker失败, 原因是:{}", correlationData.getReturned());
            //重新发送
            rabbitTemplate.convertAndSend(correlationData.getReturned().getExchange(),
                    correlationData.getReturned().getRoutingKey(),
                    correlationData.getReturned().getMessage().getBody());
        } else {
            logger.info("confirm==>发送到broker成功{}", correlationData);
        }
    }

    /**
     * 消息未到达队列，会触发该方法
     *
     * @param returnedMessage
     */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        logger.error("消息未到达队列:{}", returnedMessage);
        //重新发送
        rabbitTemplate.convertAndSend(returnedMessage.getExchange(),
                returnedMessage.getRoutingKey(),
                returnedMessage.getMessage().getBody());
    }

}
