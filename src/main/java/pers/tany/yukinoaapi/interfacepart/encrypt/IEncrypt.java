package pers.tany.yukinoaapi.interfacepart.encrypt;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class IEncrypt {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IEncrypt() {
        throw new UnsupportedOperationException("IEncrypt 是工具类,不允许实例化");
    }


    /**
     * 转二进制为十六进制
     *
     * @param buf 二进制
     * @return 十六进制
     */
    
    public static String parseByte2HexStr(byte[] buf) {
        StringBuffer sb = new StringBuffer();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 转十六进制为二进制
     *
     * @param hexStr 十六进制
     * @return 二进制
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        } else {
            byte[] result = new byte[hexStr.length() / 2];
            for (int i = 0; i < hexStr.length() / 2; i++) {
                int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
                int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
                result[i] = (byte) (high * 16 + low);
            }
            return result;
        }
    }

    /**
     * 加密字符串
     *
     * @param res 需加密的字符串
     * @param key 加密的密码
     * @return 加密后的结果
     */
    
    public static String encrypt(String res, String key) {
        byte[] bs = res.getBytes();
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) ((bs[i]) ^ key.hashCode());
        }
        return parseByte2HexStr(bs);
    }

    /**
     * 解密字符串
     *
     * @param res 需要解密的密文
     * @param key 秘钥
     * @return 解密后的结果
     */
    
    public static String decrypt(String res, String key) {
        @Nullable byte[] bs = parseHexStr2Byte(res);
        for (int i = 0; i < bs.length; i++) {
            bs[i] = (byte) ((bs[i]) ^ key.hashCode());
        }
        return new String(bs);
    }
}


