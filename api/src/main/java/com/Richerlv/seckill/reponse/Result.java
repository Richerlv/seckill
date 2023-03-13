package com.example.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Richerlv
 * @date: 2023/1/4 12:54
 * @description: Controller返回结果封装
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    //是否成功
    private boolean success;

    //数据
    private T data;

    //消息
    private String error;

    public Result(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public Result(boolean success, String error) {
        this.success = success;
        this.error = error;
    }
}
