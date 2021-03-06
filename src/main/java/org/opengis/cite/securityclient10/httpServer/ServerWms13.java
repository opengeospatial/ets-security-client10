package org.opengis.cite.securityclient10.httpServer;

import java.io.PrintWriter;
import java.util.Enumeration;

import javax.xml.transform.TransformerException;

import org.opengis.cite.securityclient10.Identifiers;
import org.opengis.cite.securityclient10.Namespaces;
import org.opengis.cite.securityclient10.Schemas;
import org.opengis.cite.securityclient10.util.XMLUtils;
import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to emulate an OGC WMS 1.3.0 instance.
 * 
 * Pass in the client request and servlet response to #handleRequest, and the method will auto fill the
 * servlet response.
 * 
 * Details of how this fake WMS 1.3.0 works are based on Web Map Service Implementation Specification 
 * (OGC 06-042).
 *
 */
public class ServerWms13 extends EmulatedServer {
	/**
	 * Create an emulated WMS 1.3.0.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @param options ServerOptions object with emulated server configuration
	 */
	public ServerWms13(ServerOptions options) {
		this.documentFactory.setNamespaceAware(true);
		this.options = options;
	}
	
	/**
	 * Build a valid WMS 1.3.0 response for the client request, and automatically complete the response.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws TransformerException {
		System.out.println("Building WMS 1.3.0 Response");
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
				buildCapabilities(request, response, true);
			}
		} else if (serviceValue == null || requestValue == null || !serviceValue.equals("WMS")) {
			buildException("Invalid query parameters", response);
		} else {
			
			// Handle potential other request types
			switch (requestValue) {
				case "GetCapabilities":
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
					buildException("Unsupported Operation", response);
					break;
				default:
					buildException("Invalid request parameter", response);
					break;
			}
		}
		
	}
	
	/**
	 * Return an HTTP response to the client with valid headers and a body containing the  Capabilities 
	 * XML document. If `completeCapabilities` is true, then a complete capabilities document with a 
	 * Content section (Layers) will be generated, and the embedded links will use the "/full" URL. If 
	 * false, then a partial capabilities document will be generated without a Content section (Layers), 
	 * and clients must use authentication to request the complete capabilities.
	 * 
	 * Source: Annex E.1, Annex H.1
	 * @param request Source request from client, used to build absolute URLs for HREFs
	 * @param response Response to build to send back to client
	 * @param completeCapabilities If true, build a complete capabilities document
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response, boolean completeCapabilities) throws TransformerException {
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
		
		PrintWriter printWriter = getWriterForResponse(response);
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		Document doc = domImplementation.createDocument(Namespaces.WMS, "WMS_Capabilities", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xlink", Namespaces.XLINK);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:ows", Namespaces.OWS);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.WMS + " " + Schemas.WMS_13);

		// Service Section
		Element service = doc.createElementNS(Namespaces.WMS, "Service");
		rootElement.appendChild(service);
		
		Element name = doc.createElementNS(Namespaces.WMS, "Name");
		name.setTextContent("WMS");
		service.appendChild(name);
		
		Element title = doc.createElementNS(Namespaces.WMS, "Title");
		title.setTextContent("ETS Security Client 1.0 WMS 1.3.0");
		service.appendChild(title);
		
		Element abstractElement = doc.createElementNS(Namespaces.WMS, "Abstract");
		abstractElement.setTextContent("WMS 1.3.0 for validating secure client requests under ETS Security Client 1.0");
		service.appendChild(abstractElement);
		
		Element onlineResource = doc.createElementNS(Namespaces.WMS, "OnlineResource");
		onlineResource.setAttribute("xmlns:xlink", Namespaces.XLINK);
		onlineResource.setAttribute("xlink:type", "simple");
		onlineResource.setAttribute("xlink:href", href);
		service.appendChild(onlineResource);
		
		// Capability Section
		Element capability = doc.createElementNS(Namespaces.WMS, "Capability");
		rootElement.appendChild(capability);
		
		// Capability > Request
		Element requestElement = doc.createElementNS(Namespaces.WMS, "Request");
		capability.appendChild(requestElement);
		
		// Capability > Request > GetCapabilities
		Element getCapabilities = doc.createElementNS(Namespaces.WMS, "GetCapabilities");
		requestElement.appendChild(getCapabilities);
		
		Element getCapabilitiesFormat = doc.createElementNS(Namespaces.WMS, "Format");
		getCapabilitiesFormat.setTextContent("application/vnd.ogc.wms_xml");
		getCapabilities.appendChild(getCapabilitiesFormat);
		
		Element getCapabilitiesDCPType = doc.createElementNS(Namespaces.WMS, "DCPType");
		getCapabilities.appendChild(getCapabilitiesDCPType);
		
		Element getCapabilitiesDCPTypeHTTP = doc.createElementNS(Namespaces.WMS, "HTTP");
		getCapabilitiesDCPType.appendChild(getCapabilitiesDCPTypeHTTP);
		
		Element getCapabilitiesDCPTypeHTTPGet = doc.createElementNS(Namespaces.WMS, "Get");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPGet);
		
		Element getCapabilitiesDCPTypeHTTPGetOR = doc.createElementNS(Namespaces.WMS, "OnlineResource");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPGet.appendChild(getCapabilitiesDCPTypeHTTPGetOR);
		
		Element getCapabilitiesDCPTypeHTTPPost = doc.createElementNS(Namespaces.WMS, "Post");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPPost);
		
		Element getCapabilitiesDCPTypeHTTPPostOR = doc.createElementNS(Namespaces.WMS, "OnlineResource");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPPost.appendChild(getCapabilitiesDCPTypeHTTPPostOR);
		
		// Capability > Request > GetMap
		Element getMap = doc.createElementNS(Namespaces.WMS, "GetMap");
		requestElement.appendChild(getMap);
		
		Element getMapFormat = doc.createElementNS(Namespaces.WMS, "Format");
		getMapFormat.setTextContent("image/png");
		getMap.appendChild(getMapFormat);
		
		Element getMapDCPType = doc.createElementNS(Namespaces.WMS, "DCPType");
		getMap.appendChild(getMapDCPType);
		
		Element getMapDCPTypeHTTP = doc.createElementNS(Namespaces.WMS, "HTTP");
		getMapDCPType.appendChild(getMapDCPTypeHTTP);
		
		Element getMapDCPTypeHTTPGet = doc.createElementNS(Namespaces.WMS, "Get");
		getMapDCPTypeHTTP.appendChild(getMapDCPTypeHTTPGet);
		
		Element getMapDCPTypeHTTPGetOR = doc.createElementNS(Namespaces.WMS, "OnlineResource");
		getMapDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", Namespaces.XLINK);
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getMapDCPTypeHTTPGet.appendChild(getMapDCPTypeHTTPGetOR);
		
		// Capability > Exception
		Element exception = doc.createElementNS(Namespaces.WMS, "Exception");
		capability.appendChild(exception);
		
		Element exceptionFormat = doc.createElementNS(Namespaces.WMS, "Format");
		exceptionFormat.setTextContent("application/vnd.ogc.se_xml");
		exception.appendChild(exceptionFormat);
		
		if (completeCapabilities) {
			// Capability > Layer
			Element layer = doc.createElementNS(Namespaces.WMS, "Layer");
			capability.appendChild(layer);
			
			Element layerTitle = doc.createElementNS(Namespaces.WMS, "Title");
			layerTitle.setTextContent("False Layer Data");
			layer.appendChild(layerTitle);
			
			Element layerSrs = doc.createElementNS(Namespaces.WMS, "CRS");
			layerSrs.setTextContent("EPSG:4326");
			layer.appendChild(layerSrs);
		}
		
		// Capability > ExtendedSecurityCapabilities
		Element vendorSpecificCapabilities = buildExtendedSecurityCapabilities(doc, baseHref);
		capability.appendChild(vendorSpecificCapabilities);
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
	}
	
	/**
	 * Build an ows:Get element for an ows:Operation endpoint, using `href` as the embedded URL.
	 * Will add annotations as necessary from the ServerOptions.
	 * 
	 * @param doc Document for creating elements
	 * @param href String with URL to embed
	 * @param methods Methods to support in HTTP Methods constraint
	 * @return Element tree
	 */
	private Element buildGetElement(Document doc, String href, String[] methods) {
		Element get = doc.createElementNS(Namespaces.OWS, "Get");
		get.setAttribute("xmlns:xlink", Namespaces.XLINK);
		get.setAttribute("xlink:type", "simple");
		get.setAttribute("xlink:href", href);
		
		// Add SAML2 constraint
		if (this.options.getAuthentication().equals("saml2") && this.options.getIdpUrl() != null) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.SAML2);
			get.appendChild(constraint);
			
			Element constraintValuesReference = doc.createElementNS(Namespaces.OWS, "ValuesReference");
			constraintValuesReference.setAttributeNS(Namespaces.OWS, "ows:reference", this.options.getIdpUrl());
			constraint.appendChild(constraintValuesReference);
		}
		
		// Add HTTP Methods Constraint
		if (this.options.getHttpMethods()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.HTTP_METHODS);
			get.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			for (int i = 0; i < methods.length; i++) {
				String method = methods[i];
				
				Element value = doc.createElementNS(Namespaces.OWS, "Value");
				value.setTextContent(method);
				constraintAllowedValues.appendChild(value);
			}
		}
		
		// Add W3C CORS Constraint
		if (this.options.getCors()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.W3C_CORS);
			get.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP Exception Handling Constraint
		if (this.options.getHttpExceptionHandling()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.EXCEPTION_HANDLING);
			get.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		return get;
	}
	
	/**
	 * Build an ows:Post element for an ows:Operation endpoint, using `href` as the embedded URL.
	 * Will add annotations as necessary from the ServerOptions.
	 * 
	 * @param doc Document for creating elements
	 * @param href String with URL to embed
	 * @param methods Methods to support in HTTP Methods constraint
	 * @return Element tree
	 */
	private Element buildPostElement(Document doc, String href, String[] methods) {
		Element post = doc.createElementNS(Namespaces.OWS, "Post");
		post.setAttribute("xmlns:xlink", Namespaces.XLINK);
		post.setAttribute("xlink:type", "simple");
		post.setAttribute("xlink:href", href);
		
		// Add SAML2 constraint
		if (this.options.getAuthentication().equals("saml2") && this.options.getIdpUrl() != null) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.SAML2);
			post.appendChild(constraint);
			
			Element constraintValuesReference = doc.createElementNS(Namespaces.OWS, "ValuesReference");
			constraintValuesReference.setAttributeNS(Namespaces.OWS, "ows:reference", this.options.getIdpUrl());
			constraint.appendChild(constraintValuesReference);
		}
		
		// Add HTTP Methods Constraint
		if (this.options.getHttpMethods()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.HTTP_METHODS);
			post.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			for (int i = 0; i < methods.length; i++) {
				String method = methods[i];
				
				Element value = doc.createElementNS(Namespaces.OWS, "Value");
				value.setTextContent(method);
				constraintAllowedValues.appendChild(value);
			}
		}
		
		// Add W3C CORS Constraint
		if (this.options.getCors()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.W3C_CORS);
			post.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP Exception Handling Constraint
		if (this.options.getHttpExceptionHandling()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.EXCEPTION_HANDLING);
			post.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP POST Content-Type Constraint
		if (this.options.getHttpPostContentType()) {
			Element constraint = doc.createElementNS(Namespaces.OWS, "Constraint");
			constraint.setAttribute("name", Identifiers.CONTENT_TYPE);
			post.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			Element value = doc.createElementNS(Namespaces.OWS, "Value");
			value.setTextContent("application/x-www-form-urlencoded");
			constraintAllowedValues.appendChild(value);
		}
		
		return post;
	}
	
	/**
	 * Create the ExtendedSecurityCapabilities element and populate it with security annotations based on
	 * the test run properties for the ETS.
	 */
	private Element buildExtendedSecurityCapabilities(Document doc, String href) {
		Element extendedSecurityCapabilities = doc.createElementNS(Namespaces.OWS_SECURITY, "ExtendedSecurityCapabilities");
		String completeCapabilitiesUrl = href + "/full";
		
		Element operationsMetadata = doc.createElementNS(Namespaces.OWS, "OperationsMetadata");
		extendedSecurityCapabilities.appendChild(operationsMetadata);
		
		// GetCapabilities
		Element getCapabilities = doc.createElementNS(Namespaces.OWS, "Operation");
		getCapabilities.setAttribute("name", "GetCapabilities");
		operationsMetadata.appendChild(getCapabilities);
		
		Element getCapabilitiesDcp = doc.createElementNS(Namespaces.OWS, "DCP");
		getCapabilities.appendChild(getCapabilitiesDcp);
		
		Element getCapabilitiesDcpHttp = doc.createElementNS(Namespaces.OWS, "HTTP");
		getCapabilitiesDcp.appendChild(getCapabilitiesDcpHttp);
		
		// GetCapabilities GET
		String[] getCapabilitiesMethods = {"GET", "POST"};
		Element getCapabilitiesDcpHttpGet = this.buildGetElement(doc, completeCapabilitiesUrl, getCapabilitiesMethods);
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpGet);
		
		// GetCapabilities POST
		Element getCapabilitiesDcpHttpPost = this.buildPostElement(doc, completeCapabilitiesUrl, getCapabilitiesMethods);
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpPost);
		
		// GetMap
		Element getMap = doc.createElementNS(Namespaces.OWS, "Operation");
		getMap.setAttribute("name", "GetMap");
		operationsMetadata.appendChild(getMap);
		
		Element getMapDcp = doc.createElementNS(Namespaces.OWS, "DCP");
		getMap.appendChild(getMapDcp);
		
		Element getMapDcpHttp = doc.createElementNS(Namespaces.OWS, "HTTP");
		getMapDcp.appendChild(getMapDcpHttp);
		
		// GetMap GET
		String[] getMapMethods = {"GET"};
		Element getMapDcpHttpGet = this.buildGetElement(doc, completeCapabilitiesUrl, getMapMethods);
		getMapDcpHttp.appendChild(getMapDcpHttpGet);
		
		return extendedSecurityCapabilities;
	}
	
	/**
	 * Return a Service Exception for `reason`. Response will have content type 
	 * "application/vnd.ogc.se_xml" and HTTP status code 404.
	 * 
	 * Source: Annex E.2, Annex H.2
	 * 
	 * @param reason String with reason for Service Exception.
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildException(String reason, HttpServletResponse response) throws TransformerException {
		response.setContentType("application/vnd.ogc.se_xml");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		
		PrintWriter printWriter = getWriterForResponse(response);
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		Document doc = domImplementation.createDocument(Namespaces.OGC, "ServiceExceptionReport", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.OGC + " " + Schemas.WMS_13_SE);
		
		Element serviceException = doc.createElementNS(Namespaces.OGC, "ServiceException");
		serviceException.setTextContent(reason);
		rootElement.appendChild(serviceException);
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
	}
}
