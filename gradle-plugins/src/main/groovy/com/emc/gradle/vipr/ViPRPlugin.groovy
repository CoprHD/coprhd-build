package com.emc.gradle.vipr

import org.gradle.api.*
import org.gradle.api.tasks.*
import com.emc.gradle.GradleUtils

class ViPRPlugin implements Plugin<Project>  {
    void apply(Project project) {
        project.plugins.apply('vipr-base')
        
        configureSource(project)
        configureEclipse(project)
        configureIdea(project)
    }
    
    void configureSource(Project project) {
        project.apply {
            delegate = project
            configurations {
                /// Compile dependencies that do not get deployed
                provided
                // Runtime only dependencies
                runtimeOnly
            }
            test {
                ignoreFailures = true
            }
            
            // Add provided configurations to the classpath
            project.sourceSets.main {
                compileClasspath += [project.configurations.provided]
            }
            project.sourceSets.test {
                compileClasspath += [project.configurations.provided]
            }

            if (project.file("src/java").exists()) {
                sourceSets.main.java.srcDirs = [ 'src/java' ]
                sourceSets.test.java.srcDirs = [ 'src/test' ]
            }
            else {
                sourceSets.test.java.srcDirs = [ 'src/main/test' ]
                if (project.file("src/conf").exists()) {
                    sourceSets.test.resources.srcDirs = [ 'src/conf' ]
                }
            }
            
            // Add some entries to the manifest
            jar {
                manifest {
                    attributes "Build-Version": rootProject.version
                }
            }
        }
        
        project.afterEvaluate {
            GradleUtils.getProjectDependencies(project, 'compile').each { childProject->
                project.sourceSets.main {
                    compileClasspath += [childProject.configurations.provided]
                }
            }
            GradleUtils.getProjectDependencies(project, 'testCompile').each { childProject->
                project.sourceSets.test {
                    compileClasspath += [childProject.configurations.provided]
                }
            }
        }
    }
    
    void configureEclipse(Project project) {
        project.apply {
            delegate = project
            eclipse.classpath.plusConfigurations += [project.configurations.provided]
        }
    }
    
    void configureIdea(Project project) {
        project.apply {
            delegate = project
            idea {
                idea {
                    module {
                        scopes.COMPILE.plus += [project.configurations.provided]
                    }
                }
            }
        }
    }
}
