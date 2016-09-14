package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 易宝请求参数实体类
 * @author yangxuan
 * @date 20150323
 *
 */
@Entity
public class t_yee_req_params  extends Model{

	public long memberId;  //用户id
	public Date time;  //操作时间
	public int oprateType;  //操作类型id
	public String oprateTypeName;  //操作类型名称
	public String requestNo;  //请求流水号
	public String reqValue;  //请求参数值
	public String sign;  //加签值
	public String url;  //请求地址
}
