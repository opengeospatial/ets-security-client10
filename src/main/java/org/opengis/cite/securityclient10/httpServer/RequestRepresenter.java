package org.opengis.cite.securityclient10.httpServer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
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
	
	public RequestRepresenter() {
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
		
		this.createDocument();
	}
	
	/**
	 * Initialize the barebones document
	 */
	private void createDocument() {
		this.requestsDocument = this.documentBuilder.newDocument();
		
		Element rootElement = this.requestsDocument.createElement("HttpRequestSet");
		this.requestsDocument.appendChild(rootElement);
	}
	
	public void serializeRequest(HttpServletRequest request) {
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
		try {
			bodyContent = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		body.setTextContent(bodyContent);
		requestElement.appendChild(body);
	}
	
	public void saveToPath(Path path) {		
		// Convert to String
		StringWriter stringWriter = new StringWriter();
		
		try {
			this.transformer.transform(new DOMSource(this.requestsDocument), new StreamResult(stringWriter));
		} catch (TransformerException e) {
			throw new RuntimeException(e);
		}
		
		String xmlDoc = stringWriter.toString();
		
		System.out.println("Writing to file: " + path.toString());
		
		// Open file and write string
		try {
			PrintWriter outputFile = new PrintWriter(path.toAbsolutePath().toString());
			outputFile.print(xmlDoc);
			outputFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
