/**
 * 
 */
package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.opengis.cite.securityclient10.Namespaces;
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
	
	private DocumentBuilderFactory documentFactory;
	private DocumentBuilder documentBuilder;
	private TransformerFactory transformerFactory;
	private Transformer transformer;

	/**
	 * Create an emulated WMS 1.3.0.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 * @throws ParserConfigurationException Exception if new document builder could not be created
	 * @throws TransformerConfigurationException Exception if new transformer could not be created
	 */
	public ServerWms13() throws ParserConfigurationException, TransformerConfigurationException {
		// Create factories and builders and re-use them
		this.documentFactory = DocumentBuilderFactory.newInstance();
		this.documentFactory.setNamespaceAware(true);
		this.documentBuilder = documentFactory.newDocumentBuilder();
		
		this.transformerFactory = TransformerFactory.newInstance();
		this.transformer = transformerFactory.newTransformer();
		
		// Adjust defaults for XML document-to-String output
		this.transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		this.transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	}
	
	/**
	 * Build a valid WMS 1.3.0 response for the client request, and automatically complete the response.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		System.out.println("Building WMS 1.3.0 Response");
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
					// Return a GetCapabilities document
					buildCapabilities(request, response);
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
	 * XML document.
	 * 
	 * Source: Annex E.1, Annex H.1
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
		Document doc = domImplementation.createDocument(Namespaces.WMS, "WMS_Capabilities", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xlink", Namespaces.XLINK);
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.WMS + " " + Namespaces.WMS_13);

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
		
		printWriter.print(documentToString(doc));
	}
	
	/**
	 * Return a Service Exception for `reason`. Response will have content type 
	 * "application/vnd.ogc.se_xml" and HTTP status code 404.
	 * 
	 * Source: Annex E.2, Annex H.2
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
		Document doc = domImplementation.createDocument(Namespaces.OGC, "ServiceExceptionReport", null);
		
		Element rootElement = doc.getDocumentElement();
		rootElement.setAttribute("version", "1.3.0");
		rootElement.setAttributeNS(Namespaces.XMLNS, "xmlns:xsi", Namespaces.XSI);
		rootElement.setAttributeNS(Namespaces.XSI, "xsi:schemaLocation", Namespaces.OGC + " " + Namespaces.WMS_EXC_13);
		
		Element serviceException = doc.createElementNS(Namespaces.OGC, "ServiceException");
		serviceException.setTextContent(reason);
		rootElement.appendChild(serviceException);
		
		printWriter.print(documentToString(doc));
	}
}
