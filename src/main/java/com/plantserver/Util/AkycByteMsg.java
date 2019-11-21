package com.plantserver.Util;

import java.util.HashMap;

public class AkycByteMsg {
    private short flag, count;
    private int uid;
    private ParserUtil parserUtil = new ParserUtil();
    private HashMap<Long, short[]> data;

    public AkycByteMsg(byte[] input) throws NullPointerException {
        this.uid = (int) parserUtil.shiftBytes(input, 0, "int");
        this.flag = (short) parserUtil.shiftBytes(input, 4, "short");
        this.count = (short) parserUtil.shiftBytes(input, 6, "short");
        System.out.println("解析到头部8字节:" + this.uid + "|=|" + this.flag + "|=|" + this.count);
        if (this.count == (input.length - 8) / 24)
            System.out.println("--校验数据部成功--");
        else {
            throw new NullPointerException();
        }
        for (int i = 0; i < this.count; i++) {
            long ts = getTSInLine(input, 8 + i * 24);
            short ax = getShakeInLine(input, 16 + i * 24);
            short ay = getShakeInLine(input, 18 + i * 24);
            short az = getShakeInLine(input, 20 + i * 24);
            short wx = getShakeInLine(input, 22 + i * 24);
            short wy = getShakeInLine(input, 24 + i * 24);
            short wz = getShakeInLine(input, 26 + i * 24);
            short temp = getTempInLine(input, 28 + i * 24);
            this.data.put(ts, new short[]{ax, ay, az, wx, wy, wz, temp});
        }
        System.out.println("=====解析数据部分" + this.count + "条数据完毕=====");
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

    private long getTSInLine(byte[] byteArr, int offset) {
        return (long) parserUtil.shiftBytes(byteArr, offset, "long");
    }

    private short getShakeInLine(byte[] byteArr, int offset) {
        return (short) parserUtil.shiftBytes(byteArr, offset, "short");
    }

    private short getTempInLine(byte[] byteArr, int offset) {
        return (short) parserUtil.shiftBytes(byteArr, offset, "short");
    }

    public HashMap<Long, short[]> getData() {
        return this.data;
    }
}
