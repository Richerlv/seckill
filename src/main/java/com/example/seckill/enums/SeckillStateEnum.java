package com.example.seckill.enums;

import com.example.seckill.pojo.Seckill;

/**
 * @author: Richerlv
 * @date: 2023/1/3 19:08
 * @description: 秒杀结果状态枚举
 */
public enum SeckillStateEnum {
    SUCCESS(1, "秒杀成功"),
    END(0, "秒杀结束"),
    REPEAT_KILL(-1, "重复秒杀"),
    INNER_ERROR(-2, "系统异常"),
    DATA_REWRITE(-3, "数据篡改");

    //秒杀状态
    private int state;

    //秒杀状态信息
    private String stateInfo;

    SeckillStateEnum(int state, String stateInfo) {
        this.state = state;
        this.stateInfo = stateInfo;
    }

    public int getState() {
        return this.state;
    }

    public String getStateInfo() {
        return this.stateInfo;
    }

    public static SeckillStateEnum stateOf(int index) {
        for(SeckillStateEnum state : values()) {
            if(state.getState() == index) {
                return state;
            }
        }
        return null;
    }
}
