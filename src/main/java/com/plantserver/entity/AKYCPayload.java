package com.plantserver.entity;

import com.plantserver.Util.ParserUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


public class AKYCPayload extends BytePayload {
    private static final Logger log = LoggerFactory.getLogger(AKYCPayload.class);
    @Resource
    private ParserUtil parserUtil;

    //Uid of the current equipment 4B [0,1,2,3]
    @Getter
    private int uid;

    //Flag of the current payload 2B [4,5]
    //todo flag 12b reserved for future use
    private byte[] flag = new byte[2];
    // 2b 00实时 01计算数据 [4]
    private int workMode;
    // 2b 00振动温度 01功率 [4]
    private int dataMode;

    //sizeof(using package struct) 2B [6,7]
    //the numbers of the following data packages, Max is 60
    private byte[] sizeByte = new byte[1], numByte = new byte[1];
    // size 每条数据长度(B) num 共有几条数据
    private int size, num;

    @Getter
    private ArrayList<Object> data = new ArrayList<>();

    public AKYCPayload(@NotNull byte[] input) throws NullPointerException {
        uid = (int) parserUtil.shiftBytes(input, 0, "int");

        System.arraycopy(input, 4, flag, 0, 2);
        workMode = (flag[0] >> 6) & 0x03;
        dataMode = (flag[0] >> 4) & 0x03;

        System.arraycopy(input, 6, sizeByte, 0, 1);
        size = ByteBuffer.wrap(sizeByte).order(ByteOrder.LITTLE_ENDIAN).getInt();
        System.arraycopy(input, 7, numByte, 0, 1);
        num = ByteBuffer.wrap(numByte).order(ByteOrder.LITTLE_ENDIAN).getInt();

        log.info("[Payload Header]Receive byte[] from" + this.uid +
                "which work mode is" + WORKMAP.get(workMode) +
                ", per data size is" + size +
                " and total num is" + num);

        // 数据部分校验完整性
        if (num == (input.length - 8) / size)
            log.info("[Payload Header]Payload integrity check success");
        else {
            log.error("[Payload Header]Payload integrity check fail");
            throw new NullPointerException();
        }

        int offset = 0;
        // count前八位表明数据域格式,调用不同的解码
        switch (dataMode) {
            case 0: {
                for (int i = 0; i < num; i++) {
                    byte[] tmp = new byte[size];
                    System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                    MPU6500 listItem = new MPU6500(tmp);
                    data.add(listItem);
                }
                break;
            }
            case 1: {
                for (int i = 0; i < num; i++) {
                    byte[] tmp = new byte[size];
                    System.arraycopy(input, 8 + offset * size, tmp, 0, size);
                    VAPE listItem = new VAPE(tmp);
                    data.add(listItem);
                }
            }
        }
        System.out.println("[Payload Data] ***read " + DATAMAP.get(dataMode) + " message completed***");
    }

    public String getWorkMode(){
        return WORKMAP.get(workMode);
    }

    public String getDataMode(){
        return DATAMAP.get(dataMode);
    }
}

