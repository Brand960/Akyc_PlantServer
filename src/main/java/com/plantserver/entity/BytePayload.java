package com.plantserver.entity;

import com.plantserver.Util.ParserUtil;

import java.util.HashMap;
import java.util.Map;

class BytePayload {

    static final Map<Integer, Float> GMAP;
    static final Map<Integer, String> WORKMAP;
    static final Map<Integer, String> DATAMAP;

    static {
        GMAP = new HashMap<>();
        GMAP.put(0, 9.8f * 2); // 正负1g
        GMAP.put(1, 9.8f * 4); // 正负2g
        GMAP.put(2, 9.8f * 6); // 正负3g
        GMAP.put(3, 9.8f * 8); // 正负4g

        WORKMAP = new HashMap<>();
        WORKMAP.put(0, "realTime");
        WORKMAP.put(1, "perHour");

        DATAMAP = new HashMap<>();
        DATAMAP.put(0, "shake");
        DATAMAP.put(1, "power");
    }

    static final float TSCALE = 40f * 2;

    private ParserUtil parserUtil = new ParserUtil();

    long getLongValue(byte[] input, int offset) {
        return (long) parserUtil.shiftBytes(input, offset, "long");
    }

    short getShortValue(byte[] input, int offset) {
        return (short) parserUtil.shiftBytes(input, offset, "short");
    }

    int getIntValue(byte[] input, int offset) {
        return (int) parserUtil.shiftBytes(input, offset, "int");
    }

    float getFloatValue(byte[] input, int offset) {
        return parserUtil.jvmBytes2Float(input, offset);
    }
}
