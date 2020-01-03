package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
        timestamp = (long) Math.abs(getIntValue(input, 0)) *1000+Math.abs(getIntValue(input, 4));
        t1 = getFloatValue(input, 8);
        t2 = getFloatValue(input, 12);
        t3 = getFloatValue(input, 16);
        t4 = getFloatValue(input, 20);
    }
}
