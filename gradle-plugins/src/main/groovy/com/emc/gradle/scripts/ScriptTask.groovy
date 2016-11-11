package com.emc.gradle.scripts

import java.io.File;
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class ScriptTask extends DefaultTask {
    @Input
    String scriptName
    @Input
    String fileName
    @Input
    String javaHome
    @Input
    String installDir
    @Input
    String extraDefines = ""
    @Input
    String jvmArgs = ""
    @Input
    String maxMemory = ""
    @Input
    String maxMemoryFactor = ""
    @Input
    String minMemory = ""
    @Input
    String minMemoryFactor = ""
    @Input
    String youngGenMemory = ""
    @Input
    String youngGenMemoryFactor = ""
    @Input
    String maxPermMemory = ""
    @Input
    String maxPermMemoryFactor = ""
    @Input
    FileCollection classpath
    @Input
    String extraClasspath = ""
    @Input
    String mainClass = ""
    @Input
    String args = ""
    @Input
    boolean heapDump
    @Input
    boolean gcDetails
    @Input
    String filePath
    @Input
    String startupTimeoutSec = "0"
    @Input
    String startupPollIntervalSec = "10"
    @Input
    String dbsvcInitFlagFile = "/var/run/storageos/dbsvc_initialized"

    String getFileName() {
        return fileName ?: scriptName
    }

    String getFilePath() {
        return filePath ?: "/bin/"
    }

    @OutputFile
    File getOutputFile() {
        return project.file("${project.buildDir}${getFilePath()}${getFileName()}")
    }
    
    String getAllJvmArgs() {
        def allJvmArgs = []
        if (getHeapDump()) {
            allJvmArgs.add("-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${getInstallDir()}/logs/${getScriptName()}-\$\$.hprof")
        }
        if (getGcDetails()) {
            allJvmArgs.add("-XX:+PrintGCDateStamps -XX:+PrintGCDetails")
        }
        if (getJvmArgs()) {
            if (allJvmArgs.size() > 0) {
                allJvmArgs.add("\\\n    ")
            }
            allJvmArgs.add(getJvmArgs())
        }
        return allJvmArgs.join(" ")
    }
    
    String getJavaArgs() {
        return "${getMainClass()} ${getArgs()}"
    }
   
    String getExtraCode() {
        String extraCode = ""
        if (getMaxMemoryFactor() || getMinMemoryFactor() || getYoungGenMemoryFactor()) {
            extraCode += """
_get_mem_total_mb() {
    local a b c && read a b c </proc/meminfo && echo \$(( \${b} / 1024 ))
}

_get_mem_fraction_mb() {
    case \${#} in
        1) echo "\${1}";;
        2) awk -v a="\${1%m}" -v b="\${2}" -v mem="\${MEM_TOTAL_MB:-\$(_get_mem_total_mb)}" \\
               'BEGIN { printf("%.0f\\n", a + b * mem); }';;
        *) return 2
    esac
}\n\n"""
        }
        extraCode += """
_wait_for_dbsvc_init() {
    start_time=\$(date +%s)
    while [ ! -e ${getDbsvcInitFlagFile()} -a \$((\$(date +%s) - \${start_time})) -lt ${getStartupTimeoutSec()} ] ; do
        sleep ${getStartupPollIntervalSec()}
    done
}

_wait_for_dbsvc_init
[ "\${0##*/}" = "dbsvc" ] && rm -f ${getDbsvcInitFlagFile()}
"""
        return extraCode
    }
    
    String getMemoryValue(String value, String factor) {
        if (factor) {
            String baseValue = value ?: "0"
            return "\$(_get_mem_fraction_mb ${baseValue} ${factor})m"
        }
        return value
    }
    
    String getMaxMemoryArg() {
        getMemoryValue(getMaxMemory(), getMaxMemoryFactor())
    }
    
    String getMinMemoryArg() {
        getMemoryValue(getMinMemory(), getMinMemoryFactor())
    }
    
    String getYoungGenMemoryArg() {
        getMemoryValue(getYoungGenMemory(), getYoungGenMemoryFactor())
    }
    
    String getMaxPermMemoryArg() {
        getMemoryValue(getMaxPermMemory(), getMaxPermMemoryFactor())
    }

    String getMemoryArgs() {
        def memoryArgs = []
        if (getMaxMemoryArg()) {
            memoryArgs.add("-Xmx${getMaxMemoryArg()}")
        }
        if (getMinMemoryArg()) {
            memoryArgs.add("-Xms${getMinMemoryArg()}")
        }
        if (getYoungGenMemoryArg()) {
            memoryArgs.add("-Xmn${getYoungGenMemoryArg()}")
        }
        if (getMaxPermMemoryArg()) {
            memoryArgs.add("-XX:MaxPermSize=${getMaxPermMemoryArg()}")
        }
        return memoryArgs.join(" ")
    }
    
    FileCollection getClasspath() {
        classpath ?: project.files()
    }
    
    String getFullClasspath() {
        def classpath = getClasspath()
        def extraClasspath = getExtraClasspath()
        
        // No classpath defined
        if (classpath.isEmpty() && extraClasspath) {
            return ""
        }
        def cp = ["${getInstallDir()}/conf", "\${LIB_DIR}"]
        cp.addAll(classpath.collect { "\${LIB_DIR}/${it.name}" })
        if (extraClasspath) {
            cp.add(extraClasspath)
        }
        cp.add("\${LIB_DIR}/tools.jar")
        if (getFileName().startsWith("controllersvc")) {
            cp.add("/data/drivers/*")
        }
        return cp.join(":")
    }
    
    String getScript() {
        """#!/bin/sh
LIB_DIR="${getInstallDir()}/lib"
export JAVA_HOME="${getJavaHome()}"
export PATH="\${JAVA_HOME}/bin:${getInstallDir()}/bin:/bin:/usr/bin"
${getExtraDefines()}
export CLASSPATH="${getFullClasspath()}"
${getExtraCode()}
# Save PID
pid_file_name=\${0##*/}
pid_file_name=\${pid_file_name%%-coverage}
echo \$\$ >/var/run/storageos/\${pid_file_name}.pid


exec -a \$0 \${JAVA_HOME}/bin/java -ea -server -d64 ${getMemoryArgs()} -Dproduct.home="\${PRODUCT_HOME:-${getInstallDir()}}\" \\
     ${getAllJvmArgs()} \\
     ${getJavaArgs()}"""
    }
    
    @TaskAction
    def generate() {
        def script = getScript()
        def outputFile = getOutputFile()
        logger.debug("Generating script {}: \n{}", outputFile, script)
        outputFile.write(script)
    }
}
