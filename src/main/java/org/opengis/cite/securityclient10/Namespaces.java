package org.opengis.cite.securityclient10;

import java.net.URI;

/**
 * XML namespace names.
 * 
 * @see <a href="http://www.w3.org/TR/xml-names/">Namespaces in XML 1.0</a>
 *
 */
public class Namespaces {

    private Namespaces() {
    }

    /** ISO 19136 (GML 3.2) */
    public static final String GML = "http://www.opengis.net/gml/3.2";
    /** OpenGIS OGC */
    public static final String OGC = "http://www.opengis.net/ogc";
    /** OGC 06-121r3 (OWS 1.1) */
    public static final String OWS = "http://www.opengis.net/ows/1.1";
    /** Schematron (ISO 19757-3) namespace */
    public static final URI SCH = URI
            .create("http://purl.oclc.org/dsdl/schematron");
    /** SOAP 1.2 message envelopes. */
    public static final String SOAP_ENV = "http://www.w3.org/2003/05/soap-envelope";
    /** OpenGIS WMS */
    public static final String WMS = "http://www.opengis.net/wms";
    /** WMS 1.3.0 */
    public static final String WMS_130 = "http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd";
    /** WMS 1.3.0 Service Exception */
    public static final String WMS_EXC_130 = "http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd";
    /** W3C XLink */
    public static final String XLINK = "http://www.w3.org/1999/xlink";
    /** W3C XMLNS */
    public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
    /** W3C XML Schema */
    public static final String XSD = "http://www.w3.org/2001/XMLSchema";
    /** W3C XSI */
    public static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    
}
