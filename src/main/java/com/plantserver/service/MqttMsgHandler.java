package com.plantserver.service;


import com.plantserver.Util.AkycByteMsg;
import com.plantserver.Util.InfluxUtil;
import com.plantserver.Util.RedisUtil;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component("messageHandler")
public class MqttMsgHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMsgHandler.class);

    @Value("${spring.influx.database1}")
    private String database1;

    @Value("${spring.influx.database2}")
    private String database2;

    @Resource
    private InfluxDB influxDB;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private InfluxUtil influxUtil;

//    private static MqttMsgHandler msgHandler;
//
//    @PostConstruct //通过@PostConstruct实现初始化bean之前进行的操作
//    public void init() {
//        msgHandler = this;
//    }

    @Override
    public void handleMessage(Message<?> message) {
        byte[] byteArr = (byte[]) message.getPayload();

        // 这里对数据进行处理
        AkycByteMsg msg;
        try {
            msg = new AkycByteMsg(byteArr);
        } catch (Exception NullPointerException) {
            log.error("错误数据校验数据部分未通过");
            return;
        }
        if (msg.getFlag() == 1) {
            workMode1(msg);
        } else {
            workMode2(msg);
        }
    }

    /**
     * 用于处理实时数据传输
     *
     * @param msg 自定义工具类
     */
    private void workMode1(AkycByteMsg msg) {
        BatchPoints batchPoints = BatchPoints.database(database1).build();
        HashMap<String, Object> map = new HashMap<>();
        HashMap<Long, Object[]> data = msg.getData();
        for (Map.Entry<Long, Object[]> entry : data.entrySet()) {
            String uid = String.valueOf(msg.getUid());
            long ts = entry.getKey();
            Object[] entryData = entry.getValue();

            Point tmpPointShake = influxUtil.shakePoint(uid, ts, entryData);
            Point tmpPointTemp = influxUtil.tempPoint(uid, ts, entryData);

            batchPoints.point(tmpPointShake);
            batchPoints.point(tmpPointTemp);

            map.put(String.valueOf(ts), entryData[0] + ","
                    + entryData[1] + "," + entryData[2] + ","
                    + entryData[3] + "," + entryData[4] + ","
                    + entryData[5] + "," + entryData[6]);
        }
        try {
            influxDB.write(batchPoints);
        } catch (Exception e) {
            log.error("=====存入数据库失败！=====\n错误信息" + e.getMessage());
        }

        try {
            // 实时数据需要覆盖存入redis，供画图，sensor_uid:<timestamp,"ax,ay,ax,wx,wy,wz,temp">
            redisUtil.hmset("sensor_" + msg.getUid(), map);
            // 更新工作状态为1(测试数据传输模式)
            redisUtil.set("sensor_" + msg.getUid() + "_flag", "1");
        } catch (Exception e) {
            log.error("=====存入redis失败！=====\n错误信息" + e.getMessage());
        }
    }

    /**
     * 用于处理每小时高频测试数据
     *
     * @param msg 自定义工具类
     */
    private void workMode2(AkycByteMsg msg) {
        BatchPoints batchPoints = BatchPoints.database(database2).build();
        HashMap<Long, Object[]> data = msg.getData();
        for (Map.Entry<Long, Object[]> entry : data.entrySet()) {
            long ts = entry.getKey();
            Object[] entryData = entry.getValue();
            String uid = String.valueOf(msg.getUid());

            Point tmpPointShake = influxUtil.shakePoint(uid, ts, entryData);
            Point tmpPointTemp = influxUtil.tempPoint(uid, ts, entryData);

            batchPoints.point(tmpPointShake);
            batchPoints.point(tmpPointTemp);
        }
        try {
            influxDB.write(batchPoints);
        } catch (Exception e) {
            log.error("=====存入数据库失败！=====\n错误信息" + e.getMessage());
        }

        // 更新工作状态为2(测试数据传输模式)
        redisUtil.set("sensor_" + msg.getUid() + "_flag", "2");
    }
}
