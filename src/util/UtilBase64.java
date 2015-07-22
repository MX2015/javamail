package util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * BASE64加密解密
 */
public class UtilBase64 {
    /**
     * BASE64解密
     * @param key  base64密文
     * @return  明文
     * @throws Exception
     */
    public static byte[] decryptBASE64(String key) throws Exception {
        return (new BASE64Decoder()).decodeBuffer(key);
    }

    /**
     * BASE64加密
     * @param key   明文
     * @return  base64密文
     * @throws Exception
     */
    public static String encryptBASE64(byte[] key) throws Exception {
        return (new BASE64Encoder()).encodeBuffer(key).trim();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("875729140@qq.com\t" + UtilBase64.encryptBASE64("875729140@qq.com".getBytes()));
        System.out.println("aaa@localhost\t" + UtilBase64.encryptBASE64("aaa@localhost".getBytes()));
        System.out.println("bbb@localhost\t" + UtilBase64.encryptBASE64("bbb@localhost".getBytes()));
        System.out.println("tuzhihao\t" + UtilBase64.encryptBASE64("tuzhihao".getBytes()));
        System.out.println("123456\t" + UtilBase64.encryptBASE64("123456".getBytes()));
        System.out.println("198822319\t" + UtilBase64.encryptBASE64("198822319leixin".getBytes()));
    }
}