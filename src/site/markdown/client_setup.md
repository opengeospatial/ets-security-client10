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

cURL is a command line client for URL protocols such as HTTP and HTTPS.
It can work as a very simple and barely capable secure test client, as it
can make HTTP and HTTPS connections to download a capabilities document.
It cannot interpret that document or make subsequent automatic requests.

In the following examples I will use these command line parameters:

* `-i`
	- Includes the response headers from the Test Server in the output
* `-k`
	- Disables certificate verification, necessary to accept a 
	self-signed certificate

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
