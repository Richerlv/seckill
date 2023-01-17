package com.example.seckill.config;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * @author: Richerlv
 * @date: 2023/1/7 15:43
 * @description:
 */

@Configuration
public class RabbitmqConfig {

    @Resource
    private CachingConnectionFactory connectionFactory;

    @Resource
    private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;

    @Value("${spring.rabbitmq.listener.simple.concurrency}")
    private int concurrency;

    @Value("${spring.rabbitmq.listener.simple.max-concurrency}")
    private int maxConcurrency;

    @Value("${spring.rabbitmq.listener.simple.prefetch}")
    private int prefetch;

    private final static String NOPAY_DEAD_QUEUE = "nopay_dead_queue";
    private final static String NOPAY_DEAD_EXCHANGE = "nopay_dead_exchange";
    private final static String NOPAY_DEAD_ROUTINGKEY = "nopay_dead_routingkey";
    private final static String PAY_QUEUE = "pay_queue";
    private final static String PAY_EXCHANGE = "pay_exchange";
    private final static String PAY_ROUTINGKEY = "pay_routingkey";
    private final static String MAIL_QUEUE = "sendMailQueue";
    private final static String MAIL_EXCHANGE = "sendMailExchange";
    private final static String MAIL_ROUTINGKEY = "sendMailRoutingKey";
    private final static String ORDER_QUEUE = "order_queue";
    private final static String ORDER_EXCHANGE = "order_exchange";
    private final static String ORDER_ROUTINGKEY = "order_routingkey";
    private final static String DEAL_QUEUE = "deal_queue";
    private final static String DEAL_EXCHANGE = "deal_exchange";
    private final static String DEAL_ROUTINGKEY = "deal_routingkey";

    /**
     * 单一消费者
     * @return
     */
    @Bean(name = "singleListenerContainer")
    public SimpleRabbitListenerContainerFactory listenerContainer(){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        return factory;
    }

    /**
     * 多个消费者-->高并发时提高消费效率
     * @return
     */
    @Bean(name = "multiListenerContainer")
    public SimpleRabbitListenerContainerFactory multiListenerContainer(){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory,connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        //确认消费模式-NONE
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setPrefetchCount(prefetch);
        return factory;
    }

    /**
     * 下单后异步发送邮件的消息模型
     */
    @Bean
    public Queue sendMailQueue() {
        return new Queue(MAIL_QUEUE, true);
    }

    @Bean
    public TopicExchange sendMailExchange() {
        return new TopicExchange(MAIL_EXCHANGE, true, false);
    }

    @Bean
    public Binding sendEmailBinding() {
        return BindingBuilder.bind(sendMailQueue()).to(sendMailExchange()).with(MAIL_ROUTINGKEY);
    }

    /**
     * 构建死信队列“失效”未支付订单消息模型
     */

    //支付队列
    @Bean
    public Queue payQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", NOPAY_DEAD_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", NOPAY_DEAD_ROUTINGKEY);
        arguments.put("x-message-ttl", 1000 * 60);

        return QueueBuilder.durable(PAY_QUEUE).withArguments(arguments).build();
    }

    //死信队列
    @Bean
    public Queue noPayQueue() {
        return new Queue(NOPAY_DEAD_QUEUE, true);
    }

    //支付交换机
    @Bean
    public TopicExchange payExchange() {
        return new TopicExchange(PAY_EXCHANGE, true, false);
    }

    //死信交换机
    @Bean
    public TopicExchange noPayExchange() {
        return new TopicExchange(NOPAY_DEAD_EXCHANGE, true, false);
    }

    //支付绑定
    @Bean
    public Binding payBinding() {
        return BindingBuilder.bind(payQueue()).to(payExchange()).with(PAY_ROUTINGKEY);
    }

    //死信绑定
    @Bean
    public Binding noPayBinding() {
        return BindingBuilder.bind(noPayQueue()).to(noPayExchange()).with(NOPAY_DEAD_ROUTINGKEY);
    }

    /**
     * redis预减库存成功异步下单消息模型
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTINGKEY);
    }

    /**
     * 用户支付/取消订单的消息模型
     */
    @Bean
    public Queue dealQueue() {
        return new Queue(DEAL_QUEUE, true);
    }

    @Bean
    public TopicExchange dealExchange() {
        return new TopicExchange(DEAL_EXCHANGE, true, false);
    }

    @Bean
    public Binding dealBinding() {
        return BindingBuilder.bind(dealQueue()).to(dealExchange()).with(DEAL_ROUTINGKEY);
    }

}
