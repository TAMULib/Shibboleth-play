package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

/**
 * The basic application controller. Display some mostly static pages.
 * 
 * @author Scott Phillips, http://www.scottphillips.com
 */

public class Application extends Controller {

    public static void index() {
        render();
    }
    
    public static void shibboleth() {
    	render();
    }

}