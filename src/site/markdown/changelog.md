
# Release Notes

## 0.5 (2018-11-05)

- Add notes about using IPv6 for addresses
- Reset test endpoint URL popup window when starting the test run
- Update developer information in pom.xml
- Add support for "HTTP Methods" annotation
- Switch to using ENUM definitions for hard-coded strings
- Add support for "W3C CORS" annotation
- Extract constraint builders to methods in EmulatedServer classes
- Add support for "HTTP Exception Handling" annotation
- Add support for "HTTP POST Content-Type" annotation
- Add link to latest version of standard (authentication required)

## 0.4.1 (2018-10-25)

- Fix case on a source class file causing compilation issues

## 0.4 (2018-10-25)

This release adds the SAML 2.0 Web Browser SSO Profile for testing.

- Add `ServerOptions` class for passing test run properties to the emulated server
- Support receiving more than one request on the emulated server before the test server is killed and results sent to TestNG tests
- Allow specification of a single authentication method (not supporting multiple at the same time)
- Add sample scripts in the `scripts` directory that can emulate secure clients
- Hard-code the number of expected requests for the SAML 2 workflow
- Add SAML 2 workflow support for WMS 1.1.1 emulated server
- Document how to use the SAML 2 workflow, including a sample test session
- Create webpage for testing secure client SAML 2 workflow, and abandon it because SAML 2 workflow has issues working in browser CORS sandbox which limit usability of AJAX
- Support catching `POST` and `OPTIONS` in addition to `GET` for `TestServer`
- Support requests to sub-paths under the randomly generated test session nonce path
- Add Ruby sample SAML 2 test client to `scripts` directory
- Standardize on using "Partial Capabilities" and "Complete Capabilities"
- Extract SAML 2 methods to `EmulatedServer`
- Simplify catching of some exceptions, cleaning up the code
- Add SAML 2 workflow support for WMS 1.3.0 emulated server
- Add SAML 2 workflow support for WPS 2.0 emulated server
- Adjust WMS 1.1.1 to use "WMS" as the Name in the capabilities document
- Improve TEAM Engine setup page documentation
- Update TEAM Engine interface to allow specifying the authentication type and the Identity Provider URL
- Re-add filtering of sensitive test run properties
- Open pop-up with test session URL in TEAM Engine
- Use `AssertionConsumerServiceURL` instead of `Issuer` for passing the SP callback URL to the IdP
- Use `SubjectCOnfirmationData/@Recipient` in Ruby SAML client for parsing SP callback URL
- Add name for TEAM Engine pop-up window to re-use it in subsequent tests

## 0.3 (2018-10-02)

This release adds support for other emulated server types, WMS 1.3.0 and
WPS 2.0.

- Add WMS 1.3.0 emulated server, responds to `GetCapabilities`
- Rename WMS 1.1.1 emulated server Java class
- Standardize on using "wms13" instead of "wms130"
- Add WPS 2.0 emulated server, responds to `GetCapabilities`
- Extract common emulated server code to superclass
- Support secure clients that may use CORS
- Remove references to supporting WPS 1.0.0

## 0.2 (2018-09-24)

This release makes it usable with TEAM Engine.

- Prevent maven from mangling resources such as the sample Java keystore
- Use maven shade plugin to prevent servlet API version conflict between embedded Jetty and host Tomcat for TEAM Engine
- Update the CTL script to load default JKS path and password
- Use `0.0.0.0` interface as default for Test Server under TEAM Engine
- Print error when unknown service type is specified. Only WMS 1.1.1 is supported in this release
- Add guide for TEAM Engine deployment under Debian
- Add guide for TEAM Engine deployment with CA certificates
- Add test run property "path"
- Add test run property "address"
- Use nonce for requests XML temporary file name
- Omit JKS path and password from test results as system administrator may want to keep these private
- Add documentation for manual testing checklists
- Handle exceptions in test server preparation stage and pass those exceptions to TestNG, which will pass them to the test user

## 0.1 (2018-09-11)

The initial release implements the following test requirements:

- validation of secure client HTTPS connection
- support for WMS 1.1.1 conformance class

These are based on OGC-17-007. Please see the documentation at
`src/site/markdown/index.md` for more instructions.