package com.Richerlv.seckill.controller;

import com.Richerlv.seckill.dao.SeckillMapper;
import com.Richerlv.seckill.dao.SuccessKilledMapper;
import com.Richerlv.seckill.pojo.SuccessKilled;
import com.Richerlv.seckill.service.CaptchasService;
import com.Richerlv.seckill.service.MailService;
import com.Richerlv.seckill.service.RedisService;
import com.Richerlv.seckill.service.SeckillService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.OutputStream;

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

    @Autowired
    private CaptchasService captchasUtils;

    @RequestMapping(value = "/test")
    @ResponseBody
    public String test(HttpServletRequest request, HttpServletResponse response) {
        BufferedImage img = captchasUtils.createCaptchas(2, "1111111111");
        try {
            response.setContentType("image/png");
            OutputStream out = response.getOutputStream();
            ImageIO.write(img, "png", out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "test";
    }
}
