package com.plantserver.service;

import com.plantserver.Util.ParserUtil;
import com.plantserver.config.MqttGateway;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
public class MqttMsgPublisher {
    @Resource
    private MqttGateway mqttGateway;
    @Resource
    private ParserUtil parserUtil;

    private static MqttMsgPublisher mqttMsgPublisher;

    @PostConstruct //通过@PostConstruct实现初始化bean之前进行的操作
    public void init() {
        mqttMsgPublisher = this;
    }

    public void sendMqtt() {
        byte[] sendData = mqttMsgPublisher.parserUtil.toStringHex("F8 FF FF FF FF FF FF FF F9 FF FF FF FA FF FB FF FC FF FD FF FE FF FF FF 20 00 20 00 20 00 0D 0A");
        mqttMsgPublisher.mqttGateway.sendToMqtt(sendData, "byteMsgTest");
    }
}
