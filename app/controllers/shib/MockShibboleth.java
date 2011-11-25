package controllers.shib;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Play;
import play.jobs.OnApplicationStart;
import play.mvc.Http.Header;


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
	public static Map<String, Header> headers = null;

	/**
	 * Convenience method for retrieving a mock HTTP header.
	 * 
	 * @param name
	 *            The name of the HTTP header
	 * @return The Play MVC Header object.
	 */
	public static Header get(String name) {
		if (headers == null)
			reload();
		return headers.get(name);
	}

	/**
	 * Convenience method for setting a mock HTTP header from a test
	 * 
	 * @param name
	 *            The name of the HTTP header
	 * @param value
	 *            The value of the HTTP header, will be removed if null.
	 */
	public static Header set(String name, String value) {
		if (value == null)
			headers.remove(name);

		Header header = new Header(name, value);
		headers.put(name, header);

		return header;
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
		headers = new HashMap<String, Header>();
	}

	/**
	 * Convenience method to reload the all the mock headers defined the
	 * applications.conf file. Look at anything of the form:
	 * 
	 * shib.mock.<HTTP Header> = <Header Value>
	 */
	public static Map<String, Header> reload() {

		Map<String, Header> mockHeaders = new HashMap<String, Header>();

		Set<Object> keys = Play.configuration.keySet();
		for (Object keyObj : keys) {
			if (keyObj instanceof String) {
				String key = (String) keyObj;
				if (key.startsWith("shib.mock.")) {

					String name = key.substring("shib.mock.".length());
					String value = Play.configuration.getProperty(key);
					Header header = new Header(name, value);

					mockHeaders.put(name, header);
				}
			}
		}

		headers = mockHeaders;
		return mockHeaders;
	}

}
