package org.opengis.cite.securityclient10.levelWms130;

import org.opengis.cite.securityclient10.CommonFixture;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Includes various tests of WMS 1.3.0 conformance. See 
 * {@link org.opengis.cite.securityclient10.CommonFixture} for fixture loading methods.
 */
public class CapabilityWms130Tests extends CommonFixture {
	/**
	 * Check if this conformance level applies
	 */
	@BeforeClass
	public void validateConformanceClass() {
		Assert.assertEquals(this.testServiceType, "wms130", 
				"WMS 1.3.0 Conformance Class will not be applied.");
	}
}