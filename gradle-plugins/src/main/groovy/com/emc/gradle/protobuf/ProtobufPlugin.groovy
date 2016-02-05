package com.emc.gradle.protobuf

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.plugins.ide.idea.IdeaPlugin
import java.io.File
import com.emc.gradle.GradleUtils

class ProtobufPlugin implements Plugin<Project> {
    static final String NAME = 'protobuf'
    static final String COMPILE_CONFIGURATION = 'protoCompile'
    static final String COMPILE_PROTO_TASK = 'compileProto'
    static final String DOWNLOAD_PROTOC_TASK = 'downloadProtoc'
    static final String CLEAN_PROTO_TASK = 'cleanProto'

    void apply(Project project) {
        project.plugins.apply('java')
        
        project.extensions.create(NAME, ProtobufExtension)
        GradleUtils.getOrCreateConfiguration(project, COMPILE_CONFIGURATION)
        
        addCompileProtoTask(project)
        addCleanProtoTask(project)
        configureIdeTasks(project)
        
        project.afterEvaluate {
            addDownloadProtocTask(project)
            configureProtoc(project)
        }
    }

    /**
     * Adds a task for compiling the proto files.
     */
    void addCompileProtoTask(Project project) {
        project.sourceSets {
            generated {
                java {
                    srcDir { project.protobuf.javaDir }
                    compileClasspath = project.configurations.compile
                }
            }
        }
        def task = project.task(COMPILE_PROTO_TASK, type:CompileProto) {
            conventionMapping.with {
                executable = { project.protobuf.executable }
                srcFiles = { project.fileTree(dir:project.protobuf.src, include:"**/*.proto") }
                javaDir = { project.file(project.protobuf.javaDir) }
                cppDir = { project.protobuf.cppDir ? project.file(project.protobuf.cppDir) : null }
                pythonDir = { project.protobuf.pythonDir ? project.file(project.protobuf.pythonDir) : null }
                protoPath = {
                    def path = project.files(project.protobuf.src)
                    GradleUtils.getProjectDependencies(project, COMPILE_CONFIGURATION).each { depProject->
                        path += depProject.files(depProject.protobuf.src)
                    }
                    return path
                }
            }
            onlyIf { project.protobuf.executable }
        }

        // Add generated files to the main JAR instead of creating a separate JAR
        project.tasks.jar.from(project.sourceSets.generated.output)
        project.tasks.compileGeneratedJava.dependsOn(task)
        project.tasks.compileJava {
            dependsOn(project.tasks.compileGeneratedJava)
            classpath += project.sourceSets.generated.output
        }
        project.tasks.compileTestJava {
            dependsOn(project.tasks.compileGeneratedJava)
            classpath += project.sourceSets.generated.output
        }
        project.tasks.withType(Test) {
            classpath += project.sourceSets.generated.output
        }
    }

    /**
     * Adds a task for deleting generated sources.
     */
    void addCleanProtoTask(Project project) {
        def task = project.task(CLEAN_PROTO_TASK, type:Delete)
        task.delete { project.protobuf.javaDir }
        task.delete { project.protobuf.cppDir }
        task.delete { project.protobuf.pythonDir }
        project.tasks.clean.dependsOn(task)
        // Ensure that compile proto runs after clean proto
        project.tasks[COMPILE_PROTO_TASK].mustRunAfter(task)
    }

    /**
     * Performs additional configuration to IDE tasks.
     */
    void configureIdeTasks(Project project) {
        // Perform a clean/compile when generating IDE configurations
        project.tasks.withType(GenerateEclipseClasspath) {
            dependsOn(CLEAN_PROTO_TASK, COMPILE_PROTO_TASK)
        }
        project.tasks.withType(GenerateIdeaModule) {
            dependsOn(CLEAN_PROTO_TASK, COMPILE_PROTO_TASK)
        }
        project.plugins.withType(IdeaPlugin) {
            project.afterEvaluate {
                project.idea.module {
                    sourceDirs += project.file(project.protobuf.javaDir)
                }
            }
        }
    }

    /**
     * Adds a task for downloading protoc when on windows and installing it into a location that is searched for
     * automatically during the build.
     */
    void addDownloadProtocTask(Project project) {
        project.rootProject.with {
            if (!GradleUtils.isWindows() || tasks.findByName(DOWNLOAD_PROTOC_TASK) != null) {
                return
            }
            String protobufVersion = project.protobuf.version
            task(DOWNLOAD_PROTOC_TASK) {
                description = "Downloads the Win32 binary of the google protocol buffers"
                ext.filename = "protoc-${protobufVersion}-win32.zip"
                ext.downloadUrl = "https://protobuf.googlecode.com/files/${filename}"
                ext.downloadPath = file("${gradle.gradleUserHomeDir}/${filename}").absolutePath
                ext.destDir = getProtocDownloadPath(project)
                doLast {
                    ant.get(src:downloadUrl, dest:downloadPath, verbose:'true')
                    copy {
                        from zipTree(downloadPath)
                        into destDir
                    }
                }
            }
        }
    }

    /**
     * Configures protobuf dependency, finds the executable if not specified and checks the version
     */
    void configureProtoc(Project project) {
        def protobufVersion = project.protobuf.version
        project.dependencies {
            compile "com.google.protobuf:protobuf-java:${protobufVersion}"
        }
        if (!project.protobuf.executable) {
            project.protobuf.executable = findProtoc(project)
            if (project.protobuf.executable) {
                project.logger.info("Using protocol compiler: ${project.protobuf.executable}")
            }
            else {
                project.logger.error("*** No protocol compiler found ***")
            }
        }
        
        if (project.protobuf.executable) {
            checkProtocVersion(project)
        }
    }
    
    /**
     * Searches for protoc in some known locations.
     */
    String findProtoc(Project project) {
        File protoc = GradleUtils.findFile(project, 
                "/usr/bin/protoc",
                "/usr/local/bin/protoc",
                "${getProtocDownloadPath(project)}/protoc.exe"
        )
        return protoc?.absolutePath
    }
    
    /**
     * Ensures that the actual version of protoc matches the required version.
     * 
     * @param project
     *        the project.
     */
    void checkProtocVersion(Project project) {
        String protoc = project.protobuf.executable
        String requiredVersion = project.protobuf.version
        
        String output = "${protoc} --version".execute().text?.trim() 
        String actualVersion = output?.replaceFirst("libprotoc\\s+", "")
        if (actualVersion != requiredVersion) {
            throw new GradleException("Protobuf version required: ${requiredVersion}, actual: ${actualVersion}")
        }
    }
    
    /**
     * Gets the path used when downloading protoc in windows.
     * 
     * @param project
     *        the project
     * @return the download path
     */
    String getProtocDownloadPath(Project project) {
        project.file("${project.gradle.gradleUserHomeDir}/protoc-${project.protobuf.version}").absolutePath
    }
}
