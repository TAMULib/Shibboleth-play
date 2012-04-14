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
		
		Result result = handler.beforeLogin(ctx());
		
		if (result != null)
			return result;
		
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
		
		Logger.debug("Shib: Redirecting to Shibboleth login initiator: "+shibLoginUrl);
		return temporaryRedirect(shibLoginUrl);
	}
	
	/**
	 * Authenticate a shibboleth session
	 */
	public static Result authenticate() {
		
		ShibbolethHandler handler = getHandler();
		Result result = handler.beforeAuthentication(ctx());
		if (result != null)
			return result;
		
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
		result = handler.verifyShibbolethAttributes(ctx(), shibbolethAttributes);
		if (result != null)
			return result;
		
		// 4. Log the user in
		session().put("shibboleth",String.valueOf(System.currentTimeMillis()));
		for (String attributeName : shibbolethAttributes.keySet()) {
			session().put(attributeName, shibbolethAttributes.get(attributeName));
		}

		// 5. Redirect the user back to where they should come from.
		result = handler.afterAuthentication(ctx());
		if (result != null)
			return result;
		
		return temporaryRedirect("/");
	}
	
	public static Result logout() {
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @return Is Shibboleth being Mocked for testing?
	 */
	public static boolean isMock() {
		if (Play.isDev() || Play.isTest())
			if ("mock".equalsIgnoreCase(Play.application().configuration().getString("shib")))
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
