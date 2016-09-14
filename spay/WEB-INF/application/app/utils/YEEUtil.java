package utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultAttribute;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

public class YEEUtil {

	/** 
	* 发送post请求到直接接口
	* 
	* @param strURL 
	* @param req 
	* @return 
	*/ 
	public static String[] doPostQueryCmd(String strURL, Map<String, Object> req){ 
	String returncode = null; 
	String result = null; 
	String[] resultarr = new String[2]; 

	WSRequest request = WS.url(strURL); 
	request.timeout = 1000; 
	Map<String, String> header = new HashMap<String, String>(); 
	header.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8"); 
	request.headers = header; 

	HttpResponse response = request.params(req).get(); 

	int status = response.getStatus(); 
	returncode = Integer.toString(status); 

	result = response.getString(); 

	if(result == null){ 
	result = ""; 
	} 

	resultarr[0] = returncode; 
	resultarr[1] = result; 

	return resultarr; 
	}
	
	/**
	 * "易宝"接口 XML节点添加属性
	 * @param domain商户号
	 * @param req XML格式的字符串
	 * @return
	 * @throws DocumentException
	 */
	public static String addAttribute(String argMerCode, String req){
        Attribute attribute = new DefaultAttribute("platformNo", argMerCode);
		
		Document document = null;
		try {
			document = (Document) DocumentHelper.parseText(req);
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} // 将字符串转为XML   
		Element e = document.getRootElement();
		e.add(attribute);

        return document.asXML();
	}
	
	/**
	 * 生成流水号(最长30位)
	 * @param userId (不能为负，系统行为：0)
	 * @param operation
	 * @return
	 */
	public static String createBillNo(long userId, int operation) {
		return "" + operation + new Date().getTime();
	}
}
