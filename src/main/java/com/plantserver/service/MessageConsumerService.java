package com.plantserver.service;

import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class MessageConsumerService {

    private static final Logger log = Logger.getLogger(MessageConsumerService.class);

    @Autowired
    private InfluxDB influxDB;

    //    @JmsListener(destination="${spring.activemq.queue}")
    //    public void receiveMessage(String text) {    // 进行消息接收处理
    //        System.out.println("【*** 接收消息    ***】" + text);
    //        String[] textarr = text.split(",");
    //        //System.out.println(datestr+"-------------------");
    //        try {
    //            BatchPoints batchPoints = BatchPoints.database("plantsurv_web").build();
    //            Point tmpPoint = Point.measurement("pt_new_" + textarr[1])
    //                    .time(Long.parseLong(textarr[2]),TimeUnit.MILLISECONDS)
    //                    .tag("device", textarr[1])
    //                    .addField("seq", textarr[3])
    //                    .addField("Ax", Float.parseFloat(textarr[4]))
    //                    .addField("Ay", Float.parseFloat(textarr[5]))
    //                    .addField("Az", Float.parseFloat(textarr[6]))
    //                    .addField("Wx", Float.parseFloat(textarr[7]))
    //                    .addField("Wy", Float.parseFloat(textarr[8]))
    //                    .addField("Wz", Float.parseFloat(textarr[9]))
    //                    .build();
    //            batchPoints.point(tmpPoint);
    //            influxDB.write(batchPoints);
    //            System.out.println("============数 据 添 加 成 功==================");
    //
    //        } catch (Exception e) {
    //            e.printStackTrace();
    //        }
    //    }
}
