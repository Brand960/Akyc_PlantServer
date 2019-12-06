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
    private int size, num;

    @Getter
    private ArrayList<Object> objectList = new ArrayList<>();

    public SaferconPayload(byte[] input) throws NullPointerException {
        uid = (int) bytePayload.parserUtil.shiftBytes(input, 0, "int");

        workMode = (input[4] >> 6) & 0x03;
        dataMode = (input[4] >> 4) & 0x03;

        size = input[6] & 0xff;
        num = input[7] & 0xff;

        log.info("[Payload Header]Receive byte[] from " + this.uid +
                " which work mode is " + WORKMAP.get(workMode) +
                ", per data size is " + size +
                " and total num is " + num);

        // 数据部分校验完整性
        if (num == (input.length - 8) / size) {
            log.info("[Payload Header]Payload data integrity check success:" + size + "/" + num);
        } else {
            log.error("[Payload Header]Payload data integrity check fail" + size + "/" + num);
            throw new NullPointerException();
        }

        int offset = 0;
        // count前八位表明数据域格式,调用不同的解码
        try {
            switch (dataMode) {
                // 振动温度模式 Flag[2,3] 00
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
                // 功率模式 Flag[2,3] 01
                case 1: {
                    for (int i = 0; i < num; i++) {
                        byte[] tmp = new byte[size];
                        System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                        VAPE vape = new VAPE(tmp);
                        objectList.add(vape);
                        offset++;
                    }
                }
            }
            log.debug("============objectList============");
            for (Object element : objectList) {
                switch (DATAMAP.get(dataMode)) {
                    case "shake": {
                        assert element instanceof MPU6500;
                        log.debug("\nTs:" + ((MPU6500) element).getTimestamp() +
                                "\nAx:" + ((MPU6500) element).getAx() +
                                "\nAy:" + ((MPU6500) element).getAy() +
                                "\nAz:" + ((MPU6500) element).getAz() +
                                "\nPx:" + ((MPU6500) element).getPx() +
                                "\nPy:" + ((MPU6500) element).getPy() +
                                "\nPz:" + ((MPU6500) element).getPz() +
                                "\nTemp:" + ((MPU6500) element).getTemperature());
                        break;
                    }
                    case "power": {
                        assert element instanceof VAPE;
                        log.debug("\nTs:" + ((VAPE) element).getTimestamp() +
                                "\nV:" + ((VAPE) element).getV() +
                                "\nA:" + ((VAPE) element).getA() +
                                "\nP:" + ((VAPE) element).getP() +
                                "\nE:" + ((VAPE) element).getE());
                        break;
                    }
                }
                log.debug("============objectList============");
            }
        } catch (Exception e) {
            log.error("[Payload Data]Parse byte[] from " + this.uid +
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

