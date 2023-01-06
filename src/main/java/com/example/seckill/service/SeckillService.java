package com.example.seckill.service;

import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dto.Exposer;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.exception.RepeatKillException;
import com.example.seckill.exception.SeckillCloseException;
import com.example.seckill.exception.SeckillException;
import com.example.seckill.pojo.Seckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Richerlv
 * @date: 2023/1/3 18:47
 * @description: 站在用户角度设计接口：接口的粒度、返回值、参数
 */

public interface SeckillService {

    /**
     * 获取商品列表
     */
    List<Seckill> getAll();

    /**
     * 获取商品详情
     */
    Seckill getSeckillById(int seckillId);

    /**
     * 暴露秒杀地址接口
     *
     * 防止恶意用户提前知道秒杀接口，写脚本大量发送请求
     * 此操作可以将接口动态化，连写代码的人都不知道
     */
    Exposer exportSeckillUrl(int seckillId);

    /**
     * 秒杀
     */
    SeckillExecution executeSeckill(int seckillId, String userPhone, String md5)
            throws RepeatKillException, SeckillCloseException, SeckillException;


    /**
     * 使用存储过程执行秒杀
     */
    SeckillExecution executeProcedure(int seckillId, String userPhone, String md5);

}
