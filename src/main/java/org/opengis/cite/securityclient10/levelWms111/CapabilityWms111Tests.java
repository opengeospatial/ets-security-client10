package org.opengis.cite.securityclient10.levelWms111;

import org.opengis.cite.securityclient10.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Includes various tests of WMS 1.1.1 conformance. See 
 * {@link org.opengis.cite.securityclient10.CommonFixture} for fixture loading methods.
 */
public class CapabilityWms111Tests extends CommonFixture {
	/**
	 * Check if this conformance level applies
	 */
	@BeforeClass
	public void validateConformanceClass() {
		Assert.assertEquals(this.testServiceType, "wms111", 
				"WMS 1.1.1 Conformance Class will not be applied.");
	}
}