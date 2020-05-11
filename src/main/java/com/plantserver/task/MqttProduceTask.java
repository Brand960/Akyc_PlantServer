package com.plantserver.task;

import com.plantserver.config.MqttGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
    @Value("${spring.profiles.active}")
    private String profile;


    private static final Logger log = LoggerFactory.getLogger(MqttProduceTask.class);

    @Scheduled(cron = "0/1 * * * * ?")
    public void realTimeShakeSender() {
        Random random = new Random();
        System.out.print("\033[0;34m0/5 * * * * ?实时振动" + "\033[0m");
        int x;
        ByteBuffer bytePayload = ByteBuffer.allocate(248);
        bytePayload.put((byte) 99); // uid
        bytePayload.put((byte) 0); // uid
        bytePayload.put((byte) 0); // uid
        bytePayload.put((byte) 0); // uid
        bytePayload.put((byte) 1); // shake
        bytePayload.put((byte) 2); // realTime
        bytePayload.put((byte) 24); // packagesize 24
        bytePayload.put((byte) 10); // num 20
        long beginsec = new Date().getTime() - 1000;
        for (int i = 0; i < 10; i++) {
            bytePayload.put(toLH((int) (beginsec / 1000)));
            bytePayload.put(toLH(i + 10));
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
            bytePayload.putShort((short) (random.nextFloat() + 30));  //temp
            bytePayload.putShort((short) -24576);  //user1 对齐保留位
            // [0,1]加速度区间标识 0：+-1g 4000：+-2g 8000：+-3g -24576：+-4g
        }

        try {
            mqttGateway.sendToMqtt(bytePayload.array(), pubTopic);
        } catch (Exception e) {
            log.error("[实时振动]Send aliyun message from localhost fail\nError msg: " + e.getMessage());
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

    public static byte[] toLH(int n) {
        byte[] b = new byte[4];
        b[0] = (byte) (n & 0xff);
        b[1] = (byte) (n >> 8 & 0xff);
        b[2] = (byte) (n >> 16 & 0xff);
        b[3] = (byte) (n >> 24 & 0xff);
        return b;
    }
}
