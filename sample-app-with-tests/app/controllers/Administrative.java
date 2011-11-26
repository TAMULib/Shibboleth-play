package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import controllers.shib.Check;
import controllers.shib.Security;
import controllers.shib.Shibboleth;

/**
 * 
 * Example restricted controller. Every action defined in this class
 * requires authentication before it can be accessed.
 * 
 * @author Scott Phillips, http://www.scottphillips.com/
 */

@With(Shibboleth.class)
public class Administrative extends Controller {

	@Check("isAdmin")
    public static void restricted() {
    	render();
    }
}