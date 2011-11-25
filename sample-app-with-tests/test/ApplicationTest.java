import java.util.ArrayList;
import java.util.List;

import org.junit.*;

import controllers.shib.MockShibboleth;
import play.Logger;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;

public class ApplicationTest extends FunctionalTest {

	/**
	 * The basic test to authenticate as a user using Shibboleth.
	 */
    @Test
    public void testShibbolethAuthentication() {
    	
    	// Set up the mock shibboleth attributes that
    	// will be used to authenticate the next user which 
    	// logins in.
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","someone@your-domain.net");
    	MockShibboleth.set("SHIB_givenName", "Some");
    	MockShibboleth.set("SHIB_sn", "One");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("<dt>email</dt>[\\s]*<dd>someone\\@your-domain\\.net</dd>", response);
        assertContentMatch("<dt>firstName</dt>[\\s]*<dd>Some</dd>", response);
        assertContentMatch("<dt>lastName</dt>[\\s]*<dd>One</dd>", response);
        
    }
    
    /** 
     * Test that visiting a restricted controller forces us to login, and then
     * we are redirected back to that controller after authenticating
     */
    @Test
    public void testRestrictedController() {
    	
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","bob@gmail.com");
    	MockShibboleth.set("SHIB_givenName", "Bob");
    	MockShibboleth.set("SHIB_sn", "Smith");
    	
    	final String RESTRICTED_URL = Router.reverse("Administrative.restricted").url;
    	Response response = GET(RESTRICTED_URL, true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("Welcome Bob, you are viewing a restricted page",response);
    	
    }
    
    
    /**
     * If no attributes are received when by the application then an error should
     * result.
     */
    @Test 
    public void testNoAttributes() {
    	
    	MockShibboleth.removeAll();

    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertStatus(500, response);
    }
    
    /**
     * Only some attributes are received, but not the full expected payload.
     */
    @Test
    public void testMinimumAttributes() {
    	
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","someone@your-domain.net");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("<dt>email</dt>[\\s]*<dd>someone\\@your-domain\\.net</dd>", response);
    	
    }
    
    /**
     * Only some attributes are received, but not the full expected payload.
     */
    @Test
    public void testMultipleValues() {
    	
    	MockShibboleth.removeAll();
    	List<String> headerValues = new ArrayList<String>();
    	headerValues.add("bob@gmail.com");
    	headerValues.add("someone@your-domain.net");
    	Header header = new Header("SHIB_email",headerValues);
    	MockShibboleth.headers.put("SHIB_email", header);
    	MockShibboleth.set("SHIB_givenName", "Bob");
    	MockShibboleth.set("SHIB_sn", "Smith");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertContentType("text/html", response);
        assertContentMatch("<dt>email</dt>[\\s]*<dd>bob\\@gmail\\.com</dd>", response);
        assertContentMatch("<dt>firstName</dt>[\\s]*<dd>Bob</dd>", response);
        assertContentMatch("<dt>lastName</dt>[\\s]*<dd>Smith</dd>", response);
    }
    
    
    
    public void testLogout() {
    	
    	MockShibboleth.removeAll();
    	MockShibboleth.set("SHIB_email","someone@your-domain.net");
    	MockShibboleth.set("SHIB_givenName", "Some");
    	MockShibboleth.set("SHIB_sn", "One");
    	
    	final String LOGIN_URL = Router.reverse("shib.Shibboleth.login").url;
        Response response = GET(LOGIN_URL,true);
        assertIsOk(response);
        assertTrue(response.cookies.get("PLAY_SESSION").value.contains("someone@your-domain.net"));
        
        final String LOGOUT_URL = Router.reverse("shib.Shibboleth.logout").url;
        response = GET(LOGOUT_URL,true);
        assertIsOk(response);
		assertFalse(response.cookies.get("PLAY_SESSION").value.contains("someone@your-domain.net"));
    }
    
    
    @AfterClass
    public static void cleanup() {
    	MockShibboleth.reload();
    }
    
    
	/** Fixed a bug in the default version of this method. It dosn't follow redirects properly **/
	public static Response GET(Object url, boolean followRedirect) {
		Response response = GET(url);
		if (Http.StatusCode.FOUND == response.status && followRedirect) {
			String redirectedTo = response.getHeader("Location");
			response = GET(redirectedTo,followRedirect);
		}
		return response;
	}
}