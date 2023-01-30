package com.example.seckill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * @author: Richerlv
 * @date: 2023/1/30 21:50
 * @description:
 */

public class ConfirmCallbackService implements RabbitTemplate.ConfirmCallback {

    private Logger logger = LoggerFactory.getLogger(ConfirmCallbackService.class);

    /**
     *
     * @param correlationData 对象内部有id （消息的唯一性）和Message
     * @param ack 消息投递到exchange 的状态，true表示成功
     * @param cause 表示投递失败的原因。 （若ack为false，则cause不为null；若ack是true，则cause为null）
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            logger.error("confirm==>发送到broker失败\r\n" + "correlationData={}\r\n" + "ack={}\r\n" + "cause={}", correlationData, ack, cause);
        } else {
            logger.info("confirm==>发送到broker成功\r\n" + "correlationData={}\r\n" + "ack={}\r\n" + "cause={}", correlationData, ack, cause);
        }
    }
}
