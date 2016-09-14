package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 事件类型
 * @author cp
 * @version 6.0
 * @created 2014年9月15日 上午9:29:40
 */
@Entity
public class t_member_events extends Model {
	public long member_id;
	public Date time;
	public String serial_number;
	public long platform_id;
	public int type_id;
	public String front_url;
	public String background_url;
	public String remark;
	public String descrption;
	public String order_no;
}
