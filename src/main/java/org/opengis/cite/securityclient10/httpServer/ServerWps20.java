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
 * Class to emulate an OGC WPS 2.0 instance.
 * 
 * Pass in the client request and servlet response to #handleRequest, and the method will auto fill the
 * servlet response.
 * 
 * Details of how this fake WPS 2.0 works are based on OGC WPS 2.0.2 Interface Standard: Corrigendum 2 
 * (OGC 14-065r2), and OGC Web Services Common Standard version 2.0.0 (OGC 06-121r9).
 *
 */
public class ServerWps20 extends EmulatedServer {
	/**
	 * Create an emulated WPS 2.0.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @param options ServerOptions object with emulated server configuration
	 */
	public ServerWps20(ServerOptions options) {
		this.documentFactory.setNamespaceAware(true);
		this.options = options;
	}
	
	/**
	 * Build a valid WPS 2.0 response for the client request, and automatically complete the response.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws TransformerException {
		System.out.println("Building WPS 2.0 Response");
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
		} else if (serviceValue == null || requestValue == null || !serviceValue.equals("WPS")) {
			buildInvalidParameterException(response);
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
				case "DescribeProcess":
				case "Execute":
				case "GetStatus":
				case "GetResult":
				default:
					buildNotSupportedException(response);
					break;
			}
		}
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
		Element get = doc.createElementNS(Namespaces.OWS_2, "Get");
		get.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		
		// Add SAML2 constraint
		if (this.options.getAuthentication().equals("saml2") && this.options.getIdpUrl() != null) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.SAML2);
			get.appendChild(constraint);
			
			Element constraintValuesReference = doc.createElementNS(Namespaces.OWS_2, "ValuesReference");
			constraintValuesReference.setAttributeNS(Namespaces.OWS_2, "ows:reference", this.options.getIdpUrl());
			constraint.appendChild(constraintValuesReference);
		}
		
		// Add HTTP Methods Constraint
		if (this.options.getHttpMethods()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.HTTP_METHODS);
			get.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS_2, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			for (int i = 0; i < methods.length; i++) {
				String method = methods[i];
				
				Element value = doc.createElementNS(Namespaces.OWS_2, "Value");
				value.setTextContent(method);
				constraintAllowedValues.appendChild(value);
			}
		}
		
		// Add W3C CORS Constraint
		if (this.options.getCors()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.W3C_CORS);
			get.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS_2, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP Exception Handling Constraint
		if (this.options.getHttpExceptionHandling()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.EXCEPTION_HANDLING);
			get.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS_2, "NoValues");
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
		Element post = doc.createElementNS(Namespaces.OWS_2, "Post");
		post.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		
		// Add SAML2 constraint
		if (this.options.getAuthentication().equals("saml2") && this.options.getIdpUrl() != null) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.SAML2);
			post.appendChild(constraint);
			
			Element constraintValuesReference = doc.createElementNS(Namespaces.OWS_2, "ValuesReference");
			constraintValuesReference.setAttributeNS(Namespaces.OWS_2, "ows:reference", this.options.getIdpUrl());
			constraint.appendChild(constraintValuesReference);
		}
		
		// Add HTTP Methods Constraint
		if (this.options.getHttpMethods()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.HTTP_METHODS);
			post.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS_2, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			for (int i = 0; i < methods.length; i++) {
				String method = methods[i];
				
				Element value = doc.createElementNS(Namespaces.OWS_2, "Value");
				value.setTextContent(method);
				constraintAllowedValues.appendChild(value);
			}
		}
		
		// Add W3C CORS Constraint
		if (this.options.getCors()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.W3C_CORS);
			post.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS_2, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP Exception Handling Constraint
		if (this.options.getHttpExceptionHandling()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.EXCEPTION_HANDLING);
			post.appendChild(constraint);
			
			Element constraintNoValues = doc.createElementNS(Namespaces.OWS_2, "NoValues");
			constraint.appendChild(constraintNoValues);
		}
		
		// Add HTTP POST Content-Type Constraint
		if (this.options.getHttpPostContentType()) {
			Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
			constraint.setAttribute("name", Identifiers.CONTENT_TYPE);
			post.appendChild(constraint);
			
			Element constraintAllowedValues = doc.createElementNS(Namespaces.OWS_2, "AllowedValues");
			constraint.appendChild(constraintAllowedValues);
			
			Element value = doc.createElementNS(Namespaces.OWS_2, "Value");
			value.setTextContent("application/x-www-form-urlencoded");
			constraintAllowedValues.appendChild(value);
		}
		
		return post;
	}
	
	/**
	 * Return an HTTP response to the client with valid headers and a body containing the Capabilities 
	 * XML document. If `completeCapabilities` is true, then a complete capabilities document with a 
	 * Content section (Layers) will be generated, and the embedded links will use the "/full" URL. If 
	 * false, then a partial capabilities document will be generated without a Content section (Contents), 
	 * and clients must use authentication to request the complete capabilities.
	 * 
	 * Source: OGC 14-065r2, Annex B.4.2; OGC 06-121r9 Section 7
	 * @param request Source request from client, used to build absolute URLs for HREFs
	 * @param response Response to build to send back to client
	 * @param completeCapabilities If true, build a complete capabilities document
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response, boolean completeCapabilities) throws TransformerException {
		response.setContentType("text/xml");
		response.setStatus(HttpServletResponse.SC_OK);
		
		boolean samlAuth = (this.options.getAuthentication().equals("saml2") && this.options.getIdpUrl() != null);
		
		// Extract scheme/host/port/path for HREFs
		String baseHref = getUri(request, false);
		String href;
		
		if (samlAuth) {
			href = baseHref + "/full";
		} else {
			href = baseHref;
		}
		
		PrintWriter printWriter = getWriterForResponse(response);
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		Document doc = domImplementation.createDocument(Namespaces.WPS_20, "wps:Capabilities", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("service", "WPS");
		rootElement.setAttribute("version", "2.0.0");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:ows", Namespaces.OWS_2);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:wps", Namespaces.WPS_20);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xlink", Namespaces.XLINK);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xml", Namespaces.XML);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.WPS_20 + " " + Schemas.WPS_20);

		// Service Identification Section
		Element serviceIdentification = doc.createElementNS(Namespaces.OWS_2, "ows:ServiceIdentification");
		rootElement.appendChild(serviceIdentification);
		
		Element title = doc.createElementNS(Namespaces.OWS_2, "ows:Title");
		title.setTextContent("ETS Security Client 1.0 WPS 2.0");
		serviceIdentification.appendChild(title);
		
		Element abstractElement = doc.createElementNS(Namespaces.OWS_2, "ows:Abstract");
		abstractElement.setTextContent("WPS 2.0 for validating secure client requests under ETS Security Client 1.0");
		serviceIdentification.appendChild(abstractElement);
		
		Element serviceType = doc.createElementNS(Namespaces.OWS_2, "ows:ServiceType");
		serviceType.setTextContent("WPS");
		serviceIdentification.appendChild(serviceType);
		
		Element serviceTypeVersion = doc.createElementNS(Namespaces.OWS_2, "ows:ServiceTypeVersion");
		serviceTypeVersion.setTextContent("2.0.0");
		serviceIdentification.appendChild(serviceTypeVersion);
		
		Element fees = doc.createElementNS(Namespaces.OWS_2, "ows:Fees");
		fees.setTextContent("NONE");
		serviceIdentification.appendChild(fees);
		
		Element accessConstraints = doc.createElementNS(Namespaces.OWS_2, "ows:AccessConstraints");
		accessConstraints.setTextContent("NONE");
		serviceIdentification.appendChild(accessConstraints);
		
		// Operations Metadata Section
		Element operationsMetadata = doc.createElementNS(Namespaces.OWS_2, "ows:OperationsMetadata");
		rootElement.appendChild(operationsMetadata);
		
		// GetCapabilities Operation Section
		Element getCapabilities = doc.createElementNS(Namespaces.OWS_2, "ows:Operation");
		getCapabilities.setAttribute("name", "GetCapabilities");
		operationsMetadata.appendChild(getCapabilities);
		
		Element getCapabilitiesDcp = doc.createElementNS(Namespaces.OWS_2, "ows:DCP");
		getCapabilities.appendChild(getCapabilitiesDcp);
		
		Element getCapabilitiesDcpHttp = doc.createElementNS(Namespaces.OWS_2, "ows:HTTP");
		getCapabilitiesDcp.appendChild(getCapabilitiesDcpHttp);
		
		String[] getCapabilitiesMethods = {"GET", "POST"};
		Element getCapabilitiesDcpHttpGet = this.buildGetElement(doc, href, getCapabilitiesMethods);		
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpGet);
		
		Element getCapabilitiesDcpHttpPost = this.buildPostElement(doc, href, getCapabilitiesMethods);
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpPost);
		
		// DescribeProcess Operation Section
		Element describeProcess = doc.createElementNS(Namespaces.OWS_2, "ows:Operation");
		describeProcess.setAttribute("name", "DescribeProcess");
		operationsMetadata.appendChild(describeProcess);
		
		Element describeProcessDcp = doc.createElementNS(Namespaces.OWS_2, "ows:DCP");
		describeProcess.appendChild(describeProcessDcp);
		
		Element describeProcessDcpHttp = doc.createElementNS(Namespaces.OWS_2, "ows:HTTP");
		describeProcessDcp.appendChild(describeProcessDcpHttp);
		
		String[] describeProcessMethods = {"GET", "POST"};
		Element describeProcessDcpHttpGet = this.buildGetElement(doc, href, describeProcessMethods);
		describeProcessDcpHttp.appendChild(describeProcessDcpHttpGet);
		
		Element describeProcessDcpHttpPost = this.buildPostElement(doc, href, describeProcessMethods);
		describeProcessDcpHttp.appendChild(describeProcessDcpHttpPost);
		
		if (completeCapabilities) {
			// Contents Section
			Element contents = doc.createElementNS(Namespaces.WPS_20, "wps:Contents");
			rootElement.appendChild(contents);
			
			// Sample ProcessSummary, needed to validate Contents element
			Element processSummary = doc.createElementNS(Namespaces.WPS_20, "wps:ProcessSummary");
			processSummary.setAttribute("jobControlOptions", "sync-execute dismiss");
			contents.appendChild(processSummary);
			
			Element processTitle = doc.createElementNS(Namespaces.OWS_2, "ows:Title");
			processTitle.setTextContent("False Process");
			processSummary.appendChild(processTitle);
			
			Element processIdentifier = doc.createElementNS(Namespaces.OWS_2, "ows:Identifier");
			processIdentifier.setTextContent(href + "/false-process");
			processSummary.appendChild(processIdentifier);
		}
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
	}
	
	/**
	 * Return an Exception Report for `reason`. Response will have content type 
	 * "text/xml" and HTTP status code defined by `code`.
	 * 
	 * Source: OGC 06-121r9 Section 8
	 * 
	 * @param reason String with reason for the Exception Report.
	 * @param code Integer for HTTP status code
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildException(String reason, int code, HttpServletResponse response) throws TransformerException {
		response.setContentType("text/xml");
		response.setStatus(code);
		
		PrintWriter printWriter = getWriterForResponse(response);
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		Document doc = domImplementation.createDocument(Namespaces.OWS_2, "ExceptionReport", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.0.0");
		rootElement.setAttributeNS(Namespaces.XML, "xml:lang", "en");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.OWS_2 + " " + Schemas.OWS_2_ER);
		
		Element exceptionElement = doc.createElementNS(Namespaces.OWS_2, "Exception");
		exceptionElement.setAttribute("exceptionCode", reason);
		rootElement.appendChild(exceptionElement);
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
	}
	
	/**
	 * Return an Exception Report for Invalid Parameters. Response will have content type 
	 * "text/xml" and HTTP status code 400.
	 * 
	 * Source: OGC 06-121r9 Section 8
	 * 
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildInvalidParameterException(HttpServletResponse response) throws TransformerException {
		buildException("InvalidParameterValue", HttpServletResponse.SC_BAD_REQUEST, response);
	}
	
	/**
	 * Return an Exception Report for `reason`. Response will have content type 
	 * "text/xml" and HTTP status code 501.
	 * 
	 * Source: OGC 06-121r9 Section 8
	 * 
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildNotSupportedException(HttpServletResponse response) throws TransformerException {
		buildException("OperationNotSupported", HttpServletResponse.SC_NOT_IMPLEMENTED, response);
	}
}
