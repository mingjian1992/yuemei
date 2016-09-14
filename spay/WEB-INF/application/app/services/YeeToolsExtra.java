package services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.db.jpa.JPA;
import models.t_yee_req_params;
import models.t_yee_resq_params;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * 易宝工具插件扩展
 * @author yangxuan
 * @date 20150323
 */
public class YeeToolsExtra extends YeeTools{
	
	/**
	 * 保存请求参数(数据库以及文件)
	 * @param type
	 * @param platformMemberId
	 * @param tags
	 * @param params
	 * @param obj
	 * @param extra
	 */
	public static void recordReqParams(int type,long platformMemberId,String tags,Map<String,String> params,
			JSONObject obj,JSONObject extra){
		wpReqParams(type, platformMemberId, tags, params);
		t_yee_req_params model = buildReqModel(params,type,platformMemberId,obj);
		saveReqParams(model);
	}
	
	/**
	 * 保存易宝响应参数(数据库以及文件)
	 * @param tags
	 * @param isAyns
	 * @param respValue
	 */
	public static void recordRespParams(String tags,boolean isAyns,String respValue,String sign,String url){
		Map<String,String> maps =excuteRespValue(respValue);
		wpRespParams(tags, isAyns, maps);
		t_yee_resq_params model = buildRespModel(maps,sign,url,isAyns);
		saveRespParams(model);
	}
	
	/**
	 * 保存请求参数至数据库
	 */
	private static void saveReqParams(t_yee_req_params model){
		
		try{
			
			model.save();
		
		}catch(Exception e){
			Logger.error("保存请求参数至数据库时:%s", e.getMessage());
			//JPA.setRollbackOnly();
		}
	}
	
	/**
	 * 保存响应参数至数据库
	 * @param model
	 */
	private static void saveRespParams(t_yee_resq_params model){
		try{
			
			model.save();
		
		}catch(Exception e){
			Logger.error("保存响应参数至数据库时:%s", e.getMessage());
			JPA.setRollbackOnly();
		}
	}
	
	/**
	 * 构造请求参数实体对象
	 * @return
	 */
	private static t_yee_req_params buildReqModel(Object ... objs){
		Map<String,String> baseParams = (Map<String, String>) objs[0];
		int type = (Integer) objs[1];
		long memberId = (Long) objs[2];
		t_yee_req_params model = new t_yee_req_params();
		model.time = new Date();
		model.reqValue = baseParams.get("req");
		model.sign = baseParams.get("sign");
		model.url = baseParams.get("url");
		model.oprateType = type;
		model.oprateTypeName = CommonUtils.getCommandName(type);
		model.memberId = memberId;
		JSONObject json = (JSONObject) objs[3];
		String reqNo = json.get("pMerBillNo")==null?"":json.getString("pMerBillNo");
		model.requestNo = reqNo;
		return model;
	}
	
	/**
	 * 构造响应实体对象
	 * @return
	 */
	private static t_yee_resq_params buildRespModel(Map<String,String> maps,String sign,String url,boolean isAnys){		
		t_yee_resq_params model = new t_yee_resq_params();
		model.time = new Date();
		model.code = Integer.parseInt(maps.get("code"));
		model.ayns = isAnys==true?1:0;
		model.requestNo = maps.get("requestNo");
		model.result = maps.get("result");
		model.result = model.result.replaceAll("\r", "");
		model.result = model.result.replaceAll("\n", "");
		model.sign = sign;
		model.url = url;
		return model;
	}

}
