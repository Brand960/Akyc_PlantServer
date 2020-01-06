package com.plantserver.entity;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SaferconPayload extends BytePayload {
    private static final Logger log = LoggerFactory.getLogger(SaferconPayload.class);

    // Uid of the current equipment 4B [0,1,2,3]
    @Getter
    private int uid;

    // Flag of the current payload 2B [4,5] Little_End低位LSB在左
    // 2b 100实验 010实时 110待机 001计算中 101消息(休眠)[4]
    private int workMode;
    // 2b 100振动温度 010四温度 110功率 [4]
    private int dataMode;

    // Sizeof(using package struct) 2B [6,7]
    // The number of the following data packages, max quantity is 60
    // size 每条数据长度(B) num 共有几条数据
    @Getter
    private int size, num;

    @Getter
    private ArrayList<Object> objectList = new ArrayList<>();

    public SaferconPayload(byte[] input) throws NullPointerException {
        uid = (int) bytePayload.parserUtil.jvmBytes(input, 0, "int");
        // 与0000 0111避开高位(心跳)
        workMode = input[5] &0x07;
        dataMode = input[4] &0x07;

        size = input[6];
        num = input[7];

        // 数据部分校验完整性
        if (num == (input.length - 8) / size) {
            log.debug("["+this.uid+"_HeaderDecoder]Integrity check success. size:" + size + "/num:" + num);
        } else {
            log.error("[Payload HeaderDecoder]Payload data integrity check fail. size:" + size + "/num:" + num);
            throw new NullPointerException();
        }

        int offset = 0;
        try {
            // flag[2,3]表数据类型,调用不同的解码
            switch (dataMode) {
                // 振动温度 01
                case 1: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        MPU6500 mpu6500 = new MPU6500(tmp);
                        objectList.add(mpu6500);
                        offset++;
                    }
                    break;
                }
                // 1219温度*4 10
                case 2: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        TTTT tttt = new TTTT(tmp);
                        objectList.add(tttt);
                        offset++;
                    }
                }
                // 功率 11
                case 3: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        VAPE vape = new VAPE(tmp);
                        objectList.add(vape);
                        offset++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("["+this.uid+"_DataDecoder] work mode is " + WORKMAP.get(workMode) +
                    ", data mode is "+ DATAMAP.get(dataMode) +
                    " data parse fail\nError Message: " + e.getMessage() + "\n");
        }
    }

    public String getWorkMode() {
        return WORKMAP.get(workMode);
    }

    public String getDataMode() {
        return DATAMAP.get(dataMode);
    }
}

