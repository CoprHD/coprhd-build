package com.emc.gradle.webjar

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.publish.maven.MavenPublication
import com.emc.gradle.GradleUtils

class WebJarPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.plugins.apply('base')
        project.plugins.apply('maven-publish')

        def config = project.extensions.create('webjar', WebJarExtension)
        def prepareWebjar = project.task('prepareWebjar', type:Copy) {
            into { "${project.buildDir}/webjar" }
            from ({ config.srcDir }) {
                into { "META-INF/resources/webjars/${->project.name}/${->project.version}" }
            }
        }
        def webjarTask = project.task('webjar', type:Jar) {
            from { prepareWebjar.destinationDir }
        }
        webjarTask.dependsOn prepareWebjar
        project.assemble.dependsOn webjarTask
        
        project.configurations.maybeCreate('webjar')
        project.tasks.withType(UnpackWebJars) {
            webjars(project.configurations.webjar.files)
        }
        project.artifacts {
            webjar webjarTask
        }

        GradleUtils.configurePublishing(project)
        project.publishing.publications {
            mavenWebjar(MavenPublication) {
                pom.withXml { xml->
                    GradleUtils.resolvePom(project, xml, 'webjar')
                }
            }
        }
        // Delay adding artifacts to the publication so its dependencies will be resolved
        project.afterEvaluate {
            // Ensure the publication wasn't removed
            if (project.publishing.publications.findByName('mavenWebjar')) {
                project.publishing.publications.mavenWebjar {
                    // Ensure the artifact name conforms to the maven standard
                    artifactId = webjarTask.baseName.replaceAll("[^A-Za-z0-9_\\-.]", "-")
                    groupId = project.group
                    version = webjarTask.version
                    artifact webjarTask
                }
            }
        }
    }
}