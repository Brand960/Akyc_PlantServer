package com.plantserver.service;

import com.plantserver.Util.ParserUtil;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;

@Component("messageHandler")
public class MqttMsgHandler implements MessageHandler {

    private static final Logger log = Logger.getLogger(MqttMsgHandler.class);

    @Resource
    private InfluxDB influxDB;

    @Resource
    private ParserUtil parserUtil;

    private static MqttMsgHandler msgHandler;

    @PostConstruct //通过@PostConstruct实现初始化bean之前进行的操作
    public void init() {
        msgHandler = this;
    }

    @Override
    public void handleMessage(Message<?> message) {
        //System.out.println("【*** 接收消息    ***】" + message.getPayload());
        byte[] byteArr = (byte[])message.getPayload();
        System.out.println(Arrays.toString(byteArr));

        long res=(long)msgHandler.parserUtil.shiftBytes(byteArr,0,"long");
        int res1=(int)msgHandler.parserUtil.shiftBytes(byteArr,8,"int");
        short res2= (short)msgHandler.parserUtil.shiftBytes(byteArr, 12, "short") ;
        long res3= (long)msgHandler.parserUtil.jvmBytes(byteArr, 0, "long") ;
        int res4= (int)msgHandler.parserUtil.jvmBytes(byteArr, 8, "int") ;
        short res5= (short)msgHandler.parserUtil.jvmBytes(byteArr, 12, "short") ;
        byte[] tmp = new byte[4];
        System.arraycopy(byteArr, 28, tmp, 0, 4);
        String res6 = new String(tmp);
        System.out.println(res);
//        try {
//            BatchPoints batchPoints = BatchPoints.database("plantsurv_web").build();
//            Point tmpPoint = Point.measurement("pt_new_" + textarr[1])
//                    .time(Long.parseLong(textarr[2]), TimeUnit.MILLISECONDS)
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
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            String now = sdf.format(new Date(Long.parseLong(String.valueOf(textarr[2]))));
//            System.out.println("============" + now + "数 据 添 加 成 功" + textarr[2] + "==================");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // 这里对数据进行处理
    }
}
