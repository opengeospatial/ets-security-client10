
# Testing with SAML2

For secure clients that want to test integration with SAML2 authentication, the test suite supports specifying a SAML2 metadata file URL that will use the SAML2 Browser SSO process. Authentication for the Identity Provider is assumed to be HTTP Basic Authentication; other authentication methods (such as X.509, HTTP form, Kerberos, etc) are beyond the scope of this guide.

The testing process is as follows:

1. The Secure Client connects to the Test Suite server, and issues a GET request for the public capabilities
2. The Test Suite server responds with its basic capabilities document, that contains SAML2 metadata in its security annotations
3. The Secure Client issues a GET request for the secure capabilities document from the Test Suite server
4. The Test Suite server responds with a SAML Authentication redirect to the Identity Provider; the redirect URL contains additional parameters from the Test Suite server
5. The Secure Client loads the Identity Provider URL
6. The Identity Provider requires HTTP Basic authentication from the Secure Client, and responds with a `401 Unauthorized` response with the `WWW-Authenticate` header set to require HTTP Basic.
7. The Secure Client re-issues the GET request to the Identity Provider with the `Authorization` header set with valid HTTP Basic crendentials.
8. The Identity Provider identifies the Secure Client test user, and returns a SAMLResponse XML Document
9. The Secure Client sends a POST request to the Test Suite server with the SAMLResponse XML Document
10. The Test Suite server validates the POST request, creates a security context for the test user, and redirects to the secure capabilities document
11. The Secure Client makes a GET request to the secure capabilties document with the security context (cookies) defined
12. The Test Suite server responds with the secure capabilities document, and ends the test session
13. The Test Suite then evaluates the requests from the Secure Client against the TestNG test methods

This requires 6 requests from the Secure Client:

1. GET request for Service Provider public capabilities
2. GET request for Service Provider secure capabilities
3. GET request for Identity Provider SSO
4. GET request for Identity Provider SSO with HTTP Basic credentials
5. POST request to Service Provider SSO URL with SAML Response
6. GET request for Service Provider secure capabilities with cookies set

In this case, the "Service Provider" is the executable test suite's embedded test server.

The Test Server will generate 4 responses for the Secure Client:

1. Respond with public capabilities document containing security annotations
2. Respond with redirect to Identity Provider SSO with SAML Request query parameters
3. Respond with a cookie with security context, and redirect to the secure capabilities
4. Respond with the secure capabilities document, validating the cookie from the Secure Client

## Detailed Request Procedure (WMS 1.1.1)

#### Secure Client: Issue GET request for Capabilities

```
GET /aabbccddee HTTP/1.1
Accept: */*
Host: localhost:10080
```

#### Test Suite: Respond with basic Capabilities document

Capabilities document will have SAML2 constraint in the Vendor Specific Capabilities.

```xml
HTTP/1.1 200 OK
Content-Type: application/vnd.ogc.wms_xml

<?xml version="1.0" encoding="UTF-8"?>
…
<VendorSpecificCapabilities>
  <ows_security:ExtendedSecurityCapabilities xmlns:ows_security="http://www.opengis.net/security/1.0">
    <ows:OperationsMetadata xmlns:ows="http://www.opengis.net/ows/1.1">
      <ows:Operation name="GetCapabilities">
        <ows:DCP>
          <ows:HTTP>
            <ows:Get xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="https://localhost:10080/aabbccddee/full">
              <ows:Constraint name="urn:ogc:def:security:1.0:rc:authentication:saml2">
                  <ows:ValuesReference ows:reference="https://idp.example.org/saml/sso"/>
              </ows:Constraint>
            </ows:Get>
            <ows:Post xmlns:xlink="http://www.w3.org/1999/xlink" xlink:type="simple" xlink:href="https://localhost:10080/aabbccddee/full">
              <ows:Constraint name="urn:ogc:def:security:1.0:rc:authentication:saml2">
                  <ows:ValuesReference ows:reference="https://idp.example.org/saml/sso"/>
              </ows:Constraint>
            </ows:Post>
          </ows:HTTP>
        </ows:DCP>
      </ows:Operation>
    </ows:OperationsMetadata>
  </ows_security:ExtendedSecurityCapabilities>
</VendorSpecificCapabilities>
```

#### Secure Client: Issue GET request for Secure Capabilities

This is the URL defined in the `<ows:Get>` element.

```
GET /aabbccddee/full HTTP/1.1
Accept: */*
Host: localhost:10080
```

#### Test Suite: Respond with Redirect to Identity Provider SSO

As the request for the full capabilities does not include a security context (e.g. valid cookies), the test suite will redirect the test client to the IdP. In the `Location` header, the `SAMLRequest` query parameter will have the [SAML element](https://en.wikipedia.org/wiki/SAML_2.0#SP_Redirect_Request;_IdP_POST_Response), and the `RelayState` query parameter will have a unique token for this test session. (Note that a real Service Provider would have its own secure method of generating and maintaining the RelayState token.)

```
HTTP/1.1 302 Found
Location: https://idp.example.org/saml/sso/redirect?SAMLRequest=<DATA>&RelayState=<TOKEN>
```

#### Secure Client: Issue GET request for SSO URL

The Secure Client will load the URL from the test suite, connecting to the Identity Provider.

```
GET /saml/sso/redirect?SAMLRequest=<DATA>&RelayState=<TOKEN> HTTP/1.1
Accept: */*
Host: idp.example.org
```

#### Identity Provider: Respond with Authorization request

The Identity Provider, configured for HTTP Basic, requests that type of authentication from the Secure Client by using the `WWW-Authenticate` header.

```
HTTP/1.1 401 Unauthorized
WWW-Authenticate: Basic realm="SAML 2 Identity Provider"
```

#### Secure Client: Issue GET request for SSO URL with credentials

The Secure Client sends username and password to the Identity Provider using the `Authorization` header.

```
GET /saml/sso/redirect?SAMLRequest=<DATA>&RelayState=<TOKEN> HTTP/1.1
Accept: */*
Host: idp.example.org
Authorization: Basic <base64 encoded credentials>
```

#### Identity Provider: Respond with SAML Authentication Response Document

The Identity Provider validates the credentials from the Secure Client, and responds with the SAML Authentication Response document.

```xml
HTTP/1.1 200 OK
Content-Type: text/xml

<samlp:Response
    xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
    ID="identifier_2"
    InResponseTo="identifier_1"
    Version="2.0"
    IssueInstant="2018-10-16T09:00:00Z"
    Destination="https://localhost:10080/aabbccddee/saml2">
    <saml:Issuer>https://idp.example.org/SAML2</saml:Issuer>
    <samlp:Status>
      <samlp:StatusCode
        Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
    </samlp:Status>
    <saml:Assertion
      xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
      ID="identifier_3"
      Version="2.0"
      IssueInstant="2018-10-16T09:00:00Z">
      <saml:Issuer>https://idp.example.org/SAML2</saml:Issuer>
      <!-- a POSTed assertion MUST be signed -->
      <ds:Signature
        xmlns:ds="http://www.w3.org/2000/09/xmldsig#">...</ds:Signature>
      <saml:Subject>
        <saml:NameID
          Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">
          3f7b3dcf-1674-4ecd-92c8-1544f346baf8
        </saml:NameID>
        <saml:SubjectConfirmation
          Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          <saml:SubjectConfirmationData
            InResponseTo="identifier_1"
            Recipient="https://localhost:10080/aabbccddee/saml2"
            NotOnOrAfter="2018-10-16T09:10:00Z"/>
        </saml:SubjectConfirmation>
      </saml:Subject>
      <saml:Conditions
        NotBefore="2018-10-16T09:00:00Z"
        NotOnOrAfter="2018-10-16T09:10:00Z">
        <saml:AudienceRestriction>
          <saml:Audience>https://localhost:10080/aabbccddee/saml2</saml:Audience>
        </saml:AudienceRestriction>
      </saml:Conditions>
      <saml:AuthnStatement
        AuthnInstant="2018-10-16T09:00:00Z"
        SessionIndex="identifier_3">
        <saml:AuthnContext>
          <saml:AuthnContextClassRef>
            urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
         </saml:AuthnContextClassRef>
        </saml:AuthnContext>
      </saml:AuthnStatement>
    </saml:Assertion>
  </samlp:Response>
```

#### Secure Client: Issue POST request to SAML Callback URL

The Authentication Response is received by the Secure Client and base64 encoded then deflated and used as the `RESPONSE` parameter as a parameter to the Test Suite.

```
POST /aabbccddee/saml2 HTTP/1.1
Accept: */*
Host: localhost:10080
Content-Type: application/x-www-form-urlencoded

SAMLResponse=<RESPONSE>&RelayState=<TOKEN>
```

#### Test Suite: Respond with Redirect to Secure Capabilities with a Security Context

A Service Provider would normally validate the SAML Callback, but the Test Suite will accept anything. The Test Suite will create a cookie for the Secure Client to use, and that cookie will represent the security context. The `Secure` property for the cookie is not used as the Secure Client may be testing an HTTP-only workflow. The security context will be destroyed at the end of the test.

(It may be possible to support alternatives to cookies, such as JSON Web Tokens or HTTP Auth.)

```
HTTP/1.1 302 Found
Set-Cookie: sessionToken=asdf11; Expires=<EXPIRATION> httpOnly
Location: https://localhost:10080/aabbccddee/full?request=GetCapabilities&service=WMS
```

#### Secure Client: Issue GET request for Secure Capabilities with a Security Context

Now that the Secure Client has a cookie, it can be used to request the Secure Capabilities document.

```
GET /aabbccddee/full HTTP/1.1
Accept: */*
Cookie: sessionToken=asdf11
Host: localhost:10080
```

#### Test Suite: Respond with Secure Capabilities document

This document will be the same as the basic capabilities document. In a true WMS, this secure capabilities document would contain the actual WMS contents such as the layers that the Secure Client could now request.

```xml
HTTP/1.1 200 OK
Content-Type: application/vnd.ogc.wms_xml

<?xml version="1.0" encoding="UTF-8"?>
…
```

This is the final request in the workflow, so the Test Suite shuts down the embedded server and runs the TestNG test methods to validate the Secure Client behavior.

## Testing with curl

TODO: Write an example test where curl emulates a secure SAML2 client
