package com.Richerlv.seckill.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author: Richerlv
 * @date: 2023/1/12 20:22
 * @description: 图片验证码方法
 */

@Component
public class CaptchasService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成图片验证码
     */
    public BufferedImage createCaptchas(int seckillId, String userPhone) {
        int width = 90;
        int height = 40;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics graph = img.getGraphics();
        graph.setColor(new Color(0xDCDCDC));
        graph.fillRect(0, 0, width, height);
        //生成验证码
        Integer vCode = createRandom();
        graph.setColor(new Color(0,100,0));
        graph.setFont(new Font("Candara",Font.BOLD,24));
        //将验证码写在图片上
        graph.drawString(String.valueOf(vCode), 8, 24);
        graph.dispose();
        //将计算结果保存到redis上面去，过期时间1分钟
        String key = seckillId + userPhone + "captchas";
        redisTemplate.opsForValue().set(key, vCode, 6000, TimeUnit.SECONDS);
        return img;
    }

    public int createRandom() {
        Random random = new Random();
        int res = 0;
        int length = 4;
        for(int i = 0; i < length; i ++) {
            res *= 10;
            res += random.nextInt(10);
        }
        return res;
    }

    /**
     * 校验验证码
     * @param seckillId
     * @param userPhone
     * @param code
     * @return
     */
    public boolean verifyCode(int seckillId, String userPhone, int code) {
        String key = seckillId + userPhone + "captchas";
        Integer codeInRedis = (Integer) redisTemplate.opsForValue().get(key);
        if(codeInRedis != null) {
            return codeInRedis == code;
        }
        return false;
    }
}
