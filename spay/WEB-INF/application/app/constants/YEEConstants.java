package constants;

import play.Play;
import services.YeeConfig;

public class YEEConstants {
	
	/**
	 * 日志根路径
	 */
	public final static String LOGFILEROOT = Play.configuration.getProperty("logroot");
	
	public final static String merchantNo = YeeConfig.getProperty("yee_merCustId");
	
	public final static String YEEPAY = "yeepay.com";

	/**
	 * 接口操作类型
	 */
	public static final int CREATE_ACCOUNT = 1; //开户
	public static final int REGISTER_SUBJECT = 2;  //标的登记(ws)(标的第一个投资的时候开始登记)
	public static final int REGISTER_CREDITOR = 3;  //登记债权人（投标）
	public static final int REGISTER_GUARANTOR = 4;  //登记担保方--易宝用不上
	public static final int REGISTER_CRETANSFER = 5;  //登记债权转让（债权转让）
	public static final int AUTO_SIGING = 6;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 7;  //自动还款签约
	public static final int RECHARGE = 8;  //充值
	public static final int TRANSFER = 9;  //转账（放款成功资金转移）
	public static final int REPAYMENT = 10;  //还款
	public static final int UNFREEZE = 11;  //解冻保证金
	public static final int DEDUCT = 12;  //自动代扣充值
	public static final int WITHDRAWAL = 13;  //提现
	public static final int ACCOUNT_BALANCE = 14;  //账户余额查询(ws)
	public static final int BANK_LIST = 15;  //商户端获取银行列表
	public static final int USER_INFO = 16;  //账户信息查询(ws)
	public static final int QUERY_TRADE = 17;  //交易查询(ws)
	public static final int BOUND_CARD = 18;  //绑定银行卡
	public static final int UNBOUND_CARD = 19;  //取消绑定银行卡
	public static final int TRANSFER_USER_TO_MER = 32;  //用户转商户
	public static final int TRANSFER_MER_TO_USERS = 33;  //商户转用户(发送投标奖励post)
	public static final int TRANSFER_MER_TO_USER = 34;  //商户转用户（发放cps奖励，单笔ws ）
	public static final int UNFREZZ_AMOUNT = 36;  //解除冻结资金
	public static final int UPDATE_PAY_PASS = 37;  //修改支付密码
	public static final int AUTO_REPAYMENT = 38; //自动还款
	public static final int CONFIRM_TRANSFER = 39; //取消或者是确认某一笔转账
	public static final int UPDATE_MOBILE = 40; //修改手机号码
	
	public static final int ENTERPRISE_REGISTER = 41; //企业注册 
	public static final int TRANSFER_USER_TO_USER = 42;//用户转用户
	
	//compensatoryRepayment
	public static final int COMPENSATORYREPAYMENT = 55;
	
	
	public static final String SP2P_MOBILE_S2SURL = Play.configuration.getProperty("sp2p_mobile_s2sURL");
	public static String[] IPS_URL_TEST = {
		"",  //0
		Play.configuration.getProperty("yee.register"),  //1开户
		"",  //2
		Play.configuration.getProperty("yee.invest.bid"),  //3登记债权人
		"",  //4
		Play.configuration.getProperty("yee.transfer"),  //5登记债权转让
		Play.configuration.getProperty("yee.auto.invest"),  //6自动投标签约
		Play.configuration.getProperty("yee.pay.autoRepayment"),  //7自动还款签约
		Play.configuration.getProperty("yee.recharge"),  //8充值
		Play.configuration.getProperty("yee.money.transfer"),  //9转账
		Play.configuration.getProperty("yee.repayment"),  //10还款
		Play.configuration.getProperty("yee.pay.autoRepayment"),  //11自动还款
		Play.configuration.getProperty("yee.pay.autoRepayment"),  //12解冻保证金
		Play.configuration.getProperty("yee.withdraw"),	//13提现
		"",  //14
		"",  //15
		"",  //16
		"",	//17
		Play.configuration.getProperty("yee.bindcard"),	//18绑卡
		Play.configuration.getProperty("yee.unbindcard"),	//19取消绑卡
		"",  //20
		"",  //21
		"",  //22
		"",  //23
		"",  //24
		"",  //25
		"",  //26
		"",  //27
		"",  //28
		"",  //29
		"",  //30
		"",  //31
		Play.configuration.getProperty("yee.user.to.mar"),  //32
		"",  //33
		"",  //34
		"",  //35
		"",  //36
		Play.configuration.getProperty("yee.pay.pass"),  //37
		"",  //38
		"",  //39
		Play.configuration.getProperty("yee.pay.mobile"),   //40
		Play.configuration.getProperty("yee.pay.enterprise"),//41
		Play.configuration.getProperty("yee.repayment")//还款
	};
	
	public static final String BID_FLOWS = "flow";
	public static final String AUTO_INVEST = "autoInvest";
	public static final String AUTO_PAYMENT = "autoPayment";
	public static final boolean isTest = true;
	
	public static String[] QUERY_DEDAIL = {"","","",
		"PAYMENT_RECORD",  //标的投资放款记录
		"","","","",
		"RECHARGE_RECORD",  //充值记录
		"WITHDRAW_RECORD",  //提现记录
		"",
		"REPAYMENT_RECORD",  //标的还款记录
		"","",
		"PAYMENT_RECORD",  //标的投资放款记录
		};
	
	public static final String YEE_SIGN_URL = Play.configuration.getProperty("yee.fix");  //证书路径
	public static final String YEE_SIGN_PASS = Play.configuration.getProperty("yee.fix.pass");  //证书密钥
	public static final String YEE_URL_REDICT = Play.configuration.getProperty("yee.url.redict");  //直接接口访问路径
	
	public static final int P2P_LOAN = 1;  //转账---1放款
	public static final int COMPENSATE = 2;  //代偿（线下收款，本金垫付--商户转用户）
	public static final int COMPENSATE_REPAYMENT = 3;  //代偿还款（本金垫付后借款人还款 -- 用户转商户）
	public static final int P2P_TRANSFER = 4;  //转账---4债权转让
	public static final int P2P_RED = 5;  //红包奖励
	
	/**
	 * 保存在数据库中绑卡状态
	 */
	public static final int CARD_NO_BANG = 0;  //未绑定
	public static final int CARD_SUBMIT_SUCCESS = 1;  //受理成功
	
	/**
	 * 资金托管返回的绑卡状态
	 */
	public static final String CARD_HANDDLE = "VERIFYING";  //认证中
	public static final String CARD_SUCCESS = "VERIFIED";  //已认证
	
	/**
	 * 用户类型
	 */
	public static final String MEMBER_PERSON = "PERSONAL"; //个人用户
	public static final String MEMBER_ENTERPRISE = "ENTERPRISE"; //企业用户
	
	/**
	 * 资金托管返回的会员激活状态
	 */
	public static final String USER_ACTIVATED = "ACTIVATED";  //已激活
	public static final String USER_DEACTIVATED = "DEACTIVATED";  //未激活
	
	/**
	 * 标的还款方式
	 */
	public static final String BID_ERPATTYPE = "99";  //秒还还款
	
	/**
	 * 登记标的操作方式
	 */
	public static final String BID_OPERRATION_TYPE = "2";  //流标 2
	
	/**
	 * 登记债权人操作方式
	 */
	public static final String REGISTER_CREDITOR_TYPE = "2";  //
	
	/**
	 * 易宝在数据库的平台id号
	 */
	public static final String PLATFORM_NO = Play.configuration.getProperty("yee.platform.no");  
	
	/**
	 * WS请求P2P
	 */
	public static final String YEE_CALLBACK = Play.configuration.getProperty("yee.callback");
	
	/**
	 * 查询补单的类型
	 */
	public class QueryType{
		public static final int QUERY_INVEST_TYPE = 3;  //登记债权人
		public static final int QUERY_TRANSFERS_TYPE = 5;  //登记债权转让
		public static final int QUERY_RECAHARGE_TYPE = 8;  //充值
		public static final int QUERY_WITHDRAW_TYPE = 9;  //提现
		public static final int QUERY_REPAYMENT_TYPE = 11;  //还款
		public static final int QUERY_MONEY_TRANSFER_TYPE = 14;  //转账(放款)
	}
	
	public class Status {
		public static final String SUCCESS = "1";		//成功
		public static final String FAIL = "2";			//失败
		public static final String HANDLING = "3";		//处理中
	}
	
	/**
	 * 提现记录
	 */
	public static final String WITHDRAW_RECORD = "WITHDRAW_RECORD";  //提现记录
	/**
	 * 充值记录
	 */
	public static final String RECHARGE_RECORD = "RECHARGE_RECORD";  //充值记录
	/**
	 * 划款记录
	 */
	public static final String CP_TRANSACTION = "CP_TRANSACTION";  //划款记录
	/**
	 * 冻结/解冻接口
	 */
	public static final String FREEZERE_RECORD = "FREEZERE_RECORD";  //冻结/解冻接口
	//add by yangxuan start
	/**
	 * 直接转账-此接口可以从平台的商户账户批量划转资金到其他个人账户
	 */
	public static final String DIRECT_TRANSACTION = "DIRECT_TRANSACTION";  
	//added
}
