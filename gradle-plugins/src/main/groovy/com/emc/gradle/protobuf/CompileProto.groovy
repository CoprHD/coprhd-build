package com.emc.gradle.protobuf

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

/**
 * Task to compile google proto files into generated sources for java/cpp/python.
 * The <tt>javaDir</tt> field is required, the <tt>cppDir</tt> and <tt>pythonDir</tt> fields are optional.
 */
class CompileProto extends DefaultTask {
    @Input
    String executable
    @InputFiles
    FileCollection srcFiles
    @InputFiles
    FileCollection protoPath

    @OutputDirectory
    File javaDir
    @OutputDirectory
    @Optional
    File cppDir
    @OutputDirectory
    @Optional
    File pythonDir

    void createOutputDirs() {
        getJavaDir().mkdirs()
        getCppDir()?.mkdirs()
        getPythonDir()?.mkdirs()
    }

    @TaskAction
    void compileProto() {
        createOutputDirs()

        def args = [
            getExecutable(),
            "--java_out=${getJavaDir().absolutePath}"
        ]
        if (getCppDir()) {
            args << "--cpp_out=${getCppDir().absolutePath}"
        }
        if (getPythonDir()) {
            args << "--python_out=${getPythonDir().absolutePath}"
        }
        getProtoPath()?.each { args << "--proto_path=${it.absolutePath}" }

        getSrcFiles().each { srcFile ->
            project.exec { commandLine args + [srcFile ]}
        }
    }
}