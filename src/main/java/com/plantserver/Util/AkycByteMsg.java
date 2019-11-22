package com.plantserver.Util;

import java.util.HashMap;

public class AkycByteMsg {
    private int uid;
    private short flag;
    private char count, type;
    private ParserUtil parserUtil = new ParserUtil();
    private HashMap<Long, Object[]> data;

    public AkycByteMsg(byte[] input) throws NullPointerException {
        this.uid = (int) parserUtil.shiftBytes(input, 0, "int");
        this.flag = (short) parserUtil.shiftBytes(input, 4, "short");
        this.type = (char) parserUtil.shiftBytes(input, 6, "char");
        this.count = (char) parserUtil.shiftBytes(input, 7, "char");

        System.out.println("解析到头部8字节:" + this.uid + "|=|" + this.flag + "|=|" + this.count);
        if (this.count == (input.length - 8) / 24)
            System.out.println("--校验数据部成功--");
        else {
            throw new NullPointerException();
        }

        // count前八位表明数据域格式,调用不同的解码
        if (this.type == 1) {
            tempParser(input);
        } else if (this.type == 2) {
            vaParser(input);
        } else {
            xyztParser(input);
        }
        System.out.println("=====解析数据部分" + this.count + "条数据完毕=====");
    }

    public int getUid() {
        return this.uid;
    }

    public short getFlag() {
        return this.flag;
    }

    public char getType() {
        return this.type;
    }

    private long getLongValue(byte[] input, int offset) {
        return (long) parserUtil.shiftBytes(input, offset, "long");
    }

    private short getShortValue(byte[] input, int offset) {
        return (short) parserUtil.shiftBytes(input, offset, "short");
    }

    private int getIntValue(byte[] input, int offset) {
        return (int) parserUtil.shiftBytes(input, offset, "int");
    }


    public HashMap<Long, Object[]> getData() {
        return this.data;
    }

    private void xyztParser(byte[] input) {
        for (int i = 0; i < (int) this.count; i++) {
            long ts = getLongValue(input, 8 + i * 24);
            short ax = getShortValue(input, 16 + i * 24);
            short ay = getShortValue(input, 18 + i * 24);
            short az = getShortValue(input, 20 + i * 24);
            short wx = getShortValue(input, 22 + i * 24);
            short wy = getShortValue(input, 24 + i * 24);
            short wz = getShortValue(input, 26 + i * 24);
            int temp = getIntValue(input, 28 + i * 24);
            this.data.put(ts, new Object[]{ax, ay, az, wx, wy, wz, temp});
        }
    }

    private void vaParser(byte[] input) {
        for (int i = 0; i < (int) this.count; i++) {
            long ts = getLongValue(input, 8 + i * 18);
            short v = getShortValue(input, 16 + i * 18);
            short a = getShortValue(input, 18 + i * 18);
            short p = getShortValue(input, 20 + i * 18);
            int e = getShortValue(input, 22 + i * 18);
            this.data.put(ts, new Object[]{v, a, p, e});
        }
    }

    private void tempParser(byte[] input) {
        for (int i = 0; i < (int) this.count; i++) {
            long ts = getLongValue(input, 8 + i * 12);
            short t1 = getShortValue(input, 20 + i * 12);
            short t2 = getShortValue(input, 22 + i * 12);
            short t3 = getShortValue(input, 24 + i * 12);
            short t4 = getShortValue(input, 26 + i * 12);
            this.data.put(ts, new Object[]{t1, t2, t3, t4});
        }
    }
}
