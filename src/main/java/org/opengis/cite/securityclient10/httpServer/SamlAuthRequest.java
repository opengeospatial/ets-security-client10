package org.opengis.cite.securityclient10.httpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.zip.DeflaterOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.opengis.cite.securityclient10.util.XMLUtils;
import org.sonatype.plexus.components.cipher.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SamlAuthRequest {
	private DocumentBuilderFactory documentFactory;
	private DocumentBuilder documentBuilder;
	private TransformerFactory transformerFactory;
	private Transformer transformer;
	
	private Document doc;
	/**
	 * String referring to the Service Provider SAML callback URL.
	 * The Identity Provider will use this to redirect the User Agent back
	 * to the Service Provider.
	 */
	private String href;

	/**
	 * Initialize a SAML 2.0 Authentication Request object with a callback URL.
	 * @param href The callback URL to embed in the document
	 */
	public SamlAuthRequest(String href) {
		this.href = href;
		
		// Create factories and builders and re-use them
		try {
			this.documentFactory = DocumentBuilderFactory.newInstance();
			this.documentFactory.setNamespaceAware(true);
			this.documentBuilder = documentFactory.newDocumentBuilder();
			
			this.transformerFactory = TransformerFactory.newInstance();
			this.transformer = transformerFactory.newTransformer();
		} catch (ParserConfigurationException | TransformerConfigurationException e) {
			// Exception with default configuration
			e.printStackTrace();
		}
		
		// Adjust defaults for XML document-to-String output
		this.transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		this.transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		this.transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		
		this.buildDocument();
	}
	
	/**
	 * Convert the SAML 2.0 Authentication Response document to a String, compress using ZLib deflate,
	 * encode as base64, then use URL encoding to prepare for usage as a query parameter.
	 * @return URL-safe String
	 * @throws TransformerException Document could not be transformed to a String
	 */
	public String toUrlParameterString() throws TransformerException {
		String plain = XMLUtils.writeDocumentToString(this.doc, false);
		return this.deflateAndBase64String(plain);
	}
	
	private void buildDocument() {
		this.doc = this.documentBuilder.newDocument();
		
		Element rootElement = this.doc.createElement("samlp:AuthnRequest");
		rootElement.setAttribute("xmlns:samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
		rootElement.setAttribute("xmlns:saml", "urn:oasis:names:tc:SAML:2.0:assertion");
		rootElement.setAttribute("ID", "identifier_1");
		rootElement.setAttribute("Version", "2.0");
		ZonedDateTime date = ZonedDateTime.now(ZoneId.of("UTC"));
		rootElement.setAttribute("IssueInstant", date.toString());
		rootElement.setAttribute("AssertionConsumerServiceURL", href + "/saml2");
		this.doc.appendChild(rootElement);
		
		Element issuer = this.doc.createElement("saml:Issuer");
		issuer.setTextContent(href);
		rootElement.appendChild(issuer);
		
		Element nameIdPolicy = this.doc.createElement("samlp:NameIDPolicy");
		nameIdPolicy.setAttribute("AllowCreate", "true");
		nameIdPolicy.setAttribute("Format", "urn:oasis:names:tc:SAML:2.0:nameid-format:transient");
		rootElement.appendChild(nameIdPolicy);
	}
	
	/**
	 * Deflate (compress) the input String then encode it in base64, then URL encode
	 * @param input String to compress and encode
	 * @return URL Encoded and Base64 encoded version of the deflated String
	 */
	private String deflateAndBase64String(String input) {
		// Convert to bytes
		byte[] inputBytes = null;
		try {
			inputBytes = input.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// If UTF-8 is unsupported, there are problems
			e.printStackTrace();
		}
		
		// Compress
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		DeflaterOutputStream outputStream = new DeflaterOutputStream(output);
		String base64String = null;
		try {
			outputStream.write(inputBytes, 0, inputBytes.length);
			outputStream.close();
			byte[] encodedBytes = Base64.encodeBase64(output.toByteArray());
			base64String = new String(encodedBytes);
		} catch (IOException e) {
			// When the deflate output stream does not accept a write, or close
			e.printStackTrace();
		}
		
		// URL Encode
		String outputString = null;
		try {
			outputString = URLEncoder.encode(base64String, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// When the string cannot be encoded to UTF-8
			e.printStackTrace();
		}
		
		return outputString;
	}
}
