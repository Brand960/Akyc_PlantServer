package com.plantserver.service;


import com.plantserver.Util.AkycByteMsg;
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

    @Value("${spring.influx.measurement.shake}")
    private String shakeMeasurment;

    @Value("${spring.influx.measurement.temperature}")
    private String tempMeasurment;

    @Resource
    private InfluxDB influxDB;

    @Resource
    private RedisUtil redisUtil;

//    @Resource
//    private ParserUtil parserUtil;

    private static MqttMsgHandler msgHandler;

    @PostConstruct //通过@PostConstruct实现初始化bean之前进行的操作
    public void init() {
        msgHandler = this;
    }

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
            processMode1(byteArr, msg);
        } else {
            processMode2(byteArr, msg);
        }
    }

    /**
     * 用于处理实时数据传输
     *
     * @param byteArr 要解析的原数组，不做拷贝切割
     * @param msg     自定义工具类
     */
    private void processMode1(byte[] byteArr, AkycByteMsg msg) {
        BatchPoints batchPoints = BatchPoints.database(database1).build();
        HashMap<String, Object> map = new HashMap<>();
        HashMap<Long, Object[]> data = msg.getData();
        for (Map.Entry<Long, Object[]> entry : data.entrySet()) {
            long ts = entry.getKey();
            Object[] entryData = entry.getValue();
            Point tmpPointShake = Point.measurement(shakeMeasurment)
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Ax", (short) entryData[0])
                    .addField("Ay", (short) entryData[1])
                    .addField("Az", (short) entryData[2])
                    .addField("Wx", (short) entryData[3])
                    .addField("Wy", (short) entryData[4])
                    .addField("Wz", (short) entryData[5])
                    .build();
            Point tmpPointTemp = Point.measurement(tempMeasurment)
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Temp", (int) entryData[6])
                    .build();
            batchPoints.point(tmpPointShake);
            batchPoints.point(tmpPointTemp);
            map.put(String.valueOf(ts), entryData[0] + ","
                    + entryData[1] + "," + entryData[2] + ","
                    + entryData[3] + "," + entryData[4] + ","
                    + entryData[5] + "," + entryData[6]);
        }
        influxDB.write(batchPoints);

        // 实时数据需要覆盖存入redis，供画图，sensor_uid:<timestamp,"ax,ay,ax,wx,wy,wz,temp">
        redisUtil.hmset("sensor_" + msg.getUid(), map);
        // 更新工作状态为1(测试数据传输模式)
        redisUtil.set("sensor_" + msg.getUid() + "_flag", "1");
    }

    /**
     * 用于处理每小时高频测试数据
     *
     * @param byteArr 要解析的原数组，不做拷贝切割
     * @param msg     自定义工具类
     */
    private void processMode2(byte[] byteArr, AkycByteMsg msg) {
        BatchPoints batchPoints = BatchPoints.database(database2).build();
        HashMap<Long, Object[]> data = msg.getData();
        for (Map.Entry<Long, Object[]> entry : data.entrySet()) {
            long ts = entry.getKey();
            Object[] entryData = entry.getValue();
            Point tmpPointSake = Point.measurement(shakeMeasurment)
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Ax", (short) entryData[0])
                    .addField("Ay", (short) entryData[1])
                    .addField("Az", (short) entryData[2])
                    .addField("Wx", (short) entryData[3])
                    .addField("Wy", (short) entryData[4])
                    .addField("Wz", (short) entryData[5])
                    .build();
            Point tmpPointTemp = Point.measurement(tempMeasurment)
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Temp", (int) entryData[6])
                    .build();
            batchPoints.point(tmpPointSake);
            batchPoints.point(tmpPointTemp);
        }
        influxDB.write(batchPoints);

        // 更新工作状态为2(测试数据传输模式)
        redisUtil.set("sensor_" + msg.getUid() + "_flag", "2");
    }
}
