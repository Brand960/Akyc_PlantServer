package com.plantserver.service;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component("messageHandler")
public class MQTTMessageHandler implements MessageHandler {

    private static final Logger log = Logger.getLogger(MessageConsumerService.class);

    @Autowired
    private InfluxDB influxDB;

    @Override
    public void handleMessage(Message<?> message) {
        //System.out.println("【*** 接收消息    ***】" + message.getPayload());
        String[] textarr = message.getPayload().toString().split(",");
        //System.out.println(datestr+"-------------------");
        try {
            BatchPoints batchPoints = BatchPoints.database("plantsurv_web").build();
            Point tmpPoint = Point.measurement("pt_new_" + textarr[1])
                    .time(Long.parseLong(textarr[2]), TimeUnit.MILLISECONDS)
                    .tag("device", textarr[1])
                    .addField("seq", textarr[3])
                    .addField("Ax", Float.parseFloat(textarr[4]))
                    .addField("Ay", Float.parseFloat(textarr[5]))
                    .addField("Az", Float.parseFloat(textarr[6]))
                    .addField("Wx", Float.parseFloat(textarr[7]))
                    .addField("Wy", Float.parseFloat(textarr[8]))
                    .addField("Wz", Float.parseFloat(textarr[9]))
                    .build();
            batchPoints.point(tmpPoint);
            influxDB.write(batchPoints);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String now = sdf.format(new Date(Long.parseLong(String.valueOf(textarr[2]))));
            System.out.println("============" + now + "数 据 添 加 成 功" + textarr[2] + "==================");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 这里对数据进行处理
    }
}
