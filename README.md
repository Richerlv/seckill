# 高并发抢单系统

## 技术：
### SpringBoot
### Mybatis
### MySQL
### Redis
### RabbitMQ

## 项目亮点：
### 1.MySQL、Redis双层都解决了超卖问题
#### MySQL: 通过行级锁
#### Redis: Lua脚本将一系列redis操作语句整合在一起，顺序执行，不会被其他指令干扰
### 2.Redis做缓存预热、Redis预减库存
### 3.RabbitMQ异步发送邮件、异步下单
### 4.死信队列监听订单，超时未支付则”失效“订单
### 5.MySQL优化:使用存储过程，降低网络延迟对系统的影响
### 6.隐藏秒杀接口 + 图片验证码预防刷子流量
### 7.Mysql订单表联合索引解决重复秒杀问题
