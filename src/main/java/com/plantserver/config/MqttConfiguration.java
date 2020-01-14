package com.plantserver.config;

import com.plantserver.service.MqttMsgHandler;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@Configuration
@IntegrationComponentScan
public class MqttConfiguration {

    @Value("${spring.mqtt.url}")
    private String hostUrl;

    @Value("${spring.mqtt.client.consumer_id}")
    private String consumerId;

    @Value("${spring.mqtt.client.producer_id}")
    private String producerId;

    @Value("${spring.mqtt.default.subtopic}")
    private String defaultTopic;

    @Value("${spring.mqtt.default.pubTopic}")
    private String msgTopic;

    @Value("${spring.mqtt.completionTimeout}")
    private int completionTimeout;

    private static final Logger log = LoggerFactory.getLogger(MqttConfiguration.class);

    // 消息处理器
    @Resource
    private MessageHandler messageHandler;

    // mqtt客户端工厂类
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setServerURIs(new String[]{hostUrl});
        mqttConnectOptions.setKeepAliveInterval(2);
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }


    // 接收通道（消息消费者）
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    // 配置消息适配器，配置订阅客户端
    @Bean
    public MessageProducer inbound() {
        String[] topics = defaultTopic.split(",");
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(consumerId, mqttClientFactory());
        for (String topic : topics) {
            if (!StringUtils.isEmpty(topic)) {
                adapter.addTopic(topic, 1);
            }
        }
        adapter.setCompletionTimeout(completionTimeout);
        // 设置转换器，接收bytes
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(true);
        adapter.setConverter(converter);
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    // 通过通道获取数据
    // 接收消息处理器（订阅）
    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        log.info("[初始化]订阅:"+defaultTopic);
        return messageHandler;
    }


    // 配置消息适配器，配置发布端
    @Bean
    public MessageChannel mqttOutPutChannel() {
        return new DirectChannel();
    }

    // 接收消息处理器（发布）
    @Bean
    @ServiceActivator(inputChannel = "mqttOutPutChannel")
    public MessageHandler outbound() {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(producerId, mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(msgTopic);
        return messageHandler;
    }
}