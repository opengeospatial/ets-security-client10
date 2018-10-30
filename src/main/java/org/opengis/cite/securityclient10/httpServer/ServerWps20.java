package org.opengis.cite.securityclient10.httpServer;

import java.io.PrintWriter;
import java.util.Enumeration;

import javax.xml.transform.TransformerException;

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
		
		Element getCapabilitiesDcpHttpGet = doc.createElementNS(Namespaces.OWS_2, "ows:Get");
		getCapabilitiesDcpHttpGet.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpGet);
		
		// ows:Constraint for SAML2
		if (samlAuth) {
			addSamlConstraintToElement(doc, getCapabilitiesDcpHttpGet);
		}
		
		// ows:Constraint for HTTP Methods
		if (this.options.getHttpMethods()) {
			String[] methods = {"GET", "POST"};
			addHttpMethodsConstraintToElement(doc, getCapabilitiesDcpHttpGet, methods);
		}
		
		Element getCapabilitiesDcpHttpPost = doc.createElementNS(Namespaces.OWS_2, "ows:Post");
		getCapabilitiesDcpHttpPost.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		getCapabilitiesDcpHttp.appendChild(getCapabilitiesDcpHttpPost);
		
		// ows:Constraint for SAML2
		if (samlAuth) {
			addSamlConstraintToElement(doc, getCapabilitiesDcpHttpPost);
		}
		
		// ows:Constraint for HTTP Methods
		if (this.options.getHttpMethods()) {
			String[] methods = {"GET", "POST"};
			addHttpMethodsConstraintToElement(doc, getCapabilitiesDcpHttpPost, methods);
		}
		
		// DescribeProcess Operation Section
		Element describeProcess = doc.createElementNS(Namespaces.OWS_2, "ows:Operation");
		describeProcess.setAttribute("name", "DescribeProcess");
		operationsMetadata.appendChild(describeProcess);
		
		Element describeProcessDcp = doc.createElementNS(Namespaces.OWS_2, "ows:DCP");
		describeProcess.appendChild(describeProcessDcp);
		
		Element describeProcessDcpHttp = doc.createElementNS(Namespaces.OWS_2, "ows:HTTP");
		describeProcessDcp.appendChild(describeProcessDcpHttp);
		
		Element describeProcessDcpHttpGet = doc.createElementNS(Namespaces.OWS_2, "ows:Get");
		describeProcessDcpHttpGet.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		describeProcessDcpHttp.appendChild(describeProcessDcpHttpGet);
		
		// ows:Constraint for SAML2
		if (samlAuth) {
			addSamlConstraintToElement(doc, describeProcessDcpHttpGet);
		}
		
		// ows:Constraint for HTTP Methods
		if (this.options.getHttpMethods()) {
			String[] methods = {"GET", "POST"};
			addHttpMethodsConstraintToElement(doc, describeProcessDcpHttpGet, methods);
		}
		
		Element describeProcessDcpHttpPost = doc.createElementNS(Namespaces.OWS_2, "ows:Post");
		describeProcessDcpHttpPost.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		describeProcessDcpHttp.appendChild(describeProcessDcpHttpPost);
		
		// ows:Constraint for SAML2
		if (samlAuth) {
			addSamlConstraintToElement(doc, describeProcessDcpHttpPost);
		}
		
		// ows:Constraint for HTTP Methods
		if (this.options.getHttpMethods()) {
			String[] methods = {"GET", "POST"};
			addHttpMethodsConstraintToElement(doc, describeProcessDcpHttpPost, methods);
		}
		
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
	 * Add an OWS Constraint element as a child to Element `element` in Document `doc`.
	 * This Constraint is for HTTP Methods.
	 * 
	 * @param doc The parent document, used to create namespaced elements
	 * @param element The parent element
	 * @param methods A String[] containing the methods to add
	 */
	private void addHttpMethodsConstraintToElement(Document doc, Element element, String[] methods) {
		Element constraintHttpMethods = doc.createElement("ows:Constraint");
		constraintHttpMethods.setAttribute("name", "urn:ogc:def:security:1.0:rc:http-methods");
		element.appendChild(constraintHttpMethods);
		
		Element allowedValues = doc.createElement("ows:AllowedValues");
		constraintHttpMethods.appendChild(allowedValues);
		
		for (int i = 0; i < methods.length; i++) {
			String method = methods[i];
			
			Element value = doc.createElement("ows:Value");
			value.setTextContent(method);
			allowedValues.appendChild(value);
		}
	}
	
	/**
	 * Add an OWS Constraint element as a child to Element `element` in Document `doc`.
	 * This Constraint is for SAML 2 authentication.
	 * 
	 * @param doc The parent document, used to create namespaced elements
	 * @param element The parent element
	 */
	private void addSamlConstraintToElement(Document doc, Element element) {
		Element constraint = doc.createElementNS(Namespaces.OWS_2, "Constraint");
		constraint.setAttribute("name", "urn:ogc:def:security:1.0:rc:authentication:saml2");
		element.appendChild(constraint);
		
		Element constraintValues = doc.createElementNS(Namespaces.OWS_2, "ValuesReference");
		constraintValues.setAttributeNS(Namespaces.OWS_2, "ows:reference", this.options.getIdpUrl());
		constraint.appendChild(constraintValues);
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
