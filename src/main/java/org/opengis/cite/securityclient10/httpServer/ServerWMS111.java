package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
public class ServerWMS111 extends EmulatedServer {
	
	private DocumentBuilderFactory documentFactory;
	private DocumentBuilder documentBuilder;
	private TransformerFactory transformerFactory;
	private Transformer transformer;
	
	/**
	 * Create an emulated WMS 1.1.1.
	 * 
	 * Currently hard-codes the output style for the XML string to have indented XML, and the XML 
	 * declaration.
	 */
	public ServerWMS111() {
		// Create factories and builders and re-use them
		this.documentFactory = DocumentBuilderFactory.newInstance();
		
		try {
			this.documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
		
		this.transformerFactory = TransformerFactory.newInstance();
		try {
			this.transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
		
		// Adjust defaults for XML document-to-String output
		this.transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		this.transformer.setOutputProperty(OutputKeys.METHOD, "xml");
	}
	
	/**
	 * Build a valid WMS 1.1.1 response for the client request, and automatically complete the response.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
	 * @param response
	 * @throws IOException 
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/vnd.ogc.wms_xml");
		response.setStatus(HttpServletResponse.SC_OK);
		
		// Extract scheme/host/port/path for HREFs
		String href = String.format("%s://%s:%d%s",
				request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				request.getRequestURI());
		
		PrintWriter printWriter = response.getWriter();
		Document body = this.documentBuilder.newDocument();
		
		Element rootElement = body.createElement("WMT_MS_Capabilities");
		rootElement.setAttribute("version", "1.1.1");
		body.appendChild(rootElement);
		
		// Service Section
		Element service = body.createElement("Service");
		rootElement.appendChild(service);
		
		Element name = body.createElement("Name");
		name.setTextContent("ets-security-client-10-wms-111");
		service.appendChild(name);
		
		Element title = body.createElement("Title");
		title.setTextContent("ETS Security Client 1.0 WMS 1.1.1");
		service.appendChild(title);
		
		Element abstractElement = body.createElement("Abstract");
		abstractElement.setTextContent("WMS 1.1.1 for validating secure client requests under ETS Security Client 1.0");
		service.appendChild(abstractElement);
		
		Element onlineResource = body.createElement("OnlineResource");
		onlineResource.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		onlineResource.setAttribute("xlink:type", "simple");
		onlineResource.setAttribute("xlink:href", href);
		service.appendChild(onlineResource);
		
		// Capability Section
		Element capability = body.createElement("Capability");
		rootElement.appendChild(capability);
		
		// Capability > Request
		Element requestElement = body.createElement("Request");
		capability.appendChild(requestElement);
		
		// Capability > Request > GetCapabilities
		Element getCapabilities = body.createElement("GetCapabilities");
		requestElement.appendChild(getCapabilities);
		
		Element getCapabilitiesFormat = body.createElement("Format");
		getCapabilitiesFormat.setTextContent("application/vnd.ogc.wms_xml");
		getCapabilities.appendChild(getCapabilitiesFormat);
		
		Element getCapabilitiesDCPType = body.createElement("DCPType");
		getCapabilities.appendChild(getCapabilitiesDCPType);
		
		Element getCapabilitiesDCPTypeHTTP = body.createElement("HTTP");
		getCapabilitiesDCPType.appendChild(getCapabilitiesDCPTypeHTTP);
		
		Element getCapabilitiesDCPTypeHTTPGet = body.createElement("Get");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPGet);
		
		Element getCapabilitiesDCPTypeHTTPGetOR = body.createElement("OnlineResource");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPGet.appendChild(getCapabilitiesDCPTypeHTTPGetOR);
		
		Element getCapabilitiesDCPTypeHTTPPost = body.createElement("Post");
		getCapabilitiesDCPTypeHTTP.appendChild(getCapabilitiesDCPTypeHTTPPost);
		
		Element getCapabilitiesDCPTypeHTTPPostOR = body.createElement("OnlineResource");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:type", "simple");
		getCapabilitiesDCPTypeHTTPPostOR.setAttribute("xlink:href", href);
		getCapabilitiesDCPTypeHTTPPost.appendChild(getCapabilitiesDCPTypeHTTPPostOR);
		
		// Capability > Request > GetMap
		Element getMap = body.createElement("GetMap");
		requestElement.appendChild(getMap);
		
		Element getMapFormat = body.createElement("Format");
		getMapFormat.setTextContent("image/png");
		getMap.appendChild(getMapFormat);
		
		Element getMapDCPType = body.createElement("DCPType");
		getMap.appendChild(getMapDCPType);
		
		Element getMapDCPTypeHTTP = body.createElement("HTTP");
		getMapDCPType.appendChild(getMapDCPTypeHTTP);
		
		Element getMapDCPTypeHTTPGet = body.createElement("Get");
		getMapDCPTypeHTTP.appendChild(getMapDCPTypeHTTPGet);
		
		Element getMapDCPTypeHTTPGetOR = body.createElement("OnlineResource");
		getMapDCPTypeHTTPGetOR.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:type", "simple");
		getMapDCPTypeHTTPGetOR.setAttribute("xlink:href", href);
		getMapDCPTypeHTTPGet.appendChild(getMapDCPTypeHTTPGetOR);
		
		// Capability > Exception
		Element exception = body.createElement("Exception");
		capability.appendChild(exception);
		
		Element exceptionFormat = body.createElement("Format");
		exceptionFormat.setTextContent("application/vnd.ogc.se_xml");
		exception.appendChild(exceptionFormat);
		
		// Add a doctype
		DOMImplementation domImplementation = body.getImplementation();
		DocumentType doctype = domImplementation.createDocumentType("doctype", 
				null,
				"http://schemas.opengis.net/wms/1.1.1/capabilities_1_1_1.dtd");
		
		printWriter.print(documentToString(body, doctype));
	}
	
	/**
	 * Return a Service Exception for `reason`. Response will have content type 
	 * "application/vnd.ogc.se_xml" and HTTP status code 404.
	 * 
	 * Source: Annex A.4
	 * 
	 * @param reason
	 * @param response
	 * @throws IOException
	 */
	public void buildException(String reason, HttpServletResponse response) throws IOException {
		response.setContentType("application/vnd.ogc.se_xml");
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		
		PrintWriter printWriter = response.getWriter();
		Document body = this.documentBuilder.newDocument();
		
		Element rootElement = body.createElement("ServiceExceptionReport");
		rootElement.setAttribute("version", "1.1.1");
		body.appendChild(rootElement);
		
		Element serviceException = body.createElement("ServiceException");
		serviceException.setTextContent(reason);
		rootElement.appendChild(serviceException);
		
		// Add a doctype
		DOMImplementation domImplementation = body.getImplementation();
		DocumentType doctype = domImplementation.createDocumentType("doctype", 
				null,
				"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd");
		
		printWriter.print(documentToString(body, doctype));
	}
	
	/**
	 * Use a Transformer to convert the XML Document to a String. Doctype (public and system) argument
	 * will be inserted into XML Document string.
	 * 
	 * @param document XML document to convert
	 * @param doctype XML document type that will be added to String representation
	 * @return String containing the XML document
	 */
	private String documentToString(Document document, DocumentType doctype) {
		StringWriter stringWriter = new StringWriter();
		
		// Add the DOCTYPE parameters, as long as they are not null
		if (doctype.getPublicId() != null) {
			this.transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
		}
		
		if (doctype.getSystemId() != null) {
			this.transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
		}
		
		try {
			this.transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		return stringWriter.toString();
	}
}
