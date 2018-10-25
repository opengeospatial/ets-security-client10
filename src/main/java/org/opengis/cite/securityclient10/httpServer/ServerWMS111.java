package org.opengis.cite.securityclient10.httpServer;

import java.io.PrintWriter;
import java.util.Enumeration;

import org.opengis.cite.securityclient10.Namespaces;
import org.opengis.cite.securityclient10.Schemas;
import org.opengis.cite.securityclient10.util.XMLUtils;
import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;

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
	 * Create an emulated WMS 1.1.1.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @param options ServerOptions object with emulated server configuration
	 */
	public ServerWms111(ServerOptions options) {
		this.options = options;
	}
	
	/**
	 * Build a valid WMS 1.1.1 response for the client request, and automatically complete the response.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws TransformerException {
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
				buildCapabilities(request, response, true);
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
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
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
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildException(String reason, HttpServletResponse response) throws TransformerException {
		response.setContentType("application/vnd.ogc.se_xml");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		
		PrintWriter printWriter = getWriterForResponse(response);
		DOMImplementation domImplementation = this.documentBuilder.getDOMImplementation();
		DocumentType doctype = domImplementation.createDocumentType("doctype", null,
				Schemas.WMS_111_SE);
		Document doc = domImplementation.createDocument(null, "ServiceExceptionReport", doctype);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.1.1");
		
		Element serviceException = doc.createElement("ServiceException");
		serviceException.setTextContent(reason);
		rootElement.appendChild(serviceException);
		
		printWriter.print(XMLUtils.writeDocumentToString(doc, true));
	}
}
