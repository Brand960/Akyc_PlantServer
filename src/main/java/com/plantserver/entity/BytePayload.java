package com.plantserver.entity;

import com.plantserver.util.ParserUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
class BytePayload {

    @Resource
    ParserUtil parserUtil;
    static BytePayload bytePayload;

    @PostConstruct
    public void init() {
        bytePayload = this;
    }

    static final Map<Integer, Float> GMAP;
    static final Map<Integer, String> WORKMAP;
    static final Map<Integer, String> DATAMAP;

    static {
        GMAP = new HashMap<>();
        GMAP.put(0, 9.8f); // 正负1g
        GMAP.put(1, 9.8f * 2); // 正负2g
        GMAP.put(2, 9.8f * 3); // 正负3g
        GMAP.put(3, 9.8f * 4); // 正负4g

        WORKMAP = new HashMap<>();
        WORKMAP.put(4, "perHour");
        WORKMAP.put(2, "realTime");
        WORKMAP.put(6, "standBy");
        WORKMAP.put(1, "calculating");
        WORKMAP.put(5, "message");

        DATAMAP = new HashMap<>();
        DATAMAP.put(1, "shake");
        DATAMAP.put(2, "temperature");
        DATAMAP.put(3, "power");
    }

    static final float TSCALE = 40f;

    long getLongValue(byte[] input, int offset) {
        return (long) bytePayload.parserUtil.jvmBytes(input, offset, "long");
    }

    short getShortValue(byte[] input, int offset) {
        return (short) bytePayload.parserUtil.jvmBytes(input, offset, "short");
    }

    int getIntValue(byte[] input, int offset) {
        return (int) bytePayload.parserUtil.jvmBytes(input, offset, "int");
    }

    float getFloatValue(byte[] input, int offset) {
        return (float) bytePayload.parserUtil.jvmBytes(input, offset, "float");
    }
}
