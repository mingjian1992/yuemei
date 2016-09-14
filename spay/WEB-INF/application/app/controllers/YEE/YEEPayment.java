package controllers.YEE;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import services.YEE;
import services.YeeTools;
import services.YeeConfig;
import services.YeeToolsExtra;
import utils.Converter;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.PaymentUtil;
import utils.YEEUtil;
import business.DealDetail;
import business.GateWay;
import business.Member;

import com.shove.security.Encrypt;

import constants.Constants;
import constants.IPSConstants;
import constants.YEEConstants;
import controllers.Application;
import controllers.BaseController;
import controllers.YEE.SecureSign.SignUtil;
import models.t_yee_resq_params;

public class YEEPayment extends BaseController {

	private static final String TRANSFERPAGE = "/YEE/YEEPayment/transfer.html";
	
    /**
     * 易宝业务入口(放入正式环境前要去掉的两个方法，一个是日志，一个是充值（pd测试）用的)
     * @param getWayId  第三方接口id
     * @param domain  商户号
     * @param type  操作类型
     * @param platformId  平台的唯一id
     * @param memberId  会员id
     * @param memberName  会员名称
     * @param arg3DesXmlPara  P2P传过来的的解密内容
     * @param argSign  
     * @param argIpsAccount
     * @param autoInvest  自动投标标识
     * @param wsUrl
     * @param argeXtraPara  P2P传过来的的解密内容
     */
	public void transfer(int getWayId, String domain, int type, int platformId, long memberId, String memberName, 
			String argSign, String autoInvest, String arg3DesXmlPara,String argeXtraPara,String isApp,String isWS ) {
		Logger.debug("YEEPayment->transfer->>>易宝业务入口");
		ErrorInfo error = new ErrorInfo();
		
		String argMerCode =YeeConfig.getProperty("yee_merCustId");  //平台商户号
		Logger.debug("平台商户编号:argMerCode:"+argMerCode);
	
		//用户信息查询
		if(type == YEEConstants.ACCOUNT_BALANCE || type == YEEConstants.USER_INFO){
			Logger.debug("YEEPayment->transfer->>>易宝账户余额查询(ws)||账户信息查询(ws)操作renderText");
			String info = YEE.queryUserInfo(argMerCode, memberId);
			Logger.debug("返回给P2P平台info:=%s",info);
			renderText(info);
		}
		
		//与使用平台的校验
		if(!PaymentUtil.expansionCheckSign(argMerCode+arg3DesXmlPara+argeXtraPara, argSign)) {
			Logger.error("------------------------资金托管平台校验失败-------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		//3DES解密得到json值
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		JSONObject json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		Logger.debug("YEEPayment->transfer->>>3DES解密arg3DesXmlPara转换成json数据取名json:"+json);
		
		JSONObject jsonXtraPara = null;
		if(null != argeXtraPara){//不为空才解析xml形式的字符串
			argeXtraPara = Encrypt.decrypt3DES(argeXtraPara, Constants.ENCRYPTION_KEY);
			jsonXtraPara = (JSONObject)Converter.xmlToObj(argeXtraPara);
			Logger.debug("YEEPayment->transfer->>>3DES解密argeXtraPara转换成json数据取名:jsonXtraPara="+jsonXtraPara);
		}
		Logger.debug("YEEPayment->transfer->>>jsonXtraPara:\t"+jsonXtraPara);
		
		//交易记录查询 
		if(type == YEEConstants.QUERY_TRADE){
			Logger.debug("YEEPayment->transfer->>>交易记录查询操作renderText(info)");
			String info = YEE.query(argMerCode, json, jsonXtraPara);
			Logger.info("---------------交易记录查询 返回给P2P平台:"+info+"-----------");
			
			renderText(info);
		}
		
		//jsonMark用来封装下面信息保存到交易记录或事件中->t_member_details->summary  下面json是主要参数
		JSONObject jsonMark = new JSONObject();
		jsonMark.put("pS2SUrl", json.getString("pS2SUrl"));
		jsonMark.put("type", type);
		jsonMark.put("memberId", memberId+"");
		String pMerBillNo = json.containsKey("pMerBillNo") ? json.getString("pMerBillNo") : "";
		jsonMark.put("pMerBillNo", pMerBillNo);
		jsonMark.put("platformId", platformId+"");
		jsonMark.put("pMemo1", json.getString("pMemo1"));
		jsonMark.put("domain", domain);
		Logger.debug("jsonMark用来封装下面信息保存到交易记录或事件中->summary/remark\t"+jsonMark.toString());
		
		//商户转用户没有传这两个值TRANSFER_MER_TO_USER = 34
		if(type != YEEConstants.TRANSFER_MER_TO_USER){
			jsonMark.put("pMemo2", json.getString("pMemo2"));
			jsonMark.put("pMemo3", json.getString("pMemo3"));
		}
		
		//登记债权转让的时候会传递pMemo2（装让模式）
		if (type == YEEConstants.REGISTER_CRETANSFER) {
			jsonMark.put("pMemo2", json.getString("pMemo2"));
		}
		
		//充值操作添加此字段RECHARGE=8
		if(YEEConstants.RECHARGE == type){
			jsonMark.put("amount", json.getString("pTrdAmt"));
		}
		
		//用户转商户操作添加此字段TRANSFER_USER_TO_MER=32
		if(YEEConstants.TRANSFER_USER_TO_MER == type){
			jsonMark.put("amount", jsonXtraPara.getString("TransAmt"));
		}
		
		if (YEEConstants.REPAYMENT_SIGNING == type) {
			jsonMark.put("orderNo", json.getString("orderNo"));
		}
		
		String summary = jsonMark.toString();
		
		//标的登记,直接返回成功信息到P2P易宝无标的登记接口，2.0改成投标时登记REGISTER_SUBJECT=2
		if(type == YEEConstants.REGISTER_SUBJECT ){
			
			Map<String, String> args =null;
			
			Logger.debug("登记标操作类型:="+json.getString("pOperationType"));
			if(json.getString("pOperationType").equals(YEEConstants.BID_OPERRATION_TYPE)){//流标
				
				Logger.debug("---------------流标保存参数:"+summary+"-----------");
				
				args = YEE.flowCall(argMerCode, json, memberId, jsonXtraPara, summary);
				if(json.getString("pMemo2").equals("auto_flow_bid")){//modify by zhouyuxiong if是自动流标，返回json
					renderJSON(args);
				}
				
			}else{//标的登记直接返回p2p平台,没有专有的标的登记接口。会在投标时易宝会登记标的信息。
				
				Logger.debug("---------------标的登记保存参数:"+summary+"-----------");
				
				args = YEE.bidCall(argMerCode, json, jsonXtraPara, memberId);
			}
			
			args.put("redictMark", "2");
			args.put("url", json.getString("pWebUrl"));
			
			Logger.debug("---------------标的登记返回:"+args+"-----------");
			
			render(TRANSFERPAGE,args);
		}
		
		//转账9
		if(type == YEEConstants.TRANSFER){
			int transferType = json.getInt("pTransferType");
			String args = null;
			
			//放款1  放款WS出来更具所有投标时的流水号一个一个的转账确认
			if(YEEConstants.P2P_LOAN == transferType){
				
				Logger.debug("---------------放款:"+summary+"-----------");
				
				args = YEE.loanWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
				
				Logger.debug("---------------放款转账结果返回给P2P平台:"+args+"-----------");
			}
			
			//债权转让4
			if(YEEConstants.P2P_TRANSFER == transferType){
				
				Logger.debug("---------------转账债权转让:"+summary+"-----------");
				
				args = YEE.transferWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
				
				Logger.debug("---------------债权转让转账结果返回给P2P平台:"+args+"-----------");
			}
			
			//代偿还款（本金垫付后借款人还款 -- 用户转商户）3
			if(YEEConstants.COMPENSATE_REPAYMENT == transferType){
				
//				Logger.debug("---------------债权转让结果:"+summary+"-----------");
//				
//				args = YEE.transferWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
//				
//				Logger.debug("---------------债权转让转账结果返回给P2P平台:"+args+"-----------");
				if("Y".equals(isWS)){
					renderText("form_post");
				}
				compensatoryRepayment(memberId, transferType, platformId, json, jsonXtraPara, error,domain);
			}
			
			//（线下收款，本金垫付--商户转用户）2
			if(YEEConstants.COMPENSATE == transferType){
				
				
//				args = YEE.transferWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
				YEE buz = new YEE();
				args = buz.compensatory(memberId, transferType, platformId, json, jsonXtraPara,argMerCode, error);
				
			}
			
			//（发红包--商户转用户）5
			if(YEEConstants.P2P_RED == transferType){
				
				YEE buz = new YEE();
				args = buz.redPaperSend(memberId, transferType, platformId, json, jsonXtraPara,argMerCode, error);
				
			}
			
			renderText(args);
		}
		
		//确认或者取消某一笔转账
		if (type == YEEConstants.CONFIRM_TRANSFER) {
			String args = YEE.confirmTransfer(platformId, json, jsonXtraPara, argMerCode, error);
			
			Logger.debug("---------------确认或者取消某一笔转账结果返回给P2P平台:"+args+"-----------");
			
			renderText(args);
		}
		
		/**
		 * 平台划款（WS商户转用户）34
		 */
		if(type == YEEConstants.TRANSFER_MER_TO_USER){
			String args = YEE.plateToTransfer(platformId, memberId, json, jsonXtraPara, argMerCode, error);
			
			Logger.debug("---------------平台划款结果返回给P2P平台:"+args+"-----------");
			
			renderText(args);
		}
		
		/**
		 * 解除冻结资金36
		 */
		if(type == YEEConstants.UNFREZZ_AMOUNT){
			String args = YEE.revocationTransfer(platformId, argMerCode, json, jsonXtraPara, memberId);
			
			Logger.debug("---------------解除冻结资金结果返回给P2P平台:"+args+"-----------");
			
			renderText(args);
		}
		
		/*自动投标*/
		if(YEEConstants.AUTO_INVEST.equals(autoInvest)) {
			Logger.debug("---------------自动投标:"+json+"-----------");
			
			String args = YEE.antoTransfer(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			
			if(error.code < 0) {
				flash.error(error.msg);
				Application.error();
			}
			
			Logger.debug("---------------自动投标返回给P2P平台:"+args+"-----------");
			
			renderText(args);
		}
		
		/*判断订单号是否已存在*/
		if(DealDetail.isSerialNumberExist(platformId, pMerBillNo) || DealDetail.isExistOfEvent(platformId, pMerBillNo)) {
			flash.error("已提交处理，请勿重复提交");
			
			Application.error();
		}
		
		/*根据身份证判断是否开户1*/
		if(type == IPSConstants.CREATE_ACCOUNT) {
			Logger.debug("YEEPayment->transfer->>>开户操作");
			String idNumber = json.getString("pIdentNo");
			
			/*身份证已存在*/
			if(Member.isCreateAccount(json.getString("pIdentNo"), domain, error)) {
				Logger.debug("YEEPayment->transfer->>>开户用户身份证存在");
				/*用户平台关系表中不存在该身份证会员的信息*/
				if(error.code == 1) {
					Member member = new Member();
					long id = Member.queryIdByIdNumber(json.getString("pIdentNo"));
					member.memberId = id;
					member.platformId = platformId;
					member.memberId = platformId;
					member.platformMembername = memberName;
					member.addPlatformmember(error);
				
				/*不同平台，使用相同的支付接口，表示身份证会员已开户，在平台关系表中插入数据*/	
				}else if(error.code == 2) {
					Member member = new Member();
					member.idNumber = idNumber;
					
					member.memberId = platformId;
					member.platformMembername = memberName;
					member.platformMemberAccount = member.queryAccount(idNumber, platformId);
					
					member.addPlatformmember(error);
					
					flash.error(error.msg);
					Application.error();
				}else if(error.code == 3) {
					
					Member.updateAccount(platformId, platformId, Member.queryAccount(idNumber, platformId));
					flash.error("已开户");
					Application.error();
				}else if(error.code == 4) {
					
				}else {
					flash.error(error.msg);
					Application.error();
				}
			}else {
				Logger.debug("YEEPayment->transfer->>>身份证不存在");
				/*身份证不存在，根据请求在用户表和用户平台关系表中添加记录*/
				Member member = new Member();
				
				member.idNumber = json.getString("pIdentNo");
				member.mobile = json.getString("pMobileNo");
				member.platformId = platformId;
				member.platformMemberId = memberId;
				member.memberId = platformId;
				member.platformMembername = memberName;
				
				Map<String, String> info = member.add();
				
				String content = "您在资金托管平台注册的用户名：" + info.get("name") + "  密码：" + info.get("password");
				
//				EmailUtil.sendEmail(json.getString("pEmail"), "注册信息", content);
			}
		}
		
		jsonMark.put("pWebUrl", json.getString("pWebUrl"));  //此处添加该记录是因为ws请求没有传这个值过来
		summary = jsonMark.toString();
		
		Logger.debug("YEEPayment->transfer->>>YEE->entrance->>>接口请求入口"+json);
		/*接口请求入口*/
		Map<String, String> args = YEE.entrance(type, platformId, memberId, memberName, argMerCode, json, summary, jsonXtraPara, domain, error);
		
		//判断交易记录是否添加成功，失败跳到失败页面
		if(error.code < 0){
			flash.error(error.msg);
			Application.error();
		}
		
        args.put("redictMark", "1");  //登记标的接口易宝没有，故用该字段在页面区别提交
		
		Logger.debug("YEEPayment->transfer->>>WEB请求参数到易宝:\n"+args+"\n");
		
		if(IPSConstants.STRTRUE.equalsIgnoreCase(isApp)){
			Logger.debug("YEEPayment->transfer->>>isAppInvoke spay..... type: ["+type+"]");
			args.put("url", args.get("url").replace("bha", "bhawireless"));
			renderJSON(args);
		}else{
			render(TRANSFERPAGE,args);
		}
	}
	
	/**
	 * 代偿还款
	 * @param memberId
	 * @param type
	 * @param platform
	 * @param json
	 * @param extrajson
	 * @param error
	 * @param domain
	 */
	private static void compensatoryRepayment(long memberId, int type, int platform,
			JSONObject json, JSONObject extrajson,ErrorInfo error,String domain){
		
		Object pDetails = json.get("pDetails");
		JSONArray jsonArr = null;
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		double countAmt = 0.00;
		String usrCustId = "";
		for (int i = 0;i<jsonArr.size();i++) {
			JSONObject pRow = (JSONObject)jsonArr.get(i);
			countAmt += Double.parseDouble(pRow.getString("pTrdAmt"));
			usrCustId = pRow.getString("pFIpsAcctNo");
		}
		
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.debug("------------------------用户转商户(代偿还款)json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", usrCustId);  //出款人平台用户编号
		userMap.put("requestNo", json.getString("pMerBillNo"));  //请求流水号
		userMap.put("userType", "MEMBER");  //出款人类型
		userMap.put("bizType", "TRANSFER");  //固定值TRANSFER
		userMap.put("callbackUrl", Constants.BASE_URL + "yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "yee/notifys");
		
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		Map<String, String> properties = new HashMap<String, String>();
		
		properties.put("targetUserType", "MERCHANT");  //收款人用户类型
		properties.put("amount", String.format("%.2f", countAmt));  //转入金额
		properties.put("targetPlatformUserNo", domain);  //商户编号
		properties.put("bizType", "TRANSFER");  //商户编号
		arrJson.add(properties);
		JSONObject mark = YEE.json2ExtraJsonAppend(json,extrajson);
		mark.put("type", YEEConstants.COMPENSATORYREPAYMENT);
		mark.put("platformId", platform);
		mark.put("memberId", memberId);
		mark.put("domain", domain);
		
		
		DealDetail.addEvent(memberId,3+200, platform, json.getString("pMerBillNo"), null, null, mark.toString(), "用户转商户(代偿还款)");
		
		DealDetail detail = new DealDetail(platform, memberId, json.getString("pMerBillNo"), 
				YEEConstants.COMPENSATORYREPAYMENT, extrajson.getDouble("amount"), false, "用户转商户(代偿还款)");
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致用户转商户失败";
			
		}
		
		userMap.put("details", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = YEEUtil.addAttribute(domain, req);
		
		// 处理换行以及回车换行
		req=req.replace("\r\n", "");
		req=req.replace("\n", "");
				/*将使用平台的请求信息转化成第三方支付的请求参数*/
        String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
        Map<String, String> args = new HashMap<String, String>();       
        args.put("redictMark", "1");  //登记标的接口易宝没有，故用该字段在页面区别提交
        args.put("req", req);
        args.put("sign", sign);
        args.put("url",Play.configuration.getProperty("yee.transfer") );
        printMap("代偿还款请求参数",args);
        render("/YEE/YEEPayment/transfer.html",args);
	}
	
	/**
	 * 同步通知
	 * @param resp xml格式的资金托管方返回数据
	 * @param sign 签名
	 */
    public static void callBack(String resp, String sign, boolean isRepair){
    	ErrorInfo error = new ErrorInfo();
    	String url =  Constants.BASE_URL + "yee/callBack?resp="+resp+"&sign="+sign;
//    	YeeTools.wpRespParams("易宝同步回调", false, resp);
    	YeeToolsExtra.recordRespParams("易宝同步回调", false, resp,sign,url);
		Logger.debug("---------------------yee callback response:"+resp+"---------------------------");
		Logger.debug("---------------------yee callback sign:"+sign+"-------------------------------");
		Logger.debug("------------------------支付平台校验成功------------------------------");
		String merchantNo = YEEConstants.YEEPAY;
//		if (!isRepair) {
//			if (!SignUtil.verifySign(resp, sign, merchantNo)) {
//				
//				throw new RuntimeException("验签失败！");
//			}
//		}
	
		Map<String, String> args = YEE.exit(resp, error);
		
		Logger.debug("请求p2p平台:"+JSONObject.fromObject(args).toString());
		
		render(args);
	}
    
    /**
     * 异步回调
     * @param notify xml格式的资金托管方返回数据
     * @param sign 签名
     */
    public static void notifys(String notify, String sign, boolean isRepair){
    	String url =  Constants.BASE_URL + "yee/notifys?notify="+notify+"&sign="+sign;
    	Logger.info("yee anys callback url link:%s", url);
    	ErrorInfo error = new ErrorInfo();
//    	YeeTools.wpRespParams("易宝异步回调", true, notify);
    	YeeToolsExtra.recordRespParams("易宝异步回调", true, notify,sign,url);
		Logger.debug("------------------------yee anys callback result:"+notify+"------------------------------");
		Logger.debug("------------------------yee anys callback sign:"+sign+"------------------------------");
		String merchantNo = YEEConstants.YEEPAY;
//		if (!isRepair) {
//			if (!SignUtil.verifySign(notify, sign, merchantNo)) {
//				
//				throw new RuntimeException("验签失败！");
//			}
//		}
		
		String args = YEE.notifyExit(notify, error);
		
		if(Integer.parseInt(args) > 0 || Integer.parseInt(args) == Constants.ALREADY_RUN){
			renderText("SUCCESS");
		}
		
		renderText("已处理");
    }
    
    /**
     * 转账确认
     * @param notify
     * @param sign
     */
    public static void comTransaction(String notify,String sign){
    	String url =  Constants.BASE_URL + "yee/comTransaction?notify="+notify+"&sign="+sign;
    	YeeToolsExtra.recordRespParams("易宝异步回调", true, notify,sign,url);
    	renderText("SUCCESS");
    }
    
    /**
     * 补单
     */
    public static void offRepair(){
    	String id = params.get("id");
    	if (StringUtils.isBlank(id)) {
    		
    		renderText("ERROR");
		}
    	
    	t_yee_resq_params res = null;
    	try {
			res = t_yee_resq_params.findById(Long.parseLong(id));
		} catch (Exception e) {
			e.printStackTrace();
			
			renderText("ERROR");
		}
    	
    	if (null == res) {
			
    		renderText("ERROR");
		}
    	
    	notifys(res.result, res.sign, true);
    }
    
    //打印日志用
	public static void logFileList(String path)throws Exception{
	    path = path.replace("~", File.separator);
	    System.out.println(path);
	    File file = new File(path);
	    String[] list = null;
	    String result = "";
	    if(file.isFile()){
	    System.out.println("file");
	    result = IOUtils.toString(new FileInputStream(file),"utf-8");
	    renderText(result);
	    }else{
	    System.out.println("dir");
	    list = file.list();
	    }
	    /*File children = null;
	    for(String l : list){
	    children = new File(path+l);
	    if(children.isFile()){
	    System.out.println("isFile:"+children.getAbsolutePath());
	    }else{
	    System.out.println("isDir:"+children.getAbsolutePath());
	    }
	    }*/
	    renderJSON(list);
	   
	    }
}
