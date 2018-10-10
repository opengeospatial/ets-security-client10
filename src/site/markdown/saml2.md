
# Testing with SAML2

For secure clients that want to test integration with SAML2 authentication, the test suite supports specifying a SAML2 metadata file URL that will use the SAML2 Browser SSO process. The testing process is as follows:

1. The Secure Client connects to the Test Suite server, and issues a GET request for the public capabilities
2. The Test Suite server responds with its basic capabilities document, that contains SAML2 metadata in its security annotations
3. The Secure Client issues a GET request for the secure capabilities document from the Test Suite server
4. The Test Suite server responds with a SAML Authentication redirect to the Identity Provider; the redirect URL contains additional parameters from the Test Suite server
5. The Secure Client loads the Identity Provider URL, and authenticates to the Identity Provider
6. The Identity Provider identifies the Secure Client test user, and returns a SAMLResponse XML Document
7. The Secure Client sends a POST request to the Test Suite server with the SAMLResponse XML Document
8. The Test Suite server validates the POST request, creates a security context for the test user, and redirects to the secure capabilities document
9. The Secure Client makes a GET request to the secure capabilties document with the security context (cookies) defined
10. The Test Suite server responds with the secure capabilities document, and ends the test session
11. The Test Suite then evaluates the requests from the Secure Client against the TestNG test methods

This requires 5 requests from the Secure Client:

1. GET request for Service Provider public capabilities
2. GET request for Service Provider secure capabilities
3. GET request for Identity Provider SSO
4. POST request to Service Provider SSO URL with SAML Response
5. GET request for Service Provider secure capabilities with cookies set

In this case, the "Service Provider" is the executable test suite's embedded test server.

The Test Server will generate 4 responses for the Secure Client:

1. Respond with public capabilities document containing security annotations
2. Respond with redirect to Identity Provider SSO with SAML Request query parameters
3. Respond with a cookie with security context, and redirect to the secure capabilities
4. Respond with the secure capabilities document, validating the cookie from the Secure Client

## Testing with curl

TODO: Write an example test where curl emulates a secure SAML2 client
