package com.plantserver.task;

import com.plantserver.config.MqttGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

@Component
public class MqttProduceTask {
    @Resource
    private MqttGateway mqttGateway;
    @Value("${spring.mqtt.default.pubTopic}")
    private String pubTopic;

    private static final Logger log = LoggerFactory.getLogger(MqttProduceTask.class);

    //        byte[] sendData = parserUtil
//                .toStringHex(//"\t80 00 00 00    \t\t\t00 \t\t\t\t  00\t\t    18  \t\t\t01"
//                        //+ "00 00 01 6e cf 02 b7 20 \t\t       80 00 7F FF 80 00 7F FF 80 00 7F FF\t\t  80 00\t\t\t C0 00");
//                        "\t80 00 00 00    \t\t\t10 \t\t\t\t  00\t\t    18  \t\t\t01"
//                                + "00 00 01 6e cf 02 b7 20 \t\t       3B C2 21 C2 3B C2 21 C2 3B C2 21 C2 3B C2 21 C2");
//

    @Scheduled(cron = "1/1 * * * * ?")
    public void saferconShakeSender1() throws InterruptedException {
        int x;
        // 每次生成三条
        ByteBuffer bytePayload = ByteBuffer.allocate(32);
        bytePayload.putInt(-2147483648);  // uid 4B
        bytePayload.putShort((short) 0);  // flag 2B    0实时振动 1000实时功率 4000测试振动 5000测试功率
        bytePayload.put((byte) 24); // packagesize 24
        bytePayload.put((byte) 1); // num 3条数据

        for (int i = 0; i < 1; i++) {
            Thread.sleep(200);
            Random random = new Random(new Date().getTime());
            // System.currentTimeMillis()
            bytePayload.putLong(new Date().getTime());
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //ax
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //ay
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //az
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //px
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //py
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //pz
            x = random.nextFloat() > 0.5 ? -1 : 1;
            bytePayload.putShort((short) (random.nextFloat() * 32767 * x));  //temp
            bytePayload.putShort((short) -24576);  //user1 对齐保留位
            // [0,1]加速度区间标识 0：+-1g 4000：+-2g 8000：+-3g -24576：+-4g
        }

        try {
            mqttGateway.sendToMqtt(bytePayload.array(), pubTopic);
        } catch (Exception e) {
            log.error("[Mqtt Sender]send test message from localhost fail");
        }
    }

    @Scheduled(cron="0/1 * * * * ?")
    public void saferconPowerSender1() throws InterruptedException {
        int x;
        // 每次生成三条
        ByteBuffer bytePayload = ByteBuffer.allocate(32);
        bytePayload.putInt(-2147483648);  // uid 4B
        bytePayload.putShort((short) 4096);  // flag 2B 0实时振动 4096实时功率 16384测试振动 20480测试功率
        bytePayload.put((byte) 24); // packagesize 1B 24字节/条
        bytePayload.put((byte) 1); // num 1B 3条数据

        for (int i = 0; i < 1; i++) {
            Thread.sleep(200);
            Random random = new Random(new Date().getTime());
            // System.currentTimeMillis()
            bytePayload.putLong(new Date().getTime());
            x = random.nextFloat() > 0.5 ? -1 : 1;
            float v = random.nextFloat() * 1000f * x;
            bytePayload.putFloat(v);  //v
            x = random.nextFloat() > 0.5 ? -1 : 1;
            float a = random.nextFloat() * 1000f * x;
            bytePayload.putFloat(a);  //a
            x = random.nextFloat() > 0.5 ? -1 : 1;
            float p = random.nextFloat() * 1000f * x;
            bytePayload.putFloat(p);  //p
            x = random.nextFloat() > 0.5 ? -1 : 1;
            float e = random.nextFloat() * 1000f * x;
            bytePayload.putFloat(e);  //e
        }

        try {
            mqttGateway.sendToMqtt(bytePayload.array(), pubTopic);
        } catch (Exception e) {
            log.error("[Mqtt Sender]send test message from localhost fail");
        }
    }

    private byte[] reverseByteArr(byte[] data) {
        ArrayList<Byte> al = new ArrayList<Byte>();
        for (int i = data.length - 1; i >= 0; i--) {
            al.add(data[i]);
        }

        byte[] buffer = new byte[al.size()];
        for (int i = 0; i <= buffer.length - 1; i++) {
            buffer[i] = al.get(i);
        }
        return buffer;
    }
}
