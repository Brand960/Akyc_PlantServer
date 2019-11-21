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
        //System.out.println("【*** 接收消息    ***】" + message.getPayload());
        //        long res = (long) msgHandler.parserUtils.shiftBytes(byteArr, 0, "long");
        //        int res1 = (int) msgHandler.parserUtils.shiftBytes(byteArr, 8, "int");
        //        short res2 = (short) msgHandler.parserUtils.shiftBytes(byteArr, 12, "short");
        //
        //        long res3 = (long) msgHandler.parserUtils.jvmBytes(byteArr, 0, "long");
        //        int res4 = (int) msgHandler.parserUtils.jvmBytes(byteArr, 8, "int");
        //        short res5 = (short) msgHandler.parserUtils.jvmBytes(byteArr, 12, "short");
        //        byte[] tmp = new byte[4];
        //        System.arraycopy(byteArr, 28, tmp, 0, 4);
        //        String res6 = new String(tmp);
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
//        int i = (byteArr.length - 8) / 24, offset = 8, temp;
//        short ax, ay, az, wx, wy, wz;
//        long ts;
        //for (int j = 0; j < i; j++) {
        // 时间戳8B振动数据2B*6温度4B，共24B一条
//            ts = msg.getTSInLine(byteArr, offset);
//            ax = msg.getShakeInLine(byteArr, offset + 8);
//            ay = msg.getShakeInLine(byteArr, offset + 10);
//            az = msg.getShakeInLine(byteArr, offset + 12);
//            wx = msg.getShakeInLine(byteArr, offset + 14);
//            wy = msg.getShakeInLine(byteArr, offset + 16);
//            wz = msg.getShakeInLine(byteArr, offset + 18);
//            temp = msg.getTempInLine(byteArr, offset + 20);
        // 实时数据拼接
        BatchPoints batchPoints = BatchPoints.database(database1).build();
        HashMap<String, Object> map = new HashMap<>();
        HashMap<Long, short[]> data = msg.getData();
        for (Map.Entry<Long, short[]> entry : data.entrySet()) {
            long ts = entry.getKey();
            short[] entryData = entry.getValue();
            map.put(String.valueOf(entry.getKey()), entryData[0] + ","
                    + entryData[1] + "," + entryData[2] + ","
                    + entryData[3] + "," + entryData[4] + ","
                    + entryData[5] + "," + entryData[6]);
            Point tmpPoint = Point.measurement(shakeMeasurment)
                    .addField("count", msg.getCount())
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Ax", entryData[0])
                    .addField("Ay", entryData[1])
                    .addField("Az", entryData[2])
                    .addField("Wx", entryData[3])
                    .addField("Wy", entryData[4])
                    .addField("Wz", entryData[5])
                    .addField("Temp", entryData[6])
                    .build();
            batchPoints.point(tmpPoint);
            // 偏移量指向增加
            //offset += 24;
        }
        influxDB.write(batchPoints);

        // 实时数据需要覆盖存入redis，供画图
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
//        int i = (byteArr.length - 8) / 24, offset = 8, temp;
//        short ax, ay, az, wx, wy, wz;
//        long ts;
//        for (int j = 0; j < i; j++) {
//            ts = msg.getTSInLine(byteArr, offset);
//            ax = msg.getShakeInLine(byteArr, offset + 8);
//            ay = msg.getShakeInLine(byteArr, offset + 10);
//            az = msg.getShakeInLine(byteArr, offset + 12);
//            wx = msg.getShakeInLine(byteArr, offset + 14);
//            wy = msg.getShakeInLine(byteArr, offset + 16);
//            wz = msg.getShakeInLine(byteArr, offset + 18);
//            temp = msg.getTempInLine(byteArr, offset + 20);
        BatchPoints batchPoints = BatchPoints.database(database2).build();
        HashMap<Long, short[]> data = msg.getData();
        for (Map.Entry<Long, short[]> entry : data.entrySet()) {
            long ts = entry.getKey();
            short[] entryData = entry.getValue();
            Point tmpPoint = Point.measurement(shakeMeasurment)
                    .addField("count", msg.getCount())
                    .tag("sn", String.valueOf(msg.getUid()))
                    .time(ts, TimeUnit.MILLISECONDS)
                    .addField("Ax", entryData[0])
                    .addField("Ay", entryData[1])
                    .addField("Az", entryData[2])
                    .addField("Wx", entryData[3])
                    .addField("Wy", entryData[4])
                    .addField("Wz", entryData[5])
                    .addField("Temp", entryData[6])
                    .build();
            batchPoints.point(tmpPoint);
//            offset += 24;
        }
        influxDB.write(batchPoints);

        // 更新工作状态为2(测试数据传输模式)
        redisUtil.set("sensor_" + msg.getUid() + "_flag", "2");
    }
}
