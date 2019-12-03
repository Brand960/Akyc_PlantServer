package com.plantserver.service;

import com.plantserver.entity.AKYCPayload;
import com.plantserver.Util.PointUtil;
import com.plantserver.Util.RedisUtil;
import com.plantserver.entity.MPU6500;
import com.plantserver.entity.VAPE;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

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
    private PointUtil pointUtil;

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
        AKYCPayload payload;
        try {
            payload = new AKYCPayload(byteArr);
        } catch (Exception NullPointerException) {
            log.error("错误数据校验数据部分未通过");
            return;
        }
        if (payload.getWorkMode().equals("realTime")) {
            realTimeMode(payload);
        } else {
            perHourMode(payload);
        }
    }


    /**
     * 用于处理实时数据传输
     *
     * @param payload 自定义工具类
     */
    private void realTimeMode(AKYCPayload payload) {
        BatchPoints batchPoints = BatchPoints.database(database1).build();
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<Object> data = payload.getData();
        for (Object element : data) {
            // 每个数据包不变的部分uid timestamp data数组
            String uid = String.valueOf(payload.getUid());
            switch (payload.getDataMode()) {
                case "shake": {
                    assert element instanceof MPU6500;
                    Point tmpPoint = pointUtil.tempPoint(uid, (MPU6500) element);
                    Point shakePoint = pointUtil.shakePoint(uid, (MPU6500) element);
                    batchPoints.point(tmpPoint);
                    batchPoints.point(shakePoint);
                    //todo 下划线后缀区分同sn的shake和power属性
                    map.put(((MPU6500) element).getTimestamp() + "_shake", ((MPU6500) element).getAx() + ","
                            + ((MPU6500) element).getAy() + "," + ((MPU6500) element).getAz() + ","
                            + ((MPU6500) element).getPx() + "," + ((MPU6500) element).getPy() + ","
                            + ((MPU6500) element).getPz() + "," + ((MPU6500) element).getTemperature());
                    break;
                }
                case "power": {
                    assert element instanceof VAPE;
                    Point powerPoint = pointUtil.powerPoint(uid, (VAPE) element);
                    batchPoints.point(powerPoint);
                    map.put(((VAPE) element).getTimestamp() + "_power", ((VAPE) element).getV() + ","
                            + ((VAPE) element).getA() + "," + ((VAPE) element).getP() + ","
                            + ((VAPE) element).getE());
                    break;
                }
                default: {
                    log.error("[Payload Handler]Influx point create from data fail");
                    return;
                }
            }


        }
        try {
            influxDB.write(batchPoints);
        } catch (Exception e) {
            log.error("=====存入数据库失败！=====\n错误信息" + e.getMessage());
        }

        try {
            // 实时数据需要覆盖存入redis，供画图，sensor_uid:<timestamp,"ax,ay,ax,wx,wy,wz,temp">
            redisUtil.hmset("sensor_" + payload.getUid(), map);
            // 更新工作状态为0(实时数据传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_flag", "1");
        } catch (Exception e) {
            log.error("=====存入redis失败！=====\n错误信息" + e.getMessage());
        }
    }

    /**
     * 用于处理每小时高频测试数据
     *
     * @param payload 自定义工具类
     */
    private void perHourMode(AKYCPayload payload) {
        BatchPoints batchPoints = BatchPoints.database(database2).build();
        ArrayList<Object> data = payload.getData();
        for (Object element : data) {
            // 每个数据包不变的部分uid timestamp data数组
            String uid = String.valueOf(payload.getUid());
            switch (payload.getDataMode()) {
                case "shake": {
                    assert element instanceof MPU6500;
                    Point tmpPoint = pointUtil.tempPoint(uid, (MPU6500) element);
                    Point shakePoint = pointUtil.shakePoint(uid, (MPU6500) element);
                    batchPoints.point(tmpPoint);
                    batchPoints.point(shakePoint);
                    break;
                }
                case "power": {
                    assert element instanceof VAPE;
                    Point powerPoint = pointUtil.powerPoint(uid, (VAPE) element);
                    batchPoints.point(powerPoint);
                    break;
                }
                default: {
                    log.error("[Payload Handler]Influx point create from data fail");
                    return;
                }
            }


            try {
                influxDB.write(batchPoints);
            } catch (Exception e) {
                log.error("=====存入数据库失败！=====\n错误信息" + e.getMessage());
            }

            // 更新工作状态为1(测试数据传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_flag", "2");
        }
    }
}