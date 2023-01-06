package com.example.seckill.dao;

import com.example.seckill.pojo.Seckill;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author: Richerlv
 * @date: 2023/1/2 20:23
 * @description:
 */

@Mapper
public interface SeckillMapper {

    /**
     * 获取订单列表（可分页）
     * @return
     */
    List<Seckill> getAll(int start, int offset);


    /**
     * 获取一个订单(根据Id)
     */
    Seckill getSeckillById(int seckillId);



    /**
     * 减库存
     */
    int decrCount(int seckillId, Date killTime);


    /**
     * 执行存储过程
     * @param params
     */
    void killByProcedure(Map<String, Object> params);
}
