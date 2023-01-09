package com.example.seckill.service;

import com.example.seckill.dao.SuccessKilledMapper;
import com.example.seckill.dto.MailDto;
import com.example.seckill.pojo.SuccessKilled;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Richerlv
 * @date: 2023/1/7 20:12
 * @description:
 */

@Service
public class RabbitmqReceiverService {

    private Logger logger = LoggerFactory.getLogger(RabbitmqSenderService.class);

    @Autowired
    private MailService mailService;

    @Autowired
    private SuccessKilledMapper successKilledMapper;

    /**
     * 秒杀成功异步发送邮件-接收消息
     */
    @RabbitListener(queues = "sendMailQueue",containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(SuccessKilled info) {
        logger.info("秒杀异步邮件通知-接收消息:{}",info);
        try {
            //TODO:发邮件
            MailDto dto = new MailDto();
            dto.setReceiver(new String[]{info.getEmail()});
            dto.setSubject("亲爱的顾客您好！恭喜您秒杀成功！");
            dto.setContent("您刚才参与了\"" + info.getSeckill().getName() + "\"活动： 恭喜您秒杀成功，请于2分钟内支付，超时订单会失效哦！");

            mailService.sendSimpleEmail(dto);
        } catch (Exception e) {
            logger.error("秒杀异步邮件通知-接收消息-发生异常：",e.getMessage());
        }
    }

//    /**
//     * 秒杀成功进入支付-消费消息
//     */
//    @RabbitListener(queues = "pay_queue", containerFactory = "multiListenerContainer")
//    public void consumePayMsg(SuccessKilled info) {
//        logger.info("秒杀成功进入支付-接收消息：{}", info);
//        try {
//            //TODO：判断是不是“未支付”
//            if(info.getStatus() == 0) {
//                //TODO: 支付
//                int payRes = successKilledMapper.pay(info);
//                if(payRes > 0) {
//                    logger.info("{}：支付成功", info.getUserPhone());
//                } else {
//                    logger.error("{}：支付失败", info.getUserPhone());
//                }
//            } else {
//                logger.error("{}：订单状态异常", info);
//            }
//        } catch (Exception e) {
//            logger.error("秒杀成功进入支付-接收消息-发生异常：{}", e.fillInStackTrace());
//        }
//    }

    /**
     * 秒杀成功进入支付-监听者
     */
    @RabbitListener(queues = "nopay_dead_queue", containerFactory = "multiListenerContainer")
    public void consumePayMsg(SuccessKilled info) {
        logger.info("秒杀成功进入支付-监听者：{}", info);
        try {
            //TODO：判断是不是“未支付”
            if(info.getStatus() == 0) {
                //TODO: "失效"订单
                int payRes = successKilledMapper.inValid(info);
                if(payRes > 0) {
                    logger.info("{}：处理失效订单成功", info.getUserPhone());
                } else {
                    logger.error("{}：处理失效订单失败", info.getUserPhone());
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功进入支付-监听者-发生异常：{}", e.fillInStackTrace());
        }
    }
}
