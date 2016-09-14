package utils.loan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shove.security.Encrypt;

import constants.Constants;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

public class LoanUtil {

	/**
	 * 生成随机数
	 * 
	 * @param length
	 *            位数
	 * @return
	 */
	public static String getRandomNum(int length) {
		try {
			if (length <= 0) {
				return "";
			}
			Random r = new Random();
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < length; i++) {
				result.append(Integer.toString(r.nextInt(10)));
			}
			return result.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 将字符串转化成32位大写的MD5值
	 * 
	 * @param s
	 * @return
	 */
	public static final String getMD5Info(String s) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };

		try {
			byte[] btInput = s.getBytes("UTF-8");
			// 获得MD5摘要算法的 MessageDigest 对象
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			// 使用指定的字节更新摘要
			mdInst.update(btInput);
			// 获得密文
			byte[] md = mdInst.digest();
			// 把密文转换成十六进制的字符串形式
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xf];
				str[k++] = hexDigits[byte0 & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 发送ws请求
	 * 
	 * @param strURL
	 * @param req
	 * @return
	 */
	public static String[] doPostQueryCmd(String strURL, Map<String, Object> req) {
		String returncode = "";
		String result = "";
		String[] resultarr = new String[2];
		
		HttpResponse response = null;
		try{
			WSRequest request = null;
			request= WS.url(strURL);
	
			request.timeout = 6000;
			Map<String, String> header = new HashMap<String, String>();
			header.put("Content-Type",
					"application/x-www-form-urlencoded;charset=utf-8");
			request.headers = header;
			
			response = request.params(req).get();
			
		}catch(Exception e){
			Logger.info("ws请求时：%s", e.getMessage());
		}

		if(response != null){
			int status = response.getStatus();
			returncode = Integer.toString(status);
			result = response.getString();
		}

		Logger.info("======WS请求结果：========");
		Logger.info("status：%s", returncode);
		Logger.info("result：%s", result);
		
		resultarr[0] = returncode;
		resultarr[1] = result;

		return resultarr;
	}

	/**
	 * 将对象进行转化成JSON字符串
	 * 
	 * @param obModel
	 * @return String
	 */
	public final static String toJson(Object object) {
		Gson gson = new Gson();
		return gson.toJson(object);
	}

	/**
	 * 将JSON字符串转化成JsonObject
	 * 
	 * @param String
	 * @return JsonObject
	 */
	public final static List<Map> toList(String json) {
		Gson gson = new Gson();
		List<Map> list = gson.fromJson(json, List.class);
		return list;
	}

	/**
	 * 将JSON字符串转化成JsonObject
	 * 
	 * @param String
	 * @return JsonObject
	 */
	public final static Map toMap(String json) {
		Gson gson = new Gson();
		Map map = gson.fromJson(json, Map.class);

		return map;
	}

	/**
	 * 字符串编码
	 * 
	 * @param sStr
	 * @param sEnc
	 * @return String
	 */
	public final static String UrlEncoder(String sStr, String sEnc) {
		String sReturnCode = "";
		try {
			sReturnCode = URLEncoder.encode(sStr, sEnc);
		} catch (UnsupportedEncodingException ex) {

		}
		return sReturnCode;
	}

	/**
	 * 字符串解码
	 * 
	 * @param sStr
	 * @param sEnc
	 * @return String
	 */
	public final static String UrlDecoder(String sStr, String sEnc) {
		String sReturnCode = "";
		try {
			sReturnCode = URLDecoder.decode(sStr, sEnc);
		} catch (UnsupportedEncodingException ex) {

		}
		return sReturnCode;
	}

	public static String parseMapToXml(LinkedHashMap<String, String> xmlMap) {

		String strxml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq>";
		try {

			for (Map.Entry<String, String> entry : xmlMap.entrySet()) {

				String key = entry.getKey();
				String value = "";
				if (entry.getValue().getClass().isAssignableFrom(String.class)) {
					value = entry.getValue().toString();
				}

				strxml = strxml + "<" + key + ">" + value + "</" + key + ">";
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		strxml = strxml + "</pReq>";

		return strxml;
	}

	public static void printLoggerFromLoan(String ResultCode, String Message,String ReturnTimes,
			String... details) {

		StringBuffer detailInfo = new StringBuffer();
		for (int i = 1; i <= details.length; i++) {
			detailInfo.append("[" + i + "]" + ":" + details[i - 1] + "; ");
		}

		Logger.info("======Params From Loan========");
		Logger.info("ResultCode = %s", ResultCode);
		Logger.info("Message = %s", Message);
		Logger.info("ReturnTimes = %s", ReturnTimes);
		// Logger.info("detail = %s",Encrypt.encrypt3DES(detailInfo,
		// Constants.ENCRYPTION_KEY));

		Logger.info("detail = %s", detailInfo.toString());
	}

	public static void printLoggerToP2P(String pWebUrl, String pMerCode,
			String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		Logger.info("======Params To P2P========");
		Logger.info("Url = %s", pWebUrl);
		Logger.info("pMerCode = %s",pMerCode == null ? "null" : Encrypt.encrypt3DES(pMerCode,Constants.ENCRYPTION_KEY));
		Logger.info("pErrCode = %s", pErrCode);
		Logger.info("pErrMsg = %s", pErrMsg);
		// Logger.info("p3DesXmlPara = %s", p3DesXmlPara);
		Logger.info("pSign = %s", pSign);

		Logger.info("p3DesXmlPara = %s",
				Encrypt.decrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY));
	}

	public static void printLoggerFromP2P(String version, int type,
			long memberId, String memberName, String domain, String argMerCode,
			String arg3DesXmlPara, String argeXtraPara, String argSign,
			String argIpsAccount) {
		Logger.info("======Params From P2P========");

		String baseInfo = "version:" + version + "; type:" + type
				+ "; memberId:" + memberId + "; memberName:" + memberName
				+ "; domain:" + domain + "; argMerCode:" + argMerCode
				+ "; argIpsAccount:" + argIpsAccount + "; argSign:" + argSign;

		// Logger.info("baseInfo = %s", baseInfo == null ? "null" :
		// Encrypt.encrypt3DES(argMerCode,Constants.ENCRYPTION_KEY));
		// Logger.info("arg3DesXmlPara = %s", arg3DesXmlPara);
		// Logger.info("argeXtraPara = %s", argeXtraPara);
		Logger.info("arg3DesXmlPara = %s", arg3DesXmlPara == null ? "null" : Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY));
		Logger.info("argeXtraPara = %s", argeXtraPara == null ? "null" : Encrypt.decrypt3DES(argeXtraPara, Constants.ENCRYPTION_KEY));
		Logger.info("baseInfo = %s", baseInfo);
	}

	public static void printLoggerToLoan(Map<String, Object> args) {
		StringBuffer detail = new StringBuffer();

		for (String key : args.keySet()) {
			detail.append(key + ": " + args.get(key) + ";");
		}

		Logger.info("======Params To Loan========");
		// Logger.info("detail = %s", Encrypt.encrypt3DES(detail.toString(),Constants.ENCRYPTION_KEY));

		Logger.info("detail = %s", detail.toString());
	}

	/**
	 * 按token从指定位置截取字符串，从0开始
	 * @param str
	 * @param begin 开始位置，包含begin
	 * @param end 结束位置，不包含end，-1表示str结束位置
	 * @param token
	 * @return
	 */
	public static String subString(String str, int begin, int end,String token) {
    	String[] array = str.split(",");
    	List list = Arrays.asList(array);
    	
		if(end == -1){
			end = list.size();
		}
    	
    	List newList = list.subList(begin, end);
    	
    	String newString = newList.toString().replace(" ", "");
    			
		return newString.substring(1,newString.length()-1);
	}

}
