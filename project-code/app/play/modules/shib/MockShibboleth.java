package play.modules.shib;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;


/**
 * 
 * Mock Shibboleth header attributes. The Shibboleth module uses this class to
 * store mock HTTP headers. The headers are stored internally and when
 * Shibboleth is mocked these attributes are used instead of looking for real
 * HTTP attributes. The class is designed so that individual test cases may
 * modify these headers as needed for each particular test.
 * 
 * @author Scott Phillips, http://www.scottphillips.com/
 */
public class MockShibboleth {

	/** A mock set of HTTP headers */
	public static Map<String, String[]> headers = null;

	/**
	 * Convenience method for retrieving a mock HTTP header.
	 * 
	 * @param name
	 *            The name of the HTTP header
	 * @return The Play MVC Header object.
	 */
	public static String get(String name) {
		if (headers == null)
			reload();
		
		String[] values = headers.get(name);
		
		if (values != null)
			return values[0];
		
		return null;
	}

	/**
	 * Convenience method for setting a mock HTTP header from a test
	 * 
	 * @param name
	 *            The name of the HTTP header
	 * @param value
	 *            The value of the HTTP header, will be removed if null.
	 */
	public static String set(String name, String value) {
		if (value == null)
			headers.remove(name);
		
		String[] values = {value};
		headers.put(name, values);

		return value;
	}

	/**
	 * Convenience method for removing a mock HTTP header.
	 * 
	 * @param name
	 *            The name of the header to remove.
	 */
	public static void remove(String name) {
		set(name, null);
	}

	/**
	 * Convenience method for resetting all mock headers.
	 */
	public static void removeAll() {
		headers = new HashMap<String, String[]>();
	}
	
	public static Map<String,String[]> getHeaders() {
		if (headers == null)
			reload();
		
		return headers;
	}

	/**
	 * Convenience method to reload the all the mock headers defined the
	 * applications.conf file. Look at anything of the form:
	 * 
	 * shib.mock.<HTTP Header> = <Header Value>
	 */
	public static Map<String, String[]> reload() {

		Map<String, String[]> mockHeaders = new HashMap<String, String[]>();

		Set<String> keys = Play.application().configuration().keys();
		for (String key : keys) {
			if (key.startsWith("shib.mock.")) {

				String name = key.substring("shib.mock.".length());
				String[] values = {Play.application().configuration().getString(key)};
				mockHeaders.put(name, values);
				
				
				//DELETEME
				Logger.debug("reloading: "+name+" = "+values[0]);
			}
		}

		headers = mockHeaders;
		return mockHeaders;
	}

}
