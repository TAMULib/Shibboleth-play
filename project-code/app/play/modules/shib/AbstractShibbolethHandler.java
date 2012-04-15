package play.modules.shib;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import play.Logger;
import play.Play;
import play.mvc.Http.Context;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import views.html.defaultpages.badRequest;

public class AbstractShibbolethHandler extends Results implements ShibbolethHandler {

	@Override
	public Result beforeLogin(Context context) {
		return null;
	}

	
	@Override
	public Map<String, String> getShibbolethAttributes(Map<String, String[]> headers) {
		Map<String, String> attributeMapping = getAttributeMapping();
		Map<String, String> shibbolethAttributes = new HashMap<String,String>();
		for (String attributeName : attributeMapping.keySet()) {
			
			String headerName = attributeMapping.get(attributeName);
			String[] headerValues = headers.get(headerName);
			if (headerValues == null || headerValues.length == 0) {
				Logger.trace("Shib: Did not recieve header '"+headerName+"', skipping.");
				continue;
			} 
			if (headerValues.length > 1)
				Logger.warn("Shib: Recieved multiple '"+headerName+"' headers for attribute '"+attributeName+"', picking the first one.");
				
			String headerValue = headerValues[0];
			
			shibbolethAttributes.put(attributeName, headerValue);
			Logger.debug("Shib: Recieved attribute, '" + attributeName + "' = '"+ headerValue + "'");
		}
		
		return attributeMapping;
	}

	@Override
	public Result verifyShibbolethAttributes(Context context, Map<String, String> shibbolethAttributes) {
		
		boolean foundAllRequiredAttributes = true;
		for (String required: getRequiredAttributes()) {
			if (!shibbolethAttributes.containsKey(required)) {
				foundAllRequiredAttributes = false;
				Logger.warn("Shib: missing required attribute '"+required+"'");
			}
		}
		
		if (!foundAllRequiredAttributes)
			return badRequest("Shibboleth authentication failed because the required security attributes were not found.");

		Logger.debug("Shib: All required attributes present.");
		return null;
	}


	@Override
	public Result afterLogin(Context context) {
		return null;
	}

	@Override
	public Result beforeLogout(Http.Context context) {
		return null;
	}

	@Override
	public Result afterLogout(Http.Context context) {
		return null;
	}

	
	
	
	
	
	private Map<String,String> attributeMap = null;
	
	/**
	 * @return The mapped session attributes from the Play! configuration.
	 *         HashMap of Attribute names to HTTP Header names
	 */
	protected synchronized Map<String, String> getAttributeMapping() {
		
		if (this.attributeMap == null) {
			Map<String, String> attributeMap = new HashMap<String, String>();
	
			Set<String> keys = Play.application().configuration().keys();
			for (String key : keys) {
				if (key.startsWith("shib.attribute.")) {
					String attribute = key.substring("shib.attribute.".length());
					String header = Play.application().configuration().getString(key);
	
					attributeMap.put(attribute, header);
				}
			}
			
			this.attributeMap = attributeMap;
		}

		return attributeMap;
	}

	private List<String> requiredAttributes = null;
	
	/**
	 * @return The list of attributes required for successful authentication.
	 */
	protected synchronized List<String> getRequiredAttributes() {

		if (this.requiredAttributes == null) {
			String requiredAttributesString = Play.application().configuration().getString("shib.require");
			
			if (requiredAttributesString == null)
				return Collections.EMPTY_LIST;
			
			String[] requiredAttributes = requiredAttributesString.split(",");
			
			for (int i = 0; i < requiredAttributes.length; i++) {
				requiredAttributes[i] = requiredAttributes[i].trim();
			}
			
			this.requiredAttributes = Arrays.asList(requiredAttributes);
		}
		
		return requiredAttributes;
	}
	
	
	
}
