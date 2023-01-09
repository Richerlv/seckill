package com.example.seckill.controller;

import com.example.seckill.dto.MailDto;
import com.example.seckill.pojo.SuccessKilled;
import com.example.seckill.service.MailService;
import com.example.seckill.service.RedisService;
import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.exception.RepeatKillException;
import com.example.seckill.exception.SeckillCloseException;
import com.example.seckill.service.SeckillService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * @author: Richerlv
 * @date: 2023/1/2 10:04
 * @description:
 */

@Controller
public class testController {

    @Autowired
    private RedisService redisDao;

    @Autowired
    private SeckillMapper seckillDao;
    @Autowired
    private MailService mailService;
    @Autowired
    private SuccessKilledMapper successKilledDao;
    @Autowired
    private SeckillService seckillService;

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    private Logger logger = LoggerFactory.getLogger(testController.class);

    @RequestMapping(value = "/{seckillId}/{userPhone}/pay")
    public String test8(@PathVariable("seckillId")Integer seckillId, @PathVariable("userPhone")String userPhone) {
        SuccessKilled successKilled = successKilledMapper.getSuccessKilledById(seckillId, userPhone);
        int res = successKilledMapper.pay(successKilled);
        logger.info("支付结果：{}", res);
        return "test";
    }
}
