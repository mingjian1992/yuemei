package utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class Converter {
	
	public static void main(String[] args) {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq><pStatus>9999</pStatus><pMerBillNo>1#1#1410251813193</pMerBillNo><pIdentNo>410621198406155011</pIdentNo><pRealName>环迅</pRealName><pIpsAcctNo></pIpsAcctNo><pIpsAcctDate></pIpsAcctDate><pMemo1>http://172.16.6.171:9000/IPSAction/IPSCallBack;http://172.16.6.171:9000/IPSAction/IPSCallBack;1;2;0;null</pMemo1><pMemo2>pMemo2</pMemo2><pMemo3>pMemo3</pMemo3></pReq>";
				
		JSONObject json = (JSONObject) xmlToObj(xml);
		
		xml = jsonToXml(json.toString(), "pReq", null, null, null);
		
	}
	
	/**
	 * xml字符串转json字符串
	 * @param xml
	 * @return
	 */
	public static String xmlToJson(String xml){  
        return new XMLSerializer().read(xml).toString();  
    }
	
	/**
	 * json字符串转xml字符串
	 * @param json
	 * @return
	 */
    public static String jsonToXml(String json){  
        XMLSerializer xmlSerializer = new XMLSerializer();  
        xmlSerializer.setTypeHintsEnabled(false);      
        
        return xmlSerializer.write(JSONSerializer.toJSON(json));  
    }
    
	/**
	 * json字符串转xml字符串
	 * @param json
	 * @param rootName
	 * @param elementName
	 * @param objectName
	 * @param arrayName
	 * @return
	 */
    public static String jsonToXml(String json, String rootName, String elementName, String objectName, String arrayName){  
        XMLSerializer xmlSerializer = new XMLSerializer();  
        xmlSerializer.setTypeHintsEnabled(false);
        
        if (StringUtils.isNotBlank(rootName)) {
        	xmlSerializer.setRootName(rootName);
		}
        
        if (StringUtils.isNotBlank(elementName)) {
        	xmlSerializer.setElementName(elementName);
		}
        
        if (StringUtils.isNotBlank(objectName)) {
        	xmlSerializer.setObjectName(objectName);
        }
        
        if (StringUtils.isNotBlank(arrayName)) {
        	xmlSerializer.setArrayName(arrayName);
        }
        
        return xmlSerializer.write(JSONSerializer.toJSON(json));
    }
    
	/**
	 * xml字符串转json对象/数组
	 * @param xml
	 * @return
	 */
	public static JSON xmlToObj(String xml) {
		return new XMLSerializer().read(xml);  
	}
	
	public static String parseMapToXml(LinkedHashMap<String, String> xmlMap){

		String strxml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq>";
		try {
			
			for (Map.Entry<String, String> entry : xmlMap.entrySet()) {
				
				String key = entry.getKey();
				String value = "";
				if (entry.getValue().getClass().isAssignableFrom(String.class)) {
					value = entry.getValue().toString();
				}
				if(value == null){
					new Exception("参数" + key + "不能为null!");
					
						throw new Exception("参数" + key + "不能为null!");
					
				}
				if(value.equals("")){				
					throw new Exception("参数" + key + "不能为null!");
				}
				strxml = strxml + "<" + key + ">" + value + "</" + key + ">";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		strxml = strxml + "</pReq>";
		Logger.info(strxml);
		return strxml;
	}
}
