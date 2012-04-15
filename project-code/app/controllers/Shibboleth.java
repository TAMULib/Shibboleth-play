package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;

import play.modules.shib.DefaultShibbolethHandler;
import play.modules.shib.MockShibboleth;
import play.modules.shib.ShibbolethHandler;
import play.mvc.Controller;
import play.mvc.Result;


public class Shibboleth extends Controller{

	/**
	 * Initiate a shibboleth login
	 */
	public static Result login() throws UnsupportedEncodingException {
		ShibbolethHandler handler = getHandler();
		
		// 1. Pre-login hook
		Result result = handler.beforeLogin(ctx());
		if (result != null)
			return result;
		
		// 2. If shibboleth login is turned off skip straight to authentication
		Boolean login = Play.application().configuration().getBoolean("shib.login.enable");
		if (login == null || login == false)
			return authenticate();
		
		
		// 3. Determine the shibboleth login initiation url
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
			shibLoginUrl = Play.application().configuration().getString("shib.login.url");
			if (shibLoginUrl == null)		
					shibLoginUrl = "/Shibboleth.sso/Login";
			shibLoginUrl += "?target=" + URLEncoder.encode(authenticationURL,"UTF-8");
			
			if (returnURL != null)
				shibLoginUrl += URLEncoder.encode("?return="+returnURL,"UTF-8");
		}
		
		// 4. Forward the user off to the initiator
		Logger.debug("Shib: Redirecting to Shibboleth login initiator: "+shibLoginUrl);
		return temporaryRedirect(shibLoginUrl);
	}
	
	/**
	 * Authenticate a shibboleth session
	 */
	public static Result authenticate() {
		
		ShibbolethHandler handler = getHandler();
		
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
		
		// 2. Extract shibboleth attributes from the HTTP headers
		Map<String,String[]> headers = request().headers();
		if (isMock())
			headers = MockShibboleth.getHeaders();
		Map<String,String> shibbolethAttributes = handler.getShibbolethAttributes(headers);
		
		// 3. Check for required attributes.
		Result result = handler.verifyShibbolethAttributes(ctx(), shibbolethAttributes);
		if (result != null)
			return result;
		
		// 4. Log the user in
		session().put("shibboleth",String.valueOf(System.currentTimeMillis()));
		for (String attributeName : shibbolethAttributes.keySet()) {
			session().put(attributeName, shibbolethAttributes.get(attributeName));
		}

		// 5. Post-authentication hook
		result = handler.afterLogin(ctx());
		if (result != null)
			return result;
		
		// 6. Determine where the user should be redirected.
		String url = flash().get("url");
		if (url == null) {
			String[] urls = request().queryString().get("return"); 
			if (urls != null && urls.length > 0)
				url = urls[0];
		} 
		if (url == null) {
			url = Play.application().configuration().getString("shib.login.return");
		}
		if (url == null) {
			url = "/";
		}
		
		// 7. Redirect the user back to the application.
		Logger.debug("Shib: Redirecting user back to destination location: "+url);
		return temporaryRedirect(url);	
	}
	
	
	/**
	 * Logout of a shibboleth session.
	 */
	public static Result logout() throws UnsupportedEncodingException {
		
		ShibbolethHandler handler = getHandler();
		
		// 1. First allow any pre-logout hoosk to run
		Result result = handler.beforeLogout(ctx());
		if (result != null)
			return result;
		
		// 2. Actually preform the logout
		session().clear();	
		
		// 3. Allow any post-logout hooks to run.
		result = handler.afterLogout(ctx());
		if (result != null)
			return result;
		
		// 4. Either logout using shibboleth or redirect to somewhere usefull.
		Boolean logout = Play.application().configuration().getBoolean("shib.logout.enable");
		String shibLogoutReturn = Play.application().configuration().getString("shib.logout.return");
		if (shibLogoutReturn == null)
			shibLogoutReturn = "/";
		
		if (logout != null && logout == true) {
			// Logout using shibboleth.
			String shibLogoutUrl = Play.application().configuration().getString("shib.logout.url");
			if (shibLogoutUrl == null)
				shibLogoutUrl = "/Shibboleth.sso/Logout";
			
			shibLogoutUrl += "?return="+URLEncoder.encode(shibLogoutReturn,"UTF-8");
			
			Logger.debug("Shib: Redirecting to Shibboleth logout initiator: "+shibLogoutUrl);
			return temporaryRedirect(shibLogoutUrl);
		} else {
			// Just clear the session and move on.
			return temporaryRedirect(shibLogoutReturn);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @return Is Shibboleth being Mocked for testing?
	 */
	public static boolean isMock() {
		if (Play.isDev() || Play.isTest())
			if ("mock".equalsIgnoreCase(Play.application().configuration().getString("shib.mode")))
				return true;
		return false;
	}
	
	private static ShibbolethHandler handler = null;
	
	public static synchronized ShibbolethHandler getHandler() {
		if (handler == null) {
			String handlerString = Play.application().configuration().getString("shib.handler");
			
			if (handlerString == null) {
				// No configuration, so just use the default handler
				handler = new DefaultShibbolethHandler();
			} else {
				try {
				handler = (ShibbolethHandler) Class.forName(
						handlerString,
						true,
						Play.application().classloader()).newInstance();
				} catch (Throwable t) {
					throw Play.application().configuration().reportError(
							"shib.handler", 
							"Error creating Shibboleth Handler: "+handlerString, 
							t);
				}
			}
		}
		return handler;
	}
	
	
//	private static String getConfig(String key) {
//		return getConfig(key,null);
//	}
//	
//	private static String getConfig(String key, String defaultValue) {
//		String value = Play.application().configuration().getString(key);
//		
//		if (value == null)
//			return defaultValue;
//		else
//			return value;
//	}
//
//	/**
//	 * @return The mapped session attributes from the Play! configuration.
//	 *         HashMap of Attribute names to HTTP Header names
//	 */
//	private static Map<String, String> getAttributeMaping() {
//		Map<String, String> attributeMap = new HashMap<String, String>();
//
//		Set<String> keys = Play.application().configuration().keys();
//		for (String key : keys) {
//			if (key.startsWith("shib.attribute.")) {
//				String attribute = key.substring("shib.attribute.".length());
//				String header = Play.application().configuration().getString(key);
//
//				attributeMap.put(attribute, header);
//			}
//		}
//
//		return attributeMap;
//	}
//	
//	
//	/**
//	 * @return The list of attributes required for successful authentication.
//	 */
//	private static List<String> getRequiredAttributes() {
//
//		
//		String shibRequired = getConfig("shib.require",null);
//		
//		if (shibRequired == null)
//			return Collections.EMPTY_LIST;
//		
//		String[] requiredAttributes = shibRequired.split(",");
//		
//		for (int i = 0; i < requiredAttributes.length; i++) {
//			requiredAttributes[i] = requiredAttributes[i].trim();
//		}
//		
//		return Arrays.asList(requiredAttributes);
//	}
	
	
	
}
