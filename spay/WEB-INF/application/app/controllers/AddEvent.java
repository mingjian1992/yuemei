package controllers;

import play.Logger;
import utils.Converter;
import business.DealDetail;


public class AddEvent extends BaseController {

	
	public static void addEvent(String memberId, String type,
			String platformId, String serialNumber, String frontUrl,
			String backgroundUrl, String remark, String descrption) {

		DealDetail.addEvent(Long.parseLong(memberId), Integer.parseInt(type),
				Long.parseLong(platformId), serialNumber, frontUrl,
				backgroundUrl, remark, descrption);

		renderText("");
	}

	
	public static void addDealDetail(long memberId, int platformId,
			String serialNumber, long operation, double amount, boolean status,
			String summary) {
//		Logger.info("addDealDetail"+);
		DealDetail.addDealDetail(memberId, platformId, serialNumber, operation,
				amount, status, summary);
		renderText("");
	}
	
}