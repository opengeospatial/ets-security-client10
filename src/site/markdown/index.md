
# Test suite: ets-security-client10

## Scope

This executable test suite (ETS) verifies the conformance of the secure 
client behavior as the implementation under test (IUT) with respect to 
the following specification(s):

* \[OGC-17-007] OGC Web Service Security 1.0

Conformance testing is a kind of "black box" testing that examines the 
externally visible characteristics or behaviors of the IUT while disregarding 
any implementation details.

Several conformance classes are defined in the principal specification; 
the ones listed below are covered by this test suite:

* Conformance Class "Common Security" (abstract)
    - Validation of secure client HTTPS usage
* Conformance Class "OWS Common"
    - Only affects Capabilities document presented to secure client
* Conformance Class "WMS 1.1.1"
    - Only affects Capabilities document presented to secure client
* Conformance Class "WMS 1.3.0"
    - Only affects Capabilities document presented to secure client

## Test suite structure

The test suite definition file (testng.xml) is located in the root package, 
`org.opengis.cite.securityclient10`. A conformance class corresponds to 
a &lt;test&gt; element, each of which includes a set of test classes that 
contain the actual test methods. The general structure of the test suite 
is shown in Table 1.

<table>
  <caption>Table 1 - Test suite structure</caption>
  <thead>
    <tr style="text-align: left; background-color: LightCyan">
      <th>Conformance class</th>
      <th>Test classes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Conformance Level Common Security</td>
      <td>org.opengis.cite.securityclient10.levelCommonSecurity.*</td>
    </tr>
    <tr>
      <td>Conformance Level OWS Common</td>
      <td>org.opengis.cite.securityclient10.levelOwsCommon.*</td>
    </tr>
    <tr>
      <td>Conformance Level WMS 1.1.1</td>
      <td>org.opengis.cite.securityclient10.levelWms111.*</td>
    </tr>
    <tr>
      <td>Conformance Level WMS 1.3.0</td>
      <td>org.opengis.cite.securityclient10.levelWms130.*</td>
    </tr>
  </tbody>
</table>

The Javadoc documentation provides more detailed information about the test 
methods that constitute the suite.


## Test run arguments

The test run arguments are summarized in Table 2. The _Obligation_ descriptor can 
have the following values: M (mandatory), O (optional), or C (conditional).

<table>
	<caption>Table 2 - Test run arguments</caption>
	<thead>
    <tr>
      <th>Name</th>
      <th>Value domain</th>
	    <th>Obligation</th>
	    <th>Description</th>
    </tr>
  </thead>
	<tbody>
    <tr>
      <td>service_type</td>
      <td>String</td>
      <td>M</td>
      <td>
        <p>A string representing the service type to emulate for secure 
      clients. Will affect the query parameters required to get 
      capabilities. Valid values are:</p>
        <ul>
          <li><code>wms111</code> for WMS 1.1.1 Conformance Class</li>
          <li><code>wms13</code> for WMS 1.3.0 Conformance Class</li>
          <li><code>wps10</code> for WPS 1.0.0 on the OWS Common Conformance Class</li>
        </ul>
      </td>
    </tr>
	  <tr>
      <td>address</td>
      <td>String</td>
      <td>M</td>
      <td>Host interface on which to bind test server.</td>
    </tr>
    <tr>
      <td>port</td>
      <td>Integer</td>
      <td>M</td>
      <td>Port on which to bind test server.</td>
    </tr>
    <tr>
      <td>host</td>
      <td>String</td>
      <td>M</td>
      <td>Host name that will be advertised to clients. May be an IP
      address or domain name. Clients must be able to resolve this IP or
      name to the machine running the test server. If you are using a
      certificate from a Certificate Authority, this parameter must match
      the common name on that certificate.</td>
    </tr>
    <tr>
      <td>path</td>
      <td>String</td>
      <td>O</td>
      <td>
        <p>URL Path at which the Test Server will listen. For example,
      <code>test-session</code> would result in the Test Server creating 
      a servlet for <code>https://host:port/test-session</code>.</p>
      <p>If left blank, a random string will be generated.</p>
      <p>When used with TEAM Engine, the CTL script will automatically
        fill in a path such that the test session web page can inform
        the tester of the URL <em>before</em> starting the test session.
      </p>
      </td>
    </tr>
    <tr>
      <td>JKS_path</td>
      <td>String</td>
      <td>M</td>
      <td>A Java KeyStore containing the X.509 certificates for the host address
      must be located at jks_path in order for the embedded Jetty server to
      provide HTTPS. Self-signed certificates are permitted, although the test
      client will have to trust that certificate manually.</td>
    </tr>
    <tr>
      <td>JKS_password</td>
      <td>String</td>
      <td>M</td>
      <td>The password required to unlock the Java KeyStore. Using a CDATA section
      is recommended to wrap passwords that have character data that may
      interfere with XML character entities.</td>
    </tr>
    <tr>
      <td>http_methods</td>
      <td>String</td>
      <td>O</td>
      <td><p>(not yet implemented)</p>
        <p>As part of the annotated capabilities document presented to the secure
        client, include Requirements Class "HTTP Methods" 
        (https://www.opengis.net/def/security/1.0/rc/http-methods).</p>
        <p>Enabling this will include an <code>ows:Constraint</code> for listing all supported 
        HTTP methods.</p>
        <p>Only a value of <code>true</code> will enable this property, any other value will
        be evaluated as <code>false</code>.</p>
        <p>If the <code>w3c_cors</code> property is set to <code>true</code>, then this property will be
        overridden to <code>true</code> as well.</p>
      </td>
    </tr>
    <tr>
      <td>w3c_cors</td>
      <td>String</td>
      <td>O</td>
      <td><p>(not yet implemented)</p>
        <p>As part of the annotated capabilities document presented to the secure
        client, include Requirements Class "W3C CORS" 
        (https://www.opengis.net/def/security/1.0/rc/cors).</p>
        <p>Enabling this will include an <code>ows:Constraint</code> for the W3C recommendation
        "Cross Origin Resource Sharing".</p>
        <p>Only a value of <code>true</code> will enable this property, any other value will
        be evaluated as <code>false</code>.</p>
        <p>If set to <code>true</code>, then the <code>http_methods</code> test run property will also be
        set to <code>true</code> regardless of your configuration; "HTTP Methods" is 
        required for this Requirements Class.</p>
      </td>
    </tr>
    <tr>
      <td>http_exception_handling</td>
      <td>String</td>
      <td>O</td>
      <td><p>(not yet implemented)</p>
        <p>As part of the annotated capabilities document presented to the secure
        client, include Requirements Class "HTTP Exception Handling" 
        (https://www.opengis.net/def/security/1.0/rc/http-exception-handling).</p>
        <p>Enabling this will include an <code>ows:Constraint</code> for enabling HTTP error
        code mapping to OWS Common exception codes.</p>
        <p>Only a value of <code>true</code> will enable this property, any other value will
        be evaluated as <code>false</code>.</p>
      </td>
    </tr>
    <tr>
      <td>http_post_content_type</td>
      <td>String</td>
      <td>O</td>
      <td><p>(not yet implemented)</p>
        <p>As part of the annotated capabilities document presented to the secure
        client, include Requirements Class "HTTP POST Content-Type" 
        (https://www.opengis.net/def/security/1.0/rc/content-type).</p>
        <p>Enabling this will include an <code>ows:Constraint</code> for listing the mime-types
        permitted to be submitted by HTTP POST.</p>
        <p>Only a value of <code>true</code> will enable this property, any other value will
        be evaluated as <code>false</code>.</p>
      </td>
    </tr>
	</tbody>
</table>

Additional test run properties may be added in the future to support
other authentication methods such as SAML 2.0 and OpenID Connect.

## How to run the tests

The test suite is built using [Apache Maven v3](https://maven.apache.org/). 
Maven is necessary for self-hosting the test suite in your own test
environment.

The test suite can be ran in the following environments:

* Under an IDE such as Eclipse
* As a self-contained JAR command-line application
* As a module under TEAM Engine

### 1. Integrated development environment (IDE)

Use a Java IDE such as Eclipse, NetBeans, or IntelliJ. Clone the repository and build the project.

Set the main class to run: `org.opengis.cite.security-client10.TestNGController`

Arguments: The first argument must refer to an XML properties file containing the 
required test run arguments. If not specified, the default location at 
`${user.home}/test-run-props.xml` will be used.

The TestNG results file (`testng-results.xml`) will be written to a subdirectory
in `${user.home}/testng/` having a UUID value as its name.

### 2. Command shell (console)

One of the build artifacts is an "all-in-one" JAR file that includes the test 
suite and all of its dependencies; this makes it very easy to execute the test 
suite in a command shell:

`java -jar ets-security-client10-0.1-SNAPSHOT-aio.jar [-o|--outputDir $TMPDIR] [test-run-props.xml]`

This will require you to first compile and build the test suite as a Java
application.

### 3. OGC test harness

Use [TEAM Engine](https://github.com/opengeospatial/teamengine), the official OGC test harness.
The latest test suite release are usually available at the [beta testing facility](http://cite.opengeospatial.org/te2/). 
You can also [build and deploy](https://github.com/opengeospatial/teamengine) the test 
harness yourself and use a local installation.

For more details, please see the guide on [setting up TEAM Engine](team_setup.html) 
with this ETS.

## Debugging the ETS

If you need to debug a secure client connection and inspect the HTTP or
HTTPS details, add these to your Java VM arguments:

```
-DDEBUG=true -Dorg.eclipse.jetty.LEVEL=DEBUG -Djavax.net.debug=ssl,handshake,data
```

This will print very detailed information about cipher suites and any
TLS extensions available from the client.

## About the included sample Java KeyStore

This repository contains a sample Java KeyStore with a self-signed 
certificate for testing purposes. The keystore can be created with the 
following command.

```sh
$ keytool -keystore src/main/resources/security.jks -storepass "ets-security-client" -genkey -alias dummy-key -keyalg RSA -sigalg SHA256withRSA -dname "cn=ETS Test Operator, ou=None, o=None, c=us"
```

This creates `src/main/resources/security.jks` with a single key 
(`dummy-key`) and protects the file with a password 
(`ets-security-client`). As this is a self-signed certificate, secure 
clients must allow insecure server certificates or install the 
certificate to their keystore.

When running the test suite from an IDE, this KeyStore will be used
as it is specified in `src/main/config/test-run-props.xml`.

To create a PEM version of the file for secure clients to use:

```sh
$ keytool -exportcert -alias dummy-key -keystore src/main/resources/security.jks -storepass "ets-security-client" -rfc -file src/main/resources/security.pem
```

## How to contribute

If you would like to get involved, you can:

* [Report an issue](https://github.com/opengeospatial/ets-securityclient10/issues) such as a defect or 
an enhancement request
* Help to resolve an [open issue](https://github.com/opengeospatial/ets-securityclient10/issues?q=is%3Aopen)
* Fix a bug: Fork the repository, apply the fix, and create a pull request
* Add new tests: Fork the repository, implement and verify the tests on a new topic branch, 
and create a pull request (don't forget to periodically rebase long-lived branches so 
there are no extraneous conflicts)
