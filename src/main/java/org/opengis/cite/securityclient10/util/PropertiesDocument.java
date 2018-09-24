package org.opengis.cite.securityclient10.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to wrap a Document that is of the XML Properties schema.
 * Useful for reading/writing Documents without having to create
 * your own DocumentBuilders.
 *
 */
public class PropertiesDocument {
	
	private DocumentBuilderFactory documentBuilderFactory;
	private DocumentBuilder documentBuilder;
	
	private Document document;
	private Properties properties;
	
	/**
	 * Create blank PropertiesDocument with blank Properties object
	 * @throws ParserConfigurationException Exception if document builder could not be created
	 */
	public PropertiesDocument() throws ParserConfigurationException {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.properties = new Properties();
	}
	
	/**
	 * Create a Properties Document from an input XML file
	 * @param xmlFile File to open for parsing
	 * @throws SAXException Exception on parsing input as XML
	 * @throws TransformerFactoryConfigurationError Exception if TransformerFactory could not be created
	 * @throws TransformerException Exception if XML could not be transformed to input stream
	 * @throws TransformerConfigurationException Exception if transformer could not use input XML
	 * @throws IOException Exception if XML could not be read for properties file
	 * @throws ParserConfigurationException Exception if Document Builder could not be created
	 * @throws InvalidPropertiesFormatException Exception if XML is not in Properties format
	 */
	public PropertiesDocument(File xmlFile) throws SAXException, IOException, 
			TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError, 
			ParserConfigurationException {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		this.document = this.documentBuilder.parse(xmlFile);
		this.properties = documentToProperties(this.document);
	}

	/**
	 * Create a Properties Document using the input as the existing Document.
	 * @param testRunArgs Document to use as base
	 * @throws TransformerFactoryConfigurationError Exception if TransformerFactory could not be created
	 * @throws TransformerException Exception if XML could not be transformed to input stream
	 * @throws TransformerConfigurationException Exception if transformer could not use input XML
	 * @throws IOException Exception if XML could not be read for properties file
	 * @throws InvalidPropertiesFormatException Exception if XML is not in Properties format
	 * @throws ParserConfigurationException Exception if Document Builder could not be created
	 */
	public PropertiesDocument(Document testRunArgs) throws TransformerConfigurationException, 
			InvalidPropertiesFormatException, TransformerException, TransformerFactoryConfigurationError, 
			IOException, ParserConfigurationException {
		this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
        this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		this.document = this.documentBuilder.newDocument();
		this.properties = documentToProperties(testRunArgs);
	}
	
	/**
	 * Create a Document corresponding to the internal Properties object.
	 * Note: You must manually set the SYSTEM DocType to "http://java.sun.com/dtd/properties.dtd" when
	 * transforming this Document.
	 * 
	 * @return Document with Properties format
	 */
	@SuppressWarnings("unchecked")
	public Document getDocument() {
		Document doc = this.documentBuilder.newDocument();
		
		Element rootElement = doc.createElement("properties");
		doc.appendChild(rootElement);
		
		Enumeration<String> keys = (Enumeration<String>) this.properties.propertyNames();
		
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Element entry = doc.createElement("entry");
			entry.setAttribute("key", key);
			entry.setTextContent(this.getProperty(key));
			rootElement.appendChild(entry);
		}
		
		return doc;
	}
	
	/**
	 * Return the Properties object
	 * @return Properties object
	 */
	public Properties getProperties() {
		return this.properties;
	}
	
	/**
	 * Return a value for a key in the Properties object
	 * @param key String with key name
	 * @return String with value
	 */
	public String getProperty(String key) {
		return this.properties.getProperty(key);
	}
	
	/**
	 * Update a value in the Properties object
	 * @param key String with key name
	 * @param value String with value
	 */
	public void setProperty(String key, String value) {
		this.properties.setProperty(key, value);
	}
	
	/**
	 * Convert an input XML Document to a Properties object
	 * @param doc Document with Properties format
	 * @return Properties object
	 */
	private Properties documentToProperties(Document doc)  {
		Properties props = new Properties();
		
		// Iterate over Document and insert entries into Properties
		NodeList entries = doc.getDocumentElement().getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            props.setProperty(entry.getAttribute("key"), entry.getTextContent());
        }
		
		return props;
		
	}
}
