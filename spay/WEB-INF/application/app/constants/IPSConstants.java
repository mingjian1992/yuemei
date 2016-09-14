package constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shove.security.Encrypt;

import business.BackstageSet;
import business.Supervisor;
import play.Play;
import utils.Security;

/**
 * 常量值
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-7 下午04:07:56
 */
public class IPSConstants {
	/**
	 * 环迅支付
	 */
	public static final boolean IPS_TEST_HX = "true".equals(Play.configuration.getProperty("IPS.TEST.HX")) ? true : false;
	public static String CERT_MD5 = null;
	public static String PUB_KEY = null;
	public static String DES_KEY = null;
	public static String DES_IV = null;
	
	
	/** 小写字符串 true */
	public static String STRTRUE = "true";
	
	

	/**
	 * 环迅地址
	 */
	
	public static String[] IPS_URL_FORMAL = null;
	public static String[] IPS_URL_TEST = {"",
		Play.configuration.getProperty("hx.account.create"),	//标的登记
		Play.configuration.getProperty("hx.subject.register"),	//登记债权人
		Play.configuration.getProperty("hx.creditor.register"), //登记担保方
		Play.configuration.getProperty("hx.guarantor.register"),//登记债权转让
		Play.configuration.getProperty("hx.cretansfer.register"),//自动投标签约
		Play.configuration.getProperty("hx.auto.sign"),		//自动还款签约
		Play.configuration.getProperty("hx.repayment.sign"),//充值
		Play.configuration.getProperty("hx.do.trade"),		//还款
		Play.configuration.getProperty("hx.transfer"),		//转账
		Play.configuration.getProperty("hx.repayment.trade"),//还款
		Play.configuration.getProperty("hx.unfreeze.guarantee"),//解冻保证金
		Play.configuration.getProperty("hx.co.trade"),		//自动代扣充值
		Play.configuration.getProperty("hx.dw.trade"),		//提现
		Play.configuration.getProperty("hx.balance.query"), //账户余额查询
		Play.configuration.getProperty("hx.bank.list"),		//商户端获取银行列表
		Play.configuration.getProperty("hx.user.info"),		//账户信息查询
		Play.configuration.getProperty("hx.query.trade"),		//账户信息查询
	};
	public static String[] IPS_URL = IPS_TEST_HX ? IPS_URL_TEST : IPS_URL_FORMAL;
	
	public static String IPS_ACCOUNT_WEB = Constants.BASE_URL + "ips/ipsCommit"; 
	public static String IPS_ACCOUNT_S2S = Constants.BASE_URL + "ips/ipsCommitAsynch";
	public static String WS_URL = Play.configuration.getProperty("ws.url");
	public static String WS_URL_QUERY = Play.configuration.getProperty("ws.url.query");
	/**
	 * 接口操作类型
	 */
	public static final int CREATE_ACCOUNT = 1; //开户
	public static final int REGISTER_SUBJECT = 2; //标的登记
	public static final int REGISTER_CREDITOR = 3; //登记债权人
	public static final int REGISTER_GUARANTOR = 4; //登记担保方
	public static final int REGISTER_CRETANSFER = 5; //登记债权转让
	public static final int AUTO_SIGING = 6;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 7; //自动还款签约
	public static final int RECHARGE = 8; //充值
	public static final int TRANSFER = 9;  //转账
	public static final int REPAYMENT = 10; //还款
	public static final int UNFREEZE = 11; //解冻保证金
	public static final int DEDUCT = 12; //自动代扣充值
	public static final int WITHDRAWAL = 13; //提现
	public static final int ACCOUNT_BALANCE = 14; //账户余额查询
	public static final int BANK_LIST = 15; //商户端获取银行列表
	public static final int USER_INFO = 16; //账户信息查询
	public static final int QUERY_TRADE = 17; //交易查询
	
	public static final String BID_FLOWS = "flow";
	public static final String AUTO_INVEST = "autoInvest";
	public static final boolean isTest = true;
	
	

	public static final String MER_CODE = Play.configuration.getProperty("pay.mer_no");//平台商户号
	public static final String MER_NAME = Play.configuration.getProperty("pay.mer_name");//平台商户名
	public static final String MER_IDENT_NO = Play.configuration.getProperty("pay.mer_ident_no");//平台证件号码
	
	public static final String GUARANTOR_CODE = Play.configuration.getProperty("pay.guarantor_code");//担保机构商户号
	public static final String GUARANTOR_NAME = Play.configuration.getProperty("pay.guarantor_name");//担保机构名称
	
	public static final String PLATFORM = "2";		//所在资金托管的平台id

	public static final String ACTION = Play.configuration.getProperty("pay.action");	//资金托管url
	public static final String CALLBACK_URL = Play.configuration.getProperty("pay.callback.url");		//回调
	
	public static final String DOMAIN = Encrypt.encrypt3DES(Play.configuration.getProperty("pay.domain"), Constants.ENCRYPTION_KEY);//p2p平台域名
	
	public static final String YEE_SIGN_URL = Play.configuration.getProperty("yee.fix");  //证书路径
	public static final String YEE_SIGN_PASS = Play.configuration.getProperty("yee.fix.pass");  //证书密钥
	
	
	
	
	
	public static final String CACHE_TIME = "1h";	//标，投资等信息缓存时间
	
	public static final String INDENT_TYPE = "1";		//证件类型：1#身份证
	public static final String ACCT_TYPE = "1";		//账户类型：1#个人
	public static final String ACCT_AGENCY = "0";		//账户类型：0#机构
	public static final String UNFREEZENT_YPE = "1";		//1#解冻借款方;2#解冻担保方
	public static final String CHANNEL_TYPE = "1";	//充值渠道类型：1#网银充值
	public static final String IPS_FEE_TYPE = "1";	//谁付ips手续费：1#平台支付，2#用户支付
	public static final String VALID_TYPE = "N";	//自动还款有限期类型：N#长期有效
	public static final String VALID_DATE = "0";	//自动还款有效期：0
	public static final String OUT_TYPE = "1";	//提现模式：1#普通提现，2#定向提现<暂不开放>
	public static final String DAY_CYCLE_TYPE = "1";	//借款周期类型：1#天，3#月
	public static final String MONTH_CYCLE_TYPE = "3";	//借款周期类型：1#天，3#月
	public static final String PAID_MONTH_EQUAL_PRINCIPAL_INTEREST = "1"; // 按月还款、等额本息
	public static final String PAID_MONTH_ONCE_REPAYMENT = "2"; // 按月付息、一次还款
	public static final String ONCE_REPAYMENT = "3"; // 等额本金
	public static final String OTHER_REPAYMENT = "99"; // 还款方式：其他
	public static final String OPERATION_TYPE_1 = "1"; // 标的操作类型：1#新增，2#结束
	public static final String OPERATION_TYPE_2 = "2"; // 标的操作类型：1#新增，2#结束
	public static final String REPAYMENT_TYPE = "1"; // 还款类型：1#手动还款，2#自动还款
	public static final String REPAYMENT_TYPE_2 = "2"; // 还款类型：1#手动还款，2#自动还款
	public static final String BID_FLOW = "flow"; // 流标标示
	public static final String AUTO_PAYMENT = "autoPayment"; // 自动还款标示
	
	public static final String BID_CREATE = "create"; //发标
	public static final String BID_CANCEL = "cancel"; //审核中->撤销
	public static final String BID_CANCEL_B = "cancelB"; //提前借款->借款中不通过
	public static final String BID_CANCEL_S = "cancelS"; //审核中->审核不通过
	public static final String BID_CANCEL_I = "cancelI"; //筹款中->借款中不通过
	public static final String BID_CANCEL_M = "cancelM"; //满标->放款不通过
	public static final String BID_CANCEL_F = "cancelF"; //提前借款->撤销
	public static final String BID_CANCEL_N = "cancelN"; //筹款中->撤销
	public static final String BID_ADVANCE_LOAN = "flowA"; //提前借款->流标
	public static final String BID_FUNDRAISE = "flowI"; //筹款中->流标
	
	public static double MIN_AMOUNT = 1.0;						//标的借款额度限额最小值
	public static int EXCUTED = -10;	//已执行
	
	public class IPSOperation {
		public static final int CREATE_IPS_ACCT = 1;			//开户
		public static final int REGISTER_SUBJECT = 2;			//标的登记
		public static final int FLOW_BID = 22;					//标的登记（流标）
		public static final int REGISTER_CREDITOR = 3;			//登记债权人接口
		public static final int REGISTER_GUARANTOR = 4;			//登记担保方
		public static final int REGISTER_CRETANSFER = 5;		//登记债权转让接口
		public static final int AUTO_NEW_SIGNING = 6;			//自动投标签约
		public static final int REPAYMENT_SIGNING = 7;			//自动还款签约
		public static final int DO_DP_TRADE = 8;				//充值
		public static final int TRANSFER = 9;					//转账(WS)
		public static final int REPAYMENT_NEW_TRADE = 10;		//还款
		public static final int GUARANTEE_UNFREEZE = 11;		//解冻保证金
		public static final int DO_DW_TRADE = 13;				//提现
		public static final int QUERY_FOR_ACC_BALANCE = 14;		//账户余额查询(WS)
		public static final int GET_BANK_LIST = 15;				//获取银行列表查询(WS)
		public static final int QUERY_MER_USER_INFO = 16;		//账户信息查询(WS)
		public static final int QUERY_TRADE = 17;				//交易查询(WS)
		public static final int BOUND_BANKCARD = 18;			//绑卡
		public static final int UNBOUND_BANKCARD = 19;			//取消绑卡
		
		public static final int TRANSFER_USER_TO_MER = 32;		//用户转商户
		public static final int TRANSFER_MER_TO_USER = 33;		//商户转用户
		public static final int TRANSFER_MER_TO_USER_SINGLE = 34;	//商户转用户-单笔(WS)
		
		public static final int UNFREEZE_INVEST_AMOUNT = 35;	//解冻投资金额(post)
		public static final int UNFREEZE_INVEST_AMOUNT_WS = 36;	//解冻投资金额(ws)
		
		public static final int TRANSFER_ONE = 91;				//转账-1放款(WS)
		public static final int TRANSFER_TWO = 92;				//转账-2代偿(WS)
		public static final int TRANSFER_THREE = 93;			//转账-3代偿还款(WS)
		public static final int TRANSFER_FOUR = 94;				//转账-4债权转让(WS)
		public static final int LOGIN = 100;				    //登陆
	}
	
	public class RepairOperation {
//		public static final String CREATE_IPS_ACCT = "01";			//开户
		public static final String REGISTER_SUBJECT = "02";			//标的登记
		public static final String REGISTER_CREDITOR = "03";			//登记债权人接口
//		public static final String REGISTER_CREDITOR = "04";			//登记担保方
		public static final String REGISTER_CRETANSFER = "05";		//登记债权转让接口
//		public static final String AUTO_NEW_SIGNING = "06";			//自动投标签约
//		public static final String REPAYMENT_SIGNING = "07";			//自动还款签约
		public static final String DO_DP_TRADE = "08";				//充值
//		public static final String TRANSFER = "09";					//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = "11";		//还款
//		public static final String GUARANTEE_UNFREEZE = "10";		//解冻保证金
		public static final String DO_DW_TRADE = "09";				//提现
//		public static final String QUERY_FOR_ACC_BALANCE = "14";		//账户余额查询(WS)
//		public static final String GET_BANK_LIST = "15";				//获取银行列表查询(WS)
//		public static final String QUERY_MER_USER_INFO = "16";		//账户信息查询(WS)
//		public static final String QUERY_TRADE = "17";				//交易查询(WS)
		
		public static final String TRANSFER_ONE = "14";				//转账-1放款(WS)
		public static final String TRANSFER_FOUR = "15";				//转账-4债权转让(WS)
		public static final String TRANSFER = "14"; 
	}
	
	public static class IPSWebUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCB";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectCB";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorCB";			//登记债权人接口
		public static final String REGISTER_GUARANTOR = CALLBACK_URL + "registerGuarantorCB";		//登记担保方
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCB";		//登记债权转让接口
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCB";			//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCB";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradeCB";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCB";							//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradeCB";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCB";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradeCB";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCB";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCB";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCB";		//账户信息查询(WS)
		public static final String TRANSFER_USER_TO_MER = CALLBACK_URL + "transferUserToMerCB";		//转账-用户转商户
		public static final String TRANSFER_MER_TO_USER = CALLBACK_URL + "transferMerToUserCB";		//转账-商户转用户(WS)
		public static final String UNFREEZE_INVEST_AMOUNT = CALLBACK_URL + "unfreezeInvestAmountCB";	//解冻投资金额
		
		public static final String UNBOUND_BANK = CALLBACK_URL + "unBoundBankCardCB";	//取消绑卡
		public static final String BOUND_BANK = CALLBACK_URL + "boundBankCardCB";	//绑卡
	}
	
	public static class IPSS2SUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCBSys";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectCBSys";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorCBSys";			//登记债权人接口
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCBSys";		//登记债权转让接口
		public static final String REGISTER_GUARANTOR = CALLBACK_URL + "registerGuarantorCBSys";		//登记担保方
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCBSys";				//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCBSys";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradeCBSys";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCBSys";							//转账(WS)
		public static final String COMPENSATE = CALLBACK_URL + "compensateCBSys";						//本金垫付(WS)
		public static final String OFFLINE_REPAYMENT = CALLBACK_URL + "offlineRepaymentCBSys";			//线下收款(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradeCBSys";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCBSys";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradeCBSys";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCBSys";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCBSys";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCBSys";		//账户信息查询(WS)
		public static final String TRANSFER_USER_TO_MER = CALLBACK_URL + "transferUserToMerCBSys";	//转账-用户转商户
		public static final String TRANSFER_MER_TO_USER = CALLBACK_URL + "transferMerToUserCBSys";	//转账-商户转用户(WS)
		public static final String UNFREEZE_INVEST_AMOUNT = CALLBACK_URL + "unfreezeInvestAmountCBSys";	//解冻投资金额
		
		public static final String UNBOUND_BANK = CALLBACK_URL + "unBoundBankCardCBSys";	//取消绑卡
		public static final String BOUND_BANK = CALLBACK_URL + "boundBankCardCBSys";	//绑卡
	}
	
	public static class IPSWSUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCB";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectWS";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorWS";			//登记债权人接口
		public static final String REGISTER_GUARANTOR = CALLBACK_URL + "registerGuarantorCB";		//登记担保方
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCB";		//登记债权转让接口
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCB";			//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCB";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradeWS";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCB";							//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradeWS";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCB";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradeWS";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCB";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCB";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCB";		//账户信息查询(WS)
		public static final String TRANSFER_USER_TO_MER = CALLBACK_URL + "transferUserToMerCB";		//转账-用户转商户
		public static final String TRANSFER_MER_TO_USER = CALLBACK_URL + "transferMerToUserCB";		//转账-商户转用户(WS)
	}
	
	public static class IPSPostUrl {
		public static final String CREATE_IPS_ACCT = CALLBACK_URL + "createAcctCB";					//开户
		public static final String REGISTER_SUBJECT = CALLBACK_URL + "registerSubjectPost";			//标的登记
		public static final String REGISTER_CREDITOR = CALLBACK_URL + "registerCreditorPost";		//登记债权人接口
		public static final String REGISTER_GUARANTOR = CALLBACK_URL + "registerGuarantorCB";		//登记担保方
		public static final String REGISTER_CRETANSFER = CALLBACK_URL + "registerCretansferCB";		//登记债权转让接口
		public static final String AUTO_NEW_SIGNING = CALLBACK_URL + "autoNewSigningCB";			//自动投标签约
		public static final String REPAYMENT_SIGNING = CALLBACK_URL + "repaymentSigningCB";			//自动还款签约
		public static final String DO_DP_TRADE = CALLBACK_URL + "doDpTradePost";						//充值
		public static final String TRANSFER = CALLBACK_URL + "transferCB";							//转账(WS)
		public static final String REPAYMENT_NEW_TRADE = CALLBACK_URL + "repaymentNewTradePost";		//还款
		public static final String GUARANTEE_UNFREEZE = CALLBACK_URL + "guaranteeUnfreezeCB";		//解冻保证金
		public static final String DO_DW_TRADE = CALLBACK_URL + "doDwTradePost";						//提现
		public static final String QUERY_FOR_ACC_BALANCE = CALLBACK_URL + "queryForAccBalanceCB";	//账户余额查询(WS)
		public static final String GET_BANK_LIST = CALLBACK_URL + "getBankListCB";					//获取银行列表查询(WS)
		public static final String QUERY_MER_USER_INFO = CALLBACK_URL + "queryMerUserInfoCB";		//账户信息查询(WS)
		public static final String TRANSFER_USER_TO_MER = CALLBACK_URL + "transferUserToMerCB";		//转账-用户转商户
		public static final String TRANSFER_MER_TO_USER = CALLBACK_URL + "transferMerToUserCB";		//转账-商户转用户(WS)
		public static final String UNFREEZE_INVEST_AMOUNT = CALLBACK_URL + "unfreezeInvestAmountPost";		//解冻投资金额
	}
	
	public class IpsCheckStatus {
		public static final int NONE = 0;
		public static final int EMAIL = 1;
		public static final int REAL_NAME = 2;
		public static final int MOBILE = 3;
		public static final int IPS = 4;
	}
	
	public class TransferType {
		public static final int INVEST = 1;					//投资(放款)
		public static final int COMPENSATE = 2;				//代偿
		public static final int COMPENSATE_REPAYMENT = 3;	//代偿还款
		public static final int CRETRANSFER = 4;			//债权转让
	}
	
	public class Status {
		public static final int SUCCESS = 1;		//成功
		public static final int FAIL = 2;			//失败
		public static final int HANDLING = 3;		//处理中
		public static final int NONE = 4;			//未查询到交易
		
		public static final int UNFREEZING = 10;		//待解冻
		public static final int UNFREEZED = 11;			//已解冻
	}
	
	public class CompensateType {
		public static final int COMPENSATE = 1;			//本金垫付
		public static final int OFFLINE_REPAYMENT = 2;	//线下收款
	}
	
	public class RegisterGuarantorType {
		public static final int COMPENSATE = 1;			//本金垫付
		public static final int OFFLINE_REPAYMENT = 2;	//待收款线下收款
		public static final int OFFLINE_REPAYMENT_OVERDUE = 3;	//逾期待收款线下收款
	}
	
	public static boolean IS_REPAIR_TEST = false; 
	
	/**
	 * 解冻投资金额的请求方式，默认WS
	 */
	public static boolean IS_WS_UNFREEZE = true;

}
