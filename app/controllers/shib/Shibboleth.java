package controllers.shib;

import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.internal.runners.statements.Fail;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.data.validation.Required;
import play.libs.Crypto;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Router;
import play.utils.Java;


/**
 * Shibboleth Authentication for the Play! Framework.
 * 
 * @author Scott Phillips, http://www.scottphillips.com/
 *
 */
public class Shibboleth extends Controller {
	
	
	@Before(unless={"login", "authenticate", "logout"})
	static void checkAccess() throws Throwable {
		
		// Redirect to login
		if(!session.contains("shibboleth")) {
			flash.put("url", "GET".equals(request.method) ? request.url : null); 
			login();
		}

		// Check authentication profiles
		Check check = getActionAnnotation(Check.class);
		if (check == null)
			check = getControllerInheritedAnnotation(Check.class);

		for(String profile : check.value()) {
			boolean hasProfile = (Boolean)Security.invoke("check", profile);
			if(!hasProfile) {
				Security.invoke("onCheckFailed", profile);
			}
		}

	}

	
	/**
	 * Initiate a shibboleth login.
	 */
	public static void login() throws Throwable {
		
		// Determine where the Shibboleth Login initiator is
		String shibLogin = Play.configuration.getProperty("shib.login.url",null);
		if (shibLogin == null)
			shibLogin = request.getBase() + "/Shibboleth.sso/Login";
		if (isMock())
			shibLogin = Router.reverse("shib.Shibboleth.authenticate").url;
		
		// Append the target query string
		shibLogin += "?target="+request.getBase();
		shibLogin += Router.reverse("Shibboleth.authenticate").url;
		
		// Since we are redirecting we can't actually set the flash, so we'll
		// embed it in the target url.
		if (flash.get("url") != null)
			if (isMock())
				shibLogin += "&return="+URLEncoder.encode(flash.get("url"));
			else
				shibLogin += URLEncoder.encode("?return="+flash.get("url"));
		
		
		Logger.debug("Shib: Redirecting to Shibboleth login initiator: "+shibLogin);
		
		redirect(shibLogin);
	}

	/**
	 * Authenticate the session after returning from Shibboleth. 
	 */
	public static void authenticate() throws Throwable {
		
		// 1. Log all headers received, if debugging
		if (Logger.isDebugEnabled()) {
			String log = "Shib: Recieved the following headers: \n";
			for (String name : request.headers.keySet()) {
				for (String value : request.headers.get(name).values) {
					log += "    '"+name+"' = '"+value+"'\n";
				}
			}
			Logger.debug(log);
		}
		
		// TODO: Add check for special HTTP parameter from shibboleth for all connections
		session.put("shibboleth",String.valueOf(new Date().getTime()));
		
		// 2. Map each header to a session attribute
		Map<String,String> attributeMap = getAttributeMap();
		for (String attribute : attributeMap.keySet()) {
			
			String headerName = attributeMap.get(attribute);
			Header headers = null;
			if ( isMock() ) 
				// Get the fake headers
				headers = MockShibboleth.get(headerName);
			else
				// Use the real headers
				headers = request.headers.get(headerName);
			
			if (headers == null) {
				Logger.warn("Shib: Did not find header '"+headerName+"' for attribute '"+attribute+"'.");
				continue;
			}
			
			if (headers.values.size() > 1) {
				Logger.warn("Shib: Recieved multiple '"+headerName+"' headers for attribute '"+attribute+"', picking the first one.");
			}
			
			String value = headers.value();
			
			if (value == null) {
				Logger.warn("Shib: Recieved header '"+headerName+"' for attribute '"+attribute+"', but it was null.");
				continue;
			}
			
			// Store on the session.
			session.put(attribute, value);
			Logger.debug("Shib: Recieved attribute, '"+attribute+"' = '"+value+"'");
		}
		
		// Redirect to the original URL
		redirectToOriginalURL();
	}

	/**
	 * Logout of shibboleth, clear the session, and if configured
	 * initiate a Shibboleth Logout.
	 */
	public static void logout() throws Throwable {
		Security.invoke("onDisconnect");
		session.clear();
		Security.invoke("onDisconnected");
				
		String useLogout = Play.configuration.getProperty("shib.logout","false"); 
		if ( useLogout.equalsIgnoreCase("true")) {
			// Determine where the Shibboleth logout initiator is
			String shibLogout = Play.configuration.getProperty("shib.logout.url",null);
			if (shibLogout == null)
				shibLogout = request.getBase() + "/Shibboleth.sso/Logout";
			
			String shibReturn = Play.configuration.getProperty("shib.logout.return",null);
			if (shibReturn == null)
				shibReturn = request.getBase() + "/";
			
			
			// Append the target query string
			shibLogout += "?return="+shibReturn;
			
			Logger.debug("Shib: Redirecting to Shibboleth logout initiator: "+shibLogout);
		}
		
		String shibReturn = Play.configuration.getProperty("shib.logout.return",null);
		if (shibReturn == null)
			shibReturn = request.getBase() + "/";

		redirect(shibReturn);
	}

	private static boolean isMock() {
		if (Play.mode == Mode.DEV && 
			"mock".equalsIgnoreCase(Play.configuration.getProperty("shib")) )
			return true;
		else
			return false;
	}
	

	/**
	 * Redirect to the original user's email
	 */
	private static void redirectToOriginalURL() throws Throwable {
		Security.invoke("onAuthenticated");
		String url = flash.get("url");
		if(url == null) 
			url = request.params.get("return");
		if(url == null)
			url = "/";
		
		redirect(url);
	}
	
	
	/**
	 * @return The mapped session attributes from the Play! configuration. HashMap of Attribute names to HTTP Header names
	 */
	private static Map<String,String> getAttributeMap() {
		Map<String,String> attributeMap = new HashMap<String,String>();
		
		Set<Object> keys = Play.configuration.keySet();
		for (Object keyObj : keys) {
			if (keyObj instanceof String) {
				String key = (String) keyObj;
				if (key.startsWith("shib.attribute.")) {
					String attribute = key.substring("shib.attribute.".length());
					String header = Play.configuration.getProperty(key);
					
					attributeMap.put(attribute, header);
				}
			}
		}
		
		return attributeMap;
	}

}
