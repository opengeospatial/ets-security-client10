
# Release Notes

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