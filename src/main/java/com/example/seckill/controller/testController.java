package com.example.seckill.controller;

import com.example.seckill.dao.SeckillMapper;
import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.SeckillExecution;
import com.example.seckill.enums.SeckillStateEnum;
import com.example.seckill.exception.RepeatKillException;
import com.example.seckill.exception.SeckillCloseException;
import com.example.seckill.exception.SeckillException;
import com.example.seckill.service.SeckillService;
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
    private SeckillMapper seckillDao;
    @Autowired
    private SuccessKilledMapper successKilledDao;
    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value = "/test")
    @ResponseBody
    public String test(@CookieValue("md5") String md5) {
        System.out.println(md5);
        return "ok";
    }

    @RequestMapping(value = "/test2")
    public String test2(Model model) {
        model.addAttribute("item", seckillDao.getSeckillById(2));
        return "test";
    }

    @RequestMapping(value = "/test3")
    public String test3(Model model) {
        model.addAttribute("item", seckillDao.getAll(2, 1));
        return "test";
    }

    @RequestMapping(value = "/test4")
    public String test4(Model model) {
        model.addAttribute("item", seckillDao.decrCount(3, new Date()));
        return "test";
    }

    @RequestMapping(value = "/test6")
    public String test6(Model model) {
        model.addAttribute("item", successKilledDao.insertSuccessKilled(1, "123456", new Date()));
        return "test";
    }

    @RequestMapping(value = "/test7")
    public String test7(Model model) {
        model.addAttribute("item", seckillService.exportSeckillUrl(2));
        return "test";
    }

    @RequestMapping(value = "/test8")
    @ResponseBody
    public SeckillExecution test8(Model model,  int seckillId,  String userPhone) {

        try{
            SeckillExecution res = seckillService.executeSeckill(seckillId, userPhone, seckillService.exportSeckillUrl(2).getMd5());
            return res;
        } catch (RepeatKillException e1) {
            return new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
        } catch (SeckillCloseException e2) {
            return new SeckillExecution(seckillId, SeckillStateEnum.END);
        } catch (Exception e) {
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
