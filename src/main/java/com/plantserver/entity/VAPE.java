package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VAPE extends BytePayload {
    public long timestamp;
    public float v; //the data of current value the unit is (A)
    public float a; //the data of voltage value the unit is (V)
    public float p; //the data of power value the unit is (W)
    public float e; //the data of electric energy value the unit is (Kwh)

    VAPE(byte[] input) throws NullPointerException {
        timestamp = getLongValue(input, 0);
        v = getFloatValue(input, 8);
        a = getFloatValue(input, 12);
        p = getFloatValue(input, 14);
        e = getFloatValue(input, 16);
    }
}
