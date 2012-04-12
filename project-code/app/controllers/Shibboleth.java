package controllers;

import java.net.URLEncoder;
import java.util.Map;

import play.Logger;
import play.Play;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Router;


public class Shibboleth extends Controller{

	/**
	 * Initiate a shibboleth login
	 */
	public static Result login() {
		
		String authenticationURL = controllers.routes.Shibboleth.authenticate().absoluteURL(request());
		String returnURL = flash("url");
		
		String shibLoginUrl;
		if (isMock()) {
			// We are mocking the shibboleth login initiator, in this case we
			// just send them directly to the authenticate action, with the
			// return url.
			
			shibLoginUrl = authenticationURL;
			
			// Save the url redirect
			if (returnURL != null)
				shibLoginUrl += "?return="+URLEncoder.encode(returnURL, "UTF-8");
			
		} else {
			shibLoginUrl = getConfig("shib.login.url","/Shibboleth.sso/Login");
			shibLoginUrl += "?target=" + URLEncoder.encode(authenticationURL,"UTF-8");
			
			if (returnURL != null)
				shibLoginUrl += URLEncoder.encode("?return="+returnURL,"UTF-8");
		}
		
		Logger.debug("Shib: Redirecting to Shibboleth login initiator: "+shibLoginUrl);

		return temporaryRedirect(shibLoginUrl);
	}
	
	/**
	 * Authenticate a shibboleth session
	 */
	public static Result authenticate() {
		
		// 1. Log all headers received, if tracing (it fills up the logs fast!)
		if (Logger.isTraceEnabled()) {
			String log = "Shib: Recieved the following headers: \n";
			for (String name : request().headers().keySet()) {
				for (String value : request().headers().get(name)) {
					log += "    '" + name + "' = '" + value + "'\n";
				}
			}
			Logger.trace(log);
		}
		
		
		
		return null;
	}
	
	public static Result logout() {
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @return Is Shibboleth being Mocked for testing?
	 */
	private static boolean isMock() {
		if ((Play.isDev() || Play.isTest()) && "mock".equalsIgnoreCase(getConfig("shib")))
			return true;
		else
			return false;
	}
	
	
	private static String getConfig(String key) {
		return getConfig(key,null);
	}
	
	private static String getConfig(String key, String defaultValue) {
		String value = Play.application().configuration().getString(key);
		
		if (value == null)
			return defaultValue;
		else
			return value;
	}
	
	
	
	
	
	
}
