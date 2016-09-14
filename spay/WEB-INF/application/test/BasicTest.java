import org.junit.Test;

import com.shove.security.Encrypt;

import business.DealDetail;
import constants.Constants;
import constants.YEEConstants;
import play.test.UnitTest;
import services.YEE;
import utils.Converter;
import utils.ErrorInfo;
import net.sf.json.*;

public class BasicTest extends UnitTest {
	 
	@Test
	public void YEEConstants_IPS_URL_TEST_length(){
		System.out.println(YEEConstants.IPS_URL_TEST[0]);
	}
	
	@Test
	public void YEE_transferMerToUsersTest(){
		YEE.transferMerToUsersTest();
	}
	
	@Test
	public void tt(){
//		JSONObject json = new JSONObject();
//		json.put("123", "123");
//		System.out.println(Converter.jsonToXml(json.toString()));
		
		DealDetail detail = new DealDetail();
		ErrorInfo error = new ErrorInfo();
		detail.checkWithdraw(error);
	}
	public static void main(String [] as){
		 Encrypt.decrypt3DES("", Constants.ENCRYPTION_KEY);
	}
}
