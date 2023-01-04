package com.example.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author: Richerlv
 * @date: 2023/1/3 18:54
 * @description: 暴露秒杀地址结果封装
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Exposer {

    //秒杀是否开始
    private boolean exposed;

    //商品ID
    private int seckillId;

    //md5 一种加密算法
    private String md5;

    //当前时间(毫秒级)
    private long now;

    //秒杀开始时间
    private long start;

    //秒杀结束时间
    private long end;

    //秒杀开始构造方法
    public Exposer(boolean exposed, int seckillId, String md5) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.md5 = md5;
    }

    //秒杀还未开始构造方法
    public Exposer(boolean exposed, int seckillId, long now, long start, long end) {
        this.exposed = exposed;
        this.seckillId = seckillId;
        this.now = now;
        this.start = start;
        this.end = end;
    }

    //没有查到商品的构造方法
    public Exposer(boolean exposed, int seckillId) {
        this.exposed = exposed;
        this.seckillId = seckillId;
    }

}
