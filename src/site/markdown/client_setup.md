# Secure Client Setup Instructions

This document will cover how you should configure your secure client to
connect to the test harness.

For all usages (IDE, JAR, TEAM Engine), a test endpoint URL of the
following format should be generated for your test run:

```
https://host:port/uniquenonce
```

The unique nonce is a random string of characters that should prevent
your test session endpoint from potentially conflicting with another
concurrent test session.

## cURL

[cURL][] is a command line client for URL protocols such as HTTP and HTTPS.
It can work as a very simple and barely capable secure test client, as it
can make HTTP and HTTPS connections to download a capabilities document.
It cannot interpret that document or make subsequent automatic requests.

In the following examples I will use these command line parameters:

* `-i`
	- Includes the response headers from the Test Server in the output
* `-k`
	- Disables certificate verification, necessary to accept a 
	self-signed certificate

[cURL]: https://curl.haxx.se

### Making an HTTPS Request

Example with IDE Test Server emulating WMS 1.1.1:

```sh
$ curl -ik https://127.0.0.1:10080/6iwyg0phykqnt7mw
HTTP/1.1 404 Not Found
Date: Mon, 10 Sep 2018 05:19:31 GMT
Content-Type: application/vnd.ogc.se_xml;charset=iso-8859-1
Content-Length: 272
Server: Jetty(9.4.11.v20180605)

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ServiceExceptionReport
  SYSTEM "http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd">
<ServiceExceptionReport version="1.1.1">
  <ServiceException>Invalid query parameters</ServiceException>
</ServiceExceptionReport>
```

In the above case, cURL was able to connect to the server over HTTPS. As
no query parameters were used, the test server returns a Service 
Exception.

### Making an HTTP Request

Example with IDE Test Server emulating WMS 1.1.1:

```sh
$ curl -i http://127.0.0.1:10080/6iwyg0phykqnt7mw
HTTP/1.1 404 Not Found
Date: Mon, 10 Sep 2018 05:31:35 GMT
Content-Type: application/vnd.ogc.se_xml;charset=iso-8859-1
Content-Length: 272
Server: Jetty(9.4.11.v20180605)

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE ServiceExceptionReport
  SYSTEM "http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd">
<ServiceExceptionReport version="1.1.1">
   <ServiceException>Invalid query parameters</ServiceException>
</ServiceExceptionReport>
```

Here the "https" in the url has been replaced with "http", which makes
cURL open an HTTP connection without TLS. The server responds with a
Service Exception due to the missing parameters.

From a client perspective, this is identical to the HTTPS example.
However the test suite will recognize the connection does not use HTTPS
and can execute custom tests accordingly.

### Making an HTTPS Request with valid parameters

Example with IDE Test Server emulating WMS 1.1.1. Note that some URL
characters have been escaped for the Bash shell.

```sh
$ curl -ik https://127.0.0.1:10080/o1zd1z39oemgxpv8\?request\=GetCapabilities\&service\=WMS
HTTP/1.1 200 OK
Date: Mon, 10 Sep 2018 05:34:50 GMT
Content-Type: application/vnd.ogc.wms_xml;charset=iso-8859-1
Content-Length: 1986
Server: Jetty(9.4.11.v20180605)

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE WMT_MS_Capabilities
  SYSTEM "http://schemas.opengis.net/wms/1.1.1/capabilities_1_1_1.dtd">
<WMT_MS_Capabilities version="1.1.1">
   <Service>
      <Name>ets-security-client-10-wms-111</Name>
      <Title>ETS Security Client 1.0 WMS 1.1.1</Title>
      <Abstract>WMS 1.1.1 for validating secure client requests under ETS Security Client 1.0</Abstract>
      <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink"
                      xlink:href="https://127.0.0.1:10080/o1zd1z39oemgxpv8"
                      xlink:type="simple"/>
   </Service>
   <Capability>
      <Request>
         <GetCapabilities>
            <Format>application/vnd.ogc.wms_xml</Format>
            <DCPType>
               <HTTP>
                  <Get>
                     <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink"          
                                     xlink:href="https://127.0.0.1:10080/o1zd1z39oemgxpv8"
                                     xlink:type="simple"/>
                  </Get>
                  <Post>
                     <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink"
                                     xlink:href="https://127.0.0.1:10080/o1zd1z39oemgxpv8"
                                     xlink:type="simple"/>
                  </Post>
               </HTTP>
            </DCPType>
         </GetCapabilities>
         <GetMap>
            <Format>image/png</Format>
            <DCPType>
               <HTTP>
                  <Get>
                     <OnlineResource xmlns:xlink="http://www.w3.org/1999/xlink"          
                                     xlink:href="https://127.0.0.1:10080/o1zd1z39oemgxpv8"
                                     xlink:type="simple"/>
                  </Get>
               </HTTP>
            </DCPType>
         </GetMap>
      </Request>
      <Exception>
         <Format>application/vnd.ogc.se_xml</Format>
      </Exception>
   </Capability>
</WMT_MS_Capabilities>
```

As the query parameters are correct, a Capabilities document is
returned to the secure client.

## QGIS 3

[QGIS][] is a free and open source GIS application. It is available for 
multiple platforms, making it an accessible GIS tool with a common user
experience.

QGIS is a part of the [Open Source Geospatial Foundation][OSGeo].

In the following examples I will show how to add the ETS Test Server to
QGIS as a remote service.

[QGIS]: https://qgis.org/
[OSGeo]: https://www.osgeo.org

### WMS 1.1.1 and HTTP

With the ETS set up such that it emulates a WMS 1.1.1 service, start a
test session and it will generate a unique URL. Note that the Test Service
will wait 5 minutes (300 seconds) for a secure client connection, so you
do not need to be overly rushed to enter the details and start the test.

In QGIS, go to the "Layer" menu, "Add Layer", and select "Add WMS/WMTS 
Layer…". Click the "New" button to create a new connection, and fill in
the following details.

**Name**: CITE Security Client 1.0  
**URL**: \<URL for your test session, using `http` instead of `https`\>  
**Authentication Configurations**: No authentication  
**Referer**: <Leave Blank>  
**DPI-Mode**: all  

Leave the remaining checkboxes at their defaults, which should be "blank".
Click "OK" to save the connection.

To start the test session, ensure the "CITE Security Client 1.0" is 
selected in the drop-down select box, and click "Connect".

QGIS will try to retrieve the Capabilities document from the Test Server, 
triggering the ETS servlets and capturing the request. QGIS will display
no layers in the list for the Test Server, as none are advertised.

The ETS should now have finished the test session, and the results can
be viewed in the TestNG file or TEAM Engine.

For future test sessions, you can edit the "CITE Security Client 1.0"
connection that is saved in QGIS, and change the URL to your new test
session endpoint.

### WMS 1.1.1 and HTTPS

The instructions are identical to the HTTP-only instructions, except you 
will use `https` in the URL scheme instead of `http`.

When you click "Connect", a warning dialog will pop up about potential
certificate problems, especially if you are using a self-signed 
certificate. In this case you can ignore them and continue the test, as
it is your own certificate.
