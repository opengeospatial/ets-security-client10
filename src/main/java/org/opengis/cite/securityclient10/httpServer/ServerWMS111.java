package org.opengis.cite.securityclient10.httpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Enumeration;
import java.util.zip.DeflaterOutputStream;

import org.opengis.cite.securityclient10.Namespaces;
import org.opengis.cite.securityclient10.Schemas;
import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;
import org.sonatype.plexus.components.cipher.Base64;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

/**
 * Class to emulate an OGC WMS 1.1.1 instance.
 * 
 * Pass in the client request and servlet response to #handleRequest, and the method will auto fill the
 * servlet response.
 * 
 * Details of how this fake WMS 1.1.1 works are based on Web Map Service Implementation Specification 
 * (OGC 01-068r3).
 *
 */
public class ServerWms111 extends EmulatedServer {
	/**
	 * Test Run Properties for this emulated server
	 */
	ServerOptions options;
	
	/**
	 * RelayState token for SAML2
	 */
	String relayState;
	
	/**
	 * Create an emulated WMS 1.1.1.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @param options ServerOptions object with emulated server configuration
	 * @throws ParserConfigurationException Exception if new document builder could not be created
	 * @throws TransformerConfigurationException Exception if new transformer could not be created
	 */
	public ServerWms111(ServerOptions options) throws ParserConfigurationException, TransformerConfigurationException {
		this.options = options;
		this.relayState = "token";
	}
	
	/**
	 * Extract the uri from a request object.
	 * If contextOnly is true, then any path segments after the context path are excluded.
	 * 
	 * @param request The request to extract
	 * @param contextOnly Only include up to the context path
	 * @return The uri
	 */
	public static String getUri(HttpServletRequest request, Boolean contextOnly) {
		String path;
		if (contextOnly) {
			path = "/" + request.getRequestURI().split("/")[1];
		} else {
			path = request.getRequestURI();
		}
		
		return String.format("%s://%s:%d%s",
				request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				path);
	}
	
	/**
	 * Build a valid WMS 1.1.1 response for the client request, and automatically complete the response.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		System.out.println("Building WMS 1.1.1 Response");
		System.out.println("Query Params: " + request.getQueryString());
		
		enableCors(response);
		
		// If mandatory query parameters are missing, return an exception
		// Required: "SERVICE", "REQUEST"
		String serviceValue = null;
		String requestValue = null;
		
		// Iterate the query parameters and look for service and request, ignoring case
		for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
			String parameter = e.nextElement();
			
			if (parameter.compareToIgnoreCase("service") == 0) {
				serviceValue = request.getParameter(parameter);
			} else if (parameter.compareToIgnoreCase("request") == 0) {
				requestValue = request.getParameter(parameter);
			}
		}
		
		// If it is the SAML callback URL then process the SAML Authentication Response and set up a
		// security context for the user.
		if (request.getPathInfo().endsWith("/saml2")) {
			if (validateSamlAuthenticationResponse(request, response)) {
				buildSecurityContext(request, response);
			}
		} else if (serviceValue == null || requestValue == null || !serviceValue.equals("WMS")) {
			buildException("Invalid query parameters", response);
		} else {
			
			// Handle potential other request types
			switch (requestValue) {
				case "GetCapabilities":
				case "capabilities":
					// Return a Capabilities document
					if (request.getPathInfo().endsWith("/full") && this.getAuthenticationEnabled()) {
						if (validateSecureRequest(request, response)) {
							buildCapabilities(request, response, true);
						}
					} else {
						buildCapabilities(request, response, false);
					}
					break;
				case "GetMap":
					// Return a GetMap document
					buildException("Operation not supported by test server", response);
					break;
				case "GetFeatureInfo":
				case "DescribeLayer":
				case "GetLegendGraphic":
				case "GetStyles":
				case "PutStyles":
					buildException("Unsupported Operation", response);
					break;
				default:
					buildException("Invalid request parameter", response);
					break;
			}
		}
		
	}
	
	/**
	 * Return an HTTP response to the client with valid headers and a body containing the Capabilities
	 * document. If `completeCapabilities` is true, then a complete capabilities document with a Content 
	 * section (Layers) will be generated, and the embedded links will use the "/full" URL. If false, then 
	 * a partial capabilities document will be generated without a Content section (Layers), and clients
	 * must use authentication to request the complete capabilities.
	 * 
	 * Source: Annex A
	 * 
	 * @param request Source request from client, used to build absolute URLs for HREFs
	 * @param response Response to build to send back to client
	 * @param completeCapabilities If true, build a complete capabilities document
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response, boolean completeCapabilities) throws IOException, TransformerException {
		response.setContentType("application/vnd.ogc.wms_xml");
		response.setStatus(HttpServletResponse.SC_OK);
		
		// Extract scheme/host/port/path for HREFs
		String baseHref = getUri(request, false);
		String href;
		
		if (completeCapabilities) {
			href = baseHref + "/full";
		} else {
			href = baseHref;
		}
		
		
		PrintWriter printWriter = response.getWriter();
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		DocumentType doctype = domImplementation.createDocumentType("doctype", null,
				Schemas.WMS_111);
		Document doc = domImplementation.createDocument(null, "WMT_MS_Capabilities", doctype);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.1.1");
		
		// Service Section
		Element service = doc.createElement("Service");
		rootElement.appendChild(service);
		
		Element name = doc.createElement("Name");
		name.setTextContent("ets-security-client-10-wms-111");
		service.appendChild(name);
		
		Element title = doc.createElement("Title");
		title.setTextContent("ETS Security Client 1.0 WMS 1.1.1");
		service.appendChild(title);
		
		Element abstractElement = doc.createElement("Abstract");
		abstractElement.setTextContent("WMS 1.1.1 for validating secure client requests under ETS Security Client 1.0");
		service.appendChild(abstractElement);
		
		Element onlineResource = doc.createElement("OnlineResource");
		onlineResource.setAttribute("xmlns:xlink", Namespaces.XLINK);
		onlineResource.setAttribute("xlink:type", "simple");
		onlineResource.setAttribute("xlink:href", href);
		service.appendChild(onlineResource);
		
		// Capability Section
		Element capability = doc.createElement("Capability");
		rootElement.appendChild(capability);
		
		// Capability > Request
		Element requestElement = doc.createElement("Request");
		capability.appendChild(requestElement);
		
		// Capability > Request > GetCapabilities
		Element getCapabilities = doc.createElement("GetCapabilities");
		requestElement.appendChild(getCapabilities);
		
		Element getCapabilitiesFormat = doc.createElement("Format");
		getCapabilitiesFormat.setTextContent("application/vnd.ogc.wms_xml");
		getCapabilities.appendChild(getCapabilitiesFormat);
		
		Element getCapabilitiesDCPType = doc.createElement("DCPType");
		getCapabilities.appendChild(getCapabilitiesDCPType);
		
		Element getCapabilitiesDCPTypeHTTP = doc.createElement("HTTP");
		getCapabilitiesDCPType.appendChild(getCapabilitiesDCPTypeHTTP);
		
		Element getCapabilitiesDCPTypeHTTPGet = doc.createElement("Get");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPGet);
		
		Element getCapabilitiesDCPTypeHTTPGetOR = doc.createElement("OnlineResource");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPGet.appendChild(getCapabilitiesDCPTypeHTTPGetOR);
		
		Element getCapabilitiesDCPTypeHTTPPost = doc.createElement("Post");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPPost);
		
		Element getCapabilitiesDCPTypeHTTPPostOR = doc.createElement("OnlineResource");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPPost.appendChild(getCapabilitiesDCPTypeHTTPPostOR);
		
		// Capability > Request > GetMap
		Element getMap = doc.createElement("GetMap");
		requestElement.appendChild(getMap);
		
		Element getMapFormat = doc.createElement("Format");
		getMapFormat.setTextContent("image/png");
		getMap.appendChild(getMapFormat);
		
		Element getMapDCPType = doc.createElement("DCPType");
		getMap.appendChild(getMapDCPType);
		
		Element getMapDCPTypeHTTP = doc.createElement("HTTP");
		getMapDCPType.appendChild(getMapDCPTypeHTTP);
		
		Element getMapDCPTypeHTTPGet = doc.createElement("Get");
		getMapDCPTypeHTTP.appendChild(getMapDCPTypeHTTPGet);
		
		Element getMapDCPTypeHTTPGetOR = doc.createElement("OnlineResource");
		getMapDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getMapDCPTypeHTTPGet.appendChild(getMapDCPTypeHTTPGetOR);
		
		// Capability > Exception
		Element exception = doc.createElement("Exception");
		capability.appendChild(exception);
		
		Element exceptionFormat = doc.createElement("Format");
		exceptionFormat.setTextContent("application/vnd.ogc.se_xml");
		exception.appendChild(exceptionFormat);
		
		if (completeCapabilities) {
			// Capabilities > Layer
			Element layer = doc.createElement("Layer");
			capability.appendChild(layer);
			
			Element layerTitle = doc.createElement("Title");
			layerTitle.setTextContent("False Layer Data");
			layer.appendChild(layerTitle);
			
			Element layerSrs = doc.createElement("SRS");
			layerSrs.setTextContent("EPSG:4326");
			layer.appendChild(layerSrs);
		}
		
		// Capability > VendorSpecificCapabilities
		Element vendorSpecificCapabilities = buildVendorSpecificCapabilities(doc, baseHref);
		capability.appendChild(vendorSpecificCapabilities);
		
		printWriter.print(documentToString(doc));
	}
	
	/**
	 * Create the VendorSpecificCapabilities element and populate it with security annotations based on
	 * the test run properties for the ETS.
	 */
	private Element buildVendorSpecificCapabilities(Document doc, String href) {
		Element vendorSpecificCapabilities = doc.createElement("VendorSpecificCapabilities");
		
		String completeCapabilitiesUrl = href + "/full";
		
		// Add SAML2 constraint
		if (this.options.getAuthentication().equals("saml2") && this.options.getSaml2Url() != null) {
			Element extendedSecurityCapabilities = doc.createElement("ows_security:ExtendedSecurityCapabilities");
			extendedSecurityCapabilities.setAttribute("xmlns:ows_security", Namespaces.OWS_SECURITY);
			vendorSpecificCapabilities.appendChild(extendedSecurityCapabilities);
			
			Element operationsMetadata = doc.createElement("ows:OperationsMetadata");
			operationsMetadata.setAttribute("xmlns:ows", Namespaces.OWS);
			extendedSecurityCapabilities.appendChild(operationsMetadata);
			
			// GetCapabilities
			Element getCapabilities = doc.createElement("ows:Operation");
			getCapabilities.setAttribute("name", "GetCapabilities");
			operationsMetadata.appendChild(getCapabilities);
			
			Element getCapabilitiesDcp = doc.createElement("ows:DCP");
			getCapabilities.appendChild(getCapabilitiesDcp);
			
			Element getCapabilitiesDcpHttp = doc.createElement("ows:HTTP");
			getCapabilitiesDcp.appendChild(getCapabilitiesDcpHttp);
			
			// GetCapabilities GET
			Element getCapabilitiesDcpHttpGet = doc.createElement("ows:Get");
			getCapabilitiesDcpHttpGet.setAttribute("xmlns:xlink", Namespaces.XLINK);
			getCapabilitiesDcpHttpGet.setAttribute("xlink:type", "simple");
			getCapabilitiesDcpHttpGet.setAttribute("xlink:href", completeCapabilitiesUrl);
			getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpGet);
			
			Element getCapabilitiesDcpHttpGetConstraint = doc.createElement("ows:Constraint");
			getCapabilitiesDcpHttpGetConstraint.setAttribute("name", "urn:ogc:def:security:1.0:rc:authentication:saml2");
			getCapabilitiesDcpHttpGet.appendChild(getCapabilitiesDcpHttpGetConstraint);
			
			Element getCapabilitiesDcpHttpGetConstraintValues = doc.createElement("ows:ValuesReference");
			getCapabilitiesDcpHttpGetConstraintValues.setAttribute("ows:reference", this.options.getSaml2Url());
			getCapabilitiesDcpHttpGetConstraint.appendChild(getCapabilitiesDcpHttpGetConstraintValues);
			
			// GetCapabilities POST
			Element getCapabilitiesDcpHttpPost = doc.createElement("ows:Post");
			getCapabilitiesDcpHttpPost.setAttribute("xmlns:xlink", Namespaces.XLINK);
			getCapabilitiesDcpHttpPost.setAttribute("xlink:type", "simple");
			getCapabilitiesDcpHttpPost.setAttribute("xlink:href", completeCapabilitiesUrl);
			getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpPost);
			
			Element getCapabilitiesDcpHttpPostConstraint = doc.createElement("ows:Constraint");
			getCapabilitiesDcpHttpPostConstraint.setAttribute("name", "urn:ogc:def:security:1.0:rc:authentication:saml2");
			getCapabilitiesDcpHttpPost.appendChild(getCapabilitiesDcpHttpPostConstraint);
			
			Element getCapabilitiesDcpHttpPostConstraintValues = doc.createElement("ows:ValuesReference");
			getCapabilitiesDcpHttpPostConstraintValues.setAttribute("ows:reference", this.options.getSaml2Url());
			getCapabilitiesDcpHttpPostConstraint.appendChild(getCapabilitiesDcpHttpPostConstraintValues);
		}
		
		return vendorSpecificCapabilities;
	}

	/**
	 * Return a Service Exception for `reason`. Response will have content type 
	 * "application/vnd.ogc.se_xml" and HTTP status code 404.
	 * 
	 * Source: Annex A.4
	 * 
	 * @param reason String with reason for Service Exception.
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildException(String reason, HttpServletResponse response) throws IOException, TransformerException {
		response.setContentType("application/vnd.ogc.se_xml");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		
		PrintWriter printWriter = response.getWriter();
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		DocumentType doctype = domImplementation.createDocumentType("doctype", null,
				Schemas.WMS_111_SE);
		Document doc = domImplementation.createDocument(null, "ServiceExceptionReport", doctype);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.1.1");
		
		Element serviceException = doc.createElement("ServiceException");
		serviceException.setTextContent(reason);
		rootElement.appendChild(serviceException);
		
		printWriter.print(documentToString(doc));
	}
	
	/**
	 * Generate a deflated and base64-encoded representation of a SAML authentication request document.
	 * This is meant to be used as a query parameter for clients to use when authenticating to a SAML 2
	 * Identity Provider.
	 * 
	 * The base path of the request handler is necessary to include the SAML callback URL that is sent to
	 * the Identity Provider.
	 * 
	 * @param href The base path of the request handler
	 * @return String of auth request
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	private String buildSamlAuthRequest(String href) throws TransformerException {
		Document doc = this.documentBuilder.newDocument();
		
		Element rootElement = doc.createElement("samlp:AuthnRequest");
		rootElement.setAttribute("xmlns:samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
		rootElement.setAttribute("xmlns:saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		rootElement.setAttribute("ID", "identifier_1");
		rootElement.setAttribute("Version", "2.0");
		ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
		rootElement.setAttribute("IssueInstant", date.toString());
		rootElement.setAttribute("AssertionConsumerServiceIndex", "0");
		doc.appendChild(rootElement);
		
		Element issuer = doc.createElement("saml:Issuer");
		issuer.setTextContent(href + "/saml2");
		rootElement.appendChild(issuer);
		
		Element nameIdPolicy = doc.createElement("samlp:NameIDPolicy");
		nameIdPolicy.setAttribute("AllowCreate", "true");
		nameIdPolicy.setAttribute("Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
		rootElement.appendChild(nameIdPolicy);
		
		// Temporarily disable the XML Declaration for this fragment
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		String flatDoc = documentToString(doc);
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		
		return deflateAndBase64String(flatDoc);
	}
	
	/**
	 * Create a security context for the client, returning a response that sets a cookie and redirects to
	 * the complete capabilities document.
	 * Note: In a complete SAML 2.0 implementation, the redirect URI would be determined from the
	 * authentication response from the IdP, and the IdP received that URI from the redirect that was made
	 * by this Service Provider. In this test case, we only support GetCapabilities so that is where the
	 * redirect will go instead.
	 * 
	 * @param request The request from the client
	 * @param response The response that will be modified
	 */
	private void buildSecurityContext(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.setHeader("Set-Cookie", "sessionToken=sample-token; Max-age=600; httpOnly");
		String baseUrl = getUri(request, true);
		response.setHeader("Location", baseUrl + "/full?request=GetCapabilities&service=WMS");
	}
	
	/**
	 * Deflate (compress) the input String then encode it in base64, then URL encode
	 * @param input String to compress and encode
	 * @return URL Encoded and Base64 encoded version of the deflated String
	 */
	private String deflateAndBase64String(String input) {
		// Convert to bytes
		byte[] inputBytes = null;
		try {
			inputBytes = input.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// If UTF-8 is unsupported, there are problems
			e.printStackTrace();
		}
		
		// Compress
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DeflaterOutputStream outputStream = new DeflaterOutputStream(output);
		String base64String = null;
		try {
			outputStream.write(inputBytes, 0, inputBytes.length);
			outputStream.close();
			byte[] encodedBytes = Base64.encodeBase64(output.toByteArray());
			base64String = new String(encodedBytes);
		} catch (IOException e) {
			// When the deflate output stream does not accept a write, or close
			e.printStackTrace();
		}
		
		// URL Encode
		String outputString = null;
		try {
			outputString = URLEncoder.encode(base64String, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// When the string cannot be encoded to UTF-8
			e.printStackTrace();
		}
		
		return outputString;
	}
	
	/**
	 * Return true if authentication has been set to SAML2 or another valid value in the test server 
	 * options. Returns false if no authentication method has been enabled.
	 * @return Boolean
	 */
	private boolean getAuthenticationEnabled() {
		return this.options.getAuthentication() != null;
	}
	
	/**
	 * Check if the request body has a valid SAML 2.0 Authentication Response document. For the test suite,
	 * no check is actually done and "true" can always be returned.
	 * 
	 * @param request The request from the client
	 * @param response The response to send to the client
	 * @return If the SAML Authentication response is valid
	 */
	private boolean validateSamlAuthenticationResponse(HttpServletRequest request, HttpServletResponse response) {
		return true;
	}
	
	/**
	 * Validate a request to a secure resource has a valid Security Context.
	 * In this case, a security context is defined using an HTTP cookie. If the cookie is invalid or 
	 * missing, then respond with a redirect to the Identity Provider (if using SAML2) or Service Exception
	 * (if no authentication defined).
	 * If the cookie is valid, then simply return true, leaving the response alone.
	 * 
	 * @param request The request from the client
	 * @param response The response to send to the client
	 * @return If the request has a valid security context
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	private boolean validateSecureRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		String cookie = request.getHeader("Cookie");
		
		if (!this.getAuthenticationEnabled()) {
			buildException("Authentication undefined in test run properties", response);
			return false;
		} else if (cookie == null) {
			// If the cookie is missing, then redirect to IdP
			
			String idpUrl = this.options.getSaml2Url() + "?RelayState=" + this.relayState + "&SAMLRequest=" 
					+ this.buildSamlAuthRequest(getUri(request, true));
			
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", idpUrl);
			return false;
		} else if (cookie.contains("sessionToken=")) {
			// Cookie is valid, do nothing
			return true;
		} else {
			// Cookie is malformed, return service exception
			buildException("Cookie is missing sessionToken", response);
			return false;
		}
	}
}
