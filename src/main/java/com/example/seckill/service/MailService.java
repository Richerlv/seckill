package com.example.seckill.service;

import com.example.seckill.dto.MailDto;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author: Richerlv
 * @date: 2023/1/7 19:30
 * @description:
 */

@Service
public class MailService {

    private final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送简单文本文件
     */
    @Async
    public void sendSimpleEmail(MailDto dto){
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setSubject(dto.getSubject());
            message.setText(dto.getContent());
            message.setTo(dto.getReceiver());

            mailSender.send(message);
            logger.info("发送简单文本文件-发送成功!");
        }catch (Exception e){
            logger.error("发送简单文本文件-发生异常： ",e.getMessage());
        }
    }

    /**
     * 发送花哨邮件
     * @param dto
     */
    @Async
    public void sendHTMLMail(MailDto dto){
        try {
            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper messageHelper=new MimeMessageHelper(message,true,"utf-8");
            messageHelper.setFrom(from);
            messageHelper.setTo(dto.getReceiver());
            messageHelper.setSubject(dto.getSubject());
            messageHelper.setText(dto.getContent(),true);

            mailSender.send(message);
            logger.info("发送花哨邮件-发送成功!");
        }catch (Exception e){
            logger.error("发送花哨邮件-发生异常： ",e.fillInStackTrace());
        }
    }

}
