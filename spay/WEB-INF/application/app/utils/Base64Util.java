package utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * base64 加密 解密
 * @author zyx
 * @version 6.0
 * @created 2015-3-11
 */
public class Base64Util {

	private static BASE64Encoder base64Encoder = new BASE64Encoder();// 加密
	private static BASE64Decoder base64Decoder = new BASE64Decoder();// 解密
	private static String encoding = "UTF-8";//字符编码

	/**
	 * Base64 加密
	 * 
	 * @param arg
	 * @return
	 */
	public static String encoder(String arg) {
		try {
			return base64Encoder.encode(arg.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	/**
	 * base64 解密
	 * @param arg
	 * @return
	 */
	public static String decoder(String arg) {
		try {
			return new String(base64Decoder.decodeBuffer(arg),encoding);
		} catch (IOException e) {
			return "";
		}
	}

}
