package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import controllers.shib.Check;
import controllers.shib.Security;
import controllers.shib.Shibboleth;

@With(Shibboleth.class)
public class Administrative extends Controller {

	@Check("isAdmin")
    public static void restricted() {
    	render();
    }
}