package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import models.YeeReqModel;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import play.Logger;
import play.db.jpa.JPA;
import play.libs.Codec;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import utils.Base64Util;
import utils.Converter;
import utils.ErrorInfo;
import utils.YEEUtil;
import business.DealDetail;
import business.Member;
import business.MemberOfPlatform;

import com.google.gson.JsonObject;
import com.shove.Convert;
import com.shove.security.Encrypt;

import constants.Constants;
import constants.IPSConstants;
import constants.YEEConstants;
import constants.YEEConstants.QueryType;
import constants.YEEConstants.Status;
import controllers.BaseController;
import controllers.ControllerSupport;
import controllers.YEE.YEEPayment;
import controllers.YEE.SecureSign.SignUtil;

/**
 * 易宝支付接口处理
 * @author zhs
 * @date 2014-11-29 上午10:19:36
 */
public class YEE extends BaseController{
	
	private static final String P2P_URL_KEY = "url_key";  //同步页面地址
		
	private static final String P2P_ASYN_URL_KEY  = "asyn_url_key";  //异步地址
	
	//返回至p2p的异步或者同步参数
	private static final String P2P_BASEPARAMS = "base_params";
		
	//ws,http返回成功状态码
	private static final Integer HTTP_STATUS_SUCCESS = 200;
	
	private static final String P2P_COMIT_PAGE = "/PNR/PNRPayment/p2pcommit.html";
	
//成功
	private static final String PTradeStatue_S = "1";
	
	//失败
	private static final String PTradeStatue_F = "2";
	
	//处理中
	private static final String PTradeStatue_D = "3";
	
	//未找到
	private static final String PTradeStatue_N = "4";
	
	public static Map<String, String> entrance(int type, int platformId, long platformMemberId, String platformUsername, 
			String argMerCode, JSONObject json, String summary, JSONObject jsonXtraPara, String domain, ErrorInfo error) {
		json.put("type", type);  //回调时需要type字段，如果直接在t_member_event保存备注信息时是采用直接保存json时 ，可以用此方法
		json.put("memberId", platformMemberId);
		json.put("platformId", platformId);
		json.put("domain", domain);
		
		Map<String, String> argsMap = new HashMap<String, String>();
		String req = null;
		String url = null;
		
		YeeService service = new YeeService();
		YeeReqModel reqModel = new YeeReqModel();
		//提现之前要先绑卡
		if(YEEConstants.WITHDRAWAL == type){
			reqModel.setPlatformUserNo(platformMemberId+"");
			boolean isCardBind = service.isBindCard(reqModel);
			
			if(!isCardBind){
				type = YEEConstants.BOUND_CARD;
				json.put("type", type);
			}
		}
				
		String serialNumber = json.getString("pMerBillNo");
		Logger.debug("YEE->entrance->>>type:"+type);
		
		url = YEEConstants.IPS_URL_TEST[type];
		
		if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
			switch(type){
			case YEEConstants.CREATE_ACCOUNT:
				req = register(type, platformId, platformMemberId, json, argMerCode, summary, error);
			    break;
			case YEEConstants.REGISTER_CREDITOR://投标
			    req  = investBid(platformId, platformMemberId, json, argMerCode, summary, jsonXtraPara, error);
			    break;
			case YEEConstants.REGISTER_CRETANSFER://债权转让5
				req = transfer(platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.AUTO_SIGING:
				req  = autoBid(type, platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.REPAYMENT_SIGNING:
				req  = autoPay(type,platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.RECHARGE:
			    req  = recharge(platformId, platformMemberId, json, argMerCode, summary, error);
			    break;
			case YEEConstants.REPAYMENT://还款
				req = repayment(platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.AUTO_REPAYMENT://自动还款授权
				autoRepayment(platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.WITHDRAWAL://提现
				req = withdraw(platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.BOUND_CARD://绑卡
				req = bindBankCard(type, platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.TRANSFER_USER_TO_MER:
				req = userToMerchant(type,platformId, platformMemberId, json, jsonXtraPara, argMerCode, summary, error);
				break;
			case YEEConstants.UNBOUND_CARD://取消绑卡
				req = unBindBankCard(type, platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.TRANSFER_MER_TO_USERS:
				transferMerToUsers(type, platformId, platformMemberId, json, jsonXtraPara, argMerCode, summary, error);
				break;
			case YEEConstants.UPDATE_PAY_PASS://修改支付密码
				req = updatePayPass(type, platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.UPDATE_MOBILE:
				req = updateMobile(type, platformId, platformMemberId, json, argMerCode, summary, error); 
				break;
			case YEEConstants.ENTERPRISE_REGISTER:
				req = enterpriseRegist(type, platformId, platformMemberId, json, argMerCode, summary, error);
				break;
			case YEEConstants.TRANSFER_USER_TO_USER:
				req = transferUserToUsers(type, platformId, platformMemberId, json, jsonXtraPara, argMerCode, summary, error);
				break;
			}
		}
		// 处理换行以及回车换行
		req=req.replace("\r\n", "");
		req=req.replace("\n", "");
		/*将使用平台的请求信息转化成第三方支付的请求参数*/
        String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
        
        argsMap.put("req", req);
        argsMap.put("sign", sign);
        argsMap.put("url", url);
        Logger.info("********rep:*******" + req);
//        YeeTools.wpReqParams(type,platformMemberId,"请求参数至易宝", argsMap);
        YeeToolsExtra.recordReqParams(type, platformMemberId, "请求参数至易宝", argsMap, json, jsonXtraPara);
		return argsMap;
	}
	
	/**
	 * 处理第三方传过来的参数，然后处理并请求给P2P（异步回调）
	 * @param resp
	 * @param sign
	 * @return
	 */
	public static String notifyExit(String resp, ErrorInfo error){
		error.clear();
		JSONObject json = (JSONObject)Converter.xmlToObj(resp);
		Map<String, String> args = new HashMap<String, String>();
		Map<String, String> remarkMap = null;
		
		Logger.debug("------------------------处理第三方传过来的参数json=-"+json+"-----------------------------");

		if(json.containsKey("@platformNo")){
			json.put("platformNo", json.getString("@platformNo"));
		}
		
		//自动还款签约回调通知没有requestNo流水号
		if (json.containsKey("bizType") && "AUTHORIZE_AUTO_REPAYMENT".equalsIgnoreCase(json.getString("bizType"))) {
			remarkMap = DealDetail.queryEventsByOrderNo(json.getString("platformNo"), json.getString("orderNo"), error);
			//修改手机号没有requestNo流水号
		}else if (json.containsKey("bizType") && "RESET_MOBILE".equalsIgnoreCase(json.getString("bizType"))) {
			remarkMap = new HashMap<String, String>();
			remarkMap.put("type", YEEConstants.UPDATE_MOBILE + "");
			remarkMap.put("memberId", json.getString("platformUserNo"));
			remarkMap.put("pS2SUrl", YEEConstants.SP2P_MOBILE_S2SURL);
		}else {
			//查询备份数据库的数据t_member_events或t_member_details
			if(null != DealDetail.queryEvents(json.getString("platformNo"), json.getString("requestNo"), error)){
				remarkMap = DealDetail.queryEvents(json.getString("platformNo"), json.getString("requestNo"), error);
			}else{
				remarkMap = DealDetail.queryDetails(json.getString("platformNo"), json.getString("requestNo"), error);
			}
		}
		
		if (null == remarkMap || null == remarkMap.get("type")) {
			
			return "";
		}
		
		int type = Integer.parseInt(remarkMap.get("type"));
		
		//因为快捷充值的时候会回调绑卡的接口
		if (type == YEEConstants.RECHARGE && json.containsKey("cardNo")) {
			type = YEEConstants.BOUND_CARD;
		}
		
		switch(type){
		case YEEConstants.CREATE_ACCOUNT:
			args = registerCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_SUBJECT:
			args = toAuthorizeAutoRepaymentCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CREDITOR:
			args = investBidCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CRETANSFER:
			args = transferCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_SIGING:
			args = autoInvestBidCall(json, remarkMap);
			break;
		case YEEConstants.REPAYMENT_SIGNING:
			args = autoPaymentCall(json, remarkMap);
			break;
		case YEEConstants.RECHARGE:
			args = rechargeCall(json, remarkMap);
			
			//充值异步回调
			Logger.debug("*******************充值异步回调sp2p参数：%s********************************", args);
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			
			Logger.debug("******************充值异步回调p2p结果:%s********",result);
			
			break;
		case YEEConstants.REPAYMENT:
			sleep(5);//休眠 5s
			args = PaymentCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_REPAYMENT:
			args = autoPaymentConfirmCall(json, remarkMap);
		case YEEConstants.WITHDRAWAL:
			args = withdrawCall(json, remarkMap);
			break;
		case YEEConstants.BOUND_CARD:
			updateCardStatus(remarkMap, error);
		    break;
		case YEEConstants.TRANSFER_USER_TO_MER:
			sleep(5);//休眠 5s
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USERS:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USER:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.UNFREZZ_AMOUNT:
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.COMPENSATORYREPAYMENT:
			args = compensatoryRepaymentCall(json, remarkMap);
			break;
		case YEEConstants.UPDATE_MOBILE:
			args = updateMobileCall(json, remarkMap);
			break;
		case YEEConstants.TRANSFER_USER_TO_USER:
			args = transferUserToUsersCall(json, remarkMap);
			break;
		}
		
		Logger.debug("=========================================================================操作类型type:%s", type);
		//绑卡后需要在p2p平台添加一条用户银行卡信息，通过WS请求P2P平台在P2P平台添加银行卡信息(外网测试)
		if(type == YEEConstants.BOUND_CARD){
			if (json.containsKey("code") && json.getInt("code") == 1) {
				//获取第三方的参数值
				String pMerBillNo = json.getString("requestNo");
				String memberId = json.getString("platformUserNo");
				String pBankName = json.getString("bank");
				String pBankCard = json.getString("cardNo");
				
				//构造回调p2p的参数
				String pMerCode = json.getString("platformNo");
				String pErrCode = json.getString("code");
				String pErrMsg = json.containsKey("message") ? json.getString("message") : "";
				
				//构造回调p2p的加密串
				JSONObject jsonOb = new JSONObject();
				jsonOb.put("pMerBillNo", pMerBillNo);
				jsonOb.put("memberId", memberId);
				jsonOb.put("pBankName", pBankName);
				jsonOb.put("pBankCard", pBankCard);
				
				String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
				
				//构造回调p2p的签名
				String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
				
				//构造回调p2p的url参数
				Map<String, String> argsMap = new HashMap<String, String>();
				argsMap.put("pMerCode", pMerCode);
				argsMap.put("pErrCode", pErrCode);
				argsMap.put("pErrMsg", pErrMsg);
				argsMap.put("p3DesXmlPara", p3DesXmlPara);
				argsMap.put("pSign", pSign);
				
				//ws请求p2p绑卡接口
				String pS2SUrl = remarkMap.get("pS2SUrl");
				pS2SUrl = pS2SUrl.substring(0, pS2SUrl.lastIndexOf("/")) + "/boundCardCBSys";
				
				Integer status = WS.url(pS2SUrl).setParameters(argsMap).post().getStatus();
				if (status == 200) {
					return "1";
				}
				
				return "-1";
			}
		}
		
		String code = null;
		
		if(type == YEEConstants.REPAYMENT || type == YEEConstants.WITHDRAWAL || type == YEEConstants.REGISTER_CREDITOR || type == YEEConstants.AUTO_REPAYMENT){
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			JSONObject jsonReturn = JSONObject.fromObject(result);
			code = jsonReturn.getString("code");
			if(type == YEEConstants.REPAYMENT&&"-11".equals(code)){
				code = "1";
			}
			
			if(type== YEEConstants.REGISTER_CREDITOR){
				if("-10".equals(jsonReturn.getString("code"))){  //金额超标
					String billNo = jsonReturn.getString("oldMerBillNo");
					boolean status = confirmTranferResultByTranser(YeeConfig.getProperty("yee_merCustId"), billNo, "CANCEL");
					String pErrMsg = "解冻资金成功";
					code = "1";
					if(!status){
						code = "-1";
						pErrMsg = "解冻资金失败";
					}
					Logger.info(pErrMsg);
				}
			}
			
			return code;
		}
		
		//自动还款签约异步回调
		if (YEEConstants.REPAYMENT_SIGNING == type) {
			//异步回调p2p
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			Logger.debug("******************自动签约异步回调p2p结果:%s********",result);
		}
		
		//登记债权转让授权异步回调p2p
		if (YEEConstants.REGISTER_CRETANSFER == type) {
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			Logger.debug("******************登记债权转让授权异步回调p2p结果:%s********",result);
		}
		
		//开户
		if (YEEConstants.CREATE_ACCOUNT == type) {
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			Logger.debug("******************开户异步回调p2p结果:%s********",result);
		}
		
		//修改手机号码
		if (YEEConstants.UPDATE_MOBILE == type) {
			Logger.debug("*******************修改手机异步回调sp2p参数：%s********************************", args);
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
				
			Logger.debug("******************修改手机号码异步回调p2p结果:%s********",result);
		}
		
		//企业注册
		if (YEEConstants.ENTERPRISE_REGISTER == type) {
			Logger.debug("*******************企业注册异步回调sp2p参数：%s********************************", args);
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
				
			Logger.debug("******************企业注册异步回调p2p结果:%s********",result);
		}
		
		if (YEEConstants.TRANSFER_USER_TO_USER == type) {
			Logger.debug("*******************用户转用户异步回调sp2p参数：%s********************************", args);
			String result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
				
			Logger.debug("******************用户转用户异步回调p2p结果:%s********",result);
		}
		
		if(json.getString("code").equals("1")){
			code = "1";
			
		}else{
			code = "-1";
		}
		
		return code;
	}
	
	/**
	 * 处理第三方传过来的参数，然后处理并请求给P2P（同步回调）
	 * @param resp
	 * @param sign
	 * @return
	 */
	public static Map<String, String> exit(String resp, ErrorInfo error){
		error.clear();
		JSONObject json = (JSONObject)Converter.xmlToObj(resp);
		Map<String, String> args = new HashMap<String, String>();
		Map<String, String> remarkMap = null;
		
		Logger.debug("YEE->exit->>>处理第三方传过来resp格式化的参数json:"+json+"\n");

		if(json.containsKey("@platformNo")){
			json.put("platformNo", json.getString("@platformNo"));
		}
		
		//查询备份数据库的数据
		if(null != DealDetail.queryEvents(json.getString("platformNo"), json.getString("requestNo"), error)){
			remarkMap = DealDetail.queryEvents(json.getString("platformNo"), json.getString("requestNo"), error);
		}else{
			remarkMap = DealDetail.queryDetails(json.getString("platformNo"), json.getString("requestNo"), error);
		}
		
		if(remarkMap!= null && remarkMap.containsKey("memberId")){
			remarkMap.put("pMemo1", remarkMap.get("memberId"));
		}
		Logger.debug("YEE->exit->>>t_member_details->>>remarkMap:"+remarkMap.toString());
		
		int type = Integer.parseInt(remarkMap.get("type"));
		Logger.debug("YEE->exit->>>操作类型:\t"+type);
		
		switch(type){
		case YEEConstants.CREATE_ACCOUNT:
			args = registerCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_SUBJECT:
			args = toAuthorizeAutoRepaymentCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CREDITOR:
			args = investBidCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CRETANSFER:
			args = transferCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_SIGING:
			args = autoInvestBidCall(json, remarkMap);
			break;
		case YEEConstants.REPAYMENT_SIGNING:
			args = autoPaymentCall(json, remarkMap);
			break;
		case YEEConstants.RECHARGE:
			args = rechargeCall(json, remarkMap);
			break;
		case YEEConstants.REPAYMENT:
			args = PaymentCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_REPAYMENT:
			args = autoPaymentConfirmCall(json, remarkMap);
			break;
		case YEEConstants.WITHDRAWAL:
			args = withdrawCall(json, remarkMap);
			break;
		case YEEConstants.BOUND_CARD:
			updateCardStatus(remarkMap, error);
			args = Bound(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_USER_TO_MER:
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USERS:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USER:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.UNFREZZ_AMOUNT:
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.UNBOUND_CARD:
			updateCardStatusUnBound(remarkMap, error);
			args = unBound(json, remarkMap);
		    break;
		case YEEConstants.COMPENSATORYREPAYMENT:
			args = compensatoryRepaymentCall(json,remarkMap);
			break;
		case YEEConstants.UPDATE_PAY_PASS:
			args = resPass(json,remarkMap);
			break;
		case YEEConstants.UPDATE_MOBILE:
			args = updateMobileCall(json, remarkMap);
			break;
		case YEEConstants.ENTERPRISE_REGISTER:
			args = enterpriseRegistCall(json, remarkMap);
			break;
		case YEEConstants.TRANSFER_USER_TO_USER:
			args = transferUserToUsersCall(json, remarkMap);
			break;
		}
		
		Logger.debug("=========================================================================操作类型type:%s", type);
		//还款||提现||登记债权人(投标)
		if(type == YEEConstants.REPAYMENT || type == YEEConstants.WITHDRAWAL || type == YEEConstants.REGISTER_CREDITOR){
			Logger.debug("YEE->exit->>>还款||提现||登记债权人（投标）");
			String result = null;
			JSONObject jsonReturn = null;
			Logger.debug("pS2SUrl:%s", remarkMap.get("pS2SUrl"));
			//WS请求访问P2P平台
			result = WS.url(remarkMap.get("pS2SUrl")).setParameters(args).post().getString();
			
			jsonReturn = JSONObject.fromObject(result);
			
			if(type== YEEConstants.REGISTER_CREDITOR){
				if("-10".equals(jsonReturn.getString("code"))){  //金额超标
					String billNo = jsonReturn.getString("oldMerBillNo");
					boolean status = confirmTranferResultByTranser(YeeConfig.getProperty("yee_merCustId"), billNo, "CANCEL");
					String pErrCode  = "MG00000F";
					String pErrMsg = "解冻资金成功";
					if(!status){
						pErrCode  = "MG00001F";
						pErrMsg = "解冻资金失败";
					}
					Logger.info(pErrMsg);
					String pMerCode = YeeConfig.getProperty("yee_merCustId");
					String p3DesXmlPara= buildP3DesXmlPara(jsonReturn);
					String pSign = haxMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
					args.clear();
					jsonReturn.put("pMerCode",pMerCode );
					jsonReturn.put("pErrMsg", pErrMsg);
					jsonReturn.put("pErrCode", pErrCode);
					jsonReturn.put("p3DesXmlPara", p3DesXmlPara);
					jsonReturn.put("pSign",pSign);
					args.put("url", jsonReturn.getString("pPostUrl"));
				}
			}
			
			jsonReturn.put("pMerBillNo",remarkMap.get("pMerBillNo"));
			
			Logger.debug(type+"->>>WS请求返回信息"+jsonReturn);
			
			String returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
			
			//还款转账确认
			if(type == YEEConstants.REPAYMENT){ }
			
			args.clear();
			args.put("postMark", "true");
			args.put("url", jsonReturn.getString("pPostUrl"));
			args.put("result", returnResult);
			
			return args;
		}
		
		//如果是债权债权转转账确认
		if(type == YEEConstants.REGISTER_CRETANSFER){
			
			if (StringUtils.isNotBlank(remarkMap.get("pMemo2")) && "1".equals(remarkMap.get("pMemo2"))) {
				//定向转让模式才会确认债权转让
				Map<String,Object> userMap = new HashMap<String,Object>();
				JSONObject jsonOb = new JSONObject(); 
				jsonOb.put("requestNo", json.getString("requestNo"));
				jsonOb.put("mode", "CONFIRM");
				jsonOb.put("notifyUrl", "notifyUrl");
				String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
				req = YEEUtil.addAttribute(json.getString("@platformNo"), req);
				
				Logger.debug("债权债权转转账确认req:="+req);
				
				userMap.clear();
				userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
				userMap.put("req", req);
				userMap.put("service", "COMPLETE_TRANSACTION");
				//直连接口处理转账确认
				String[] resultarr = new String[2]; 
				resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
				Logger.debug("债权债权转转账确认返回信息0:"+resultarr[0]);
				
				JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
				Logger.debug("债权债权转转账确认返回信息1:jsonResult="+jsonResult);
			}
		}
		
		Logger.debug("YEE-exit->>>非还款、提现、登记债权人(投标)情况");
		args.put("url", remarkMap.get("pWebUrl"));
		args.put("redictMark", "false");
		
		return args;
	}
	
	
	/**
	 * 绑卡
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	private static Map<String, String> Bound(JSONObject json,
			Map<String, String> remarkMap) {
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------绑卡回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("memberId", remarkMap.get("memberId"));
		//绑卡后，查询易宝绑卡的状态
		String result=queryUserInfo(json.getString("platformNo"), Convert.strToLong(remarkMap.get("memberId"), 0));
		JSONObject jsonResult=JSONObject.fromObject(result);
		jsonOb.put("pStatus", jsonResult.getString("pStatus"));
		jsonOb.put("pBankName", jsonResult.getString("pBankName"));
		jsonOb.put("pBankCard", jsonResult.getString("pBankCard"));
		jsonOb.put("isCardBind", "Y");
		jsonOb.put("isApp", remarkMap.get("isApp"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		
		Logger.debug("------------------------绑卡回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}

	/**
	 * 取消绑卡
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	private static Map<String, String> unBound(JSONObject json, Map<String, String> remarkMap) {
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------取消绑定回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("memberId", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------取消绑定回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	private static Map<String, String> resPass(JSONObject json, Map<String, String> remarkMap) {
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------取消绑定回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("memberId", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------重置密码回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}

	/**
	 * 修改成绑卡的状态
	 * @param remarkMap
	 * @param error
	 */
	private static void updateCardStatus(Map<String, String> remarkMap, ErrorInfo error){
		int rusult = MemberOfPlatform.updateCardStatus(Long.parseLong(remarkMap.get("memberId")), error);
		
		Logger.debug("------------------------修改成绑卡的状态返回结果==:"+rusult+"-------------------------------");
	}
	
	private static void updateCardStatusUnBound(Map<String, String> remarkMap, ErrorInfo error){
		int rusult = MemberOfPlatform.updateCardStatusUnBound(Long.parseLong(remarkMap.get("memberId")), error);
		
		Logger.debug("------------------------取消绑卡的状态返回结果==:"+rusult+"-------------------------------");
	}
	
	/**
	 * 提现回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> withdrawCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------提现回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pMemo3", remarkMap.get("pMemo3"));
		jsonOb.put("isCardBind","N");
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------提现回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 用户转商户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> userToMerCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------用户转商户回调-------------------------------");
		
		if(json.getString("code").equals("1")){
			String platformNo = remarkMap.get("domain");
			String requestNo= remarkMap.get("pMerBillNo");
			String mode = "CONFIRM";
			boolean result = confirmTranferResult(platformNo,requestNo,mode);
		    pErrCode = "MG00000F";
		    if(!result){
		    	pErrCode = "MG00001F";
		    }
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("TransAmt", remarkMap.get("TransAmt"));  //交易金额
		jsonOb.put("UsrCustId", remarkMap.get("UsrCustId"));  //入账客户号
		jsonOb.put("pMemo1", remarkMap.get("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------用户转商户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 取消投标解冻资金回调P2P(待运松提交P2P代码)
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> unfrezzCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------取消投标解冻资金回调------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("TransAmt", remarkMap.get("amount"));  //交易金额
		jsonOb.put("UsrCustId", json.getString("requestNo"));  //入账客户号
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------取消投标解冻资金回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 商户转用户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> merToUserCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------商户转用户回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("pMemo1", remarkMap.get("pMemo1"));  //交易金额
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------商户转用户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 开户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> registerCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------开户回调-------------------------------");
		
		if(json.getString("code").equals("1") || json.getString("code").equals("101")){
			jsonOb.put("pStatus", "10");
		    pErrCode = "MG00000F";
			
		}else{
			jsonOb.put("pStatus", "9");
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAcctNo", remarkMap.get("memberId"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------开户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 自动还款授权并处理P2P那边标的登记需要的参数，然会回调
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> toAuthorizeAutoRepaymentCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pBidNo", remarkMap.get("pBidNo"));
		jsonOb.put("pOperationType", remarkMap.get("pOperationType"));
		jsonOb.put("pMemo3", remarkMap.get("pMemo3"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pIpsBillNo", "");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5(json.getString("platformNo")+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return map;
	}
	
	/**
	 *标的登记回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> bidCall(String argMerCode, JSONObject json, JSONObject jsonXtraPara, long menberId){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonOb.put("pBidNo", json.getString("pBidNo"));
		jsonOb.put("pOperationType", json.getString("pOperationType"));
		jsonOb.put("pMemo3", json.getString("pMemo3"));
		jsonOb.put("pMemo1", menberId+"");
		jsonOb.put("pIpsBillNo", "");
		
		Logger.debug("------------------------标的登记回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------标的登记P2P回调==:"+map+"-------------------------------");
		
//		JSONObject jsonReturn = JSONObject.fromObject(jsonResult);
//		
//		Logger.debug("------------------------标的登记WS回调P2P获取返回信息jsonReturn=:"+jsonReturn.toString()+"-------------------------------");
//		
//		String result = Encrypt.encrypt3DES(jsonResult.toString(), Constants.ENCRYPTION_KEY);
//		
//		map.clear();
//		map.put("url", jsonReturn.getString("pPostUrl"));
//		map.put("result", result);
		
		return map;
	}
	
	/**
	 *充值回调P2P（pd测试的时候用）
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> rechargeCall(String argMerCode, JSONObject json, JSONObject jsonXtraPara, long menberId){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonOb.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonOb.put("pMemo1", menberId+"");
		
		Logger.debug("------------------------充值（pd测试的时候用）回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------充值（pd测试的时候用）回调==:"+map+"-------------------------------");
		
		String jsonResult = WS.url(jsonXtraPara.getString("pWSUrl")).setParameters(map).post().getString();
		JSONObject jsonReturn = JSONObject.fromObject(jsonResult);
		
		Logger.debug("------------------------充值WS回调P2P获取返回信息jsonReturn=:"+jsonReturn.toString()+"-------------------------------");
		
		String result = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
		
		map.clear();
		map.put("url", jsonReturn.getString("pPostUrl"));
		map.put("result", result);
		
		return map;
	}
	
	/**
	 *流标（WS）
	 * @param json
	 * @param remarkMap
	 * @return
	 */
//	public static Map<String, String> flowCall(String argMerCode, JSONObject json, long menberId,
//			JSONObject jsonXtraPara, String summary){
//		JSONObject jsonOb = new JSONObject();
//		Map<String, String> map = new HashMap<String, String>();
//		String pErrCode = "";
//		int hasPublished =0;
//		JSONArray jsonArr = null;
//        Object pDetails = jsonXtraPara.get("pDetails");
//        
//        Logger.debug("------------------------流标请求易宝参数json=:"+json.toString()+"-------------------------------");
//        Logger.debug("------------------------流标请求易宝额外参数json=:"+jsonXtraPara.toString()+"-------------------------------");
//        
//		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
//			JSONObject pDetail = (JSONObject)pDetails; 
//			JSONObject pRow = pDetail.getJSONObject("pRow"); 
//	
//			jsonArr = new JSONArray(); 
//			jsonArr.add(pRow); 
//		} else {
//			jsonArr = jsonXtraPara.getJSONArray("pDetails");
//		} 
//		
//		int size = jsonArr.size();
//		
//		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
//		for (Object obj : jsonArr) {
//			JSONObject pRow = (JSONObject)obj;
//			JSONObject jsonObj = new JSONObject();
//        	Map<String, Object> mapObj = new HashMap<String, Object>();
//        	
//        	jsonObj.put("requestNo", pRow.getString("ipsBillNo"));
//        	jsonObj.put("platformUserNo", pRow.getString("investUserId"));
//        	
//        	String req = Converter.jsonToXml(jsonObj.toString(), "request", null, null, null);
//    		req = YEEUtil.addAttribute(argMerCode, req);
//    		
//    		mapObj.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//    		mapObj.put("req", req);
//    		mapObj.put("service", "REVOCATION_TRANSFER");
//    		
//    		//WS请求直接返回处理结果
//    		String[] resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, mapObj);
//    		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
//    		
//    		Logger.debug("------------------------流标第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
//    		
//    		if(jsonResult.getString("code").equals("1") ){
//    			hasPublished++;
//    		}
//	      }
//		
//        if(hasPublished == size){
//        	pErrCode = "MG00000F";
//        	
//        }else{
//        	pErrCode = "MG02035F";
//        }
//        
//        jsonOb.put("pOperationType", json.get("pOperationType"));
//        jsonOb.put("pMerBillNo", json.get("pMerBillNo"));
//        jsonOb.put("pIpsBillNo", jsonXtraPara.getString("freezeTrxId"));
//        jsonOb.put("pMemo3", json.get("pMemo3"));
//        jsonOb.put("pBidNo", json.get("pBidNo"));
//        jsonOb.put("pMemo1", menberId+"");
//		
//		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
//		map.put("pMerCode", argMerCode);
//		map.put("pErrMsg", "");
//		map.put("pErrCode", pErrCode);
//		map.put("p3DesXmlPara", p3DesXmlPara);
//		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
//		
//		Logger.debug("------------------------流标回调P2P参数map=:"+map+"-------------------------------");
//		
////		String jsonResult = WS.url(jsonXtraPara.getString("pWSUrl")).setParameters(map).post().getString();
////		JSONObject jsonReturn = JSONObject.fromObject(jsonResult);
////		
////		Logger.debug("------------------------流标WS回调P2P获取返回信息jsonReturn=:"+jsonReturn.toString()+"-------------------------------");
////		
////		String result = Encrypt.encrypt3DES(jsonReturn.toString(), Constants.ENCRYPTION_KEY);
////		
////		map.clear();
////		map.put("url", jsonReturn.getString("pPostUrl"));
////		map.put("result", result);
//		
//		return map;
//	}
	public static Map<String, String> flowCall(String argMerCode, JSONObject json, long menberId,
			JSONObject jsonXtraPara, String summary){
		JSONObject jsonOb = new JSONObject();
		Map<String, String> map = new HashMap<String, String>();
		String pErrCode = "";
		int hasPublished =0;
		JSONArray jsonArr = null;
        
        Logger.debug("------------------------流标请求易宝参数json=:"+json.toString()+"-------------------------------");
        Logger.debug("------------------------流标请求易宝额外参数json=:"+jsonXtraPara.toString()+"-------------------------------");
        String pBillNos[] = (jsonXtraPara.get("investInfo").toString()).split(",");
		
        Map<String, Object> userMap = new HashMap<String, Object>();
        int pBillNoSize = pBillNos.length;
        boolean flag = false;
        if(!"[]".equals(pBillNos[0])){
		//遍历数组
		for (int i = 0; i < pBillNoSize; i++) {
			
			JSONObject jsonObj = new JSONObject();
			
			jsonObj.put("requestNo", pBillNos[i]);
			jsonObj.put("mode", "CANCEL");
			jsonObj.put("notifyUrl", "notifyUrl");
        	String req = Converter.jsonToXml(jsonObj.toString(), "request", null, null, null);
    		req = YEEUtil.addAttribute(argMerCode, req);
    		
    		Logger.debug("req:="+req);
    		
    		userMap.clear();
    		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
    		userMap.put("req", req);
    		userMap.put("service", "COMPLETE_TRANSACTION");
    		
    		//WS请求直接返回处理结果
    		String[] resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
    		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
    		
    		Logger.debug("------------------------流标第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
    		
    		if(jsonResult.getString("code").equals("1") ){
    			hasPublished++;
    		}
	      }
        }else{
        	flag = true;
        }
		
        if((hasPublished == pBillNos.length)| flag ){
        	pErrCode = "MG00000F";
        	
        }else{
        	pErrCode = "MG02035F";
        }
        
        jsonOb.put("pOperationType", json.get("pOperationType"));
        jsonOb.put("pMerBillNo", json.get("pMerBillNo"));
        jsonOb.put("pIpsBillNo", jsonXtraPara.getString("freezeTrxId"));
        jsonOb.put("pMemo3", json.get("pMemo3"));
        jsonOb.put("pBidNo", json.get("pBidNo"));
        jsonOb.put("pMemo1", menberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------流标回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 *自动还款签约(此签约只是回应P2P的秒还借款，不是真正的签约，真正的签约只有在发标的时候才会去授权)
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> repaymentSigning(String argMerCode, JSONObject json, long menberId){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonOb.put("pIpsAuthNo", json.getString("pMerBillNo"));
		jsonOb.put("pMemo1", menberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return map;
	}
	
	/**
	 * 登记债权人回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> investBidCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "MG02504F";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pP2PBillNo",json.getString("requestNo"));//加该字段只是因为P2P接口需要用到“环迅”接口传过来的参数，这里用请求流水号代替
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
//		jsonOb.put("pFee", "0");
		jsonOb.put("pFee", remarkMap.get("serviceFee"));
		
		
		Logger.debug("------------------------YEE->investBidCall->登记债权人回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------YEE->investBidCall->登记债权人map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 还款回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> PaymentCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
			String platformNo = remarkMap.get("domain");
			String requestNo= remarkMap.get("pMerBillNo");
			String mode = "CONFIRM";
			boolean result = confirmTranferResult(platformNo,requestNo,mode);
		    pErrCode = "MG00000F";
		    if(!result){
		    	pErrCode = "MG00001F";
		    }
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMemo1", remarkMap.get("memberId"));//还款人id
		jsonOb.put("pMemo3",remarkMap.get("pMemo3"));
		jsonOb.put("pMerBillNo",json.getString("requestNo"));
		
		Logger.debug("------------------------还款回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------还款回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 自动还款回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> autoPaymentConfirmCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
			String platformNo = remarkMap.get("domain");
			String requestNo= remarkMap.get("pMerBillNo");
			String mode = "CONFIRM";
			boolean result = confirmTranferResult(platformNo,requestNo,mode);
		    pErrCode = "MG00000F";
		    if(!result){
		    	pErrCode = "MG00001F";
		    }
			
		}else{
			pErrCode = "";
		}
		
		jsonOb.put("pMemo1", remarkMap.get("memberId"));//还款人id
		jsonOb.put("pMemo3",remarkMap.get("pMemo3"));
		jsonOb.put("pMerBillNo",json.getString("requestNo"));
		
		Logger.debug("------------------------自动还款回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------还款回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
		
	/**
	 * “自动投标授权”回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> autoInvestBidCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAuthNo", json.getString("requestNo"));//本字段是p2p方用来标记“环迅”接口，此处用流水号代替
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		Logger.debug("------------------------自动投标授权p3DesXmlPara=:"+jsonOb.toString()+"-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------自动投标授权map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * “自动还款授权”回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> autoPaymentCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", remarkMap.get("pMerBillNo"));
		jsonOb.put("pIpsAuthNo", remarkMap.get("pMerBillNo"));
//		jsonOb.put("pMerBillNo", json.getString("requestNo"));
//		jsonOb.put("pIpsAuthNo", json.getString("requestNo"));//本字段是p2p方用来标记“环迅”接口，此处用流水号代替
		jsonOb.put("pMemo1", remarkMap.get("orderNo"));
		
		
		Logger.debug("------------------------自动还款授权回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5(json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		Logger.debug("------------------------------pSign" + Encrypt.MD5(json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY) +"-------------------------------------");
		Logger.debug("------------------------自动还款授权回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 登记债权转让回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> transferCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		Logger.debug("------------------------登记债权转让回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------登记债权转让回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 充值回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> rechargeCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pTrdAmt", remarkMap.get("amount"));
		
		Logger.debug("------------------------充值回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------充值回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 用户注册
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String register(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户注册P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("realName", Base64Util.decoder(json.getString("pRealName")));
		userMap.put("idCardType", "G2_IDCARD");
		userMap.put("idCardNo", json.getString("pIdentNo"));
		userMap.put("mobile", json.getString("pMobileNo"));
//		userMap.put("email", json.getString("pEmail"));      
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "开户");
		Logger.debug("------------------------用户注册请求易宝参数req=:"+req);
		
		return req;
	}
	
	/**
	 * 用户充值
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String recharge(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		Logger.debug("------------------------用户充值参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pTrdAmt"));
		userMap.put("feeMode", "PLATFORM");  //费率模式
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.RECHARGE, json.getDouble("pTrdAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致添加充值交易记录失败";
			
			return null;
		}
		
		Logger.debug("------------------------用户充值请求易宝req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 绑卡
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String bindBankCard(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		error.clear();
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------绑卡P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, json.toString(), "绑卡");
		
		Logger.debug("------------------------绑卡请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 取消绑卡
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param platformMemberId2 
	 * @param platformId 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @param error 
	 * @param summary 
	 * @return
	 */
//	public static String unBindBankCard(long platformMemberId, JSONObject json, String argMerCode){
//		Map<String, String> userMap = new HashMap<String, String>();
//		JSONObject jsonOb = new JSONObject(); 
//		
//		userMap.put("platformUserNo", Long.toString(platformMemberId));
//		userMap.put("requestNo", json.getString("pMerBillNo"));
//		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
//		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
//		jsonOb.putAll(userMap);
//		
//		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//		req = YEEUtil.addAttribute(argMerCode, req);
//		
//		return req;
//	}
	public static String unBindBankCard(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户取消绑定P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "取消绑卡");
		
		Logger.debug("------------------------用户取消绑定请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	
	/**
	 * 修改支付密码
	 * @param type
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @param argMerCode
	 * @param summary
	 * @param error
	 * @return
	 */
	public static String updatePayPass(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error) {
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户重置密码P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "重置密码");
		
		Logger.debug("------------------------用户取消绑定请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 修改手机号码
	 * @param type
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @param argMerCode
	 * @param summary
	 * @param error
	 * @return
	 */
	public static String updateMobile(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户重置密码P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", json.getString("platformUserNo"));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "重置手机号码");
		
		Logger.debug("------------------------用户修改手机号码请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 修改手机号码回调
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> updateMobileCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------修改手机号码回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		}else{
			pErrCode = "";
		}
		
		String mobile = json.containsKey("mobile") ? json.getString("mobile") : "";
		String requestNo = json.containsKey("requestNo") ? json.getString("requestNo") : "";
		
		jsonOb.put("pMerBillNo", requestNo);
		jsonOb.put("pIpsAcctNo", remarkMap.get("memberId"));
		jsonOb.put("pMemo1", mobile);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5(json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------修改手机号码回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 企业注册
	 * @param type
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @param argMerCode
	 * @param summary
	 * @param error
	 * @return
	 */
	public static String enterpriseRegist(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		error.clear();
		
		Map<String, String> map = new HashMap<>();
		map.put("platformNo", argMerCode);
		map.put("requestNo", json.getString("pMerBillNo"));
		map.put("platformUserNo", platformMemberId + "");
		map.put("enterpriseName", json.getString("enterpriseName"));
		map.put("bankLicense", json.getString("bankLicense"));
		map.put("orgNo", json.getString("orgNo"));
		map.put("businessLicense", json.getString("businessLicense"));
		map.put("taxNo", json.getString("taxNo"));
		map.put("legal", json.getString("legal"));
		map.put("legalIdNo", json.getString("legalIdNo"));
		map.put("contact", json.getString("contact"));
		map.put("contactPhone", json.getString("contactPhone"));
		map.put("email", json.getString("email"));
		map.put("memberClassType", json.getString("memberClassType"));
		map.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		map.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		JSONObject jsonOb = new JSONObject();
		jsonOb.putAll(map);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		Logger.debug("*****************************企业注册第三方参数：%s**********************************", jsonOb.toString());
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, json.toString(), "企业注册");

		return req;
	}
	
	/**
	 * 企业用户回调
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> enterpriseRegistCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------企业注册回调spay-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		}else{
			pErrCode = "";
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAcctNo", json.containsKey("platformUserNo") ? json.getString("platformUserNo") : remarkMap.get("memberId"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pMemo2", remarkMap.get("pMemo2"));//企业用户id
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5(json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------企业注册回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 债权转让(转让成功)--对应P2P的登记债权转让接口
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
//	public static String transfer(int platformId, long platformMemberId, JSONObject json,
//			String argMerCode, String summary, ErrorInfo error){
//		Map<String, String> userMap = new HashMap<String, String>();
//		JSONObject jsonOb = new JSONObject(); 
//		
//		Logger.debug("------------------------债权转让P2P参数json=:"+json.toString()+"-------------------------------");
//		
//		userMap.put("platformUserNo", Long.toString(platformMemberId));
//		userMap.put("requestNo", json.getString("pMerBillNo"));
//		userMap.put("amount", json.getString("pPayAmt"));  //债权购买人出资的金额
//		userMap.put("orderNo", json.getString("pBidNo"));  //标的号
//		userMap.put("paymentRequestNo", json.getString("pCreMerBillNo"));  //投标请求流水号
//		userMap.put("fee", json.getString("pFromFee"));  //债权转让管理费
//		
//		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
//		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
//		jsonOb.putAll(userMap);
//		
//		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//		req = YEEUtil.addAttribute(argMerCode, req);
//		
//		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
//				YEEConstants.REGISTER_CRETANSFER, json.getDouble("pCretAmt"), false, summary);
//		
//		if(!detail.addDealDetail()){
//			error.code = -1;
//			error.msg = "数据库异常，导致添加债权转让交易记录失败";
//			
//			return null;
//		}
//		
//		Logger.debug("------------------------债权转让请求易宝参数req=:"+req+"-------------------------------");
//		
//		return req;
//	}
	public static String transfer(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		Logger.debug("YEE->transfer->债权转让P2P参数arg3DesXmlPara到spay取名json=:"+json.toString()+"\n");
		
		userMap.put("requestNo", json.getString("pMerBillNo"));//债权转让流水号
		userMap.put("platformUserNo", Long.toString(platformMemberId));//转让人ID
		userMap.put("userType", "MEMBER");
		userMap.put("bizType", "CREDIT_ASSIGNMENT");
		
		JSONArray details = new JSONArray();
		//转让人detail 原先债权人
		JSONObject detail1 = new JSONObject();
		detail1.put("targetUserType", "MEMBER");
		detail1.put("targetPlatformUserNo", json.getString("pFromAccount"));
		detail1.put("amount", Double.parseDouble(json.getString("pPayAmt"))-Double.parseDouble(json.getString("pFromFee")));
		detail1.put("bizType", "CREDIT_ASSIGNMENT");
		details.add(detail1);
		//平台抽成服务费detail
		JSONObject detail2 = new JSONObject();
		detail2.put("targetUserType", "MERCHANT");
		detail2.put("targetPlatformUserNo", argMerCode);
		detail2.put("amount", json.getString("pFromFee"));
		detail2.put("bizType", "COMMISSION");
		details.add(detail2);
		userMap.put("details", details);
		
		StringBuilder extendMSG = new StringBuilder();
		extendMSG.append("<property name=\"tenderOrderNo\" value=\""+json.get("pBidNo")+"\" />");//项目编号
		extendMSG.append("<property name=\"creditorPlatformUserNo\" value=\""+json.getString("pFromAccount")+"\" />");//债权购买人
		extendMSG.append("<property name=\"originalRequestNo\" value=\""+json.getString("pCreMerBillNo")+"\" />");//需要转让的投资记录流水号
		
		userMap.put("extend", extendMSG.toString());
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REGISTER_CRETANSFER, json.getDouble("pCretAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致添加债权转让交易记录失败";
			
			return null;
		}
		
		Logger.debug("YEE->transfer->>>债权转让请求易宝参数req=:\n"+req+"\n");
		
		return req;
	}
	
	/**
	 * 登记债权人
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @jsonXtraPara P2P在环迅之后添加的参数
	 * @return
	 */
//	public static String investBid(int platformId, long platformMemberId, JSONObject json,
//			String argMerCode, String summary, JSONObject jsonXtraPara, ErrorInfo error){
//		Map<String, String> userMap = new HashMap<String, String>();
//		Map<String, Object> map = new HashMap<String, Object>();
//		JSONObject jsonOb = new JSONObject(); 
//		
//		Logger.debug("------------------------登记债权人P2P参数json=:"+json.toString()+"-------------------------------");
//		Logger.debug("------------------------登记债权人P2P参数jsonXtraPara=:"+jsonXtraPara.toString()+"-------------------------------");
//		
//		userMap.put("platformUserNo", Long.toString(platformMemberId));
//		userMap.put("requestNo", json.getString("pMerBillNo"));
//		userMap.put("orderNo", json.getString("pBidNo"));  //订单号
//		userMap.put("transferAmount", jsonXtraPara.getString("transferAmount"));//标的借款额
//		userMap.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));//借款人编号
//		userMap.put("paymentAmount", json.getString("pAuthAmt"));//冻结金额（至少一元）
//		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
//		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
//		jsonOb.putAll(userMap);
//		
//		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//        req = YEEUtil.addAttribute(argMerCode, req);
//		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//		
//		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
//				YEEConstants.REGISTER_CREDITOR, json.getDouble("pAuthAmt"), false, summary);
//        
//		if(!detail.addDealDetail()){
//			error.code = -1;
//			error.msg = "数据库异常，导致添加投标交易记录失败";
//			
//			return null;
//		}
//		
//		Logger.debug("------------------------登记债权人请求易宝参数req=:"+req+"-------------------------------");
//		
//		return req;
//	}
	public static String investBid(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, JSONObject jsonXtraPara, ErrorInfo error){
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("登记债权人P2P发来的参数json=:"+json.toString()+"\n");
		Logger.debug("登记债权人P2P发来的参数jsonXtraPara=:"+jsonXtraPara.toString()+"\n");
		
		userMap.put("requestNo", json.getString("pMerBillNo"));//请求流水号
		userMap.put("platformUserNo", Long.toString(platformMemberId));//出款人平台用户编号
		userMap.put("userType", "MEMBER");//出款人用户类型
		userMap.put("bizType", "TENDER");//业务类型 		
//		userMap.put("expired", "");
		
		JSONArray details = new JSONArray();
		JSONObject detail1 = new JSONObject();
//		detail1.put("amount", json.getString("pAuthAmt"));
		detail1.put("amount", String.format("%.2f",Double.parseDouble(json.get("pAuthAmt").toString())-Double.parseDouble(jsonXtraPara.get("serviceFee").toString())));
		
		detail1.put("targetUserType", "MEMBER");
		detail1.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));
		detail1.put("bizType",  "TENDER");
		details.add(detail1);
		
		JSONObject detail2 = new JSONObject();
//		detail2.put("amount", "0");//平台抽成
		detail2.put("amount",Double.parseDouble(jsonXtraPara.get("serviceFee").toString()));
		detail2.put("targetUserType", "MERCHANT");
		detail2.put("targetPlatformUserNo", argMerCode);
		detail2.put("bizType",  "COMMISSION");
		details.add(detail2);
		userMap.put("details", details);

		StringBuilder extendMSG = new StringBuilder();
		extendMSG.append("<property name=\"tenderOrderNo\" value=\""+json.get("pBidNo")+"\" />");//项目编号
		extendMSG.append("<property name=\"tenderName\" value=\"tenderName"+jsonXtraPara.getString("loanerId")+"\" />");//项目名称
		extendMSG.append("<property name=\"tenderAmount\" value=\""+jsonXtraPara.getString("transferAmount")+"\" />");//项目金额
		extendMSG.append("<property name=\"tenderDescription\" value=\"tenderDescription"+jsonXtraPara.getString("loanerId")+"\" />");//项目描述
		extendMSG.append("<property name=\"borrowerPlatformUserNo\" value=\""+jsonXtraPara.getString("loanerId")+"\" />");//项目借款人平台用户编号

		userMap.put("extend", extendMSG.toString());
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		
        req = YEEUtil.addAttribute(argMerCode, req);//根节点添加商户编号属性
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		
		JSONObject remark = json2ExtraJsonAppend(json,jsonXtraPara);
		remark.put("type", YEEConstants.REGISTER_CREDITOR);
		remark.put("memberId", platformMemberId);
		remark.put("platformId", platformId);
		remark.put("pMemo1", platformMemberId);
		remark.put("pMemo2", "pMemo2");
		remark.put("pMemo3", "pMemo3");
		DealDetail.addEvent(platformMemberId, 3+200, platformId, json.getString("pMerBillNo"), null, null, remark.toString(), "登记债权人");
		//中间件平台添加详情记录t_member_details
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REGISTER_CREDITOR, json.getDouble("pAuthAmt"), false, summary);
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致添加投标交易记录失败";
			
			return null;
		}
		
		Logger.debug("登记债权人请求易宝参数需要的参数req=:"+req+"\n");
		
		return req;
	}
	
	/**
	 * 自动投标授权
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String autoBid(int type,int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------自动投标授权json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "");
		
		Logger.debug("------------------------自动投标授权请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 自动还款授权
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String autoPay(int type, int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("orderNo", json.getString("orderNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, json.getString("orderNo"));
		
		return req;
	}
	
	public static String repayment(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------还款json=:"+json.toString()+"-------------------------------");
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("platformUserNo", Long.toString(platformMemberId));//还款人id
		userMap.put("userType", "MEMBER");//还款人用户类型
		userMap.put("bizType", "REPAYMENT");//业务类型 还款
		//计算所有平台提成
		Double total_amount = 0.0;
		JSONArray jsonArr = null; 
		Object pDetails = json.get("pDetails");
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		
		JSONArray details = new JSONArray();
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry->计算总抽成
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			
			JSONObject detail1 = new JSONObject();
			detail1.put("targetUserType", "MEMBER");
			detail1.put("targetPlatformUserNo", pRow.getString("pInAcctNo"));
//			detail1.put("amount", pRow.getString("pInAmt")); //还款金额
			detail1.put("amount", String.format("%.2f", Double.parseDouble(pRow.getString("pInAmt"))-Double.parseDouble(pRow.getString("pInFee")))); //还款金额
			detail1.put("bizType", "REPAYMENT");
			details.add(detail1);
			//抽成
			total_amount += Double.parseDouble(pRow.getString("pInFee"));
			
			String serialNumber = json.getString("pMerBillNo")+"_"+pRow.getString("pCreMerBillNo");
			
			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pInAcctNo"), platformId), serialNumber, 
						YEEConstants.REPAYMENT, pRow.getDouble("pInAmt"), false, "收到还款");
				
				if (!detail.addDealDetail()){
					error.code = -1;
					error.msg = "数据库异常，导致还款失败";
					
					return null;
				}
			}
		}
		JSONObject detail2 = new JSONObject();
		detail2.put("targetUserType", "MERCHANT");
		detail2.put("targetPlatformUserNo", argMerCode);
		detail2.put("amount", String.format("%.2f",total_amount));
		detail2.put("bizType", "COMMISSION");
		details.add(detail2);
		userMap.put("details", details);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REPAYMENT, json.getDouble("pOutAmt"), false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致还款失败";
			
			return null;
		}
		
		StringBuilder extendMSG = new StringBuilder();
		extendMSG.append("<property name=\"tenderOrderNo\" value=\""+json.get("pBidNo")+"\" />");//项目编号

		userMap.put("extend", extendMSG.toString());//项目编号
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.debug("------------------------还款请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 自动还款授权
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @param argMerCode
	 * @param summary
	 * @param error
	 * @return
	 */
	public static String autoRepayment(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		error.clear();
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject();
		
		Logger.debug("------------------------自动还款json=:"+json.toString()+"-------------------------------");
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("platformUserNo", Long.toString(platformMemberId));//还款人id
		userMap.put("userType", "MEMBER");//还款人用户类型
		userMap.put("bizType", "REPAYMENT");//业务类型 还款
		//计算所有平台提成
		Double total_amount = 0.0;
		JSONArray jsonArr = null; 
		Object pDetails = json.get("pDetails");
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		
		JSONArray details = new JSONArray();
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry->计算总抽成
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			
			JSONObject detail1 = new JSONObject();
			detail1.put("targetUserType", "MEMBER");
			detail1.put("targetPlatformUserNo", pRow.getString("pInAcctNo"));
//			detail1.put("amount", pRow.getString("pInAmt")); //还款金额
			detail1.put("amount", String.format("%.2f", Double.parseDouble(pRow.getString("pInAmt"))-Double.parseDouble(pRow.getString("pInFee")))); //还款金额
			detail1.put("bizType", "REPAYMENT");
			details.add(detail1);
			//抽成
			total_amount += Double.parseDouble(pRow.getString("pInFee"));
			
			String serialNumber = json.getString("pMerBillNo")+"_"+pRow.getString("pCreMerBillNo");
			
			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pInAcctNo"), platformId), serialNumber, 
						YEEConstants.REPAYMENT, pRow.getDouble("pInAmt"), false, "收到还款");
				
				if (!detail.addDealDetail()){
					JPA.setRollbackOnly();
					error.code = -1;
					error.msg = "数据库异常，导致还款失败";
					
					return null;
				}
			}
		}
		
		JSONObject detail2 = new JSONObject();
		detail2.put("targetUserType", "MERCHANT");
		detail2.put("targetPlatformUserNo", argMerCode);
		detail2.put("amount", String.format("%.2f",total_amount));
		detail2.put("bizType", "COMMISSION");
		details.add(detail2);
		userMap.put("details", details);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REPAYMENT, json.getDouble("pOutAmt"), false, summary);
		
		if (!detail.addDealDetail()){
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据库异常，导致还款失败";
			
			return null;
		}
		
		StringBuilder extendMSG = new StringBuilder();
		extendMSG.append("<property name=\"tenderOrderNo\" value=\""+json.get("pBidNo")+"\" />");//项目编号
		userMap.put("extend", extendMSG.toString());//项目编号
		
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.debug("------------------------还款请求易宝参数req=:"+req+"-------------------------------");
		
		Map<String, String> maps = new HashMap<String, String>();
		maps.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		maps.put("req", req);
		maps.put("service", "AUTO_TRANSACTION");
		
		
		Map<String, Object> logMap = new HashMap<String, Object>();
		/*添加日志*/
		logMap.put("req", req);
		logMap.put("sign", SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		logMap.put("url", YEEConstants.YEE_URL_REDICT);
		YeeToolsExtra.recordReqParams(YEEConstants.AUTO_REPAYMENT, platformMemberId, "请求参数至易宝", maps, json, null);
		
		//WS请求直接返回处理结果
		String result = WS.url(YEEConstants.YEE_URL_REDICT).setParameters(maps).post().getString();
		Logger.debug("------------------------还款授权回调：%s" + result);
		jsonOb = (JSONObject) Converter.xmlToObj(result);
		if (!"1".equals(jsonOb.get("code"))) {
			
			return null;
		}
		
		jsonOb.put("requestNo", json.getString("pMerBillNo"));
		YEEPayment.callBack(Converter.jsonToXml(jsonOb.toString(), "response", null, null, null), SignUtil.sign(jsonOb.toString(), YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS), false);
		
		return "";
	}
	
	/**
	 * 用户转商户
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String userToMerchant(int type ,int platformId, long platformMemberId, JSONObject json, JSONObject extrajson,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户转商户json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));  //出款人平台用户编号
		userMap.put("requestNo", json.getString("pMerBillNo"));  //请求流水号
		userMap.put("userType", "MEMBER");  //出款人类型
		userMap.put("bizType", "TRANSFER");  //固定值TRANSFER
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		Map<String, String> properties = new HashMap<String, String>();
		
		properties.put("targetUserType", "MERCHANT");  //收款人用户类型
		properties.put("amount", extrajson.getString("TransAmt"));  //转入金额
		properties.put("targetPlatformUserNo", argMerCode);  //商户编号
		properties.put("bizType", "TRANSFER");  //商户编号
		arrJson.add(properties);
		JSONObject mark = json2ExtraJsonAppend(json,extrajson);
		DealDetail.addEvent(platformMemberId,32+200, platformId, json.getString("pMerBillNo"), null, null, mark.toString(), "用户转商户");
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER_USER_TO_MER, extrajson.getDouble("TransAmt"), false, mark.toString());
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致用户转商户失败";
			
			return null;
		}
		
		userMap.put("details", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.debug("------------------------用户转商户请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 提现 
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String withdraw(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------提现json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pTrdAmt"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		//1为扣取平台  2为扣取用户
		if(json.getString("pIpsFeeType").equals("1")){
			userMap.put("feeMode", "PLATFORM");
			
		}else{
			userMap.put("feeMode", "USER");
			
		}
		
		jsonOb.putAll(userMap);
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.WITHDRAWAL, json.getDouble("pTrdAmt"), false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致提现失败";
			
			return null;
		}
		
		Logger.debug("------------------------提现请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 自动投标（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String antoTransfer(int platformId, long platformMemberId, JSONObject json,
			JSONObject jsonXtraPara, String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		
		Logger.debug("------------------------自动投标json=:"+json.toString()+"-------------------------------");
		Logger.debug("------------------------自动投标jsonXtraPara=:"+jsonXtraPara.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("orderNo", json.getString("pBidNo"));  //订单号
		userMap.put("transferAmount", jsonXtraPara.getString("transferAmount"));//标的借款额
		userMap.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));//借款人编号
		userMap.put("paymentAmount", json.getString("pAuthAmt"));//冻结金额（至少一元）
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");  
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");  
		jsonOb.putAll(userMap);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REGISTER_CREDITOR, json.getDouble("pAuthAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致解冻失败";
			
			return null;
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("service", "AUTO_TRANSFER");
		map.put("req", req);
		
		Logger.debug("------------------------自动投标WS  map=:"+map+"-------------------------------");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		/*------------------------------处理返回数据------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		Logger.debug("------------------------自动投标易宝返回结果WS  jsonResult=:"+jsonResult+"-------------------------------");
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", platformMemberId+"");
		jsonOb.put("pP2PBillNo",json.getString("pMerBillNo"));//加该字段只是因为P2P接口需要用到“环迅”接口传过来的参数，这里用请求流水号代替
		jsonOb.put("pFee", "0");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------自动投标回调参数=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 自动还款（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String antoRepayment(int platformId, long platformMemberId, JSONObject json, String argMerCode){
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		JSONArray jsonArr = null; 
		Object pDetails = json.get("pDetails");//节点数组
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		JSONObject arrJson = new JSONObject(); 
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			Map<String, String> properties = new HashMap<String, String>();
			
			properties.put("paymentRequestNo", pRow.getString("pCreMerBillNo"));  //转账请求流水号
			properties.put("targetUserNo", pRow.getString("pInAcctNo"));  //投资人会员编号
			properties.put("amount", pRow.getString("pInAmt"));  //还款金额(投资人到账金额=还款金额-还款平台提成，至少1 元)
			properties.put("fee", pRow.getString("pInFee"));  //还款平台提成
			arrJson.putAll(properties);
		}
		
		userMap.put("properties", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "repayment", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("req", req);
		map.put("service", "AUTO_REPAYMENT");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		/*------------------------------处理返回数据------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return jsonObj.toString();
	}
	
	/**
	 * 平台划款--线下收款，本金垫付（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static Map<String, Object> offerLinePayment(int platformId, long platformMemberId, JSONObject json, 
			JSONObject extraJson, String argMerCode, ErrorInfo error){
		Map<String, String> userMap = null;
		Map<String, Object> map = null;
		JSONObject jsonOb = null; 
		String[] resultarr = new String[2];
		
        Object pDetails = extraJson.get("pDetails");//节点数组 
        JSONArray jsonArr = null; 
        
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		int count = jsonArr.size();
		int pMerBillNo = 0;
		JSONObject jsonResult = null;
		int size = 0;
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			userMap = new HashMap<String, String>();
			jsonOb = new JSONObject();
			map = new HashMap<String, Object>();
			
			pMerBillNo = Integer.parseInt(json.getString("pMerBillNo")) + size++;
			userMap.put("requestNo", pMerBillNo+"");
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", argMerCode);  //出款人编号
			userMap.put("amount", pRow.getString("transAmt"));  //划款金额
			userMap.put("targetUserType", "MEMBER");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("inCustId"));  //收款人编号
			userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");  //
			userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");  //
			jsonOb.putAll(userMap);
			
			DealDetail laondetails = new DealDetail(platformId,  Long.parseLong(pRow.getString("inCustId")), pMerBillNo+"", 
					YEEConstants.TRANSFER_MER_TO_USERS, Double.parseDouble(extraJson.getString("transAmt")), false, "商户转用户--投标奖励");
			
			if (!laondetails.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致平台划款失败";
				
				return null;
			}
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
	        req = YEEUtil.addAttribute(argMerCode, req);
	        
			map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			map.put("service", "PLATFORM_TRANSFER");
			map.put("req", req);
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
			jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
			
			if(!jsonResult.get("code").equals("1")){
				count++;
			}
		}
		
		String pErrCode = "";
		if(jsonArr.size() == count){
			pErrCode = "MG00000F";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		userMap.clear();
		userMap.put("pMerCode", argMerCode);
		userMap.put("pErrMsg", "");
		userMap.put("pErrCode", pErrCode);
		userMap.put("p3DesXmlPara", p3DesXmlPara);
		userMap.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------平台划款(投标奖励)回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 平台划款--多笔--投标奖励发放（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static Map<String, Object> plateToTransfers(int platformId, long platformMemberId, JSONObject json, 
			JSONObject extraJson, String argMerCode, ErrorInfo error){
		Map<String, String> userMap = null;
		Map<String, Object> map = null;
		JSONObject jsonOb = null; 
		String[] resultarr = new String[2];
		
        Object pDetails = extraJson.get("pDetails");//节点数组 
        JSONArray jsonArr = null; 
        
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		int count = jsonArr.size();
		int pMerBillNo = 0;
		JSONObject jsonResult = null;
		int size = 0;
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			userMap = new HashMap<String, String>();
			jsonOb = new JSONObject();
			map = new HashMap<String, Object>();
			
			pMerBillNo = Integer.parseInt(json.getString("pMerBillNo")) + size++;
			userMap.put("requestNo", pMerBillNo+"");
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", argMerCode);  //出款人编号
			userMap.put("amount", pRow.getString("transAmt"));  //划款金额
			userMap.put("targetUserType", "MEMBER");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("inCustId"));  //收款人编号
			userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");  //
			userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");  //
			jsonOb.putAll(userMap);
			
			DealDetail laondetails = new DealDetail(platformId,  Long.parseLong(pRow.getString("inCustId")), pMerBillNo+"", 
					YEEConstants.TRANSFER_MER_TO_USERS, Double.parseDouble(extraJson.getString("transAmt")), false, "商户转用户--投标奖励");
			
			if (!laondetails.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致平台划款失败";
				
				return null;
			}
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
	        req = YEEUtil.addAttribute(argMerCode, req);
	        
			map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			map.put("service", "PLATFORM_TRANSFER");
			map.put("req", req);
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
			jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
			
			if(!jsonResult.get("code").equals("1")){
				count++;
			}
		}
		
		String pErrCode = "";
		if(jsonArr.size() == count){
			pErrCode = "MG00000F";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		userMap.clear();
		userMap.put("pMerCode", argMerCode);
		userMap.put("pErrMsg", "");
		userMap.put("pErrCode", pErrCode);
		userMap.put("p3DesXmlPara", p3DesXmlPara);
		userMap.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------平台划款(投标奖励)回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 确认或者是取消某一笔转账
	 * @param platformMemberId
	 * @param json
	 * @param extraJson
	 * @param argMerCode
	 * @param error
	 * @return
	 */
	public static String confirmTransfer(long platformMemberId, JSONObject json, JSONObject extraJson, String argMerCode, ErrorInfo error){
		error.clear();
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("platformNo", argMerCode);
		jsonObj.put("requestNo", json.getString("pMerBillNo"));
		jsonObj.put("mode", json.getString("mode"));
		jsonObj.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		String req = Converter.jsonToXml(jsonObj.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		userMap.put("service", "COMPLETE_TRANSACTION");
		userMap.put("req", req);
		userMap.put("sign", sign);
		
		String[] returnAttr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
		try {
			jsonObj = (JSONObject) Converter.xmlToObj(returnAttr[1]);
		} catch (Exception e) {
			Logger.debug("确认或者是取消某一笔转账时：" + e.getMessage());
			
			return "";
		}
		
		String pErrCode = "MG00000F";
		if (!jsonObj.getString("code").equals("1")) {
			pErrCode = "";
		}
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return jsonObj.toString();
	}
	
	/**
	 * 平台划款--单笔--cps发放（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String plateToTransfer(int platformId, long platformMemberId, JSONObject json, 
			JSONObject extraJson, String argMerCode, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("sourceUserType", "MERCHANT");  //出款人类型
		userMap.put("sourcePlatformUserNo", argMerCode);  //出款人编号
		userMap.put("amount", extraJson.getString("transAmt"));  //划款金额
		userMap.put("targetUserType", "MEMBER");  //收款人类型
		userMap.put("targetPlatformUserNo", extraJson.getString("inCustId"));  //收款人编号
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");  //
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");  //
		jsonOb.putAll(userMap);
		
		DealDetail laondetails = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER_MER_TO_USER, Double.parseDouble(extraJson.getString("transAmt")), false, "商户转用户-cps");
		
		if (!laondetails.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致平台划款失败";
			
			return null;
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("service", "PLATFORM_TRANSFER");
		map.put("req", req);
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		Logger.debug("------------------------平台划款(cps奖励)WS返回结果=:"+jsonResult.toString()+"-------------------------------");
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return jsonObj.toString();
	}
	
	/**
	 * 解冻（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String unFreeze(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		
		Logger.debug("------------------------解冻json=:"+json.toString()+"-------------------------------");
		
		userMap.put("freezeRequestNo", json.getString("pOriMerBillNo"));  //冻结时的请求流水号
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("service", "UNFREEZE");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.UNFREEZE, json.getDouble("pAuthAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致解冻失败";
			
			return null;
		}
		
		return resultarr[1].toString();
	}
	
	/**
	 * 冻结（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String freeze(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		userMap.put("platformUserNo", String.valueOf(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pLendAmt"));
		userMap.put("expired", "2015-12-12 12:00:00");//这个时间暂时不知道为什么要有
		jsonOb.putAll(userMap);
		
        String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("req", req);
		map.put("service", "FREEZE");
		
		String[] resultarr = new String[2];
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		return resultarr[1].toString();
	}
	
	/**
	 * 查询用户信息(WS)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String queryUserInfo(String argMerCode, long memberId){
		
		YeeService service = new YeeService();
		YeeReqModel model = new YeeReqModel();
		model.setPlatformUserNo(memberId + "");
		JSONObject jsonResult = service.account_Info(model);
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pBalance", jsonResult.getString("availableAmount"));  //可用余额
		jsonObj.put("pLock", jsonResult.getString("freezeAmount"));  //冻结金额
		jsonObj.put("pSum", jsonResult.getString("balance"));
		if (jsonResult.containsKey("bank")) {
			jsonObj.put("pBankName", jsonResult.getString("bank"));
		} else {
			jsonObj.put("pBankName", "未知");
		}
		
		if (jsonResult.containsKey("cardNo")) {
			jsonObj.put("pBankCard", jsonResult.getString("cardNo"));
		} else {
			jsonObj.put("pBankCard", "未知");
		}
		
		jsonObj.put("memberId", memberId+"");
		
		if(jsonResult.getString("activeStatus").equals(YEEConstants.USER_ACTIVATED)){
			jsonObj.put("pStatus","已激活");
			
		}else{
			jsonObj.put("pStatus", "未激活");
		}
		
		if (jsonResult.containsKey("cardStatus")) {
			if(jsonResult.getString("cardStatus").equals(YEEConstants.CARD_HANDDLE)){
				jsonObj.put("pBCardStatus","认证中");
				
			}else if(jsonResult.getString("cardStatus").equals(YEEConstants.CARD_SUCCESS)){
				jsonObj.put("pBCardStatus", "已认证");
				
			}else{
				jsonObj.put("pBCardStatus", "还未绑定");
			}
		}
		
		if (jsonResult.getString("memberType").equals(YEEConstants.MEMBER_PERSON)) {
			jsonObj.put("pMemerType", "个人用户");
		}else {
			jsonObj.put("pMemerType", "企业用户");
		}
		
		Logger.debug("------------------------查询用户信息 返回结果:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 查询用户信息(WS)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	private static void queryUserInfo( long memberId){
		YeeService service = new YeeService();
		YeeReqModel model = new YeeReqModel();
		model.setPlatformUserNo(memberId + "");
		JSONObject jsonResult = service.account_Info(model);
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pBalance", jsonResult.getString("availableAmount"));  //可用余额
		jsonObj.put("pLock", jsonResult.getString("freezeAmount"));  //冻结金额
		if (jsonResult.containsKey("bank")) {
			jsonObj.put("pBankName", jsonResult.getString("bank"));
		} else {
			jsonObj.put("pBankName", "未知");
		}
		if (jsonResult.containsKey("cardNo")) {
			jsonObj.put("pBankCard", jsonResult.getString("cardNo"));
		} else {
			jsonObj.put("pBankCard", "未知");
		}
		jsonObj.put("pBCardStatus", "未知");
		jsonObj.put("memberId", memberId+"");
		
		if(jsonResult.getString("activeStatus").equals(YEEConstants.USER_ACTIVATED)){
			jsonObj.put("pStatus","已激活");
			
		}else{
			jsonObj.put("pStatus", "未激活");
		}
		
		
		Logger.debug("------------------------查询用户信息 返回结果:"+jsonObj.toString()+"-------------------------------");
		
		renderText(jsonObj.toString());
	}
	
	/**
	 * 放款WS（对应P2P转账）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
//	public static String loanWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
//			String argMerCode, String summary, ErrorInfo error){
//		Map<String, Object> userMap = new HashMap<String, Object>();
//		JSONObject jsonOb = new JSONObject(); 
//		String[] resultarr = new String[2];
//		JSONArray jsonArr = null;
//		double amount = jsonXtraPara.getDouble("amount");
//		
//		Logger.debug("------------------------放款P2P参数 json=:"+json.toString()+"-------------------------------");
//		
//		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
//		Object pDetails = json.get("pDetails");//节点数组
//		
//		userMap.put("requestNo", json.getString("pMerBillNo"));
//		userMap.put("fee", jsonXtraPara.getString("serviceFees"));
//		userMap.put("orderNo", json.getString("pBidNo"));
//		
//		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
//		
//		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
//			JSONObject pDetail = (JSONObject)pDetails; 
//			JSONObject pRow = pDetail.getJSONObject("pRow"); 
//	
//			jsonArr = new JSONArray(); 
//			jsonArr.add(pRow); 
//		} else {
//			jsonArr = json.getJSONArray("pDetails");
//			
//		} 
//		
//		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
//		for(int i=0;i<jsonArr.size();i++){
//			Map<String, String> properties = new HashMap<String, String>();
//			JSONObject pRow = (JSONObject)jsonArr.get(i);
//			
//			properties.put("requestNo", pRow.getString("pOriMerBillNo"));  //投标请求流水号
//			properties.put("transferAmount", pRow.getString("pTrdAmt"));  //转账请求转账金额
//			properties.put("sourceUserType", "MEMBER");  //投资人会员类型
//			properties.put("targetUserType", "MEMBER");  //借款人会员类型
//			properties.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));  //借款人会员编号
//			properties.put("sourcePlatformUserNo", pRow.getString("pFIpsAcctNo"));  //投资人会员编号
//			
//			arrJson.add(properties);
//			
//            String serialNumber = json.getString("pMerBillNo")+"_"+pRow.getString("pOriMerBillNo");
//			
//			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
//				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pFIpsAcctNo"), platformId), serialNumber, 
//						YEEConstants.TRANSFER, pRow.getDouble("pTrdAmt"), false, "扣取投资金额");
//				
//				if (!detail.addDealDetail()){
//					error.code = -1;
//					error.msg = "数据库异常，导致放款失败";
//					
//					return null;
//				}
//		      }
//		}
//		
//		DealDetail laondetails = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
//				YEEConstants.TRANSFER, amount, false, summary);
//		
//		if (!laondetails.addDealDetail()){
//			error.code = -1;
//			error.msg = "数据库异常，导致放款失败";
//			
//			return null;
//		}
//		
//		userMap.put("transfers", arrJson);
//		jsonOb.putAll(userMap);
//		
//		String req = Converter.jsonToXml(jsonOb.toString(), "request", "transfer", null, null);
//		req = YEEUtil.addAttribute(argMerCode, req);
//		
//		userMap.clear();
//		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//		userMap.put("req", req);
//		userMap.put("service", "LOAN");
//		
//		//WS请求直接返回处理结果
//		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
//
//	/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*/
//		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
//		String pErrCode = "MG00000F";
//		
//		Logger.debug("------------------------放款WS返回结果=:"+jsonResult.toString()+"-------------------------------");
//		
//		if(!jsonResult.get("code").equals("1")){
//			pErrCode = "";
//		}
//		
//		//补单的情况下用，102状态说明放款有请求过易宝
//		if(jsonResult.get("code").equals("102")){
//			userMap.clear();
//	        userMap.put("requestNo", json.getString("pMerBillNo"));  //补单的请求流水号
//			
//			userMap.put("mode", "PAYMENT_RECORD");  //查询模式
//			jsonOb.putAll(userMap);
//			
//			String ueryReq = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//			ueryReq = YEEUtil.addAttribute(argMerCode, ueryReq);
//			
//			userMap.clear();
//			userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//			userMap.put("req", ueryReq);
//			userMap.put("service", "QUERY");
//			
//			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
//			
//			/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
//			JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
//			
//			if(queryResult.getString("code").equals("1")){
//				pErrCode = "MG00000F";
//			}
//		}
//		
//		JSONObject jsonObj = new JSONObject();
//		
//		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
//		jsonObj.put("pTransferType", json.getString("pTransferType"));
//		jsonObj.put("pMemo1", platformMemberId+"");
//		
//		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
//		
//		jsonOb.clear();
//		jsonOb.put("pMerCode", argMerCode);
//		jsonOb.put("pErrMsg", "");
//		jsonOb.put("pErrCode", pErrCode);
//		jsonOb.put("p3DesXmlPara", p3DesXmlPara);
//		jsonOb.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
//		
//		Logger.debug("------------------------放款回调P2P=:"+jsonObj.toString()+"-------------------------------");
//		
//		return jsonOb.toString();
//	}
	
//	public static String loanWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
//			String argMerCode, String summary, ErrorInfo error){
//		Map<String, Object> userMap = new HashMap<String, Object>();
//		JSONObject jsonOb = new JSONObject(); 
//		String[] resultarr = new String[2];
//		JSONArray jsonArr = null;
//		
//		Logger.debug("------------------------放款P2P参数 json=:"+json.toString()+"-------------------------------");
//		Logger.debug("YEE->loanWS->>>投标流水号pBillNos:="+jsonXtraPara.get("pBillNos").toString());
//		String pBillNos[] = (jsonXtraPara.get("pBillNos").toString()).split(",");
//		
//		for(int i = 0; i < pBillNos.length; i++){
//			jsonOb.put("requestNo", pBillNos[i]);
//			jsonOb.put("mode", "CONFIRM");
//			jsonOb.put("notifyUrl", "notifyUrl");
//			
//			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//			req = YEEUtil.addAttribute(argMerCode, req);
//			
//			Logger.debug("req:="+req);
//			
//			userMap.clear();
//			userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//			userMap.put("req", req);
//			userMap.put("service", "COMPLETE_TRANSACTION");
//			
//			//WS请求直接返回处理结果
//			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
//		}
//		
//
//	/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*/
//		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
//		String pErrCode = "MG00000F";
//		
//		Logger.debug("------------------------放款WS返回结果=:"+jsonResult.toString()+"-------------------------------");
//		
//		if(!jsonResult.get("code").equals("1")){
//			pErrCode = "";
//		}
//		
//		//补单的情况下用，102状态说明放款有请求过易宝----下面没有管
//		if(jsonResult.get("code").equals("102")){
//			userMap.clear();
//	        userMap.put("requestNo", json.getString("pMerBillNo"));  //补单的请求流水号
//			
//			userMap.put("mode", "PAYMENT_RECORD");  //查询模式
//			jsonOb.putAll(userMap);
//			
//			String ueryReq = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//			ueryReq = YEEUtil.addAttribute(argMerCode, ueryReq);
//			
//			userMap.clear();
//			userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//			userMap.put("req", ueryReq);
//			userMap.put("service", "QUERY");
//			
//			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
//			
//			/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
//			JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
//			
//			if(queryResult.getString("code").equals("1")){
//				pErrCode = "MG00000F";
//			}
//		}
//		
//		JSONObject jsonObj = new JSONObject();
//		
//		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
//		jsonObj.put("pTransferType", json.getString("pTransferType"));
//		jsonObj.put("pMemo1", platformMemberId+"");
//		
//		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
//		
//		jsonOb.clear();
//		jsonOb.put("pMerCode", argMerCode);
//		jsonOb.put("pErrMsg", "");
//		jsonOb.put("pErrCode", pErrCode);
//		jsonOb.put("p3DesXmlPara", p3DesXmlPara);
//		jsonOb.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
//		
//		Logger.debug("------------------------放款回调P2P=:"+jsonObj.toString()+"-------------------------------");
//		
//		return jsonOb.toString();
//	}
	
	public static String loanWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		JSONArray jsonArr = null;
		
		Logger.debug("------------------------放款P2P参数 json=:"+json.toString()+"-------------------------------");
		Logger.debug("YEE->loanWS->>>投标流水号pBillNos:="+jsonXtraPara.get("pBillNos").toString());
		String pBillNos[] = (jsonXtraPara.get("pBillNos").toString()).split(",");
		
		for(int i = 0; i < pBillNos.length; i++){
			jsonOb.put("requestNo", pBillNos[i]);
			jsonOb.put("mode", "CONFIRM");
			jsonOb.put("notifyUrl", "notifyUrl");
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
			req = YEEUtil.addAttribute(argMerCode, req);
			
			Logger.debug("放款req参数:="+req);
			
			userMap.clear();
			userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			userMap.put("req", req);
			userMap.put("service", "COMPLETE_TRANSACTION");
			
			Map<String, String> logMap = new HashMap<String, String>();
			/*添加日志*/
			logMap.put("req", req);
			logMap.put("sign", SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			logMap.put("url", YEEConstants.YEE_URL_REDICT);
			YeeToolsExtra.recordReqParams(YEEConstants.TRANSFER, platformMemberId, "请求参数至易宝", logMap, json, jsonXtraPara);
			
			//WS请求直接返回处理结果
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
			
			/*--------------------出现错误情况再次请求---------------------------*/
			JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
			
			Logger.debug("放款结果(易宝同步返回)：%s",jsonResult);
			
			if(jsonResult.get("code").equals("102")){
				i--;
			}
		}
		/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pTransferType", json.getString("pTransferType"));
		jsonObj.put("pMemo1", platformMemberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonOb.clear();
		jsonOb.put("pMerCode", argMerCode);
		jsonOb.put("pErrMsg", "");
		jsonOb.put("pErrCode", pErrCode);
		jsonOb.put("p3DesXmlPara", p3DesXmlPara);
		jsonOb.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------放款回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonOb.toString();
	}
	
	/**
	 * 债权转让WS（对应P2P转账）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String transferWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		String[] resultarr = new String[2];
		JSONArray jsonArr = null;
		double amount = 0;
		
		Logger.debug("------------------------债权转让P2P参数json=:"+json.toString()+"-------------------------------");
		
		Object pDetails = json.get("pDetails");//节点数组
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			
			userMap.put("requestNo", pRow.getString("pOriMerBillNo"));  //投标请求流水号
			userMap.put("amount", pRow.getString("pTrdAmt"));  //划款金额
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", pRow.getString("fromUserId"));  //出款人编号
			userMap.put("targetUserType", "MERCHANT");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("toUserId"));  //收款人编号
	    }
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		jsonOb.putAll(userMap);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER, amount, false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致债权转让失败";
			
			return null;
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		userMap.clear();
		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("req", req);
		userMap.put("service", "PLATFORM_TRANSFER");
		
		//WS请求直接返回处理结果
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);

	/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		Logger.debug("------------------------债权转让WS易宝处理结果 jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pTransferType", json.getString("pTransferType"));
		jsonObj.put("pMemo1", platformMemberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", jsonResult.get("description")+"");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+jsonResult.get("description")+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------债权转让回调P2P jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 取消投标解冻资金(WS)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String revocationTransfer(int platformId, String argMerCode, JSONObject json, JSONObject extraJson, long memberId){
		JSONObject jsonOb = new JSONObject(); 
		Map<String, Object> userMap = new HashMap<String, Object>();
		String[] resultarr = new String[2];
		
		userMap.put("requestNo", json.getString("pP2PBillNo"));  //之前投标的请求流水号
		userMap.put("platformUserNo", String.valueOf(memberId));  //用户编号
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		userMap.clear();
		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("req", req);
		userMap.put("service", "REVOCATION_TRANSFER");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
		
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr[1]);
		String pErrCode = "MG00000F";
		
		Logger.debug("------------------------取消投标WS易宝处理结果 jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo2 ", "Y");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", jsonResult.get("description")+"");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+jsonResult.get("description")+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------取消投标回调P2P jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	
	/**
	 * 单笔业务查询基类方法
	 * @param billNo
	 * @param model
	 * @return
	 */
	public static JSONObject queryBase(String billNo,String model){
		YeeService service = new YeeService();
		YeeReqModel reqModel = new YeeReqModel();
		reqModel.setRequestNo(billNo);
		reqModel.setMode(model);
		JSONObject json = service.query(reqModel);
		return json;
	}
	
	/**
	 * 投标业务补单查询
	 * @param billNo
	 * @return
	 */
	private static  Map<String,Object> queryInvest(String billNo){
		Map<String,Object> maps = new HashMap<String,Object>();
		JSONObject json = queryBase(billNo,YEEConstants.CP_TRANSACTION);
		int code = json.getInt("code");
		String codeValue = "MG00001F";
		String description = "未查询交易状态";
		int status = 0;
		if(code ==1){
			JSONArray arr = json.getJSONArray("records");
			if(arr.size() != 0){
				JSONObject obj = arr.getJSONObject(0);
				maps.put("requestNo", obj.getString("requestNo"));
				String statusValue = obj.getString("status");
				if("PREAUTH".equals(statusValue)|"CONFIRM".equals(statusValue)|"DIRECT".equals(statusValue)){
					status = 1;
				}else if("CANCEL".equals(statusValue)){
					status = 0;
				}
				codeValue = "MG00000F";
				description = json.getString("description");
			}
		}else if(code == 0){
			status = 2;
			description = "易宝(系统异常),可能不存在交易记录";
		}
		
		String result = PTradeStatue_F;
		if(status == 1 ){
			result = PTradeStatue_S;
		}else if(status == 2){
			result = PTradeStatue_N;
		}
		maps.put("code",codeValue );
		maps.put("respDesc", description);
		maps.put("status", result);
		return maps;
	}
	
	/**
	 * 充值记录补单查询
	 * @return
	 */
	private static Map<String,Object> queryNetSave(String billNo){
		Map<String,Object> maps = new HashMap<String, Object>();
		JSONObject json = queryBase(billNo,YEEConstants.RECHARGE_RECORD);
		int code = json.getInt("code");
		String codeValue = "MG00001F";
		String description = "未查询交易状态";
		int status = 0;
		if(code == 1){
			JSONArray arr = json.getJSONArray("records");
			if(arr.size() != 0){
				JSONObject obj = arr.getJSONObject(0);
				maps.put("requestNo", obj.getString("requestNo"));
				String statusValue = obj.getString("status");
				if("SUCCESS".equals(statusValue)){
					status = 1;
				}else if("INIT".equals(statusValue)){
					status = 0;
				}
				codeValue = "MG00000F";
				description = json.getString("description");
			}
		}
			String result = PTradeStatue_F;
			if(status == 1 ){
				result = PTradeStatue_S;
			}else if(status == 0){
				result = PTradeStatue_F;
			}
			maps.put("code",codeValue );
			maps.put("respDesc", description);
			maps.put("status", result);
		return maps;
	}
	
	/**
	 *  提现记录补单查询
	 * @param billNo
	 * @return
	 */
	private static Map<String,Object> queryCash(String billNo){
		Map<String,Object> maps = new HashMap<String, Object>();
		maps.put("requestNo", billNo);
		JSONObject json = queryBase(billNo,YEEConstants.WITHDRAW_RECORD);
		int code = json.getInt("code");
		String codeValue = "MG00001F";
		String description = "未查询交易状态";
		int status = 0;
		if(code == 1){
			JSONArray arr = json.getJSONArray("records");
			if(arr.size() != 0){
				JSONObject obj = arr.getJSONObject(0);
				String statusValue = obj.getString("status");
				if("SUCCESS".equals(statusValue)){
					if(obj.containsKey("remitStatus")){
						String remitStatus= obj.getString("remitStatus");
						if("REMIT_SUCCESS".equals(remitStatus)){
							status = 1;
						}else if("REMIT_FAILURE".equals(remitStatus)){
							status = 0;
						}else if("REMITING".equals(remitStatus)){
							status = 3;
						}
					}
					status = 1;
				}else if("INIT".equals(statusValue)){
					status = 0;
				}
				codeValue = "MG00000F";
				description = json.getString("description");
			}else{
				status = 2;
			}
		}
			String result = PTradeStatue_F;
			if(status == 1 ){
				result = PTradeStatue_S;
				description = "补单成功";
			}else if(status == 0){
				result = PTradeStatue_F;
				description = "补单失败";
			}else if(status ==2){
				result = PTradeStatue_N;
				description = "未查询到交易记录";
			}else if(status == 3){
				result = PTradeStatue_D;
				description = "交易处理中";
			}
			maps.put("code",codeValue );
			maps.put("respDesc", description);
			maps.put("status", result);
		return maps;
	}
	
	
	/**
	 * 还款交易记录查询
	 * @param bill
	 * @return
	 */
	private static Map<String,Object> queryRepayment(String billNo){
		Map<String,Object> maps = new HashMap<String,Object>();
		JSONObject json = queryBase(billNo,YEEConstants.CP_TRANSACTION);
		Logger.info("%s", json.toString());
		int code = json.getInt("code");
		String codeValue = "MG00001F";
		String description = "未查询交易状态";
		int status = 0;
		if(code ==1){
			JSONArray arr = json.getJSONArray("records");
			if(arr.size() != 0){
				JSONObject obj = arr.getJSONObject(0);
				maps.put("requestNo", obj.getString("requestNo"));
				String statusValue = obj.getString("status");
				if("PREAUTH".equals(statusValue)|"CONFIRM".equals(statusValue)|"DIRECT".equals(statusValue)){
					status = 1;
				}else if("CANCEL".equals(statusValue)){
					status = 0;
				}
				codeValue = "MG00000F";
				description = json.getString("description");
			}
		}else if(code == 0){
			status = 2;
			description = "易宝(系统异常),可能不存在交易记录";
		}
		
		String result = PTradeStatue_F;
		if(status == 1 ){
			result = PTradeStatue_S;
		}else if(status == 2){
			result = PTradeStatue_N;
		}
		maps.put("code",codeValue );
		maps.put("respDesc", description);
		maps.put("status", result);
		return maps;
		
		
	}
	
	/**
	 * 单笔业务查询WS(PAYMENT_RECORD：标的投资放款记录
       REPAYMENT_RECORD：标的还款记录
	   WITHDRAW_RECORD：提现记录
	   RECHARGE_RECORD：充值记录)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String query(String argMerCode, JSONObject json, JSONObject jsonXtraPara){
		Logger.info("json : %s", json.toString());
		Logger.info("jsonXtraPara : %s", jsonXtraPara.toString());
		int type = json.getInt("pTradeType");
		Map<String,Object> maps = null;
		JSONObject returnJson = new JSONObject();
		String respDesc = null;
		String merCustId = argMerCode;
		String respCode = null;
		JSONObject desJson = null;
		String pTradeStatue = null;
		switch(type){
		case QueryType.QUERY_INVEST_TYPE:  //投标
			maps = queryInvest(json.getString("pMerBillNo"));
			pTradeStatue  = maps.get("status")+"";
			desJson = new JSONObject();
			desJson.put("pMerBillNo", maps.get("requestNo")+"");
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", pTradeStatue);
			respDesc = (String) maps.get("respDesc");
			respCode = (String) maps.get("code");
			break;
		case QueryType.QUERY_RECAHARGE_TYPE:  //充值
			maps = queryNetSave(json.getString("pMerBillNo"));
			pTradeStatue  = maps.get("status")+"";
			desJson = new JSONObject();
			desJson.put("pMerBillNo", maps.get("requestNo")+"");
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", pTradeStatue);
			respDesc = (String) maps.get("respDesc");
			respCode = (String) maps.get("code");
			break;
		case QueryType.QUERY_WITHDRAW_TYPE:  //提现
			maps = queryCash(json.getString("pMerBillNo"));
			pTradeStatue  = maps.get("status")+"";
			desJson = new JSONObject();
			desJson.put("pMerBillNo", maps.get("requestNo")+"");
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", pTradeStatue);
			respDesc = (String) maps.get("respDesc");
			respCode = (String) maps.get("code");
			break;
		case QueryType.QUERY_REPAYMENT_TYPE:  //还款
			maps = queryRepayment(json.getString("pMerBillNo"));
			pTradeStatue  = maps.get("status")+"";
			desJson = new JSONObject();
			desJson.put("pMerBillNo", maps.get("requestNo")+"");
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", pTradeStatue);
			respDesc = (String) maps.get("respDesc");
			respCode = (String) maps.get("code");
			break;
		case QueryType.QUERY_MONEY_TRANSFER_TYPE:  //放款
			desJson = new JSONObject();
			desJson.put("pMerBillNo", json.getString("pMerBillNo"));
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", PTradeStatue_N);
			respDesc = "对不起,该接口暂不支持补单";
			respCode = "MG00000F";
			break;
		default:
			desJson = new JSONObject();
			desJson.put("pMerBillNo", json.getString("pMerBillNo"));
			desJson.put("pTradeType", type);
			desJson.put("pMerDate", jsonXtraPara.getString("ordDate"));
			desJson.put("pTradeStatue", PTradeStatue_N);
			respDesc = "对不起,该接口暂不支持补单";
			respCode = "MG00000F";
			break;
		}
		String desValue = buildP3DesXmlPara(desJson);
		String pSign = haxMD5(merCustId,respCode,respDesc,desValue);
		returnJson.put("pMerCode", merCustId);
		returnJson.put("pErrCode",respCode);
		returnJson.put("pErrMsg", respDesc);
		returnJson.put("p3DesXmlPara", desValue);
		returnJson.put("pSign", pSign);
		return returnJson.toString();
	}
	
	/**
	 *  商户转用户(投标奖励)
	 * @return
	 */
	private static void transferMerToUsers(int type ,int platformId, long platformMemberId, JSONObject json, JSONObject extrajson,
			String argMerCode, String summary, ErrorInfo error){
		
		Map<String,String> maps = new HashMap<String, String>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		
		String billNo = json.getString("pMerBillNo");
		
		String isWs = extrajson.getString("isWs");
		Object pDetails = extrajson.get("pDetails");
		JSONArray jsonArr = null;
		JSONArray details = new JSONArray();
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = extrajson.getJSONArray("pDetails");
		} 
		JSONObject detailObj = null;
		for(Object obj : jsonArr){
			detailObj = new JSONObject();
			JSONObject detail = (JSONObject)obj;
			detailObj.put("amount", detail.getString("transAmt"));
			detailObj.put("targetPlatformUserNo", detail.getString("inCustId"));
			detailObj.put("targetUserType", "MEMBER");
			detailObj.put("bizType", "TRANSFER");
			details.add(detailObj);
		}
		JsonObject desJson = new JsonObject();
		desJson.addProperty("pMerBillNo", billNo);
		userMap.put("requestNo",billNo);
		userMap.put("platformNo", argMerCode);
		userMap.put("platformUserNo", argMerCode);
		
		userMap.put("userType", "MERCHANT");//还款人用户类型
		userMap.put("bizType", "TRANSFER");//业务类型 还款
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");//业务类型 还款
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");//业务类型 还款
		userMap.put("details", details);
		
		JSONObject reqTemp = new JSONObject();
		reqTemp.putAll(userMap);
		
		String req = Converter.jsonToXml(reqTemp.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
		
		//添加交易记录失败
		DealDetail dealDetail = new DealDetail(platformId, platformMemberId, billNo, YEEConstants.TRANSFER_MER_TO_USERS, 0, false, summary);
		if (!dealDetail.addDealDetail()) {
			
			renderText("");
		}
		
		
		
		Logger.debug("%s",req);
		
		maps.put("service", "DIRECT_TRANSACTION");
		maps.put("req",req);
		maps.put("sign", sign);
		
		Map<String, Object> logMap = new HashMap<String, Object>();
		/*添加日志*/
		logMap.put("req", req);
		logMap.put("sign", sign);
		logMap.put("url", YEEConstants.YEE_URL_REDICT);
		YeeToolsExtra.recordReqParams(type, platformMemberId, "请求参数至易宝", maps, json, extrajson);
		        
		String result = WS.url(YEEConstants.YEE_URL_REDICT).setParameters(maps).post().getString();
		
		Logger.debug("直接转账(易宝返回结果):%s", result);
		
		JSONObject resultJson = (JSONObject) Converter.xmlToObj(result);
		
		//添加回调日志
		if (null != resultJson && resultJson.containsKey("resp") && resultJson.containsKey("sign")) {
			String respLog = resultJson.getString("resp");
			String signLog = resultJson.getString("sign");
	    	YeeToolsExtra.recordRespParams("易宝同步回调", false, respLog,signLog,Constants.BASE_URL + "yee/callBack?resp="+respLog+"&sign="+signLog);
		}
		
		String merCustId=argMerCode;
		String respCode="MG00000F";
		String respDesc="成功";
		String desValue =Converter.jsonToXml(json.toString(), "pReq", null,null, null);
		
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		Logger.debug("desValue : %s", desValue);
		desValue = Encrypt.encrypt3DES(desValue, Constants.ENCRYPTION_KEY);
		String code =resultJson.getString("code");
		//失败返回码
		if(!"1".equals(code)) {
			 respCode = "MG00025F";
			 respDesc = "失败";
		}
		Map<String,String> paramsToSp2p = new HashMap<String, String>();
		paramsToSp2p.put("pMerCode", merCustId);
		paramsToSp2p.put("pErrCode", respCode);
		paramsToSp2p.put("pErrMsg",respDesc);
		paramsToSp2p.put("p3DesXmlPara", desValue);
		String pSign = Encrypt.MD5(argMerCode + respCode + "" + desValue + Constants.ENCRYPTION_KEY);
		paramsToSp2p.put("pSign", pSign);
		
		Logger.debug("-----------------回调p2p:---------------------------");
		Logger.debug("--------------------desValue:%s", argMerCode + respCode + "" + desValue + Constants.ENCRYPTION_KEY);
		Logger.debug("-------------------pSign:%s", pSign);
		
		Map<String,Object> baseParams = new HashMap<String, Object>();
		baseParams.put(P2P_ASYN_URL_KEY, json.getString("pS2SUrl"));
		baseParams.put(P2P_URL_KEY, json.getString("pWebUrl"));		
		baseParams.put(P2P_BASEPARAMS, paramsToSp2p);
		
		submitByAsynch(baseParams);
		
		if (null != isWs && "Y".equalsIgnoreCase(isWs)) {
			JSONObject jsonOb = new JSONObject();
			jsonOb.clear();
			jsonOb.put("pMerCode", argMerCode);
			jsonOb.put("pErrMsg", "");
			jsonOb.put("pErrCode", respCode);
			jsonOb.put("p3DesXmlPara", desValue);
			jsonOb.put("pSign", pSign);
			
			Logger.debug("------------------------商户专用户（批量）回调P2P=:"+jsonOb.toString()+"-------------------------------");
			
			renderText(jsonOb.toString());
		}else{
			submitByFront(baseParams);
		}
	}
	
	/**
	 * 转账接口（用户转用户）
	 * @param type
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @param extrajson
	 * @param argMerCode
	 * @param summary
	 * @param error
	 */
	private static String transferUserToUsers(int type ,int platformId, long platformMemberId, JSONObject json, JSONObject extrajson,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		
		String billNo = json.getString("pMerBillNo");
		
		Object pDetails = extrajson.get("pDetails");
		JSONArray jsonArr = null;
		JSONArray details = new JSONArray();
		double amount = 0.0;
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = extrajson.getJSONArray("pDetails");
		}
		
		JSONObject detailObj = null;
		for(Object obj : jsonArr){
			detailObj = new JSONObject();
			JSONObject detail = (JSONObject)obj;
			amount += Double.parseDouble(detail.getString("transAmt"));
			detailObj.put("amount", detail.getString("transAmt"));
			detailObj.put("targetPlatformUserNo", detail.getString("inCustId"));
			detailObj.put("targetUserType", "MEMBER");
			detailObj.put("bizType", "TRANSFER");
			details.add(detailObj);
		}
		
		JsonObject desJson = new JsonObject();
		desJson.addProperty("pMerBillNo", billNo);
		userMap.put("requestNo",billNo);
		userMap.put("platformNo", argMerCode);
		userMap.put("platformUserNo", platformMemberId);
		
		userMap.put("userType", "MEMBER");//还款人用户类型
		userMap.put("bizType", "TRANSFER");//业务类型 转账
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");//业务类型 还款
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");//业务类型 还款
		userMap.put("details", details);
		
		//保存数据库记录
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER_USER_TO_USER, amount, false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致用户转用户失败";
			
			return null;
		}
		JSONObject reqTemp = new JSONObject();
		reqTemp.putAll(userMap);
		
		String req = Converter.jsonToXml(reqTemp.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		return req;
	}
	
	/**
	 * 用户转用户回调
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> transferUserToUsersCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
			String platformNo = remarkMap.get("domain");
			String requestNo= remarkMap.get("pMerBillNo");
			String mode = "CONFIRM";
			boolean result = confirmTranferResult(platformNo,requestNo,mode);
		    pErrCode = "MG00000F";
		    if(!result){
		    	pErrCode = "MG00001F";
		    }
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMemo1", remarkMap.get("memberId"));//还款人id
		jsonOb.put("pMemo3",remarkMap.get("pMemo3"));
		jsonOb.put("pMerBillNo",json.getString("requestNo"));
		
		Logger.debug("------------------------用户转用户回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------用户转用户回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
	
	
	
	@Deprecated
	public static void transferMerToUsersTest(){
		Map<String,String> maps = new HashMap<String, String>();
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		userMap.put("requestNo",System.currentTimeMillis()+"");
		userMap.put("platformNo", "10040011137");
		userMap.put("platformUserNo", "10040011137"); 
		userMap.put("userType", "MERCHANT");//还款人用户类型
		userMap.put("bizType", "TRANSFER");//业务类型 还款
		userMap.put("notifyUrl", "http://112.90.176.4:8085/cn/Application/bgUrl");//业务类型 还款
		userMap.put("callbackUrl", "http://112.90.176.4:8085/cn/Application/bgUrl");//业务类型 还款
		
		Map<String,String> detail = new HashMap<String, String>();
		detail.put("amount", "50.00");
		detail.put("targetUserType", "MEMBER");
		detail.put("targetPlatformUserNo", "7090");
		detail.put("bizType", "TRANSFER");
		JSONArray array = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("amount", "50.00");
		jsonObject.put("targetUserType", "MEMBER");
		jsonObject.put("targetPlatformUserNo", "7090");
		jsonObject.put("bizType", "TRANSFER");
		
		JSONObject jsonObject1 = new JSONObject();
		jsonObject1.put("amount", "50.00");
		jsonObject1.put("targetUserType", "MEMBER");
		jsonObject1.put("targetPlatformUserNo", "7091");
		jsonObject1.put("bizType", "TRANSFER");
		array.add(jsonObject);
		array.add(jsonObject1);
		
		
		userMap.put("details", array);
		jsonOb.putAll(userMap);
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		

		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute("10040011137", req);
		
		Logger.debug("%s",req);
		maps.put("service", "DIRECT_TRANSACTION");
		maps.put("req",req);
		maps.put("sign", "1111");
		String result = WS.url("http://119.161.147.110:8088/member/bhaexter/bhaController").setParameters(maps).post().getString();
		Logger.debug("result : %s",result);
	}
	
	/**
	 * 代偿(商户转用户),本金垫付
 	 * @param memberId p2p平台用户id
	 * @param type 接口类型
	 * @param platfrom 平台id
	 * @param json 参数
	 * @param pnrParams 参数2
	 */
	public  String compensatory(long memberId, int type, int platform,
			JSONObject json, JSONObject extrajson,String argMerCode,ErrorInfo error) {
		
		Map<String,String> maps = new HashMap<String, String>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		
		String billNo = json.getString("pMerBillNo");
		
		Object pDetails = json.get("pDetails");
		JSONArray jsonArr = null;
		JSONArray details = new JSONArray();
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		JSONObject detailObj = null;
		double transAmt = 0.00;
		for(Object obj : jsonArr){
			detailObj = new JSONObject();
			JSONObject detail = (JSONObject)obj;
			transAmt = Double.parseDouble(detail.getString("pTrdAmt"))-Double.parseDouble(detail.getString("pTTrdFee"));
			detailObj.put("amount", String.format("%.2f", transAmt));
			detailObj.put("targetPlatformUserNo", detail.getString("pTIpsAcctNo"));
			detailObj.put("targetUserType", "MEMBER");
			detailObj.put("bizType", "TRANSFER");
			details.add(detailObj);
		}
		JsonObject desJson = new JsonObject();
		desJson.addProperty("pMerBillNo", billNo);
		userMap.put("requestNo",billNo);
		userMap.put("platformNo", argMerCode);
		userMap.put("platformUserNo", argMerCode); 
		userMap.put("userType", "MERCHANT");//还款人用户类型
		userMap.put("bizType", "TRANSFER");//业务类型 还款
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");//业务类型 还款
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callback");//业务类型 还款
		userMap.put("details", details);
		
		JSONObject reqTemp = new JSONObject();
		reqTemp.putAll(userMap);
		
		String req = Converter.jsonToXml(reqTemp.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.debug("%s",req);
		
		maps.put("service", "DIRECT_TRANSACTION");
		maps.put("req",req);
		maps.put("sign", SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		String result = WS.url(YEEConstants.YEE_URL_REDICT).setParameters(maps).post().getString();
		Logger.debug("直接转账(易宝返回结果):%s", result);
		
		JSONObject resultJson = (JSONObject) Converter.xmlToObj(result);
		String desValue =Converter.jsonToXml(json.toString(), "pReq", null,null, null);
		
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		Logger.debug("desValue : %s", desValue);
		desValue = Encrypt.encrypt3DES(desValue, Constants.ENCRYPTION_KEY);
		String code =resultJson.getString("code");
		
		JsonObject respJson = new JsonObject();
		respJson.addProperty("pMerCode", argMerCode);
		json.put("pMemo1", memberId);
		String arg3DesXmlPara = buildP3DesXmlPara(json);
		if("1".equals(code)){
			respJson.addProperty("pErrCode", "MG00000F");
			respJson.addProperty("pErrMsg", "操作成功");
		}else{
			respJson.addProperty("pErrCode", "MG00001F");
			respJson.addProperty("pErrMsg", "操作失败");
		}
		respJson.addProperty("p3DesXmlPara", arg3DesXmlPara);
		String pSign  = Codec.hexMD5(respJson.get("pMerCode").getAsString()+respJson.get("pErrCode").getAsString()
				+respJson.get("pErrMsg").getAsString()+respJson.get("p3DesXmlPara").getAsString()+Constants.ENCRYPTION_KEY);
		respJson.addProperty("pSign", pSign);
		
		Logger.debug(respJson.toString());
		
		return respJson.toString();
	}
	
	/**
	 * 红包发送（商户转用户）
	 * @param memberId
	 * @param type
	 * @param platform
	 * @param json
	 * @param extrajson
	 * @param argMerCode
	 * @param error
	 * @return
	 */
	public String redPaperSend(long memberId, int type, int platform,
			JSONObject json, JSONObject extrajson,String argMerCode,ErrorInfo error) {
		
		Map<String,String> maps = new HashMap<String, String>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		
		String billNo = json.getString("pMerBillNo");
		
		Object pDetails = json.get("pDetails");
		JSONArray jsonArr = null;
		JSONArray details = new JSONArray();
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		JSONObject detailObj = null;
		double transAmt = 0.00;
		for(Object obj : jsonArr){
			detailObj = new JSONObject();
			JSONObject detail = (JSONObject)obj;
			detailObj.put("amount", detail.getString("pTrdAmt"));
			detailObj.put("targetPlatformUserNo", detail.getString("pTIpsAcctNo"));
			detailObj.put("targetUserType", "MEMBER");
			detailObj.put("bizType", "TRANSFER");
			details.add(detailObj);
		}
		JsonObject desJson = new JsonObject();
		desJson.addProperty("pMerBillNo", billNo);
		userMap.put("requestNo",billNo);
		userMap.put("platformNo", argMerCode);
		userMap.put("platformUserNo", argMerCode); 
		userMap.put("userType", "MERCHANT");//还款人用户类型
		userMap.put("bizType", "TRANSFER");//业务类型 还款
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");//业务类型 还款
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callback");//业务类型 还款
		userMap.put("details", details);
		
		JSONObject reqTemp = new JSONObject();
		reqTemp.putAll(userMap);
		
		String req = Converter.jsonToXml(reqTemp.toString(), "request", "detail", null, null);
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.debug("%s",req);
		
		maps.put("service", "DIRECT_TRANSACTION");
		maps.put("req",req);
		maps.put("sign", SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		String result = WS.url(YEEConstants.YEE_URL_REDICT).setParameters(maps).post().getString();
		Logger.debug("直接转账(易宝返回结果):%s", result);
		
		JSONObject resultJson = (JSONObject) Converter.xmlToObj(result);
		String desValue =Converter.jsonToXml(json.toString(), "pReq", null,null, null);
		
		req = req.replace("&lt;", "<").replace("&gt;", ">");
		Logger.debug("desValue : %s", desValue);
		desValue = Encrypt.encrypt3DES(desValue, Constants.ENCRYPTION_KEY);
		String code =resultJson.getString("code");
		
		JsonObject respJson = new JsonObject();
		respJson.addProperty("pMerCode", argMerCode);
		json.put("pMemo1", memberId);
		String arg3DesXmlPara = buildP3DesXmlPara(json);
		if("1".equals(code)){
			respJson.addProperty("pErrCode", "MG00000F");
			respJson.addProperty("pErrMsg", "操作成功");
		}else{
			respJson.addProperty("pErrCode", "MG00001F");
			respJson.addProperty("pErrMsg", "操作失败");
		}
		respJson.addProperty("p3DesXmlPara", arg3DesXmlPara);
		String pSign  = Codec.hexMD5(respJson.get("pMerCode").getAsString()+respJson.get("pErrCode").getAsString()
				+respJson.get("pErrMsg").getAsString()+respJson.get("p3DesXmlPara").getAsString()+Constants.ENCRYPTION_KEY);
		respJson.addProperty("pSign", pSign);
		
		Logger.debug(respJson.toString());
		return respJson.toString();
	}
	
	
	/**
	 *  代偿还款回调
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	private static Map<String,String> compensatoryRepaymentCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.debug("------------------------代偿还款回调-------------------------------");
		
		if(json.getString("code").equals("1")){
			String platformNo = remarkMap.get("domain");
			String requestNo= remarkMap.get("pMerBillNo");
			String mode = "CONFIRM";
			boolean result = confirmTranferResult(platformNo,requestNo,mode);
		    pErrCode = "MG00000F";
		    if(!result){
		    	pErrCode = "MG00001F";
		    }
			
		}else{
			pErrCode = "MG00025F";
			 
		}
		
		jsonOb.put("pMemo1", remarkMap.get("pMemo1"));  //商户流水号
		jsonOb.put("pMemo3", remarkMap.get("pMemo3"));  //交易金额
		jsonOb.put("pMerBillNo", remarkMap.get("pMerBillNo"));  //交易金额
		jsonOb.put("pTransferType", 3);  //交易金额
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.debug("------------------------代偿还款回调==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	* 划款查询-用于确认转账之前查询，否则在还款后调用确认转账接口出现死循环
	* @param billNo
	* @return
	*/
	private static boolean queryCp_Transaction(String billNo){
		JSONObject json = queryBase(billNo, YEEConstants.CP_TRANSACTION);
		int code = json.getInt("code");
		if(code ==1){
			JSONArray arr = json.getJSONArray("records");
			if(arr.size() != 0){
				JSONObject obj = arr.getJSONObject(0);
				String statusValue = obj.getString("status");
				if("PREAUTH".equals(statusValue)){
					return true;
				}else if("CANCEL".equals(statusValue)|"CONFIRM".equals(statusValue)|"DIRECT".equals(statusValue)){
					return false;
				}
			}
		}else{
			return false;
		}
		return false;
	}
	
	/**
	 * 转账确认-用于商户转用户回调后调用此接口
	 * @param platformNo
	 * @param requestNo
	 * @param mode
	 * @return
	 */
	private static boolean confirmTranferResult(String platformNo,String requestNo,String mode){
		
		if(!queryCp_Transaction(requestNo)){
			return true;
		}
		YeeService service = new YeeService();
		YeeReqModel model = new YeeReqModel();
		model.setRequestNo(requestNo);
		model.setMode(mode);
		
		JSONObject resultJson = service.complete_Transaction(model);
		String code = resultJson.getString("code");
		
		if(!"1".equals(code)){
			return false;
		}
		return true;
	}
	
	/**
	 * 转账确认-用于商户转用户回调后调用此接口
	 * @param platformNo
	 * @param requestNo
	 * @param mode
	 * @return
	 */
    private static boolean confirmTranferResultByTranser(String platformNo,String requestNo,String mode){
		
		YeeService service = new YeeService();
		YeeReqModel model = new YeeReqModel();
		model.setRequestNo(requestNo);
		model.setMode(mode);
		
		JSONObject resultJson = service.complete_Transaction(model);
		String code = resultJson.getString("code");
		
		if(!"1".equals(code)){
			return false;
		}
		return true;
	}
	
	/**
	 * 异步提交至P2P
	 * @param params
	 */
	private static String submitByAsynch(Map<String,Object> params){
		Logger.debug("--------------异步提交至P2Pstart-------------");
		String result = "";
		String url = String.valueOf(params.get(P2P_ASYN_URL_KEY));
		Map<String,String> baseParams = (Map<String, String>) params.get(P2P_BASEPARAMS);
		Logger.debug("CallBack to P2p Params : %s", params.toString());
		HttpResponse response = WS.url(url).setParameters(baseParams).post();
			if(response.getStatus().intValue() == HTTP_STATUS_SUCCESS.intValue()){
				result = response.getString();
			}
			Logger.debug("(Asyn)p2p return results:%s",result);
			return result;
	}
	
	/**
	 * 同步提交至P2P
	 * @param params
	 */
	private static void submitByFront(Map<String,Object> params){
		String action = String.valueOf(params.get(P2P_URL_KEY));
		Map<String,String> maps = (Map<String, String>) params.get(P2P_BASEPARAMS);
		Logger.debug("同步提交至P2P参数： %s", maps.toString());
		render(P2P_COMIT_PAGE,action,maps);
	}
	
	/**
	 * 多个简单Json合并
	 * @param json
	 * @return
	 */
	public static JSONObject json2ExtraJsonAppend(JSONObject ... json){
		if(json.length>1){
			int jsonSize = json.length;
			Set<String> keySet = null;
			for(int i = 1; i <jsonSize;i++){
				keySet  = json[i].keySet();
				for(String key : keySet){
					json[0].put(key, json[i].get(key));
				}
			}
		}
		return json[0];
		
	}
	
	/**
	 * 用于响应p2p中的pSign加密方法
	 * @param args 字符串数组
	 * @return 加密后value
	 */
	private static String haxMD5(String... args){
		if(args != null){
			StringBuffer buffer = new StringBuffer();
			for(String value : args){
				buffer.append(value);
			}
			buffer.append(Constants.ENCRYPTION_KEY);
			return Codec.hexMD5(buffer.toString());
		}
		Logger.error("hasMD5加密时,缺少参数");
		return "";
	}
	
	/**
	 * 构造buildP3DesXmlPara
	 * 
	 * @param jsonParams
	 * @return
	 */
	private static String buildP3DesXmlPara(JSONObject jsonParams) {
		String result = "";
		try {

			result = Converter.jsonToXml(jsonParams.toString(), "pReq", null,
					null, null);

		} catch (Exception e) { // 手动捕获异常,可能会存在Xtream转化出现异常

			Logger.error("buildP3DesXmlPara 时 %s", e.getMessage());
			return "{\"RespCode\":\"999\",\"RespDesc\":\"buildP3DesXmlPara异常\"}";
		}
		result = Encrypt.encrypt3DES(result, Constants.ENCRYPTION_KEY);
		return result;
	}
	
	
	
	/**
	 * 线程休眠 
	 * @param sec 单位/秒
	 */
	private static void sleep(int sec){
		try {
			new Thread().sleep(sec*1000);
		} catch (InterruptedException e) {
			Logger.error("线程休眠时:", e.getMessage());
		} 
	}
	
	
	/**
	 * 查询状态转化类
	 * @author yx
	 *	@create 2014年12月15日 下午4:36:31
	 */
	public static class TransStat{
		
		public static String getStat(String status,String queryTransType){
			if(YEEConstants.WITHDRAW_RECORD.equals(queryTransType)){
				if(status.equals("INIT")){
					
					return PTradeStatue_D;
				}else if(status.equals("SUCCESS")){
					
					return PTradeStatue_S;
				}
			}else if(YEEConstants.RECHARGE_RECORD.equals(queryTransType)){
				if(status.equals("INIT")){
					
					return PTradeStatue_F;
				}else if(status.equals("SUCCESS")){
					
					return PTradeStatue_S;
				}
			}else if(YEEConstants.CP_TRANSACTION.equals(queryTransType)){
				if(status.equals("N")){
					
					return PTradeStatue_S;
				}else if(status.equals("C")){
					
					return PTradeStatue_F;
				}else{
					
					return PTradeStatue_N;
				}
			} 
		return null;
	}
	}
	
}
