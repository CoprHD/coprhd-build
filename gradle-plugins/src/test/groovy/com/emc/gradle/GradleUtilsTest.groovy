package com.emc.gradle

import static com.emc.gradle.GradleUtils.isCompatibleVersion
import groovy.util.GroovyTestCase

class GradleUtilsTest extends GroovyTestCase {
    void testIsCompatibleVersion() {
        assertCompatible("1", ["1", "1.0", "1.1.0", "1.x", "10"])
        assertCompatible("1.1", ["1.1", "1.1.0", "1.2", "1.10", "10.1", "2.1.0"])

        assertNotCompatible("1", ["0", "0.1", "0.0.0"])
        assertNotCompatible("1.5", ["1", "1.4", "1.05", "1.4.9", "0.1"])
    }
    
    private void assertCompatible(String version, List<String> compatibleVersions) {
        compatibleVersions.each {
            assert isCompatibleVersion(version, it)
        }
    }
    
    private void assertNotCompatible(String version, List<String> incompatibleVersions) {
        incompatibleVersions.each {
            assert !isCompatibleVersion(version, it)
        }
    }
}