package controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import utils.Converter;
import utils.DataUtil;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.PaymentUtil;
import business.Platform;

import com.shove.security.Encrypt;

import constants.Constants;
import constants.IPSConstants;
import constants.IPSConstants.IPSOperation;
import constants.IPSConstants.IPSS2SUrl;
import constants.IPSConstants.IPSWebUrl;
import controllers.YEE.YEEPayment;

/**
 * 中间件支付控制器入口
 * @author Administrator
 *
 */
public class Payment  extends BaseController {


	/**
	 * 支付接口主入口
	 * @param version 接口版本
	 * @param type	接口类型
	 * @param memberId 会员id
	 * @param memberName 会员名称
	 * @param domain 约定密钥
	 */
	public static void spay(String version, int type, long memberId, String memberName, String domain,String isApp) {
		
		if(StringUtils.isBlank(version)) {
			flash.error("请传入晓风资金托管版本");
			Application.error();
		}
		
		if(type <= 0 ) {
			flash.error("传入参数有误");
			Application.error();
		}
		
		if(StringUtils.isBlank(domain)) {
			
			return ;
		}
		
		ErrorInfo error = new ErrorInfo();
		Platform platform = new Platform();
		platform.domain = Encrypt.decrypt3DES(domain, Constants.ENCRYPTION_KEY);
		
		if(error.code < 0) {
			flash.error(error.msg);
			Application.error();
		}
		
		Logger.debug("资金托管版本:version = %s", version);
		Logger.debug("------------------------请求的支付平台："+platform.gatewayId+"->"+platform.name+"-------------------------------");
		Logger.debug("------------------------请求的接口："+type+"-------------------------------");
		
		if(Constants.VERSION2.equals(version)){
			switch (platform.gatewayId) {
			//易宝
			case Constants.YEE:{
				String arg3DesXmlPara = params.get("arg3DesXmlPara"); // xml通过3des加密的参数
				String argSign = params.get("argSign"); // md5加密之后的校验参数
				String argeXtraPara = params.get("argeXtraPara"); // xml通过3des加密的参数2,
				String autoInvest = params.get("autoInvest"); // 自动投标标志
				String key = PaymentUtil.cacheParam(arg3DesXmlPara, argeXtraPara);  //缓存Xml节点数据
				Logger.debug("P2P平台传过来的参数->arg3DesXmlPara=:"+arg3DesXmlPara+"\nmd5加密之后的校验参数argSign=:"+argSign+"\nargeXtraPara=:"+argeXtraPara);
				String isWS = params.get("isWS");
				YEEPayment payment = new YEEPayment();
				payment.transfer(platform.gatewayId, platform.domain, type, (int)platform.id, memberId, memberName, argSign, autoInvest,arg3DesXmlPara,argeXtraPara,isApp,isWS);
				break;
			}
			}
		}
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
