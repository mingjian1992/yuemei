package services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultAttribute;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import utils.Converter;
import constants.YEEConstants;
import controllers.YEE.SecureSign.SignUtil;

/**
 * 易宝业务基类
 * @author yangxuan
 * @date 20150319
 */
public class YeeBaseService extends LinkedHashMap<String, String>{
	
	/**
	 * 设值
	 * @param key
	 * @param value
	 * @return
	 */
	public YeeBaseService putValue(String key,String value){
		put(key, value);
		return this;
	}
	
	/**
	 * 获取key集合
	 * @return
	 */
	private Set<String> getKeys(){
		return keySet();
	}
	
	/**
	 * 校验指令
	 * @return
	 */
	private boolean validateCmdId(){
		String cmdId = get("service");
		if(cmdId==null|"".equals(cmdId))
			return false;
		return true;
	}

	/**
	 * 校验必须参数
	 * @return
	 */
	public boolean validateMust(){
		if(validateCmdId()){
			String cmdId = get("service");
			String[] mustKeys = YeeConfig.getMustKeys(cmdId);
			Set<String> keys = getKeys();
			return keys.containsAll(Arrays.asList(mustKeys));
		}
		return false;
	}
	
	protected String http(){
		if(!validateMust()){
			Logger.error("------------------缺少必须参数-------------------");
			return "-1";  
		}
		String req = this.buildReq();
		String service = this.get("service");
		String sign = this.buildSign(req);
		Map<String,String> params = new HashMap<String, String>();
		params.put("service", service);
		params.put("req", req);
		params.put("sign", sign);
		HttpResponse response = WS.url(YEEConstants.YEE_URL_REDICT).setParameters(params).post();
		if(response.getStatus().intValue() == 200){
			Logger.debug("http:%s", response.getString("UTF-8"));
			return response.getString("UTF-8");
		}
		Logger.error("------------------请求易宝出现网络异常-------------------");
		return "-2";  
	}
	
	protected JSONObject doExcute() {
		String result = http();
		JSONObject json = new JSONObject();
		if("-1".equals(result)){
			json.put("msg", "缺少必须参数");
		}else if("-2".equals(result)){
			json.put("msg", "请求易宝出现网络异常");
		}else{
			json = (JSONObject) Converter.xmlToObj(result);
		}
		return json;
	}
	
	private String buildReq(){
		String service = get("service");
		String[] keys = YeeConfig.getMustKeys(service);
		JSONObject json = new JSONObject();
		for(String key : keys){
			 json.put(key, this.get(key));
		}
		String req = Converter.jsonToXml(json.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = addAttribute(req);
		return req;
	}
	
	/**
	 * "易宝"接口 XML节点添加属性
	 * @param domain商户号
	 * @param req XML格式的字符串
	 * @return
	 * @throws DocumentException
	 */
	private String addAttribute(String req){
        Attribute attribute = new DefaultAttribute("platformNo", YeeConfig.getProperty("yee_merCustId"));
		
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
	
	private String buildSign(String req){
		 return SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
	}
	
	
}
