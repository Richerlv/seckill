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
     * 根据seckillId, userPhone查询明细
     */
    SuccessKilled getSuccessKilledById(int seckillId, String userPhone);
}
