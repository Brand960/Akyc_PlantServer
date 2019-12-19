package com.plantserver.entity;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SaferconPayload extends BytePayload {
    private static final Logger log = LoggerFactory.getLogger(SaferconPayload.class);

    //Uid of the current equipment 4B [0,1,2,3]
    @Getter
    private int uid;

    //Flag of the current payload 2B [4,5]
    // todo flag 12b reserved for future use
    // 2b 00实时 01计算数据 [4]
    private int workMode;
    // 2b 00振动温度 01功率 [4]
    private int dataMode;

    //sizeof(using package struct) 2B [6,7]
    //the numbers of the following data packages, Max is 60
    // size 每条数据长度(B) num 共有几条数据
    @Getter
    private int size, num;

    @Getter
    private ArrayList<Object> objectList = new ArrayList<>();

    public SaferconPayload(byte[] input) throws NullPointerException {
        uid = (int) bytePayload.parserUtil.shiftBytes(input, 0, "int");

        workMode = (input[4] >> 6) & 0x03;
        dataMode = (input[4] >> 4) & 0x03;

        size = input[6] & 0xff;
        num = input[7] & 0xff;

        // 数据部分校验完整性
        if (num == (input.length - 8) / size) {
            log.info("[Payload HeaderDecoder]Payload data integrity check success. size:" + size + "/num:" + num);
        } else {
            log.error("[Payload HeaderDecoder]Payload data integrity check fail. size:" + size + "/num:" + num);
            throw new NullPointerException();
        }

        int offset = 0;

        try {
            // flag[2,3]表数据类型,调用不同的解码
            switch (dataMode) {
                // 振动温度 Flag[2,3] 00
                case 0: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        MPU6500 mpu6500 = new MPU6500(tmp);
                        objectList.add(mpu6500);
                        offset++;
                    }
                    break;
                }
                // 功率 Flag[2,3] 01
                case 1: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        VAPE vape = new VAPE(tmp);
                        objectList.add(vape);
                        offset++;
                    }
                }
                // 1219温度温度温度温度
                case 2: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        TTTT tttt = new TTTT(tmp);
                        objectList.add(tttt);
                        offset++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Payload DataDecoder]Parse byte[] from " + this.uid +
                    " which work mode is " + WORKMAP.get(workMode) +
                    " fail\nError Message: " + e.getMessage());
        }
    }

    public String getWorkMode() {
        return WORKMAP.get(workMode);
    }

    public String getDataMode() {
        return DATAMAP.get(dataMode);
    }
}

