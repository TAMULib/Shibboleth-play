package controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.Play;
import play.utils.Java;

/**
 * 
 * Application specific implementation of the Security class, this
 * one just logs when each hook is called.
 * 
 * @author Scott Phillips, http://www.scottphillips/
 */
public class Security extends controllers.shib.Security {

	/**
	 * This method checks that a profile is allowed to view this page/method.
	 * This method is called prior to the method's controller annotated with the
	 * check method.
	 * 
	 * @param profile
	 * @return true if you are allowed to execute this controller method.
	 */
	static boolean check(String profile) {
		Logger.debug("Security: Security.profile(\""+profile+"\")");
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
		Logger.debug("Security: Security.isConnected()");
		return session.contains("shibboleth");
	}

	/**
	 * This method is called after a successful authentication. The user's
	 * attributes will already be stored in the session object. Use this method
	 * if you require complex attribute strategies or need to sync the data with
	 * an external data source.
	 */
	static void onAuthenticated() {
		Logger.debug("Security: Security.onAuthenticated()");
	}

	/**
	 * This method is called before a user tries to sign off.
	 */
	static void onDisconnect() {
		Logger.debug("Security: Security.onDisconnect()");
	}

	/**
	 * This method is called after a successful sign off.
	 */
	static void onDisconnected() {
		Logger.debug("Security: Security.onDisconnected()");
	}

	/**
	 * This method is called if a check does not succeed. By default it shows
	 * the not allowed page (the controller forbidden method).
	 * 
	 * @param profile
	 */
	static void onCheckFailed(String profile) {
		Logger.debug("Security: Security.onCheckFailed(\""+profile+"\")");
		forbidden();
	}
	
	/**
	 * This method is called when their is a failure to extract/map attributes,
	 * such as missing required attributes.
	 * 
	 * @param attributes
	 *            Map of attributes found, may be null.
	 */
	static void onAttributeFailure(HashMap<String,String> attributes) {
		Logger.debug("Security: Security.onAttributeFailure("+attributes+")");
		error("Authentication Failure");
	}

}
