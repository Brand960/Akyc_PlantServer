package com.plantserver.Util;

public class AkycByteMsg {
    private byte[] akyc;
    private ParserUtils parserUtils=new ParserUtils();


    public AkycByteMsg(byte[] input) {
        this.akyc = input;
    }

    public long getTimestamp() {
        return (long) parserUtils.shiftBytes(this.akyc, 0, "long");
    }
}
