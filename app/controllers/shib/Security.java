package controllers.shib;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import play.Play;
import play.mvc.*;
import play.data.validation.*;
import play.libs.*;
import play.utils.*;

public class Security extends Controller {

	/**
	 * This method checks that a profile is allowed to view this page/method.
	 * This method is called prior to the method's controller annotated with the
	 * e method.
	 * 
	 * @param profile
	 * @return true if you are allowed to execute this controller method.
	 */
	static boolean check(String profile) {
		return true;
	}

	/**
	 * This method returns the current connected username
	 * 
	 * @return
	 */
	static String connected() {
		return session.get("shibboleth");
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
	 * This method is called after a successful authentication. You need to
	 * override this method if you with to perform specific actions (eg. Record
	 * the time the user signed in)
	 */
	static void onAuthenticated() {
	}

	/**
	 * This method is called before a user tries to sign off. You need to
	 * override this method if you wish to perform specific actions (eg. Record
	 * the name of the user who signed off)
	 */
	static void onDisconnect() {
	}

	/**
	 * This method is called after a successful sign off. You need to override
	 * this method if you wish to perform specific actions (eg. Record the time
	 * the user signed off)
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
