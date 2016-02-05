package com.emc.gradle.webjar

import java.io.File;
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.lesscss.LessCompiler
import org.lesscss.LessSource
import org.apache.commons.lang.StringUtils;

class LessCompile extends DefaultTask {
    boolean force = true
    boolean compress = false
    File sourceFile
    File destinationFile

    void sourceFile(Object sourceFile) {
        this.sourceFile = project.file(sourceFile)
    }

    void destinationFile(Object destinationFile) {
        this.destinationFile = project.file(destinationFile)
    }

    @OutputFile
    File getDestinationFile() {
        project.file(this.destinationFile ?: StringUtils.removeEnd(this.sourceFile.toString(), ".less") + ".css")
    }

    @TaskAction
    def compile() {
        LessCompiler compiler = new LessCompiler()
        compiler.setCompress(getCompress())
        compiler.compile(getSourceFile(), getDestinationFile(), getForce())
    }
}