package com.plantserver.Util;

public class AkycByteMsg {
    private short flag, count;
    private int uid;
    private ParserUtil parserUtil = new ParserUtil();

    public AkycByteMsg(byte[] input) {
        this.uid = (int) parserUtil.shiftBytes(input, 0, "int");
        this.flag = (short) parserUtil.shiftBytes(input, 4, "short");
        this.count = (short) parserUtil.shiftBytes(input, 6, "short");
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
        return (long) parserUtil.shiftBytes(byteArr, offset, "long");
    }

    public short getShakeInLine(byte[] byteArr, int offset) {
        return (short) parserUtil.shiftBytes(byteArr, offset, "short");
    }

    public short getTempInLine(byte[] byteArr, int offset) {
        return (short) parserUtil.shiftBytes(byteArr, offset, "short");
    }
}
