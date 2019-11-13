package com.plantserver.Util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;

public class AkycByteMsg {
    private short flag, count;
    private int uid;
    private ParserUtils parserUtils = new ParserUtils();

    public AkycByteMsg(byte[] input) {
        this.uid = (int) parserUtils.shiftBytes(input, 0, "int");
        this.flag = (short) parserUtils.shiftBytes(input, 4, "short");
        this.count = (short) parserUtils.shiftBytes(input, 6, "short");
        System.out.println("解析到头部8字节:" + this.uid + "|=|" + this.flag + "|=|" + this.count);
    }

    public int getUid() {
        return this.uid;
    }

    public short getFlag() {
        return this.flag;
    }

    public short getCount() {
        return this.count;
    }

    public long getTSInLine(byte[] byteArr, int offset) {
        return (long) parserUtils.shiftBytes(byteArr, offset, "long");
    }

    public short getShakeInLine(byte[] byteArr, int offset) {
        return (short) parserUtils.shiftBytes(byteArr, offset, "short");
    }

    public short getTempInLine(byte[] byteArr, int offset) {
        return (short) parserUtils.shiftBytes(byteArr, offset, "short");
    }
}
