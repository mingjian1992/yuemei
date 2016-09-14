package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;

import play.Logger;
import utils.Converter;
import utils.DateUtil;
import utils.FileUtil;
import constants.YEEConstants;

/**
 * 易宝工具插件
 * @author yangxuan
 * @达特20150318
 */
public class YeeTools {
	
	/**
	 * 打印参数
	 * @param tags
	 * @param params
	 */
	public static void printParams(String tags,Map<String,String> params){
		Logger.info("%s : %s", tags,params.toString());
	}
	
	/**
	 * 打印参数
	 * @param tags
	 * @param value
	 */
	public static void printParams(String tags,String value){
		Logger.info("%s : %s", tags,value);
	}
	
	/**
	 * 响应值处理方法，后期根据返回map做扩展
	 * @param respValue
	 * @return
	 */
	protected static Map<String,String> excuteRespValue(String respValue){
		JSONObject json = (JSONObject)Converter.xmlToObj(respValue);
		int code = json.getInt("code");
		Map<String,String> maps = new HashMap<String, String>();
		maps.put("code", code+"");
		maps.put("result", respValue);
		
		String requestNo = "";
		try{
			requestNo = json.getString("requestNo")==null?"~":json.getString("requestNo");
		}catch (Exception e) {
		}
		
		maps.put("requestNo", requestNo);
		return maps;
	}
	
	/**
	 * 构造响应写入参数
	 * @param respValue
	 * @return
	 */
	private static String buildRespValue(Map<String,String> maps){
		StringBuffer buffer = new StringBuffer();
		buffer.append(DateUtil.dateToString2(new Date()));
		buffer.append("####");
		String requestNo = maps.get("requestNo");
		buffer.append(requestNo);
		buffer.append("####");
		String code = maps.get("code");
		buffer.append(code);
		buffer.append("####");
		String result = maps.get("result");
		buffer.append(result);
		return buffer.toString();
	}
	
	/**
	 * 易宝回调至spay,打印并写入日志文件
	 * @param tags
	 * @param isAyns
	 * @param respValue
	 */
	public static void wpRespParams(String tags,boolean isAyns,Map<String,String> resp){
		printParams(tags,resp.toString());
		String prefix = "callback";
		if(isAyns){
			prefix = "ayns";
		}
		String value = buildRespValue(resp);
		writeRespParams(prefix,value);
	}
	
	/**
	 * spay请求至易宝打印并写入文件请求参数
	 * @param type 接口类型
	 * @param platformMemberId 用户客户号
 	 * @param tags 日志标签
	 * @param params 请求参数
	 */
	public static void wpReqParams(int type,long platformMemberId,String tags,Map<String,String> params){
		printParams(tags,params);
		String reqValue = buildReqValue(type,platformMemberId,params);
		writeReqParams(reqValue);
	}
	
	/**
	 * 封装请求参数值
	 * @param obj
	 */
	private static String buildReqValue(Object ... obj){
		StringBuffer buffer = new StringBuffer();
		int type = (Integer) obj[0];
		long platformMemberId = (Long) obj[1];
		Map<String,String> params = (Map<String, String>) obj[2];
		String cmdName =CommonUtils.getCommandName(type);
		buffer.append(DateUtil.dateToString2(new Date()));
		buffer.append("####");
		buffer.append(cmdName);
		buffer.append("("+type+")");
		buffer.append("####");
		buffer.append(platformMemberId);
		buffer.append("####");
		buffer.append(params.toString());
		return buffer.toString();
	}
	
	/**
	 * 写入响应至日志
	 * @param tags 
	 * @param value
	 */
	protected static void writeRespParams(String tags,String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append(value);
		buffer.append("\r\n");
		writeLogs("resp_"+tags,buffer.toString());
	}
	
	/**
	 * 写入请求至日志
	 * @param value
	 */
	protected static void writeReqParams(String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append(value);
		buffer.append("\r\n");
		writeLogs("req",buffer.toString());
	}
	
	/**
	 * 文件写入方法
	 * @param fileName
	 * @param value
	 */
	private static void writeLogs(String fileName,String value){
		String parentPath = YEEConstants.LOGFILEROOT + FileUtil.getPathByCurrentDate();
		String filePath = parentPath+ File.separator + DateUtil.getDate()+ fileName+".log";
		Logger.debug("writeLogs -->parentPath:%s", parentPath);
		Logger.debug("writeLogs -->filePath:%s", filePath);
		try{
			
			FileUtil.mkDir(parentPath);
			
		}catch(Exception e){
			
			Logger.error("创建文件夹时%s", e.getMessage());
			return;
		}
		
		File file = new File(filePath);
		FileOutputStream fos = null;
		Logger.debug("file getAbsoluteFile path : %s",file.getAbsoluteFile());
		try {
			fos = new FileOutputStream(file,true);
			IOUtils.write(value,fos );
		} catch (FileNotFoundException e) {
			
			Logger.error("文件写入时:%s", e.getMessage());
		} catch (IOException e) {
			
			Logger.error("文件写入时:%s", e.getMessage());
		}finally{
			try {
				fos.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 工具内部类
	 * @author yangxuan
	 */
	public static class CommonUtils{
		
		private static Map<Integer,String> maps = null;
		
		static{
			if(maps==null){
				initMaps();
			}
		}
		
		private static void initMaps(){
			maps = new HashMap<Integer, String>();
			maps.put(YEEConstants.WITHDRAWAL, "绑卡");
			maps.put(YEEConstants.CREATE_ACCOUNT, "开户");
			maps.put(YEEConstants.REGISTER_CREDITOR, "登记债权人");
			maps.put(YEEConstants.REGISTER_CRETANSFER, "债权转让");
			maps.put(YEEConstants.RECHARGE, "充值");
			maps.put(YEEConstants.TRANSFER, "转账");
			maps.put(YEEConstants.REPAYMENT, "还款");
			maps.put(YEEConstants.WITHDRAWAL, "提现");
			maps.put(YEEConstants.ACCOUNT_BALANCE, "用户余额查询");
			maps.put(YEEConstants.USER_INFO, "账户信息查询");
			maps.put(YEEConstants.QUERY_TRADE, "交易查询");
			maps.put(YEEConstants.BOUND_CARD, "绑定银行卡");
			maps.put(YEEConstants.TRANSFER_USER_TO_MER, "用户转商户");
			maps.put(YEEConstants.TRANSFER_MER_TO_USERS, "商户转用户（发放投标奖励，红包发送）");
			maps.put(YEEConstants.TRANSFER_MER_TO_USER, "商户专用户(发放cps奖励)");
			maps.put(YEEConstants.COMPENSATORYREPAYMENT, "代偿还款");
			maps.put(YEEConstants.REPAYMENT_SIGNING, "自动还款签约");
			maps.put(YEEConstants.UPDATE_PAY_PASS, "修改支付密码");
			maps.put(YEEConstants.AUTO_REPAYMENT, "自动还款");
			maps.put(YEEConstants.ENTERPRISE_REGISTER, "企业注册");
		}
		
		/**
		 * 通过接口类型获取中文接口名称
		 * @param type
		 * @return
		 */
		public static String getCommandName(int type){
			return maps.get(type);
		}
		
	}
}
