package org.opengis.cite.securityclient10;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Validate test run arguments from a Document or a HashMap.
 */
public class TestRunArgValidator {

	/**
	 * List of service type test run properties currently supported by this ETS. Should match emulated
	 * service types in org.opengis.cite.securityclient10.httpServer package.
	 */
	private static final List<String> allowedServiceTypes = Arrays.asList("wms111");
	
	/**
	 * Validate a String, String HashMap of Test Run Arguments. Raise an exception on invalid data.
	 * @param params Hashmap of test run properties.
	 */
	public static void validateMap(Map<String, String> params) {
        // Test Server Emulated Service Type
        String serviceTypeParam = params.get(TestRunArg.Service_Type.toString());
        
        if ((null == serviceTypeParam) || serviceTypeParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.Service_Type.toString());
        }
        
        if (!allowedServiceTypes.contains(serviceTypeParam)) {
        	throw new IllegalArgumentException("Unsupported service type in test run properties: " 
        			+ serviceTypeParam);
        }
        
        // Test Server Host address
        String hostParam = params.get(TestRunArg.Host.toString());
        
        if ((null == hostParam) || hostParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " 
            		+ TestRunArg.Host.toString());
        }
        
        // Test Server Port
        Integer portParam = Integer.parseInt(params.get(TestRunArg.Port.toString()));
        
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
        
        // Secure Client Requests Document Path
        String iutParam = params.get(TestRunArg.IUT.toString());
    }
}
