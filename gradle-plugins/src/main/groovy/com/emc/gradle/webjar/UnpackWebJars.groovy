package com.emc.gradle.webjar

import org.gradle.api.*
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.*

class UnpackWebJars extends DefaultTask {
    static final String PREFIX = "META-INF/resources/webjars"
    @InputFiles
    FileCollection files
    File destinationDir
    
    void webjars(Object... paths) {
        this.files = project.files(paths)
    }
    
    @OutputDirectory
    File getDestinationDir() {
        destinationDir ?: project.file("${project.buildDir}/webjars")
    }
    
    @TaskAction
    void extractWebJars() {
        getFiles().each { webjar->
            project.copy {
                into destinationDir
                from (project.zipTree(webjar)) {
                    includeEmptyDirs = false
                    include "${PREFIX}/**"
                    eachFile { details->
                        def oldPath = details.relativePath.pathString
                        def newPath = oldPath.replaceAll("${PREFIX}/(.+?)/.+?/", "\$1/")
                        details.relativePath = RelativePath.parse(details.relativePath.isFile(), newPath)
                    }
                }
            }
        }
    }
}