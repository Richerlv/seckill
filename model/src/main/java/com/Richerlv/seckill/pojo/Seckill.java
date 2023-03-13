package com.example.seckill.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author: Richerlv
 * @date: 2023/1/2 19:23
 * @description:
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class Seckill {

    private int seckillId;
    private String name;
    private int number;
    private Date start;
    private Date end;
    private Date createTime;
}
