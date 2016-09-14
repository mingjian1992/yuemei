package business;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import models.t_yee_req_params;
import models.t_yee_resq_params;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;

/**
 * @author pc-mingjian
 * @date 2015年10月14日
 * @version 6.0
 */
public class LogManagement {
	
	/**
	 * 资金托管请求日志
	 * @param page
	 * @param error
	 * @param currPage
	 * @param pageSize
	 * @param keyword
	 */
	public static void queryYeeReqParam(PageBean<t_yee_req_params> page, ErrorInfo error, int currPage, int pageSize, String keyword){
		error.clear();
		
		StringBuilder sql = new StringBuilder("select * from t_yee_req_params where 1 = 1 ");
		StringBuilder sqlCount = new StringBuilder("select count(1) from t_yee_req_params where 1 = 1 ");
		List<Object> values = new ArrayList<>();
		Map<String, Object> map = new HashMap<>();
		
		if (StringUtils.isNotBlank(keyword)) {
			 sql.append(" and (memberId like ? or oprateTypeName like ? or requestNo like ?)");
			 sqlCount.append(" and (memberId like ? or oprateTypeName like ? or requestNo like ?)");
			 values.add("%" + keyword + "%");
			 values.add("%" + keyword + "%");
			 values.add("%" + keyword + "%");
			 map.put("keyword", keyword);
		}
		
		Query query = JPA.em().createNativeQuery(sql.toString(), t_yee_req_params.class);
		query.setFirstResult((currPage - 1) * pageSize);
		query.setMaxResults(pageSize);
		for (int i = 0; i < values.size(); i++) {
			query.setParameter(i + 1, values.get(i));
		}
		
		try {
			page.page = query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "系统出现异常！";
			
			return;
		}
		
		if (page.page == null || page.page.size() == 0) {
			
			return;
		}
		
		query = JPA.em().createNativeQuery(sqlCount.toString());
		for (int i = 0; i < values.size(); i++) {
			query.setParameter(i + 1, values.get(i));
		}
		
		try {
			page.totalCount = ((BigInteger) query.getResultList().get(0)).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "系统出现异常！";
			
			return;
		}
		
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.conditions = map;
	}
	
	/**
	 * 资金托管回调日志
	 * @param page
	 * @param error
	 * @param currPage
	 * @param pageSize
	 * @param keyword
	 */
	public static void queryYeeResParam(PageBean<t_yee_resq_params> page, ErrorInfo error, int currPage, int pageSize, String keyword){
		error.clear();
		
		StringBuilder sql = new StringBuilder("select t from t_yee_resq_params t where 1 = 1 ");
		StringBuilder sqlCount = new StringBuilder("select count(t.id) from t_yee_resq_params t where 1 = 1 ");
		List<Object> values = new ArrayList<>();
		Map<String, Object> map = new HashMap<>();
		
		if (StringUtils.isNotBlank(keyword)) {
			 sql.append(" and (requestNo like ?)");
			 sqlCount.append(" and (requestNo like ?)");
			 values.add("%" + keyword + "%");
			 map.put("keyword", keyword);
		}
		
		Query query = JPA.em().createQuery(sql.toString());
		query.setFirstResult((currPage - 1) * pageSize);
		query.setMaxResults(pageSize);
		for (int i = 0; i < values.size(); i++) {
			query.setParameter(i + 1, values.get(i));
		}
		
		try {
			page.page = query.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "系统出现异常！";
			
			return;
		}
		
		if (page.page == null || page.page.size() == 0) {
			
			return;
		}
		
		query = JPA.em().createQuery(sqlCount.toString());
		for (int i = 0; i < values.size(); i++) {
			query.setParameter(i + 1, values.get(i));
		}
		
		try {
			page.totalCount = ((Long) (query.getResultList().get(0))).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "系统出现异常！";
			
			return;
		}
		
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.conditions = map;
	}
	
	/**
	 * 根据id获取回调日志
	 * @param id
	 * @param error
	 * @return
	 */
	public static t_yee_resq_params queryResParamById(long id, ErrorInfo error){
		error.clear();
		
		if (id < 0) {
			error.code = -1;
			error.msg = "记录不存在！";
			
			return null;
		}
		
		try {
			return t_yee_resq_params.findById(id);
		} catch (Exception e) {
			e.printStackTrace();
			error.code = -1;
			error.msg = "系统异常";
			
			return null;
		}
	}
}
