package com.emc.gradle.vipr

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.result.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.apache.commons.lang.StringUtils

import com.emc.gradle.GradleUtils

class ViPRBasePlugin implements Plugin<Project>  {
    void apply(Project project) {
        project.apply {
            delegate = project
            plugins.apply('java')
            plugins.apply('maven')
            plugins.apply('eclipse')
            plugins.apply('idea')

            // Checks for required version of gradle/java (if specified)
            verifyGradleVersion(project)
            verifyJavaVersion(project)
            
            // Set compiler options
            tasks.withType(JavaCompile) { 
                options.encoding = 'UTF-8'
            }
            // Set source/target compatibility from properties if specified
            def javaSourceCompatibility = projectProperty(project, 'javaSourceCompatibility')
            if (javaSourceCompatibility) {
                sourceCompatibility = javaSourceCompatibility
            }
            def javaTargetCompatibility = projectProperty(project, 'javaTargetCompatibility')
            if (javaTargetCompatibility) {
                targetCompatibility = javaTargetCompatibility
            }
        }

        configureResources(project)
        configureJars(project)
        configurePublishing(project)
        GradleUtils.addTestCompileDependencies(project)
    }

    /**
     * Adds a source JAR task and a test JAR task.
     * 
     * @param project the project
     */
    void configureJars(Project project) {
        project.apply {
            delegate = project
            // Add additional jar tasks
            task('sourceJar', type: Jar) {
                classifier = 'sources'
                from sourceSets.main.java
                from sourceSets.test.java
            }
            artifacts { archives sourceJar }
            task('testJar', type: Jar) {
                classifier = 'tests'
                from sourceSets.test.output
            }
            artifacts { testRuntime testJar }
        }
    }

    /**
     * Configures the resources of the project so they can co-exist in the source folder instead of a separate folder.
     * 
     * @param project the project.
     */
    void configureResources(Project project) {
        project.apply {
            delegate = project
            task('copyResources', type: Copy) {
                from { sourceSets.main.java.srcDirs }
                into sourceSets.main.output.resourcesDir
                exclude '**/*.java'
            }
            processResources.dependsOn('copyResources')
            
            task('copyTestResources', type: Copy) {
                from { sourceSets.test.java.srcDirs }
                into sourceSets.test.output.resourcesDir
                exclude '**/*.java'
            }
            processTestResources.dependsOn('copyTestResources')
        }
    }
    
    /**
     * Configures maven publishing support.
     * 
     * @param project the project.
     */
    void configurePublishing(Project project) {
        project.apply {
            delegate = project
            
            loadPublishConfig(project)
            String publishUrl = projectProperty(project, 'publishUrl')
            if (publishUrl) {
                // If there is no protocol this is a file path, resolve relative to root project
                if (!publishUrl.contains(':')) {
                    publishUrl = project.rootProject.file(publishUrl).toURI().toString()
                }
                
                // Configuration maven deployer
                uploadArchives {
                    repositories.mavenDeployer {
                        repository(url: publishUrl) {
                            authentication(
                                userName: projectProperty(project, 'publishUsername'),
                                password: projectProperty(project, 'publishPassword')
                            )
                        }
                        pom*.withXml { xml->
                            GradleUtils.resolvePom(project, xml)
                        }
                        pom*.whenConfigured { pom->
                            GradleUtils.removeDuplicateDependencies(project, pom)
                        }
                    }
                }
                
                // Configure local maven installer
                install {
                    repositories.mavenInstaller {
                        pom*.withXml { xml->
                            GradleUtils.resolvePom(project, xml)
                        }
                        pom*.whenConfigured { pom->
                            GradleUtils.removeDuplicateDependencies(project, pom)
                        }
                    }
                }
            }
            
            // Add placeholder tasks with the same names as the maven-publish plugin
            project.task('publish', dependsOn:uploadArchives).doFirst {
                if (!publishUrl) {
                    throw new InvalidUserDataException("Cannot publish: 'publishUrl' property was specified")
                }
            }
            project.task('publishToMavenLocal', dependsOn:install)
        }
    }
    
    /**
     * Loads the publish configuration from an external properties file specified by the 
     * <tt>publishConfig</tt> property.
     * 
     * @param project the project.
     */
    void loadPublishConfig(Project project) {
        def publishConfig = projectProperty(project, 'publishConfig')
        if (publishConfig && project.file(publishConfig).isFile()) {
            project.file(publishConfig).withInputStream {
                def props = new Properties()
                props.load(it)
                if (props.getProperty('publishUrl')) {
                    project.ext.publishUrl = props.getProperty('publishUrl')
                }
                if (props.getProperty('publishUsername')) {
                    project.ext.publishUsername = props.getProperty('publishUsername')
                }
                if (props.getProperty('publishPassword')) {
                    project.ext.publishPassword = props.getProperty('publishPassword')
                }
            }
        }
    }
    
    String projectProperty(Project project, String name) {
        project.hasProperty(name) ? project.getProperty(name) : null
    }
    
    void verifyGradleVersion(Project project) {
        String requiredVersion = projectProperty(project, 'requiredGradleVersion')
        if (requiredVersion) {
            String currentVersion = project.gradle.gradleVersion
            if (!GradleUtils.isCompatibleVersion(requiredVersion, currentVersion)) {
                throw new GradleException(
                    "Gradle ${requiredVersion} is required (current: ${currentVersion}). " +
                    "Upgrade your gradle installation at ${project.gradle.gradleHomeDir} or use gradlew")
            }
        }
    }
    
    void verifyJavaVersion(Project project) {
        String requiredVersion = projectProperty(project, 'requiredJavaVersion') ?: 
                                 projectProperty(project, 'javaSourceCompatibility')
        if (requiredVersion) {
            String currentVersion = System.getProperty('java.version')
            if (!GradleUtils.isCompatibleVersion(requiredVersion, currentVersion)) {
                throw new GradleException(
                    "Java ${requiredVersion} is required (current: ${currentVersion}). " +
                    "Java Home is currently set to ${System.getProperty('java.home')}")
            }
        }
    }
}