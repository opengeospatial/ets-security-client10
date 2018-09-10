package org.opengis.cite.securityclient10.levelOwsCommon;

import org.opengis.cite.securityclient10.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Includes various tests of OWS Common conformance. See 
 * {@link org.opengis.cite.securityclient10.CommonFixture} for fixture loading methods.
 */
public class CapabilityOwsCommonTests extends CommonFixture {
	/**
	 * Check if this conformance level applies
	 */
	@BeforeClass
	public void validateConformanceClass() {
		Assert.assertTrue(!this.testServiceType.equals("wms111") && !this.testServiceType.equals("wms130"), 
				"OWS Common Conformance Class will not be applied.");
	}
}