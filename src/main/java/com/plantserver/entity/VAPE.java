package com.plantserver.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
public class VAPE extends BytePayload {
    public long timestamp;
    public float v; //the data of current value the unit is (A)
    public float a; //the data of voltage value the unit is (V)
    public float p; //the data of power value the unit is (W)
    public float e; //the data of electric energy value the unit is (Kwh)

    VAPE(byte[] input) throws NullPointerException {
        timestamp = (long) Math.abs(getIntValue(input, 0)) * 1000 + Math.abs(getIntValue(input, 4));
        BigDecimal raw_v = BigDecimal.valueOf(getFloatValue(input, 8));
        BigDecimal raw_a = BigDecimal.valueOf(getFloatValue(input, 12));
        BigDecimal raw_p = BigDecimal.valueOf(getFloatValue(input, 16));
        BigDecimal raw_e = BigDecimal.valueOf(getFloatValue(input, 20));

        v = raw_v.setScale(3, RoundingMode.DOWN).floatValue();
        a = raw_a.setScale(3, RoundingMode.DOWN).floatValue();
        p = raw_p.setScale(3, RoundingMode.DOWN).floatValue();
        e = raw_e.setScale(3, RoundingMode.DOWN).floatValue();
    }
}
