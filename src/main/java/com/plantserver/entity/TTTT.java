package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

// 1219温度温度温度温度
@Getter
@Setter
public class TTTT extends BytePayload {
    public long timestamp;
    public float t1; //the data of current value the unit is (A)
    public float t2; //the data of voltage value the unit is (V)
    public float t3; //the data of power value the unit is (W)
    public float t4; //the data of electric energy value the unit is (Kwh)

    TTTT(byte[] input) throws NullPointerException {
        timestamp = (long) Math.abs(getIntValue(input, 0)) * 1000 + Math.abs(getIntValue(input, 4));
        BigDecimal raw_t1 = BigDecimal.valueOf(getFloatValue(input, 8));
        BigDecimal raw_t2 = BigDecimal.valueOf(getFloatValue(input, 12));
        BigDecimal raw_t3 = BigDecimal.valueOf(getFloatValue(input, 16));
        BigDecimal raw_t4 = BigDecimal.valueOf(getFloatValue(input, 20));

        t1 = raw_t1.setScale(3, RoundingMode.DOWN).floatValue();
        t2 = raw_t2.setScale(3, RoundingMode.DOWN).floatValue();
        t3 = raw_t3.setScale(3, RoundingMode.DOWN).floatValue();
        t4 = raw_t4.setScale(3, RoundingMode.DOWN).floatValue();
//        t1 = getFloatValue(input, 8);
//        t2 = getFloatValue(input, 12);
//        t3 = getFloatValue(input, 16);
//        t4 = getFloatValue(input, 20);
    }
}
