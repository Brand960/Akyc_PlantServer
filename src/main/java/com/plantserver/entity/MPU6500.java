package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MPU6500 extends BytePayload {
    public long timestamp;
    public float ax;
    public float ay;
    public float az;
    public float px;
    public float py;
    public float pz;
    public float temperature;
    //User1 2B 前两b加速度范围:00 1g|01 2g|10 3g|11 4g

    MPU6500(byte[] input) throws NullPointerException {
        // 振动换算范围 几个g
//        float scale = GMAP.get((input[22] >> 6) & 0x03);
        float scale = 2f;

        timestamp =  (long) Math.abs(getIntValue(input, 0)) *1000+Math.abs((getIntValue(input, 4)%10)*100);
        // todo -32768~32767更进一步的精度分正负
        ax = getShortValue(input, 8) / 32768f * scale;
        ay = getShortValue(input, 10) / 32768f * scale;
        az = getShortValue(input, 12) / 32768f * scale;
        px = getShortValue(input, 14) / 32768f * scale;
        py = getShortValue(input, 16) / 32768f * scale;
        pz = getShortValue(input, 18) / 32768f * scale;
        // todo 温度范围 目前徐博说是/333+21
        temperature = getShortValue(input, 20) / 333f + 21;
    }
}
