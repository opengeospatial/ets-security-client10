## Test suite: ets-security-client10

### Scope

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

Visit the [project documentation website][site docs]
for more information, including the API documentation. **Note**: project
documentation site will be available when the test suite has been
published to the OGC TEAM Engine site.

[site docs]: http://opengeospatial.github.io/ets-security-client10/

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

More details about the mandatory and optional test run properties can
be located in the [documentation](src/site/markdown/index.md).

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

This will require you to first compile and build the test suite as a Java
application.

#### 3. OGC test harness

Use [TEAM Engine](https://github.com/opengeospatial/teamengine), the official OGC test harness.
The latest test suite release are usually available at the [beta testing facility](http://cite.opengeospatial.org/te2/). 
You can also [build and deploy](https://github.com/opengeospatial/teamengine) the test 
harness yourself and use a local installation.

A detailed [guide on setting up TEAM Engine](src/site/markdown/team_setup.md) 
is included in the site docs.

### How to contribute

If you would like to get involved, you can:

* [Report an issue](https://github.com/opengeospatial/ets-securityclient10/issues) such as a defect or 
an enhancement request
* Help to resolve an [open issue](https://github.com/opengeospatial/ets-securityclient10/issues?q=is%3Aopen)
* Fix a bug: Fork the repository, apply the fix, and create a pull request
* Add new tests: Fork the repository, implement and verify the tests on a new topic branch, 
and create a pull request (don't forget to periodically rebase long-lived branches so 
there are no extraneous conflicts)
