package info.ajaxplorer.client.util;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PassManager {

    private static final char[] PASSWORD = "a2z8r4klkpxwOIeWLK".toCharArray();
    private static final byte[] SALT = {
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
        (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,
    };

    public static String encrypt(String property) throws GeneralSecurityException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return "$AJXP_ENC$" + toHex(pbeCipher.doFinal(property.getBytes()));
    }

    public static String decrypt(String property) throws GeneralSecurityException, IOException {
    	if(!property.startsWith("$AJXP_ENC$")){
    		return property;
    	}
    	property = property.replace("$AJXP_ENC$", "");
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));        
        return new String(pbeCipher.doFinal(toByte(property)));
    }
    
    public static String toHex(String txt) {
    	return toHex(txt.getBytes());
    }
    public static String fromHex(String hex) {
    	return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
    	int len = hexString.length()/2;
    	byte[] result = new byte[len];
    	for (int i = 0; i < len; i++)
    		result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
    	return result;
    }

    public static String toHex(byte[] buf) {
    	if (buf == null)
    		return "";
    	StringBuffer result = new StringBuffer(2*buf.length);
    	for (int i = 0; i < buf.length; i++) {
    		appendHex(result, buf[i]);
    	}
    	return result.toString();
    }
    private final static String HEX = "0123456789ABCDEF";
    private static void appendHex(StringBuffer sb, byte b) {
    	sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
    }    
}