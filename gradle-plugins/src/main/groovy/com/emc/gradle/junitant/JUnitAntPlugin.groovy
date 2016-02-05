package com.emc.gradle.junitant

import org.gradle.api.Plugin
import org.gradle.api.Project


class JUnitAntPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.afterEvaluate {
            project.configurations {
                junitAnt
            }
            project.dependencies {
                junitAnt 'org.apache.ant:ant-junit4:1.9.4'
                junitAnt 'org.apache.ant:ant-junit:1.9.4'
            }
            project.ant.taskdef(name: 'junit4', classname: 'org.apache.tools.ant.taskdefs.optional.junit.JUnitTask',
                        classpath: project.configurations.junitAnt.asPath)
        }
        
        def junitAnt = project.container(JUnitAnt)
        junitAnt.all { config->
            project.task("${name}Test", type:JUnitAntTask) {
                conventionMapping.classpath = { project.sourceSets[config.sourceSet].runtimeClasspath }
                conventionMapping.testDir = { project.sourceSets[config.sourceSet].output.classesDir } 
                conventionMapping.tests = { config.tests }
                conventionMapping.maxHeapSize = { config.maxHeapSize }
                conventionMapping.minHeapSize = { config.minHeapSize }
                conventionMapping.jvmArgs = { config.jvmArgs }
                conventionMapping.systemProperties = { config.systemProperties }
                conventionMapping.testResultsDir = { project.testResultsDir }
                conventionMapping.ignoreFailures = { config.ignoreFailures }
            }
        }
        project.extensions.junitAnt = junitAnt
    }
}