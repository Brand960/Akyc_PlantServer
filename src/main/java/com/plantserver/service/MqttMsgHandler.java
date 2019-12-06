package com.plantserver.service;

import com.plantserver.entity.SaferconPayload;
import com.plantserver.util.PointUtil;
import com.plantserver.util.RedisUtil;
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

    @Override
    public void handleMessage(Message<?> message) {
        byte[] byteArr = (byte[]) message.getPayload();

        // 这里对数据进行处理
        SaferconPayload payload;
        try {
            payload = new SaferconPayload(byteArr);
        } catch (Exception NullPointerException) {
            log.error("错误数据");
            return;
        }
        if (payload.getWorkMode().equals("realTime")) {
            realTimeMode(payload);
        } else if (payload.getWorkMode().equals("perHour")) {
            perHourMode(payload);
        }
    }


    /**
     * 用于处理实时数据传输
     *
     * @param payload 自定义工具类
     */
    private void realTimeMode(SaferconPayload payload) {
        BatchPoints batchPoints = BatchPoints.database(database1).build();
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<Object> data = payload.getObjectList();
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
                    map.put(uid + "_shake", ((MPU6500) element).getTimestamp()
                            + "," + ((MPU6500) element).getAx() + ","
                            + ((MPU6500) element).getAy() + "," + ((MPU6500) element).getAz() + ","
                            + ((MPU6500) element).getPx() + "," + ((MPU6500) element).getPy() + ","
                            + ((MPU6500) element).getPz());
                    map.put(uid + "_temperature", ((MPU6500) element).getTimestamp()
                            + "," + ((MPU6500) element).getTemperature());
                    break;
                }
                case "power": {
                    assert element instanceof VAPE;
                    Point powerPoint = pointUtil.powerPoint(uid, (VAPE) element);
                    batchPoints.point(powerPoint);

                    map.put(uid + "_power", ((VAPE) element).getTimestamp()
                            + "," + ((VAPE) element).getV() + ","
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
            // 实时数据需要覆盖存入redis，供画图，sensor_uid:<timestamp,"ax,ay,ax,wx,wy,wz,temp">
            redisUtil.hmset("sensor_" + payload.getUid(), map);
            // 更新工作状态为0(实时数据传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 0);
            log.info("[InfluxDB&Redis]Uid:"+payload.getUid()+" influx point and redis write success\n");
        } catch (Exception e) {
            log.error("[InfluxDB&Redis]Uid:"+payload.getUid()+" Influx point and redis write fail\nErrorMessage:" + e.getMessage());
        }
    }

    /**
     * 用于处理每小时高频测试数据
     *
     * @param payload 自定义工具类
     */
    private void perHourMode(SaferconPayload payload) {
        BatchPoints batchPoints = BatchPoints.database(database2).build();
        ArrayList<Object> data = payload.getObjectList();
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
        }
        try {
            influxDB.write(batchPoints);
            // 更新工作状态为1(测试数据传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 1);
            log.info("[InfluxDB&Redis]Influx point and redis write success");
        } catch (Exception e) {
            log.error("[InfluxDB&Redis]Influx point and redis write fail\nErrorMessage:" + e.getMessage());
        }
    }
}