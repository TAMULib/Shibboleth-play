package play.modules.shib;

import java.util.Map;

import play.mvc.Http;
import play.mvc.Result;

public interface ShibbolethHandler {
	
	public Result beforeLogin(Http.Context context);
	
	public Map<String,String> getShibbolethAttributes(Map<String,String[]> headers);
	
	public Result verifyShibbolethAttributes(Http.Context context, Map<String,String> shibbolethAttributes);
		
	public Result afterLogin(Http.Context context);
	
	
	public Result beforeLogout(Http.Context context);
	
	public Result afterLogout(Http.Context context);
	
}
