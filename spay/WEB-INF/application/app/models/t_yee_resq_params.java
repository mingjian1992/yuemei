package models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;

import play.db.jpa.Model;

/**
 * 易宝响应实体类
 * @author yangxuan
 * @date 20150323
 */
@Entity
public class t_yee_resq_params extends Model{
	
	public Date time;  //操作时间
	public String requestNo;
	public int code;
	public int ayns;
	public String result;
	public String sign;
	public String url;
	
	@Transient
	public String status;
	public String getStatus() {
		if (StringUtils.isBlank(status)) {
			if (code == 1) {
				status = "成功";
			}else {
				status = "失败";
			}
		}
		
		return status;
	}
}
