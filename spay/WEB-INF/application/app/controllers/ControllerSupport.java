package controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import constants.YEEConstants;
import play.Logger;
import utils.DateUtil;
import utils.FileUtil;

/**
 * 增强控制类
 * @author yangxuan
 *
 */
public class ControllerSupport {
	
	

	public static void printParams(String tags,Map<String,String> params){
		Logger.info("%s : %s", tags,params.toString());
	}
	
	protected static void wpRespParams(String tags,Map<String,String> params){
		printParams(tags,params);
		writeRespParams(params.toString());
	}
	
	/**
	 * 打印并写入文件请求参数
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
	 * @param value
	 */
	protected static void writeRespParams(String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append("------------------------response params start------------------------");
		buffer.append("\r\n");
		buffer.append(value);
		buffer.append("\r\n");
		buffer.append("------------------------response params end------------------------");
		buffer.append("\r\n");
		writeLogs(buffer.toString());
	}
	
	/**
	 * 写入请求至日志
	 * @param value
	 */
	protected static void writeReqParams(String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append(value);
		buffer.append("\r\n");
		writeLogs(buffer.toString());
	}
	
	private static void writeLogs(String value){
		String parentPath = YEEConstants.LOGFILEROOT + FileUtil.getPathByCurrentDate();
		String filePath = parentPath+ File.separator + DateUtil.getDate()+ ".logs";
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
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
			maps.put(YEEConstants.TRANSFER_MER_TO_USERS, "商户专用户(发送投标奖励)");
			maps.put(YEEConstants.TRANSFER_MER_TO_USER, "商户专用户(发放cps奖励)");
			maps.put(YEEConstants.COMPENSATORYREPAYMENT, "代偿还款");
		}
		
		public static String getCommandName(int type){
			return maps.get(type);
		}
		
	}
}
