package controllers;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import play.Logger;
import play.Play;
import play.utils.Java;

public class Security extends controllers.shib.Security {

	/**
	 * This method checks that a profile is allowed to view this page/method.
	 * This method is called prior to the method's controller annotated with the
	 * e method.
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
	 * This method is called after a successful authentication. You need to
	 * override this method if you with to perform specific actions (eg. Record
	 * the time the user signed in)
	 */
	static void onAuthenticated() {
		Logger.debug("Security: Security.onAuthenticated()");
	}

	/**
	 * This method is called before a user tries to sign off. You need to
	 * override this method if you wish to perform specific actions (eg. Record
	 * the name of the user who signed off)
	 */
	static void onDisconnect() {
		Logger.debug("Security: Security.onDisconnect()");
	}

	/**
	 * This method is called after a successful sign off. You need to override
	 * this method if you wish to perform specific actions (eg. Record the time
	 * the user signed off)
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

}
