package com.Richerlv.seckill.exception;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:48
 * @description:
 */

public class SeckillException extends RuntimeException{

    public SeckillException(String message) {
        super(message);
    }
    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
