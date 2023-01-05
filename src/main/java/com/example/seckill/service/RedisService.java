package com.example.seckill.service;

import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.pojo.Seckill;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * @author: Richerlv
 * @date: 2023/1/5 22:15
 * @description:
 */
@Service
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SeckillMapper seckillMapper;

    public Seckill getById(int seckillId) {
        String key = "seckill:" + seckillId;
        Seckill seckill = (Seckill) redisTemplate.opsForValue().get(key);
        return seckill;
    }

    public void putSeckill(int seckillId) {
        Seckill seckill = seckillMapper.getSeckillById(seckillId);
        if(seckill != null) {
            String key = "seckill:" + seckillId;
            redisTemplate.opsForValue().set(key, seckill);
        } else {
            System.out.println("not found");
        }
    }

}
