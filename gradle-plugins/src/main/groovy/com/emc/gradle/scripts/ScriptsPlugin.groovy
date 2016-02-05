package com.emc.gradle.scripts

import org.gradle.api.*
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import java.io.File

class ScriptsPlugin implements Plugin<Project> {
    static final String SCRIPTS_TASK_NAME = 'scripts'
    
    void apply(Project project) {
        def scripts = project.container(Script)
        project.extensions.scripts = scripts
        
        // Create a top level 'scripts' task for generating all scripts
        def scriptsTask = project.task(SCRIPTS_TASK_NAME)
        scriptsTask.description = 'Generate all scripts'
        
        scripts.all { script->
            project.afterEvaluate {
                addScriptTasks(project, script)
                project.assemble.dependsOn(scriptsTask)
            }
        }
    }
    
    void addScriptTasks(Project project, Script script) {
        // Creates script tasks named after the script
        def task = project.task("${script.name}Script", type:ScriptTask)
        configureScriptTask(project, script, task)

        // Create debug and coverage scripts if this is a service script
        if (script.service) {
            def debugTask = project.task("${script.name}DebugScript", type:ScriptTask)
            configureScriptTask(project, script, debugTask)
            // Override some properties on the debug script task
            project.configure(debugTask) {
                fileName = "${script.scriptName}-debug"
                conventionMapping.jvmArgs = { script.debugJvmArgs + " " + script.jvmArgs }
            }
            
            def coverageTask = project.task("${script.name}CoverageScript", type:ScriptTask)
            configureScriptTask(project, script, coverageTask)
            // Override some properties on the coverage script task
            project.configure(coverageTask) {
                fileName = "${script.scriptName}-coverage"
                conventionMapping.jvmArgs = { script.coverageJvmArgs + " " + script.jvmArgs }
            }
        }
    }
    
    /**
     * Configures the default values for a script task from the script object.
     */
    void configureScriptTask(Project project, Script script, ScriptTask task) {
        // Make the top level 'scripts' task depend on this script task
        project.tasks[SCRIPTS_TASK_NAME].dependsOn(task)
        
        project.configure(task) {
            scriptName = script.scriptName
            javaHome = "${script.jdkHome ?: project.jdkHome}/jre"
            installDir = script.installDir ?: project.installDir
            filePath =  script.filePath 
	    conventionMapping.with {
                extraDefines = { script.extraDefines }
                maxMemory = { script.maxMemory }
                maxMemoryFactor = { script.maxMemoryFactor }
                minMemory = { script.minMemory }
                minMemoryFactor = { script.minMemoryFactor }
                youngGenMemory = { script.youngGenMemory }
                youngGenMemoryFactor = { script.youngGenMemoryFactor }
                maxPermMemory = { script.maxPermMemory }
                maxPermMemoryFactor = { script.maxPermMemoryFactor }
                jvmArgs = { script.jvmArgs }
                heapDump = { script.heapDump }
                gcDetails = { script.gcDetails }
                classpath = {
                    // If a classpath is provided, use it
                    if (script.classpath) {
                        return script.classpath
                    }
                    // Otherwise, build a classpath from the default configurations (runtime, runtimeOnly)
                    return project.configurations.runtime.allArtifacts.files + 
                           project.configurations.runtime + 
                           project.configurations.runtimeOnly
                }
                extraClasspath = { script.extraClasspath }
                mainClass = { script.mainClass }
                args = { script.args }
                startupTimeoutSec = { script.startupTimeoutSec }
                startupPollIntervalSec = { script.startupPollIntervalSec }
                dbsvcInitFlagFile = { script.dbsvcInitFlagFile }
            }
        }
    }
}
