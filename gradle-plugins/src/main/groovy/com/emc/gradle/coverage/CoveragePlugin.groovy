package com.emc.gradle.coverage

import org.apache.maven.model.ReportSet;
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

import com.emc.gradle.GradleUtils

class CoveragePlugin implements Plugin<Project> {
    /** Sonar property => project property mapping */
    def properties = [
        'sonar.host.url': 'sonarHostUrl',
        'sonar.jdbc.url': 'sonarJdbcUrl',
        'sonar.jdbc.driverClassName': 'sonarJdbcDriver',
        'sonar.jdbc.username': 'sonarJdbcUsername',
        'sonar.jdbc.password': 'sonarJdbcPassword'
    ] 
    
    @Override
    void apply(Project project) {
        project.extensions.create('coverage', CoverageExtension, project)
        project.allprojects { p->
            p.extensions.create('coverageProperties', CoveragePropertiesExtension)
        }
        configureSonar(project)
        configureJacoco(project)
    }
    
    /**
     * Configures the sonar runner plugin on the project.
     * 
     * @param project
     */
    private void configureSonar(Project project) {
        loadSonarConfig(project)
        project.apply {
            delegate = project
            plugins.apply('sonar-runner')
            
            // Validates that if sonarRunner task is called there are the appropriate properties 
            gradle.taskGraph.whenReady { taskGraph ->
                if (taskGraph.hasTask(project.tasks['sonarRunner'])) {
                    validateSonarProperties(project)
                }
            }
            
            applySonarProperties(project)
            sonarRunner {
                sonarProperties {
                    property "sonar.java.coveragePlugin", "jacoco"
                    property "sonar.forceAnalysis", "true"
                }
            }
        }
    }
    
    /**
     * Configures the jacoco plugin on all projects. Each java project will have code coverage support
     * and the top level project will have a 'coverageReport' task that will create an aggregate report
     * for any tests run.
     * 
     * @param project
     */
    private void configureJacoco(Project project) {
        project.apply {
            delegate = project
            
            // Apply jacoco to the top level project for the overall report
            plugins.apply('jacoco')
            jacoco {
                // Delayed evaluation of the version
                toolVersion = "${->coverage.jacocoVersion}"
            }

            // Create a overall coverage report for all projects
            def coverageReport = task('coverageReport', type:JacocoReport) {
                reports {
                    csv.enabled false
                    xml.enabled true
                    html.enabled true
                }
                sourceDirectories = files()
                classDirectories = files()
                executionData = files()
            }
            
            // Collects all coverage files into one directory under the root project
            def collectCoverage = task('collectCoverage', type:Copy) {
                into { project.coverage.coverageDir }
            }
            
            // Adds jacoco to each project with the java plugin
            allprojects {
                plugins.withType(JavaPlugin) {
                    plugins.apply('jacoco')
                    jacoco {
                        // Delayed evaluation of the version
                        toolVersion = "${->coverage.jacocoVersion}"
                    }
                    // Generate HTML and XML reports
                    jacocoTestReport {
                        reports {
                            csv.enabled false
                            xml.enabled true
                            html.enabled true
                        }
                    }
                    sonarRunner.sonarProperties {
                        property "sonar.jacoco.reportPath", test.jacoco.destinationFile
                    }
                    
                    tasks.withType(Test) { task->
                        finalizedBy collectCoverage
                        doLast {
                            if (task.jacoco.destinationFile.isFile()) {
                                collectCoverage.from(task.jacoco.destinationFile) {
                                    rename { "${task.project.name}_${name}.exec" }
                                }
                                if (coverageProperties?.enabled) {
                                    coverageReport.sourceDirectories += files(task.project.sourceSets.main.allJava.srcDirs)
                                    coverageReport.classDirectories += task.project.sourceSets.main.output
                                }
                            }
                        }
                    }
                    
                }
            }
        }
    }
    
    private void validateSonarProperties(Project project) {
        Set<String> missing = [] as Set
        properties.each { key, value->
            if (!System.getProperty(key) && !project.hasProperty(value)) {
                missing.add(value)
            }
        }
        if (missing.size() > 0) {
            throw new GradleException("Missing properties required for running sonar: ${missing.join(', ')}")
        }
    }
    
    private void applySonarProperties(Project project) {
        properties.each { key, value->
            if (!System.getProperty(key)) {
                project.sonarRunner.sonarProperties {
                    property key, project[value]
                }
            }
        }
    }
    
    /**
     * Loads the sonar configuration from an external properties file specified by the
     * <tt>sonarConfig</tt> property.
     *
     * @param project the project.
     */
    void loadSonarConfig(Project project) {
        def sonarConfig = projectProperty(project, 'sonarConfig')
        if (sonarConfig && project.file(sonarConfig).isFile()) {
            project.file(sonarConfig).withInputStream {
                def props = new Properties()
                props.load(it)
                for (String name : properties.values()) {
                    if (props.getProperty(name)) {
                        project.ext[name] = props.getProperty(name)
                    }
                }
            }
        }
    }
    
    String projectProperty(Project project, String name) {
        project.hasProperty(name) ? project.getProperty(name) : null
    }
}
