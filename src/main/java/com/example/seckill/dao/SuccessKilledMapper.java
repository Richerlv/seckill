package com.example.seckill.dao;

import com.example.seckill.pojo.SuccessKilled;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author: Richerlv
 * @date: 2023/1/2 19:37
 * @description:
 */

@Mapper
public interface SuccessKilledMapper {

    /**
     * 插入明细
     */
    int insertSuccessKilled(int seckillId, String userPhone, Date nowTime);


    /**
     * 根据seckillId, userPhone查询明细 --可以用来获取秒杀结果
     */
    SuccessKilled getSuccessKilledById(int seckillId, String userPhone);

    /**
     * 支付
     */
    int pay(SuccessKilled successKilled);

    /**
     * 取消订单
     */
    int cancel(SuccessKilled successKilled);

    /**
     * 删除订单
     */
    int deleteOrder(SuccessKilled successKilled);

}
