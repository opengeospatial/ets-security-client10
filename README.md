## Test suite: ets-security-client10

### Scope

Describe scope of the test suite.

Visit the [project documentation website](http://opengeospatial.github.io/ets-security-client10/) 
for more information, including the API documentation.

### How to run the tests

The test suite is built using [Apache Maven v3](https://maven.apache.org/). 
Maven is necessary for self-hosting the test suite in your own test
environment.

All of the test suite execution environments use the Test Run Properties
to customize the parameters of the test session.

#### Test Run Properties

For IDE and JAR testing, an XML file is used to pass the test run
properties into the test suite. TEAM Engine users will instead use the
web form interface to specify these properties, and TEAM Engine will
automatically convert the form into an XML file for the test suite.

Here is a sample test run properties XML file, typically named
`test-run-props.xml` although the name is not important.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties version="1.0">
  <comment>Sample test run arguments (ets-security-client10)</comment>
  <entry key="service_type">wms111</entry>
  <entry key="host">127.0.0.1</entry>
  <entry key="port">10080</entry>
  <entry key="jks_path">security.jks</entry>
</properties>
```

##### service_type

Valid values for `service_type` are:

* `wms111` for WMS 1.1.1 Conformance Class
* `wms13` for WMS 1.3.0 Conformance Class
* `wps10` for WPS 1.0.0 on the OWS Common Conformance Class 

The base "Common Security" Conformance Class will apply regardless of setting, with
an additional Conformance Class for one of "WMS 1.1.1", "WMS 1.3.0", or
"OWS Common" depending on the type of service being emulated. Additional
OWS Common services may be added to this test suite in the future.

##### host and port

The values for `host` and `port` are for starting the embedded Jetty web
server. The test suite will fail if the server cannot bind to that address
or port.

Note that on some OSes, ports under 1024 require additional system
privileges to bind, and the test suite will fail if it attempts to bind
to such a port without the executing user having those privileges.

##### jks_path

A Java KeyStore containing the X.509 certificates for the `host` address
must be located at `jks_path` in order for the embedded Jetty server to
provide HTTPS. Self-signed certificates are permitted, although the test
client will have to trust that certificate manually.


#### 1. Integrated development environment (IDE)

Use a Java IDE such as Eclipse, NetBeans, or IntelliJ. Clone the repository and build the project.

Set the main class to run: `org.opengis.cite.security-client10.TestNGController`

Arguments: The first argument must refer to an XML properties file containing the 
required test run arguments. If not specified, the default location at 
`${user.home}/test-run-props.xml` will be used.

The TestNG results file (`testng-results.xml`) will be written to a subdirectory
in `${user.home}/testng/` having a UUID value as its name.

#### 2. Command shell (console)

One of the build artifacts is an "all-in-one" JAR file that includes the test 
suite and all of its dependencies; this makes it very easy to execute the test 
suite in a command shell:

`java -jar ets-security-client10-0.1-SNAPSHOT-aio.jar [-o|--outputDir $TMPDIR] [test-run-props.xml]`

#### 3. OGC test harness

Use [TEAM Engine](https://github.com/opengeospatial/teamengine), the official OGC test harness.
The latest test suite release are usually available at the [beta testing facility](http://cite.opengeospatial.org/te2/). 
You can also [build and deploy](https://github.com/opengeospatial/teamengine) the test 
harness yourself and use a local installation.

Please Note: In the current version of this repository there is a 
conflict in the Java servlet-api used by Tomcat (the host application
server for TEAM Engine) and the servlet-api used by Jetty (embedded in
this repository to capture HTTP requests). 

A possible solution is to use the Tomcat servlet-api when running under
TEAM Engine, and only use the embedded Jetty server when running with
IDE or JAR.

### How to contribute

If you would like to get involved, you can:

* [Report an issue](https://github.com/opengeospatial/ets-cat30/issues) such as a defect or 
an enhancement request
* Help to resolve an [open issue](https://github.com/opengeospatial/ets-cat30/issues?q=is%3Aopen)
* Fix a bug: Fork the repository, apply the fix, and create a pull request
* Add new tests: Fork the repository, implement and verify the tests on a new topic branch, 
and create a pull request (don't forget to periodically rebase long-lived branches so 
there are no extraneous conflicts)
