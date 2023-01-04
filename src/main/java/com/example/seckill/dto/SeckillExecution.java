package com.example.seckill.dto;

import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.pojo.SuccessKilled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:04
 * @description:封装秒杀结果
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SeckillExecution {

    //秒杀商品id
    private int seckillId;

    //秒杀状态
    private int state;

    //秒杀状态信息
    private String stateInfo;

    //成功的订单
    private SuccessKilled successKilled;

    //失败时调
    public SeckillExecution(int seckillId, SeckillStateEnum stateEnum) {
        this.seckillId = seckillId;
        this.state = stateEnum.getState();
        this.stateInfo = stateEnum.getStateInfo();
    }

    //成功时调
    public SeckillExecution(int seckillId, SeckillStateEnum stateEnum, SuccessKilled successKilled) {
        this.seckillId = seckillId;
        this.state = stateEnum.getState();
        this.stateInfo = stateEnum.getStateInfo();
        this.successKilled = successKilled;
    }
}
