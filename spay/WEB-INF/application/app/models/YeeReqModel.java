package models;

/**
 * 易宝请求业务模型
 * @author yangxuan
 * @date 20150319
 */
public class YeeReqModel {

	private String platformNo;  //商户编号
	private String platformUserNo;  //平台会员编号
	private String requestNo;  //各个业务的请求流水号
	private String mode;  //查询模式WITHDRAW_RECORD：提现记录RECHARGE_RECORD：充值记录CP_TRANSACTION：划款记录FREEZERE_RECORD：冻结/解冻接口
	
	
	public String getRequestNo() {
		return requestNo;
	}
	public void setRequestNo(String requestNo) {
		this.requestNo = requestNo;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getPlatformNo() {
		return platformNo;
	}
	public void setPlatformNo(String platformNo) {
		this.platformNo = platformNo;
	}
	public String getPlatformUserNo() {
		return platformUserNo;
	}
	public void setPlatformUserNo(String platformUserNo) {
		this.platformUserNo = platformUserNo;
	}
	
	
	
	
}
