/**
 * 
 */
package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
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
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
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
		
		if (serviceValue == null || requestValue == null || !serviceValue.equals("WPS")) {
			buildInvalidParameterException(response);
		} else {
			
			// Handle potential other request types
			switch (requestValue) {
				case "GetCapabilities":
					// Return a GetCapabilities document
					buildCapabilities(request, response);
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
	 * XML document.
	 * 
	 * Source: OGC 14-065r2, Annex B.4.2; OGC 06-121r9 Section 7
	 * @param request Source request from client, used to build absolute URLs for HREFs
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildCapabilities(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		response.setContentType("text/xml");
		response.setStatus(HttpServletResponse.SC_OK);
		
		// Extract scheme/host/port/path for HREFs
		String href = String.format("%s://%s:%d%s",
				request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				request.getRequestURI());
		
		PrintWriter printWriter = response.getWriter();
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
		
		Element getCapabilitiesDCP = doc.createElementNS(Namespaces.OWS_2, "ows:DCP");
		getCapabilities.appendChild(getCapabilitiesDCP);
		
		Element getCapabilitiesDCPHTTP = doc.createElementNS(Namespaces.OWS_2, "ows:HTTP");
		getCapabilitiesDCP.appendChild(getCapabilitiesDCPHTTP);
		
		Element getCapabilitiesDCPHTTPGet = doc.createElementNS(Namespaces.OWS_2, "ows:Get");
		getCapabilitiesDCPHTTPGet.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		getCapabilitiesDCPHTTP.appendChild(getCapabilitiesDCPHTTPGet);
		
		// DescribeProcess Operation Section
		Element describeProcess = doc.createElementNS(Namespaces.OWS_2, "ows:Operation");
		describeProcess.setAttribute("name", "DescribeProcess");
		operationsMetadata.appendChild(describeProcess);
		
		Element describeProcessDCP = doc.createElementNS(Namespaces.OWS_2, "ows:DCP");
		describeProcess.appendChild(describeProcessDCP);
		
		Element describeProcessDCPHTTP = doc.createElementNS(Namespaces.OWS_2, "ows:HTTP");
		describeProcessDCP.appendChild(describeProcessDCPHTTP);
		
		Element describeProcessDCPHTTPGet = doc.createElementNS(Namespaces.OWS_2, "ows:Get");
		describeProcessDCPHTTPGet.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		describeProcessDCPHTTP.appendChild(describeProcessDCPHTTPGet);
		
		Element describeProcessDCPHTTPPost = doc.createElementNS(Namespaces.OWS_2, "ows:Post");
		describeProcessDCPHTTPPost.setAttributeNS(Namespaces.XLINK, "xlink:href", href);
		describeProcessDCPHTTP.appendChild(describeProcessDCPHTTPPost);
		
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
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildException(String reason, int code, HttpServletResponse response) throws IOException, TransformerException {
		response.setContentType("text/xml");
		response.setStatus(code);
		
		PrintWriter printWriter = response.getWriter();
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
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildInvalidParameterException(HttpServletResponse response) throws IOException, TransformerException {
		buildException("InvalidParameterValue", HttpServletResponse.SC_BAD_REQUEST, response);
	}
	
	/**
	 * Return an Exception Report for `reason`. Response will have content type 
	 * "text/xml" and HTTP status code 501.
	 * 
	 * Source: OGC 06-121r9 Section 8
	 * 
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void buildNotSupportedException(HttpServletResponse response) throws IOException, TransformerException {
		buildException("OperationNotSupported", HttpServletResponse.SC_NOT_IMPLEMENTED, response);
	}
}
