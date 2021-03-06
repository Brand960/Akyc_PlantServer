package com.plantserver.service;

import com.plantserver.entity.SaferconPayload;
import com.plantserver.entity.TTTT;
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
import java.util.Date;
import java.util.HashMap;

@Component("messageHandler")
public class MqttMsgHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(MqttMsgHandler.class);

    @Value("${spring.influx.database1}")
    private String database1;

    @Value("${spring.influx.database2}")
    private String database2;

    @Value("${spring.redis.expireTime}")
    private int expireTime;

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
            log.info("[" + payload.getUid() + "] Receive payload" +
                    " mode:" + payload.getWorkMode() +"/" + payload.getDataMode() +
                    " & size:" + payload.getSize() + " /num:" + payload.getNum());
        } catch (NullPointerException e) {
            log.error("[SaferconPayload]数据解析错误");
            return;
        } catch (Exception e) {
            log.error("[SaferconPayload]未知数据解析错误\nError msg:" + e.getMessage());
            return;
        }

        // add1231 若 [4,5]是11111111 11111111则信息为SCOK不用处理数据
        if (byteArr[4] == -1 && byteArr[5] == -1) {
            try {
                // 更新SCOK状态为1
                redisUtil.set("sensor_" + payload.getUid() + "_SCOK", 1, expireTime);
                log.info("[" + payload.getUid() + "]SCOK received\n");
            } catch (Exception e) {
                log.error("[" + payload.getUid() + "]SCOK received, but redis write fail\nErrorMessage:" + e.getMessage());
            }
            // add0102 若 [5]与1100 0000即高两位是10 则信息为心跳状态包
        } else if (((byteArr[5] & 0xc0) >> 6) == 2) {
            heartBeat(payload);
        } else if (payload.getWorkMode().equals("realTime")) {
            realTimeMode(payload);
        } else if (payload.getWorkMode().equals("perHour")) {
            perHourMode(payload);
        } else if (payload.getWorkMode().equals("standBy")) {
            standByMode(payload);
        } else if (payload.getWorkMode().equals("calculating")) {
            calculating(payload);
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
        long time=0;
        for (Object element : data) {
            // 每个数据包不变的部分uid timestamp data数组
            String uid = String.valueOf(payload.getUid());
            switch (payload.getDataMode()) {
                case "shake": {
                    assert element instanceof MPU6500;
                    Point shakePoint = pointUtil.shakePoint(uid, (MPU6500) element);
                    batchPoints.point(shakePoint);
                    time=((MPU6500) element).getTimestamp();
                    // 下划线后缀区分同sn的shake和power属性
                    map.put("sensor_" + uid + "_shake", ((MPU6500) element).getTimestamp()
                            + "," + ((MPU6500) element).getAx() + ","
                            + ((MPU6500) element).getAy() + ","
                            + ((MPU6500) element).getAz() + ","
                            + ((MPU6500) element).getPx() + ","
                            + ((MPU6500) element).getPy() + ","
                            + ((MPU6500) element).getPz() + ","
                            + ((MPU6500) element).getTemperature());

                    log.debug("[" + uid + "_shake]" + ((MPU6500) element).getTimestamp()
                            + "," + ((MPU6500) element).getAx() + ","
                            + ((MPU6500) element).getAy() + ","
                            + ((MPU6500) element).getAz() + ","
                            + ((MPU6500) element).getPx() + ","
                            + ((MPU6500) element).getPy() + ","
                            + ((MPU6500) element).getPz() + ","
                            + ((MPU6500) element).getTemperature());
                    break;
                }
                case "power": {
                    assert element instanceof VAPE;
                    Point powerPoint = pointUtil.powerPoint(uid, (VAPE) element);
                    batchPoints.point(powerPoint);
                    time=((VAPE) element).getTimestamp();
                    map.put("sensor_" + uid + "_power", ((VAPE) element).getTimestamp()
                            + "," + ((VAPE) element).getV() + ","
                            + ((VAPE) element).getA() + ","
                            + ((VAPE) element).getP() + ","
                            + ((VAPE) element).getE());
                    log.debug("[" + uid + "_power]" + ((VAPE) element).getTimestamp()
                            + "," + ((VAPE) element).getV() + ","
                            + ((VAPE) element).getA() + ","
                            + ((VAPE) element).getP() + ","
                            + ((VAPE) element).getE());
                    break;
                }
                // 1219添加温度4处理
                case "temperature": {
                    assert element instanceof TTTT;
                    Point powerPoint = pointUtil.tempPoint(uid, (TTTT) element);
                    batchPoints.point(powerPoint);
                    time=((TTTT) element).getTimestamp();
                    map.put("sensor_" + uid + "_temperature4", ((TTTT) element).getTimestamp()
                            + "," + ((TTTT) element).getT1() + ","
                            + ((TTTT) element).getT2() + ","
                            + ((TTTT) element).getT3() + ","
                            + ((TTTT) element).getT4());
                    log.debug("[" + uid + "_temperature4]" + ((TTTT) element).getTimestamp()
                            + "," + ((TTTT) element).getT1() + ","
                            + ((TTTT) element).getT2() + ","
                            + ((TTTT) element).getT3() + ","
                            + ((TTTT) element).getT4());
                    break;
                }
                default: {
                    log.error("[" + uid + "]Influx point create from data fail");
                    return;
                }
            }
        }

        try {
            influxDB.write(batchPoints);
            // 实时数据需要覆盖存入redis，供画图，sensor_uid:<timestamp,"ax,ay,ax,wx,wy,wz,temperature">
            redisUtil.hmset("sensor_" + payload.getUid(), map,expireTime);
            // 更新工作状态为1(实时数据传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 1,expireTime);
            log.info("[" + payload.getUid() + "] "+time+" write realTime success\n");
        } catch (Exception e) {
            log.error("[" + payload.getUid() + "] Influx point and redis write realTime fail\nErrorMessage:" + e.getMessage());
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
        log.debug("============objectList============");
        for (Object element : data) {
            // 每个数据包不变的部分uid timestamp data数组
            String uid = String.valueOf(payload.getUid());
            switch (payload.getDataMode()) {
                case "shake": {
                    assert element instanceof MPU6500;
                    Point shakePoint = pointUtil.shakePoint(uid, (MPU6500) element);
                    batchPoints.point(shakePoint);
                    break;
                }
                case "power": {
                    assert element instanceof VAPE;
                    Point powerPoint = pointUtil.powerPoint(uid, (VAPE) element);
                    batchPoints.point(powerPoint);
                    break;
                }
                // 1219添加温度4处理
                case "temperature": {
                    assert element instanceof TTTT;
                    Point tmp4Point = pointUtil.tempPoint(uid, (TTTT) element);
                    batchPoints.point(tmp4Point);
                    break;
                }
                default: {
                    log.error("[" + payload.getUid() + "] Influx point create from data fail");
                    return;
                }
            }
        }
        log.info("[" + payload.getUid() + "_" + payload.getDataMode() + " perHour]" + new Date().getTime() + "complete parse, " +
                "object num: " + data.size());

        try {
            influxDB.write(batchPoints);
            // 更新工作状态为0(测试数据每小时传输模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 0,expireTime);
            log.info("[" + payload.getUid() + "] Influx point and redis write perHour success\n");
        } catch (Exception e) {
            log.error("[" + payload.getUid() + "] Influx point and redis write perHour fail\nErrorMessage:" + e.getMessage());
        }
    }

    /**
     * 缓存写入传感器待机
     *
     * @param payload 自定义工具类
     */
    private void standByMode(SaferconPayload payload) {
        log.debug("**************************");
        log.info("[" + payload.getUid() + "_" + payload.getWorkMode() + "]" + new Date().getTime() + "complete parse, " +
                "sensor stand by");
        try {
            // 更新工作状态为2(待机模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 2,expireTime);
            log.info("[" + payload.getUid() + "] Redis write standBy success\n");
        } catch (Exception e) {
            log.error("[" + payload.getUid() + "] Redis write standBy fail\nErrorMessage:" + e.getMessage());
        }
    }

    /**
     * 缓存写入传感器计算中
     *
     * @param payload 自定义工具类
     */
    private void calculating(SaferconPayload payload) {
        log.debug("**************************");
        log.info("[" + payload.getUid() + "_" + payload.getWorkMode() + "]" + new Date().getTime() + "complete parse, " +
                "sensor calculating");
        try {
            // 更新工作状态为3(计算模式)
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", 3,expireTime);
            log.info("[" + payload.getUid() + "] Redis write calculating success\n");
        } catch (Exception e) {
            log.error("[" + payload.getUid() + "] Redis write calculating fail\nErrorMessage:" + e.getMessage());
        }
    }

    /**
     * 缓存更新工作状态根据心跳包
     *
     * @param payload 自定义工具类
     */
    private void heartBeat(SaferconPayload payload) {
        log.debug("**************************");
        log.info("[" + payload.getUid() + "_heartbeat]" + "complete parse, " +
                "sensor workMode in redis updating");
        int work_mode = 2;
        try {
            switch (payload.getWorkMode()) {
                case "realTime":
                    work_mode = 1;
                    break;
                case "perHour":
                    work_mode = 0;
                    break;
                case "standBy":
                    work_mode = 2;
                    break;
                case "calculating":
                    work_mode = 3;
                    break;
                case "message":
                    work_mode = 4;
                    break;
            }
            // 更新心跳包数据工作状态 expired after 120s
            redisUtil.set("sensor_" + payload.getUid() + "_workMode", work_mode, expireTime);
            log.info("[" + payload.getUid() + "] Redis updated by heartbeat data success\n");
        } catch (Exception e) {
            log.error("[" + payload.getUid() + "] Redis updated by heartbeat data fail\nErrorMessage:" + e.getMessage());
        }
    }
}