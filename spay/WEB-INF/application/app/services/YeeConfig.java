package services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import models.t_payment_gateways;
import constants.Constants;
import business.GateWay;
import play.Logger;
import play.Play;

/**
 * 易宝配置信息读取类
 * @author yangxuan
 *	@create 220150319
 */
public class YeeConfig{
	
	//易宝常见配置属性
	private static Properties properties = null;
	
	
	//必须参数、ChkValue、汇付响应ChkValue key集合
	private static Map<String,String[]> maps = null;
	
	//汇付天下配置文件路径,加载Properties文件使用
	private static final String path = Play.configuration.getProperty("chinapnrconfigpath"); 

	static{
		if(properties==null){
			loadProperties();
		}
		
		if(maps==null){
			initMaps();
		}
	}
	
	/**
	 * 加载汇付天下配置文件
	 */
	private static void loadProperties(){
		Logger.debug("读取yee配置文件...");
		properties = new Properties();
		t_payment_gateways gatway = readGateWay();
		String yee_merCustId = gatway.account;
		properties.put("yee_merCustId", yee_merCustId);
		/*try {
			properties.load(new FileInputStream(new File(path))); 
			
			//商户号保存至数据库
			t_payment_gateways gatway = readGateWay();
			String chinapnr_merCustId = gatway.account;
			properties.put("chinapnr_merCustId", chinapnr_merCustId);
			
		} catch (FileNotFoundException e) {

			Logger.error("读取汇付天下配置库时 :%s", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}*/
	}
	
	/**
	 * 读取网关信息
	 */
	private static t_payment_gateways readGateWay(){
		t_payment_gateways gatWay = GateWay.queryGateWayById(Constants.YEE);
		
		return gatWay;
	}
	
	/**
	* 初始化汇付天下必须字段、ChkValue字段、响应ChkValue字段 key集合
	*/
	private static void initMaps(){
		maps = new HashMap<String, String[]>();
		//账户查询
		String[] account_info_must = {"platformNo","platformUserNo"};
		maps.put("ACCOUNT_INFO_must", account_info_must);
		//单笔业务查询
		String[] query_must = {"platformNo","requestNo","mode"};
		maps.put("QUERY_must", query_must);
		//转账确认
		String[] complete_transaction_must = {"platformNo","requestNo","mode","notifyUrl"};
		maps.put("COMPLETE_TRANSACTION_must", complete_transaction_must);
	}
	

	/**
	 * 获取properties中的 value
	 * @param key
	 * @return
	 */
	public static String getProperty(String key) {
		return properties.getProperty(key);
	}
	
	/**
	 *  获取Req请求必须字段集合
	 * @param key
	 * @return
	 */
	public static String[] getMustKeys(String key){
		return maps.get(key+"_must");
	}
	
	 

}
