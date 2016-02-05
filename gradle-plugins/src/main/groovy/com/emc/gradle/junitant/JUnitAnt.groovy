package com.emc.gradle.junitant

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class JUnitAnt {
    final String name
    String sourceSet
    List tests = []
    List jvmArgs = []
    Map systemProperties = [:]
    String maxHeapSize
    String minHeapSize
    boolean ignoreFailures
    
    JUnitAnt(String name) {
        this.name = name
        this.sourceSet = 'test'
    }
    
    void test(String name) {
        tests.add(name)
    }
    
    void tests(String... names) {
        names?.each { tests.add(it) }
    }
    
    void jvmArg(String value) {
        jvmArgs.add(value)
    }
    
    void jvmArgs(String... values) {
        values?.each { jvmArgs.add(it) }
    }
    
    void systemProperty(String name, String value) {
        systemProperties.put(name, value)
    }
}