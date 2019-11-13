package com.plantserver.Util;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Component
public class ParserUtils {

    /**
     * 解码字节成整型
     * <p>java原生解码byte[8]成long的方法</p>
     *
     * @param input  输入字节数组数据
     * @param offset 开始解析的字节位置
     * @param type   返回类型
     * @return 长整型数据
     */
    public Object jvmBytes(byte[] input, int offset, String type) {
        int length = 0;
        switch (type) {
            case "long":
                length = 8;
                break;
            case "int":
                length = 4;
                break;
            case "short":
                length = 2;
                break;
            default:
                return 0;
        }
        byte[] tmp = new byte[length];
        System.arraycopy(input, offset, tmp, 0, length);
        ByteBuffer buffer = ByteBuffer.wrap(tmp);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        switch (type) {
            case "long":
                return buffer.getLong();
            case "int":
                return buffer.getInt();
            case "short":
                return buffer.getShort();
            default:
                return 0;
        }
    }

    /**
     * byte[]数组位运算取数值
     *
     * @param input  byte数组
     * @param offset 从数组的第offset位开始
     * @param type   返回类型
     * @return 数值
     */
    public Object shiftBytes(byte[] input, int offset, String type) {
        int length = 0;
        switch (type) {
            case "long":
                length = 8;
                break;
            case "int":
                length = 4;
                break;
            case "short":
                length = 2;
                break;
            default:
                return 0;
        }
        byte[] tmp = new byte[length];
        System.arraycopy(input, offset, tmp, 0, length);
        switch (type) {
            case "long":
                return ((long) (tmp[0] & 0xFF) |
                        (long) (tmp[1] & 0xFF) << 8 |
                        (long) (tmp[2] & 0xFF) << 16 |
                        (long) (tmp[3] & 0xFF) << 24 |
                        (long) (tmp[4] & 0xFF) << 32 |
                        (long) (tmp[5] & 0xFF) << 40 |
                        (long) (tmp[6] & 0xFF) << 48 |
                        (long) (tmp[7] & 0xFF) << 56);
            case "int":
                return ((tmp[0] & 0xFF) |
                        (tmp[1] & 0xFF) << 8 |
                        (tmp[2] & 0xFF) << 16 |
                        (tmp[3] & 0xFF) << 24);
            case "short":
                return (short) ((tmp[0] & 0xFF) |
                        (tmp[1] & 0xFF) << 8);
            default:
                return 0;
        }
    }

    /**
     * 十六进制字符串转字节数组
     * <p>将测试数据转成byte[]再通过mqtt发送，每两个字符代表一个字节</p>
     *
     * @param s 输入十六进制字符串
     * @return byte[]数据
     */
    public byte[] toStringHex(String s) {
        // 删除空格
        s = s.replace(" ", "");
        // 每两个字符代表一个字节
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                // 解析每个8位字节为32位时和11111111做与运算后保证二进制补码不变转成byte
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return baKeyword;
    }
}
