package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import org.opengis.cite.securityclient10.Namespaces;
import org.opengis.cite.securityclient10.Schemas;
import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
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
 * @author jpbadger
 *
 */
public class ServerWms111 extends EmulatedServer {
	
	/**
	 * Create an emulated WMS 1.1.1.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @throws ParserConfigurationException Exception if new document builder could not be created
	 * @throws TransformerConfigurationException Exception if new transformer could not be created
	 */
	public ServerWms111() throws ParserConfigurationException, TransformerConfigurationException {}
	
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
		
		if (serviceValue == null || requestValue == null || !serviceValue.equals("WMS")) {
			buildException("Invalid query parameters", response);
		} else {
			
			// Handle potential other request types
			switch (requestValue) {
				case "GetCapabilities":
				case "capabilities":
					// Return a GetCapabilities document
					buildCapabilities(request, response);
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
	 * Return an HTTP response to the client with valid headers and a body containing the  Capabilities 
	 * XML document.
	 * 
	 *  Source: Annex A
	 * @param request Source request from client, used to build absolute URLs for HREFs
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		response.setContentType("application/vnd.ogc.wms_xml");
		response.setStatus(HttpServletResponse.SC_OK);
		
		// Extract scheme/host/port/path for HREFs
		String href = String.format("%s://%s:%d%s",
				request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				request.getRequestURI());
		
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
		
		printWriter.print(documentToString(doc));
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
}
