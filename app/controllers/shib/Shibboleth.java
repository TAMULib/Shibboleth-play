package controllers.shib;

import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * This controller provides three actions: login, authenticate, and logout. See
 * the documentation for information on how to configure the controller.
 * 
 * @author Scott Phillips, http://www.scottphillips.com/
 * 
 */
public class Shibboleth extends Controller {

	/**
	 * This method works with the @With() annotation, so that those classes will
	 * always have this method executed first. If a user has not logged in or
	 * failed an @Check() test then the appropriate action will be taken to
	 * either start logining the user in or give a forbidden error.
	 */
	@Before(unless = { "login", "authenticate", "logout" })
	static void checkAccess() throws Throwable {
		String shibLoginActivated = Play.configuration.getProperty("shib.login",
				"true");
		if (!session.contains("shibboleth")) {
			flash.put("url", "GET".equals(request.method) ? request.url : null);
			if(Boolean.parseBoolean(shibLoginActivated)){
				// Redirect to login
				flash.put("url", "GET".equals(request.method) ? request.url : null);
				Logger.debug("Shib: User requires authentication to access: "+request.url);
				login();
				
			}else{	
				//Call the "authenticate" action without initiating a shibboleth login
				Logger.debug("Shib: Login redirection desactivated. Session authentication will be proceeded");			
				authenticate();
			}
		}

		// Check authentication profiles
		Check check = getActionAnnotation(Check.class);
		if (check == null)
			check = getControllerInheritedAnnotation(Check.class);

		if (check != null) {
			for (String profile : check.value()) {
				boolean hasProfile = (Boolean) Security.invoke("check", profile);
				if (!hasProfile) {
					Security.invoke("onCheckFailed", profile);
				}
			}
		}

	}

	/**
	 * Initiate a shibboleth login.
	 */
	public static void login() throws Throwable {

		// Determine where the Shibboleth Login initiator is
		String shibLogin = Play.configuration.getProperty("shib.login.url",
				null);
		if (shibLogin == null)
			shibLogin = request.getBase() + "/Shibboleth.sso/Login";
		if (isMock())
			shibLogin = Router.reverse("shib.Shibboleth.authenticate").url;

		// Append the target query string
		shibLogin += "?target=" + Play.configuration.getProperty("application.baseUrl",request.getBase());
		shibLogin += Router.reverse("shib.Shibboleth.authenticate").url;

		// Since we are redirecting we can't actually set the flash, so we'll
		// embed it in the target url.
		if (flash.get("url") != null)
			if (isMock())
				shibLogin += "&return=" + URLEncoder.encode(flash.get("url"));
			else
				shibLogin += URLEncoder.encode("?return=" + flash.get("url"));

		Logger.debug("Shib: Redirecting to Shibboleth login initiator: "
				+ shibLogin);

		redirect(shibLogin);
	}

	/**
	 * Authenticate the session after returning from Shibboleth.
	 */
	public static void authenticate() throws Throwable {

		// 1. Log all headers received, if tracing (it fills up the logs fast!)
		if (Logger.isTraceEnabled()) {
			String log = "Shib: Recieved the following headers: \n";
			for (String name : request.headers.keySet()) {
				for (String value : request.headers.get(name).values) {
					log += "    '" + name + "' = '" + value + "'\n";
				}
			}
			Logger.trace(log);
		}

		// 2. Map each header to a session attribute
		Map<String, String> attributeMap = getAttributeMap();
		Map<String, String> extractedAttributes = new HashMap<String, String>();
		for (String attribute : attributeMap.keySet()) {

			String headerName = attributeMap.get(attribute);
			Header headers = null;
			if (isMock())
				// Get the fake headers
				headers = MockShibboleth.get(headerName);
			else
				// Use the real headers
				headers = request.headers.get(headerName);

			if (headers == null) {
				Logger.debug("Shib: Did not find header '" + headerName
						+ "' for attribute '" + attribute + "'.");
				continue;
			}

			if (headers.values.size() > 1) {
				Logger.warn("Shib: Recieved multiple '" + headerName
						+ "' headers for attribute '" + attribute
						+ "', picking the first one.");
			}

			String value = headers.value();

			if (value == null) {
				Logger.warn("Shib: Recieved header '" + headerName
						+ "' for attribute '" + attribute
						+ "', but it was null.");
				continue;
			}

			if (value.length() == 0) {
				// Shibboleth will send blank attributes for values which do not exist.
				continue;
			}
			
			// Store on the session.
			extractedAttributes.put(attribute, value);
			Logger.debug("Shib: Recieved attribute, '" + attribute + "' = '"+ value + "'");
		}

		// 3. Check for the required attributes
		for (String required : getRequiredAttributes()) {
			if (!extractedAttributes.containsKey(required)) {
				Logger.warn("Shib: Missing required attribute, '"+required+"'");
				Security.invoke("onAttributeFailure", extractedAttributes);
			}
		}

		// 4. Log the user in
		session.put("shibboleth", String.valueOf(new Date().getTime()));
		for (String attribute : extractedAttributes.keySet()) {
			session.put(attribute, extractedAttributes.get(attribute));
		}
		Logger.debug("Shib: User has succesfully authenticated with Shibboleth.");
		Security.invoke("onAuthenticated");

		// 5. Redirect to the original URL
		redirectToOriginalURL();
	}

	/**
	 * Logout of shibboleth, clear the session, and if configured initiate a
	 * Shibboleth Logout.
	 */
	public static void logout() throws Throwable {
		
		// 1. Clear out the session
		Security.invoke("onDisconnect");
		session.clear();
		Security.invoke("onDisconnected");
		Logger.debug("Shib: User has succesfully logged out using Shibboleth.");

		// 2. Determine where the user should go next.
		String shibReturn = Play.configuration.getProperty("shib.logout.return", null);
		if (shibReturn == null)
			shibReturn = Play.configuration.getProperty("application.baseUrl",request.getBase()) + "/";
		
		
		// 3. Tell shibboleth the user has logged out.
		String useLogout = Play.configuration.getProperty("shib.logout",
				"false");
		if (useLogout.equalsIgnoreCase("true")) {
			// Determine where the Shibboleth logout initiator is
			String shibLogout = Play.configuration.getProperty(
					"shib.logout.url", null);
			if (shibLogout == null)
				shibLogout = request.getBase() + "/Shibboleth.sso/Logout";

			// Append the target query string
			shibLogout += "?return=" + shibReturn;

			Logger.debug("Shib: Redirecting to Shibboleth logout initiator: " + shibLogout);
			redirect(shibLogout);
		}

		redirect(shibReturn);
	}

	/**
	 * Split a multivalue shibboleth attribute into individual components.
	 * Shibboleth attributes may contain multiple values separated by a
	 * semicolon and semicolons are escaped with a backslash. This method will
	 * split all the attributes into a list and unescape semicolons.
	 * 
	 * @param attribute
	 *            A multivalue shibboleth attribute
	 * @return A list of attribute values.
	 */
	public static List<String> split(String attribute) {
		List<String> valueList = new ArrayList<String>();
		int idx = 0;
		do {
			idx = attribute.indexOf(';', idx);

			if (idx == 0) {
				// if the string starts with a semicolon just remove it. This
				// will
				// prevent an endless loop in an error condition.
				attribute = attribute.substring(1, attribute.length());
				continue;
			} 
			if (idx > 0 && attribute.charAt(idx - 1) == '\\') {
				idx++;
				continue;
			} 
			if (idx > 0 ) {
				// First extract the value and store it on the list.
				String value = attribute.substring(0, idx);
				value = value.replaceAll("\\\\;", ";");
				valueList.add(value);

				// Next, remove the value from the string and continue to scan.
				attribute = attribute.substring(idx + 1, attribute.length());
				idx = 0;
			} 
		} while (idx >= 0);

		// The last attribute will still be left on the values string, put it
		// into the list.
		if (attribute.length() > 0) {
			attribute = attribute.replaceAll("\\\\;", ";");
			valueList.add(attribute);
		}
		
		return valueList;
	}

	/**
	 * @return Is Shibboleth being Mocked for testing?
	 */
	private static boolean isMock() {
		if (Play.mode == Mode.DEV
				&& "mock".equalsIgnoreCase(Play.configuration
						.getProperty("shib")))
			return true;
		else
			return false;
	}

	/**
	 * Redirect to the original user's url
	 */
	private static void redirectToOriginalURL() throws Throwable {
		String url = flash.get("url");
		if (url == null)
			url = request.params.get("return");
		if (url == null)
			url = Play.configuration.getProperty("shib.login.return","/");


		Logger.debug("Shib: Redirecting user back to destination location: "+url);
		redirect(url);
	}

	/**
	 * @return The mapped session attributes from the Play! configuration.
	 *         HashMap of Attribute names to HTTP Header names
	 */
	private static Map<String, String> getAttributeMap() {
		Map<String, String> attributeMap = new HashMap<String, String>();

		Set<Object> keys = Play.configuration.keySet();
		for (Object keyObj : keys) {
			if (keyObj instanceof String) {
				String key = (String) keyObj;
				if (key.startsWith("shib.attribute.")) {
					String attribute = key
							.substring("shib.attribute.".length());
					String header = Play.configuration.getProperty(key);

					attributeMap.put(attribute, header);
				}
			}
		}

		return attributeMap;
	}

	/**
	 * @return The list of attributes required for successful authentication.
	 */
	private static List<String> getRequiredAttributes() {

		
		String shibRequired = Play.configuration.getProperty("shib.require", null);
		
		if (shibRequired == null)
			return Collections.EMPTY_LIST;
		
		String[] requiredAttributes = shibRequired.split(",");
		
		for (int i = 0; i < requiredAttributes.length; i++) {
			requiredAttributes[i] = requiredAttributes[i].trim();
		}
		
		return Arrays.asList(requiredAttributes);
	}

}
