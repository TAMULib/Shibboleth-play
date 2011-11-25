package controllers.shib;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify a particular Shibboleth profile for the controller's action.
 * 
 * @author Scott Phillips, http://www.scottphillips.com
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Check {

    String[] value();
}
