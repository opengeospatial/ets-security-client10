package org.opengis.cite.securityclient10;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Validate test run arguments from a Document or a HashMap.
 */
public class TestRunArgValidator {

	/**
	 * Array of service type test run properties currently supported by this ETS. Should match emulated
	 * service types in org.opengis.cite.securityclient10.httpServer package.
	 */
	private static final String[] allowedServiceTypes = {"wms111", "wms13", "wps20"};
	
	/**
	 * Validate a String, String HashMap of Test Run Arguments. Raise an exception on invalid data.
	 * @param params Hashmap of test run properties.
	 */
	public static void validateMap(Map<String, String> params) {
        // Test Server Emulated Service Type
        final String serviceTypeParam = params.get(TestRunArg.Service_Type.toString());
        
        if ((null == serviceTypeParam) || serviceTypeParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.Service_Type.toString());
        }
        
		if (Arrays.stream(allowedServiceTypes).noneMatch(serviceTypeParam::equals)) {
        	throw new IllegalArgumentException("Unsupported service type in test run properties: " 
        			+ serviceTypeParam);
        }
        
        // Test Server address
        String addressParam = params.get(TestRunArg.Address.toString());
        
        if ((null == addressParam) || addressParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.Address.toString());
        }
        
        // Test Server Port
        Integer portParam = Integer.parseInt(params.get(TestRunArg.Port.toString()));
        
        // Test Server Hostname
        String hostParam = params.get(TestRunArg.Host.toString());
        
        if ((null == hostParam) || hostParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.Host.toString());
        }
        
        // Test Server Path
        String pathParam = params.get(TestRunArg.Path.toString());
        
        // Java Keystore Path
        String jksParam = params.get(TestRunArg.JKS_Path.toString());
        
        if ((null == jksParam) || jksParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.JKS_Path.toString());
        }
        
        // Java Keystore Password
        String jksPassParam = params.get(TestRunArg.JKS_Password.toString());
        
        if ((null == jksPassParam) || jksPassParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.JKS_Password.toString());
        }
        
        // Authentication method
        String authentication = params.get(TestRunArg.Authentication.toString());
        
        // Authentication: SAML2 URL
        String saml2Url = params.get(TestRunArg.SAML2_URL.toString());
        
        // Secure Client Requests Document Path
        String iutParam = params.get(TestRunArg.IUT.toString());
    }

	/**
	 * Validate a Properties document of Test Run Properties. Raises an exception on invalid data.
	 * @param properties Properties document
	 */
	@SuppressWarnings("unchecked")
	public static void validateProperties(Properties properties) {
		Map<String, String> args = new HashMap<String, String>();
		Enumeration<String> keys = (Enumeration<String>) properties.propertyNames();
		
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			args.put(key, properties.getProperty(key));
		}
		
		validateMap(args);
	}
}
