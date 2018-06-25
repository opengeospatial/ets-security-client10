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
        <xsl:variable name="form-data">
           <ctl:form method="POST" width="800" height="600" xmlns="http://www.w3.org/1999/xhtml">
             <h2>Test suite: ets-security-client10</h2>
             <div style="background:#F0F8FF" bgcolor="#F0F8FF">
               <p>The client implementation under test is checked against the following specification(s):</p>
               <ul>
                 <li><a href="http://www.opengeospatial.org/standards/requests/164">OGC Web Services Security Standard</a>, 
         version 0.12</li>
               </ul>
               <p>Multiple use case conformance levels are defined:</p>
               <ul>
                 <li>Use Case 0: Public Service, Public Data, Public Catalogue, Public Communication</li>
                 <li>Use Case 1: Authenticated Public Service, Public Data, Public Catalogue, Secure Communication</li>
                 <li>Use Case 2: Protected Service, Open Data, Public Catalogue, Secure Communication</li>
                 <li>Use Case 3: Protected Service, Private Data, Public Catalogue</li>
                 <li>Use Case 4: Protected Service, Private Data, Protected Catalogue, Secure Communication</li>
               </ul>
               <p>Only some of the levels are implemented for this test suite.</p>
             </div>
             <fieldset style="background:#ccffff">
               <legend style="font-family: sans-serif; color: #000099; 
                       background-color:#F0F8FF; border-style: solid; 
                       border-width: medium; padding:4px">Implementation under test</legend>
               <p>
                 <label for="service-type">
                   <h4 style="margin-bottom: 0.5em">Service Type to Emulate</h4>
                 </label>
                 <select id="service-type" name="service-type">
                    <option value="wms-111">WMS 1.1.1</option>
                    <option value="wms-130">WMS 1.3.0</option>
                    <option value="wps-100">WPS 1.0.0</option>
                 </select>
               </p>
               <p>
                 <label for="level">Conformance class: </label>
                 <input id="level-1" type="radio" name="level" value="1" checked="checked" />
                 <label for="level-1"> Use Case 1 | </label>
                 <input id="level-2" type="radio" name="level" value="2" />
                 <label class="form-label" for="level-2"> Use Case 2</label>
               </p>
             </fieldset>
             <p>
               <input class="form-button" type="submit" value="Start"/> | 
               <input class="form-button" type="reset" value="Clear"/>
             </p>
           </ctl:form>
        </xsl:variable>
        <xsl:variable name="test-run-props">
        <properties version="1.0">
          <entry key="servicetype">
            <xsl:value-of select="$form-data/values/value[@key='service-type']"/>
          </entry>
          <entry key="ics"><xsl:value-of select="$form-data/values/value[@key='level']"/></entry>
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
