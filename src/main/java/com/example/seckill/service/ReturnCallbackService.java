package com.example.seckill.service;

import com.rabbitmq.client.Return;
import com.rabbitmq.client.ReturnCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Richerlv
 * @date: 2023/1/30 21:54
 * @description:
 */

public class ReturnCallbackService implements ReturnCallback {

    private Logger logger = LoggerFactory.getLogger(ReturnCallbackService.class);

    @Override
    public void handle(Return aReturn) {
        logger.info("消息发送队列失败");
    }
}
