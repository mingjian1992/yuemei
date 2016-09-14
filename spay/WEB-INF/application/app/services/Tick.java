package services;

import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;

public class Tick implements Runnable {

	@Override
	public void run() {
		Logger.info("run");
    	String url = "http://shijia.hnkjxy.com/addVoterList.action";
    	int i = 0;
    	HttpResponse resp = null;
    	Map<String,String> maps  = null;
    	int quat = new java.util.Random().nextInt(100);
    	while(i<10000){
    		i++;
	    	maps = new HashMap<String, String>();
	    	maps.put("voterIP", "112.95."+quat+"."+i);
	    	maps.put("voterID", "3");
	    	maps.put("voterThemeID", "1");
	    	maps.put("userName", "邓军");
	    		resp = WS.url(url).setParameters(maps).setHeader("Host", "shijia.hnkjxy.com")
	    				.setHeader("Host", "shijia.hnkjxy.com")
	    				.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:34.0) Gecko/20100101 Firefox/34.0")
	    				.setHeader("Accept", "*/*")
	    				.setHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3")
	    				.setHeader("Accept-Encoding", "gzip, deflate")
	    				.setHeader("X-Requested-With", "XMLHttpRequest")
	    				.setHeader("Pragma", "no-cache")
	    				.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
	    				.setHeader("Cache-Control", "no-cache")
	    				.setHeader("Referer", "http://shijia.hnkjxy.com/")
	    				.setHeader("Content-Length", "74")
	    				.setHeader("Cookie", "JSESSIONID=3A3E10F577A2D12D9CB5AB64262B698A")
	    				.setHeader("Connection", "keep-alive").post();
				Logger.info("Result Status : %s", resp.getStatus().intValue());
				Logger.info("Result String : %s", resp.getString());
    	}
    
		
	}

}
