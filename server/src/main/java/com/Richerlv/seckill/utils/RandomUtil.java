package com.Richerlv.seckill.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author: Richerlv
 * @date: 2023/3/7 13:51
 * @description: 随机数生成工具类
 */

public class RandomUtil {

    private static final DateTimeFormatter dateFormatOne = DateTimeFormatter.ofPattern("yyyMMddHHmmssSS");

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * 生成订单编号—方式一：时间戳+N为随机数流水号
     * @return
     */
    public static String generateOrderCode() {
        return dateFormatOne.format(LocalDateTime.now()) + generateNumber(4);
    }

    public static String generateNumber(int n) {
        //StringBuffer线程安全
        StringBuffer number = new StringBuffer();
        for(int i = 1; i <= n; i ++) {
            number.append(random.nextInt(9));
        }
        return number.toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50; i++) {
            System.out.println(generateOrderCode());
        }
    }
}
