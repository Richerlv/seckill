package com.example.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Richerlv
 * @date: 2023/1/7 19:36
 * @description:
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailDto {

    //邮件主题
    private String subject;

    //邮件内容
    private String content;

    //收件人
    private String[] receiver;
}
