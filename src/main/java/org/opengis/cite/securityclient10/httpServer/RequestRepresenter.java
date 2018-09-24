package org.opengis.cite.securityclient10.httpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.stream.Collectors;

import org.opengis.cite.servlet.http.HttpServletRequest;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Proxy class to serialize incoming HttpServletRequests into an internal XML document. This allows
 * the request details to be stored after the HttpServletRequest has been closed/reset.
 * 
 */
public class RequestRepresenter {
	/**
	 * Serialized document
	 */
	private Document requestsDocument = null;
	
	private DocumentBuilderFactory documentFactory;
	private DocumentBuilder documentBuilder;
	private TransformerFactory transformerFactory;
	private Transformer transformer;
	
	/**
	 * Create a new RequestRepresenter
	 * @throws TransformerConfigurationException Exception if a new transformer could not be created
	 * @throws ParserConfigurationException Exception if new document builder could not be created
	 */
	public RequestRepresenter() throws TransformerConfigurationException, ParserConfigurationException {
		// Create factories and builders and re-use them
		this.documentFactory = DocumentBuilderFactory.newInstance();
		this.documentBuilder = documentFactory.newDocumentBuilder();
		
		this.transformerFactory = TransformerFactory.newInstance();
		this.transformer = transformerFactory.newTransformer();
		
		// Adjust defaults for XML document-to-String output
		this.transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		this.transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		
		this.createDocument();
	}
	
	/**
	 * Initialize the barebones document.
	 * The base element is {@code <HttpRequestSet> }.
	 */
	private void createDocument() {
		this.requestsDocument = this.documentBuilder.newDocument();
		
		Element rootElement = this.requestsDocument.createElement("HttpRequestSet");
		this.requestsDocument.appendChild(rootElement);
	}
	
	/**
	 * Add the metadata from the HttpServletRequest to the XML document.
	 * Each request will serialize to a {@code <Request> } element inside the root {@code <HttpRequestSet> }
	 * as so:
	 * 
	 * <pre>
	 * {@code
	 * <Request method="GET" https="true" queryString="?service=WMS&request=GetCapabilities" authentication="">
	 *   <Header name="Accepts">text/xml</Header>
	 *   <Header name="User-Agent">curl</Header>
	 *   <Body contentEncoding="utf-8" contentLength="0"></Body>
	 * </Request>
	 * }
	 * </pre>
	 * 
	 * There may be 0 or more Header elements. There is always a single Body element, which may have no
	 * text content. attributes on the Request element may be empty, but should still be specified.
	 * 
	 * @param request Request from client to serialize as XML
	 * @throws IOException Request Reader exception
	 */
	public void serializeRequest(HttpServletRequest request) throws IOException {
		Element rootElement = this.requestsDocument.getDocumentElement();
		
		Element requestElement = this.requestsDocument.createElement("Request");
		requestElement.setAttribute("method", request.getMethod());
		requestElement.setAttribute("https", request.isSecure() ? "true" : "false");
		requestElement.setAttribute("queryString", request.getQueryString());
		requestElement.setAttribute("authentication", request.getAuthType());
		rootElement.appendChild(requestElement);
		
		for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements();) {
        	String headerName = headers.nextElement();
        	String headerValue = request.getHeader(headerName);
        	
        	Element header = this.requestsDocument.createElement("Header");
        	header.setAttribute("name", headerName);
        	header.setTextContent(headerValue);
        	requestElement.appendChild(header);
        }
		
		Element body = this.requestsDocument.createElement("Body");
		body.setAttribute("contentEncoding", request.getCharacterEncoding());
		body.setAttribute("contentLength", String.valueOf(request.getContentLength()));
		String bodyContent = "";
		bodyContent = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		body.setTextContent(bodyContent);
		requestElement.appendChild(body);
	}
	
	/**
	 * Save requests to a document at a path
	 * @param path Filesystem path to save requests document
	 * @throws TransformerException Exception transforming requestsDocument to Stream
	 * @throws FileNotFoundException Exception if destination could not be opened for writing
	 */
	public void saveToPath(Path path) throws TransformerException, FileNotFoundException {		
		// Convert to String
		StringWriter stringWriter = new StringWriter();
		
		this.transformer.transform(new DOMSource(this.requestsDocument), new StreamResult(stringWriter));
		
		String xmlDoc = stringWriter.toString();
		
		System.out.println("Writing to file: " + path.toString());
		
		// Open file and write string
		PrintWriter outputFile = new PrintWriter(path.toAbsolutePath().toString());
		outputFile.print(xmlDoc);
		outputFile.close();
		
	}
}
