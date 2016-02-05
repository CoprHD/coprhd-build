package com.emc.gradle.junitant

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*

/**
 * Task for running junit tests using the ant.  Some unit tests run within gradle have issues, specifically some using
 * javaagents. This allows the unit tests to be run but also produce test reports.
 */
class JUnitAntTask extends DefaultTask {
    @InputFiles
    FileCollection classpath
    @Input
    File testDir
    @Input
    List tests
    @Input
    @Optional
    String maxHeapSize
    @Input
    @Optional
    String minHeapSize
    @Input
    List jvmArgs
    @Input
    Map systemProperties
    @Input
    boolean ignoreFailures
    @Input
    File testResultsDir
    
    @TaskAction    
    void runTests() {
        logging.captureStandardOutput LogLevel.INFO
        logging.captureStandardError LogLevel.ERROR
        
        String cp = getClasspath().asPath
        String errorProp = "${name}Error"
        String failureProp = "${name}Failure"
        
        getTestResultsDir().mkdirs()
        project.ant.junit4(fork:'yes', forkmode:'perBatch', printsummary:'yes', 
                errorproperty:errorProp, failureproperty:failureProp) {
            classpath(path: cp)
            formatter(type:'xml')
            if (getMaxHeapSize()) {
                jvmarg(value:"-Xmx${getMaxHeapSize()}")
            }
            if (getMinHeapSize()) {
                jvmarg(value:"-Xms${getMinHeapSize()}")
            }
            getJvmArgs()?.each {
                jvmarg(value:it)
            }
            getSystemProperties()?.each { key,value->
                sysproperty(key:key, value:value)
            }
            batchtest(todir:getTestResultsDir()) {
                fileset(dir:getTestDir()) {
                    getTests().each {
                        include(name:it.replace('.', '/')+".class")
                    }
                }
            }
        }
        
        if (project.ant.properties[errorProp]) {
            if (!isIgnoreFailures()) {
                throw new GradleException("Halting due to test errors")
            }
            logger.info("Ignoring test errors")
        }
        if (project.ant.properties[failureProp]) {
            if (!isIgnoreFailures()) {
                throw new GradleException("Halting due to test failures")
            }
            logger.info("Ignoring test failures")
        }
    }
}