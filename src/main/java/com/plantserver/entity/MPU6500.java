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
        float scale = GMAP.get((input[22] >> 6) & 0x03);

        timestamp = getLongValue(input, 0);
        ax = getShortValue(input, 8) / 32768f * scale;
        ay = getShortValue(input, 10) / 32768f * scale;
        az = getShortValue(input, 12) / 32768f * scale;
        px = getShortValue(input, 14);
        py = getShortValue(input, 16);
        pz = getShortValue(input, 18);
        // 温度范围 徐博说是 正负40
        temperature = getShortValue(input, 20) / 32768f * TSCALE;
    }
}
