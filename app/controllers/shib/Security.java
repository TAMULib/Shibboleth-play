package controllers.shib;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Play;
import play.mvc.*;
import play.data.validation.*;
import play.libs.*;
import play.utils.*;

/**
 * 
 * Extendible Shibboleth authentication class. This basic class defines a set of
 * hook points where applications can customize the behavior of the Shibboleth
 * authentication module.
 * 
 * @author Scott Phillips, https://www.scottphillips.com
 */

public class Security extends Controller {

	/**
	 * This method checks that a profile is allowed to view this page/method.
	 * This method is called prior to the method's controller annotated with the
	 * check method.
	 * 
	 * @param profile
	 * @return true if you are allowed to execute this controller method.
	 */
	static boolean check(String profile) {
		if (isConnected())
			return true;
		else
			return false;
	}

	/**
	 * Indicate if a user is currently connected
	 * 
	 * @return true if the user is connected
	 */
	static boolean isConnected() {
		return session.contains("shibboleth");
	}

	/**
	 * This method is called after a successful authentication. The user's
	 * attributes will already be stored in the session object. Use this method
	 * if you require complex attribute strategies or need to sync the data with
	 * an external data source.
	 */
	static void onAuthenticated() {
	}

	/**
	 * This method is called before a user tries to sign off.
	 */
	static void onDisconnect() {
	}

	/**
	 * This method is called after a successful sign off.
	 */
	static void onDisconnected() {
	}

	/**
	 * This method is called if a check does not succeed. By default it shows
	 * the not allowed page (the controller forbidden method).
	 * 
	 * @param profile
	 */
	static void onCheckFailed(String profile) {
		forbidden();
	}

	/**
	 * This method is called when their is a failure to extract/map attributes,
	 * such as missing required attributes.
	 * 
	 * @param attributes
	 *            Map of attributes found, may be null.
	 */
	static void onAttributeFailure(HashMap<String, String> attributes) {
		error("Authentication Failure");
	}

	protected static Object invoke(String m, Object... args) throws Throwable {
		Class security = null;
		List<Class> classes = Play.classloader
				.getAssignableClasses(Security.class);
		if (classes.size() == 0) {
			security = Security.class;
		} else {
			security = classes.get(0);
		}
		try {
			return Java.invokeStaticOrParent(security, m, args);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

}
