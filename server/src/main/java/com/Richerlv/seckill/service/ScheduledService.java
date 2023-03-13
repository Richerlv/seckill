package com.Richerlv.seckill.service;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author: Richerlv
 * @date: 2023/3/7 16:00
 * @description: 定时任务
 */

public class ScheduledService {


    /**
     * 每隔10秒获取超时未支付订单，失效
     */
    @Scheduled(cron = "0/10 * * * * ? ")
    public void scheduledExpireOrder() {

    }
}
