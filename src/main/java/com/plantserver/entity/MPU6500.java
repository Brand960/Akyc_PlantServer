package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        float scale = 2f / 32768f;
        timestamp = (long) Math.abs(getIntValue(input, 0)) * 1000 + Math.abs((getIntValue(input, 4) % 10) * 100);
        // todo -32768~32767更进一步的精度分正负
        BigDecimal raw_ax = new BigDecimal(getShortValue(input, 8) * scale);
        BigDecimal raw_ay = new BigDecimal(getShortValue(input, 10) * scale);
        BigDecimal raw_az = new BigDecimal(getShortValue(input, 12) * scale);
        BigDecimal raw_px = new BigDecimal(getShortValue(input, 14) * scale);
        BigDecimal raw_py = new BigDecimal(getShortValue(input, 16) * scale);
        BigDecimal raw_pz = new BigDecimal(getShortValue(input, 18) * scale);

        ax = raw_ax.setScale(3, RoundingMode.DOWN).floatValue();
        ay = raw_ay.setScale(3, RoundingMode.DOWN).floatValue();
        az = raw_az.setScale(3, RoundingMode.DOWN).floatValue();
        px = raw_px.setScale(3, RoundingMode.DOWN).floatValue();
        py = raw_py.setScale(3, RoundingMode.DOWN).floatValue();
        pz = raw_pz.setScale(3, RoundingMode.DOWN).floatValue();
        // todo 温度范围 目前徐博说是/333+21
        BigDecimal raw_temperature = BigDecimal.valueOf(getShortValue(input, 20) / 333f + 21);
        temperature = raw_temperature.setScale(3, RoundingMode.DOWN).floatValue();
    }
}
