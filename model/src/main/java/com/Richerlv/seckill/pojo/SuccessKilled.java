package com.example.seckill.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author: Richerlv
 * @date: 2023/1/2 19:31
 * @description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class SuccessKilled {

    private int seckillId;
    private String userPhone;
    private String email;
    private short status;
    private Date createTime;
    private String orderNo;

    /**
     * 变通：一个商品对应多份订单
     */
    private Seckill seckill;
}
