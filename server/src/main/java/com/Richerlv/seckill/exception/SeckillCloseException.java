package com.Richerlv.seckill.exception;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:53
 * @description:
 */

public class SeckillCloseException extends SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
