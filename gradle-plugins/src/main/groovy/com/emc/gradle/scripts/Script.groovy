package com.emc.gradle.scripts

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection

public class Script {
    final String name
    String scriptName
    String jdkHome
    String installDir
    String extraDefines = ""
    String maxMemory = ""
    String maxMemoryFactor = ""
    String minMemory = ""
    String minMemoryFactor = ""
    String youngGenMemory = ""
    String youngGenMemoryFactor = ""
    String maxPermMemory = ""
    String maxPermMemoryFactor = ""
    FileCollection classpath
    String extraClasspath = ""
    String jvmArgs = ""
    String log4jConfiguration = ""
    String mainClass = ""
    String args = "\"\${@}\""
    String filePath
    String startupTimeoutSec = "0"
    String startupPollIntervalSec = "10"
    String dbsvcInitFlagFile = "/var/run/storageos/dbsvc_initialized"
    
    // Whether the script is a service script (generate debug and coverage scripts)
    boolean service = true
    String debugPort = "0"
    String debugJvmArgs = ""
    String coverageJvmArgs = ""
    
    // Whether to use heap dumps 
    boolean heapDump = true
    // Whether to print gc details
    boolean gcDetails = true
    
    Script(String name) {
        this.name = name
        scriptName = name
    }
    
    String getJvmArgs() {
        if (log4jConfiguration) {
            jvmArgs ?: "-Dlog4j.configuration="+log4jConfiguration
        } else {
            jvmArgs ?: "-Dlog4j.configuration="+scriptName+"-log4j.properties"
        }
    }
    
    String getDebugJvmArgs() {
        debugJvmArgs ?: "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="+debugPort
    }
}
