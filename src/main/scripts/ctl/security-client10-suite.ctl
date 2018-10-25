<?xml version="1.0" encoding="UTF-8"?>
<ctl:package xmlns:ctl="http://www.occamlab.com/ctl"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:tns="http://www.opengis.net/cite/security-client10"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:tec="java:com.occamlab.te.TECore"
  xmlns:tng="java:org.opengis.cite.securityclient10.TestNGController">

<ctl:function name="tns:run-ets-security-client10">
    <ctl:param name="testRunArgs">A Document node containing test run arguments (as XML properties).</ctl:param>
    <ctl:param name="outputDir">The directory in which the test results will be written.</ctl:param>
    <ctl:return>The test results as a Source object (root node).</ctl:return>
    <ctl:description>Runs the security-client10 ${version} test suite.</ctl:description>
    <ctl:code>
        <xsl:variable name="controller" select="tng:new($outputDir)" />
        <xsl:copy-of select="tng:doTestRun($controller, $testRunArgs)" />
    </ctl:code>
 </ctl:function>

<ctl:suite name="tns:ets-security-client10-${version}">
    <ctl:title>Test suite: ets-security-client10</ctl:title>
    <ctl:description>Describe scope of testing.</ctl:description>
    <ctl:starting-test>tns:Main</ctl:starting-test>
</ctl:suite>
 
<ctl:test name="tns:Main">
    <ctl:assertion>The test subject satisfies all applicable constraints.</ctl:assertion>
        <ctl:code>
          <!--  TEAM Engine Administrator: Edit these variables -->
          <xsl:variable name="address">0.0.0.0</xsl:variable>
          <xsl:variable name="port">10080</xsl:variable>
          <xsl:variable name="host">localhost</xsl:variable>
          <xsl:variable name="jks_path">/root/ets-security-client10.jks</xsl:variable>
          <xsl:variable name="jks_password"><![CDATA[ets-security-client]]></xsl:variable>
  
          <xsl:variable name="form-data">
          <ctl:form method="POST" width="800" height="600" xmlns="http://www.w3.org/1999/xhtml">
            <h2>Test suite: ets-security-client10</h2>
            <div style="background:#F0F8FF" bgcolor="#F0F8FF">
              <p>The client implementation under test is checked against the following specification(s):</p>
              <ul>
                <li>OGC Web Services Security Standard, version 1.0</li>
              </ul>
              <p>The conformance classes tested depends on the OGC Web Service type being emulated.</p>
              <ul>
                <li>WMS 1.1.1: Conformance Class "WMS 1.1.1"</li>
                <li>WMS 1.3.0: Conformance Class "WMS 1.3.0"</li>
                <li>WPS 2.0.0: Conformance Class "OGC Common"</li>
              </ul>
              <p>The base "Common Security" Conformance Class will always apply before the service-specific conformance class is ran.</p>
            </div>
            <fieldset style="background:#ccffff">
              <label for="service-type">
                <h4 style="margin-bottom: 0.5em">Service Type to Emulate</h4>
              </label>
              <p>
                The service type will influence the conformance class that applies to the test session, as well as the behavior of the test server and capabilities document.
              </p>
              <select id="service-type" name="service-type">
                <option value="wms111">WMS 1.1.1</option>
                <option value="wms13">WMS 1.3.0</option>
                <option value="wps20">WPS 2.0.2</option>
              </select>
              <input type="hidden" id="path" name="path" value="" />
              
              <h4>Authentication Method</h4>
              <p>
                Select the Authentication method that will be a required constraint for the secure client.
              </p>
              <ul>
                <li>
                  <input type="radio" id="auth-none" name="auth" value="" checked="checked" />
                  <label for="auth-none">None</label>
                </li>
                <li>
                  <input type="radio" id="auth-saml2" name="auth" value="saml2" />
                  <label for="auth-saml2">SAML 2.0 Web Browser SSO Profile</label>
                </li>
              </ul>

              <h4>Identity Provider URL</h4>
              <p>For some authentication methods, an Identity Provider URL is required. When no authentication method is selected, then this test run property is unused by the test server (and hence why the field is disabled).</p>
              <p>
                The method by which the Identity Provider identifies the User Agent is not mandated by this test suite; it is up to you to make sure your secure client is compatible with the Identity Provider you defined below.
              </p>
              <input type="text" name="idp-url" id="idp-url" value="" disabled="disabled" style="width: 50%" />
              
              <h4>Conformance Class</h4>
              <p>Based on the Service Type selected, the following Conformance Class will apply:</p>
              <p id="active-conformance-class">Conformance Class WMS 1.1.1</p>

              <h4>Test Session Endpoint URL</h4>
              <p>When the test starts, your session URL will be as follows.</p>
              <h4 id="test-endpoint"></h4>
            </fieldset>
            <p>Suggestion: Ensure your browser has allowed pop-up windows from this domain. This will allow you to see the test console and test session URL pop up windows.</p>
            <p>
              <input class="form-button" type="submit" value="Start"/> | 
              <input class="form-button" type="reset" value="Clear"/>
            </p>
           
            <script>
              var host = "<xsl:value-of select="$host" />";
              var port = "<xsl:value-of select="$port" />";
            <![CDATA[
              // This script will update the active conformance class
              // text based on the service type selected in the
              // select element.
              window.onload = function() {
                var activeElement = document.getElementById("active-conformance-class");
                document.getElementById("service-type").addEventListener("change", function(e) {
                  var element = e.target;
                  if (element.value === "wms111") {
                    activeElement.innerText = "Conformance Class WMS 1.1.1";
                  } else if (element.value === "wms13") {
                    activeElement.innerText = "Conformance Class WMS 1.3.0";
                  } else {
                    activeElement.innerText = "Conformance Class OWS Common";
                  }               
                });

                document.querySelectorAll("input[name='auth']").forEach(function(node) {
                  node.addEventListener("change", function(e) {
                    if (this.value === "") {
                      document.getElementById("idp-url").value = "";
                      document.getElementById("idp-url").disabled = true;
                    } else {
                      document.getElementById("idp-url").disabled = false;
                    }
                  });
                });
                
                // Generate nonce for test server path
                var nonce = btoa(Math.random()).substr(5,16);
                document.getElementById("path").value = nonce;
                
                // Display test endpoint URL
                var endpointUrl = "https://" + host + ":" + port + "/" + nonce;
                document.getElementById("test-endpoint").innerText = endpointUrl;

                // Open pop up window with test endpoint URL, in case
                // tester submits form before copying the URL
                document.querySelector("form").addEventListener("submit", function(e) {
                  var w = window.open("", "", "width=640, height=480, scrollbars=no");
                  var desc = w.document.createElement("p");
                  desc.textContent = "Your Test Session URL";
                  var header = w.document.createElement("h2");
                  header.textContent = endpointUrl;

                  w.document.body.appendChild(desc);
                  w.document.body.appendChild(header);
                });
              }
              ]]>
            </script>
          </ctl:form>
        </xsl:variable>
        <xsl:variable name="test-run-props">
          <properties version="1.0">
            <entry key="service_type">
              <xsl:value-of select="$form-data/values/value[@key='service-type']"/>
            </entry>
            <entry key="address"><xsl:value-of select="$address" /></entry>
            <entry key="port"><xsl:value-of select="$port" /></entry>
            <entry key="host"><xsl:value-of select="$host" /></entry>
            <entry key="path">
              <xsl:value-of select="$form-data/values/value[@key='path']"/>
            </entry>
            <entry key="jks_path"><xsl:value-of select="$jks_path" /></entry>
            <entry key="jks_password"><xsl:value-of select="$jks_password" /></entry>
            <entry key="authentication"><xsl:value-of select="$form-data/values/value[@key='auth']"/></entry>
            <entry key="saml2_url"><xsl:value-of select="$form-data/values/value[@key='idp-url']"/></entry>
          </properties>
       </xsl:variable>
       <xsl:variable name="testRunDir">
         <xsl:value-of select="tec:getTestRunDirectory($te:core)"/>
       </xsl:variable>
       <xsl:variable name="test-results">
        <ctl:call-function name="tns:run-ets-security-client10">
          <ctl:with-param name="testRunArgs" select="$test-run-props"/>
          <ctl:with-param name="outputDir" select="$testRunDir" />
        </ctl:call-function>
      </xsl:variable>
      <xsl:call-template name="tns:testng-report">
        <xsl:with-param name="results" select="$test-results" />
        <xsl:with-param name="outputDir" select="$testRunDir" />
      </xsl:call-template>
      <xsl:variable name="summary-xsl" select="tec:findXMLResource($te:core, '/testng-summary.xsl')" />
      <ctl:message>
        <xsl:value-of select="saxon:transform(saxon:compile-stylesheet($summary-xsl), $test-results)"/>
See detailed test report in the TE_BASE/users/<xsl:value-of 
select="concat(substring-after($testRunDir, 'users/'), '/html/')" /> directory.
        </ctl:message>
        <xsl:if test="xs:integer($test-results/testng-results/@failed) gt 0">
          <xsl:for-each select="$test-results//test-method[@status='FAIL' and not(@is-config='true')]">
            <ctl:message>
Test method <xsl:value-of select="./@name"/>: <xsl:value-of select=".//message"/>
        </ctl:message>
      </xsl:for-each>
      <ctl:fail/>
        </xsl:if>
        <xsl:if test="xs:integer($test-results/testng-results/@skipped) eq xs:integer($test-results/testng-results/@total)">
        <ctl:message>All tests were skipped. One or more preconditions were not satisfied.</ctl:message>
        <xsl:for-each select="$test-results//test-method[@status='FAIL' and @is-config='true']">
          <ctl:message>
            <xsl:value-of select="./@name"/>: <xsl:value-of select=".//message"/>
          </ctl:message>
        </xsl:for-each>
        <ctl:skipped />
      </xsl:if>
    </ctl:code>
</ctl:test>

 <xsl:template name="tns:testng-report">
    <xsl:param name="results" />
    <xsl:param name="outputDir" />
    <xsl:variable name="stylesheet" select="tec:findXMLResource($te:core, '/testng-report.xsl')" />
    <xsl:variable name="reporter" select="saxon:compile-stylesheet($stylesheet)" />
    <xsl:variable name="report-params" as="node()*">
        <xsl:element name="testNgXslt.outputDir">
            <xsl:value-of select="concat($outputDir, '/html')" />
        </xsl:element>
    </xsl:variable>
    <xsl:copy-of select="saxon:transform($reporter, $results, $report-params)" />
</xsl:template>
</ctl:package>
