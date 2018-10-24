package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.StringWriter;

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

import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public class EmulatedServer {

	protected DocumentBuilderFactory documentFactory;
	protected DocumentBuilder documentBuilder;
	protected TransformerFactory transformerFactory;
	protected Transformer transformer;
	
	/**
	 * Test Run Properties for this emulated server
	 */
	protected ServerOptions options;
	
	/**
	 * RelayState token for SAML2
	 */
	protected String relayState;
	
	public EmulatedServer() throws ParserConfigurationException, TransformerConfigurationException {
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
	 * Use a Transformer to convert the XML Document to a String.
	 * 
	 * @param document XML document to convert
	 * @return String containing the XML document
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	protected String documentToString(Document document) throws TransformerException {
		StringWriter stringWriter = new StringWriter();
		
		// Add the DOCTYPE parameters, as long as they are not null
		DocumentType docType = document.getDoctype();
		if (docType != null) {
			if (docType.getPublicId() != null) {
				this.transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, docType.getPublicId());
			}
			
			if (docType.getSystemId() != null) {
				this.transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
			}
		}
		
		this.transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		return stringWriter.toString();
	}
	
	/**
	 * Enable Cross Origin Resource Sharing (CORS). This allows web-based
	 * clients to connect to this test server from their web browser
	 * security environment.
	 * @param response Reponse on which to enable CORS headers
	 */
	protected void enableCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
	}
	
	/**
	 * Subclasses must override this
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		// TODO Auto-generated method stub
	}

}
