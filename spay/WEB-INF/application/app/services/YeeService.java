package services;

import java.io.Serializable;

import constants.Constants;


import net.sf.json.JSONObject;
import models.YeeReqModel;

/**
 * 易宝业务类
 * 
 * @author yangxuan
 * @date 20150319
 */
public class YeeService extends YeeBaseService implements Serializable {

	/**
	 * 校验用户是否绑卡
	 * 
	 * @param model
	 * @return
	 */
	public boolean isBindCard(YeeReqModel model) {
		JSONObject json = account_Info(model);
		return json.get("cardNo") == null ? false : true;
	}

	/**
	 * 转账确认
	 * 
	 * @return
	 */
	public JSONObject complete_Transaction(YeeReqModel model) {
		putValue("service", "COMPLETE_TRANSACTION")
				.putValue("platformNo", YeeConfig.getProperty("yee_merCustId"))
				.putValue("requestNo", model.getRequestNo())
				.putValue("mode", model.getMode())
				.putValue("notifyUrl", Constants.BASE_URL + "yee/comTransaction");
		return doExcute();
	}

	/**
	 * 账户查询
	 * 
	 * @param model
	 * @return
	 */
	public JSONObject account_Info(YeeReqModel model) {
		putValue("service", "ACCOUNT_INFO");
		putValue("platformNo", YeeConfig.getProperty("yee_merCustId"))
				.putValue("platformUserNo", model.getPlatformUserNo());
		return doExcute();
	}

	/**
	 * 单笔交易查询
	 * 
	 * @param model
	 * @return
	 */
	public JSONObject query(YeeReqModel model) {
		putValue("service", "QUERY")
				.putValue("platformNo", YeeConfig.getProperty("yee_merCustId"))
				.putValue("requestNo", model.getRequestNo())
				.putValue("mode", model.getMode());
		return doExcute();
	}

}
