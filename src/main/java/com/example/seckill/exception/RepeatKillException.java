package com.example.seckill.exception;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:52
 * @description:
 */

public class RepeatKillException extends SeckillException {
    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
