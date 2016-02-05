package com.emc.gradle.remote

import org.gradle.api.*

import com.emc.gradle.GradleUtils

abstract class AbstractRemotePlugin implements Plugin<Project> {
    static final String START_SERVICES_SUFFIX = "StartServices"
    static final String STOP_SERVICES_SUFFIX = "StopServices"
    static final String RESTART_SERVICES_SUFFIX = "RestartServices"
    static final String REMOTE_COPY_JAR_SUFFIX = "RemoteCopyJar"
    static final String REMOTE_COPY_CONF_SUFFIX = "RemoteCopyConf"
    static final String DEPLOY_SUFFIX = "Deploy"
    static final String DEPLOY_DEPS_SUFFIX = "DeployDeps"
    static final String DEPLOY_ALL_SUFFIX = "DeployAll"
    static final String CLEAR_LOGS_SUFFIX = "ClearLogs"
    static final String CLEAR_DATA_SUFFIX = "ClearData"
    
    final String extensionName
    final Class<? extends AbstractRemoteExtension> extensionType
    String hostType
    String taskGroup
    
    AbstractRemotePlugin(String extensionName, Class<? extends AbstractRemoteExtension> extensionType) {
        this.extensionName = extensionName
        this.extensionType = extensionType
        this.hostType = extensionName
        this.taskGroup = extensionName
    }
    
    @Override
    public void apply(Project project) {
        def config = project.extensions.create(extensionName, extensionType)
        configureExtension(project, config)
        
        def stopServices = addStopServicesTask(project, config)
        def startServices = addStartServicesTask(project, config)
        configureTasks(project, config, stopServices, startServices)
    }

    protected abstract void configureExtension(Project project, AbstractRemoteExtension config)

    protected void configureTasks(Project project, AbstractRemoteExtension config, Task stopTask, Task startTask) {
        // Configure the deployAll task to run between a start/stop
        def deployAll = addDeployAllTask(project, config)
        runWhileStopped(deployAll, stopTask, startTask)

        // Restart services task
        def restartTask = addRestartServicesDummyTask(project, config)
        runWhileStopped(restartTask, stopTask, startTask)

        // Clear Logs Task
        def clearLogs = addClearLogsTask(project, config)
        runWhileStopped(clearLogs, stopTask, startTask)

        // Clear DB Task
        def clearDb = addClearDataTask(project, config)
        runWhileStopped(clearDb, stopTask, startTask)

        // Configure remote copy/deploy tasks on all subprojects
        project.subprojects { p->
            def remoteCopyJar = addRemoteCopyJarTask(p, config)
            runBetween(remoteCopyJar, stopTask, startTask)

            def remoteCopyConf = addRemoteCopyConfTask(p, config)
            runBetween(remoteCopyConf, stopTask, startTask)

            def deploy = addDeployTask(p, config)
            deploy.dependsOn(stopTask, remoteCopyJar, remoteCopyConf, startTask)

            def deployDeps = addDeployDepsTask(p, config)
            deployDeps.dependsOn(deploy)
        }
    }
    
    protected void runWhileStopped(task, stopTask, startTask) {
        task.dependsOn(stopTask)
        task.mustRunAfter(stopTask)
        task.finalizedBy(startTask)
    }
    
    protected void runBetween(task, stopTask, startTask) {
        task.mustRunAfter(stopTask)
        startTask.mustRunAfter(task)
    }

    protected String taskName(String suffix) {
        return extensionName + suffix
    }
    
    def createCopyTask(Project project, AbstractRemoteExtension config, String baseName) {
        def task = project.task(taskName(baseName), type:RemoteCopyTask)
        task.group = taskGroup
        addRemoteProperties(task, config)
        return task
    }
    
    def createExecuteTask(Project project, AbstractRemoteExtension config, String baseName) {
        def task = project.task(taskName(baseName), type:RemoteExecuteTask)
        task.group = taskGroup
        addRemoteProperties(task, config)
        return task
    }
    
    def addStartServicesTask(Project project, AbstractRemoteExtension config) {
        def task = createExecuteTask(project, config, START_SERVICES_SUFFIX)
        task.description = "Starts the services on a ${hostType}"
        task.conventionMapping.command = { "service " + config.serviceName + " start" }
        return task
    }

    def addStopServicesTask(Project project, AbstractRemoteExtension config) {
        def task = createExecuteTask(project, config, STOP_SERVICES_SUFFIX)
        task.description = "Stops the services on a ${hostType}"
        task.conventionMapping.command = { "service " + config.serviceName + " stop" }
        task.doLast {
            if (config.sleepAfterStop) {
                logger.lifecycle("Waiting while services stop")
                sleep config.sleepAfterStop
            }
        }
        return task
    }

    def addRestartServicesDummyTask(Project project, AbstractRemoteExtension config) {
        def task = project.task(taskName(RESTART_SERVICES_SUFFIX))
        task.group = taskGroup
        task.description = "Restarts all services on a ${hostType}"
        return task
    }
    
    def addDeployAllTask(Project project, AbstractRemoteExtension config) {
        def task = createCopyTask(project, config, DEPLOY_ALL_SUFFIX)
        task.description = "Deploys all jars to a ${hostType} and restarts the services"
        task.conventionMapping.destDir = { config.rootDir + "/lib" }
        task.conventionMapping.files = { 
            project.files(project.subprojects.configurations.runtime.allArtifacts.files)
        }
        return task
    }
    
    def addDeployTask(Project project, AbstractRemoteExtension config) {
        def task = project.task(taskName(DEPLOY_SUFFIX))
        task.description = "Deploys the jar and configuration to a ${hostType} and restarts the services"
        task.group = taskGroup
        return task
    }

    def addDeployDepsTask(Project project, AbstractRemoteExtension config) {
        def deployDepsName = taskName(DEPLOY_DEPS_SUFFIX)
        def task = project.task(deployDepsName)
        task.description = "Deploys the jar and configuration (including dependencies) to a ${hostType} and restarts the services"
        task.group = taskGroup
        project.afterEvaluate {
            GradleUtils.getProjectDependencies(project, 'compile').each {
                task.dependsOn("${it.path}:${deployDepsName}")
            }
        }
        return task
    }

    def addRemoteCopyJarTask(Project project, AbstractRemoteExtension config) {
        def task = createCopyTask(project, config, REMOTE_COPY_JAR_SUFFIX)
        task.description = "Copies the jar to a ${hostType} and restarts the services"
        task.conventionMapping.destDir = { config.rootDir + "/lib" }
        task.conventionMapping.files = { project.configurations.runtime.artifacts.files }
        return task
    }
    
    def addRemoteCopyConfTask(Project project, AbstractRemoteExtension config) {
        def task = createCopyTask(project, config, REMOTE_COPY_CONF_SUFFIX)
        task.description = "Copies the configuration to a ${hostType} and restarts the services"
        task.conventionMapping.destDir = { config.rootDir + "/conf" }
        task.conventionMapping.files = { project.fileTree(config.confSrc) }
        task.onlyIf { project.file(config.confSrc).isDirectory() }
        return task
    }

    def addClearLogsTask(Project project, AbstractRemoteExtension config) {
        def task = createExecuteTask(project, config, CLEAR_LOGS_SUFFIX)
        task.description = "Deletes all log files on a ${hostType}"
        task.conventionMapping.command = { "rm -f " + config.rootDir + "/logs/*" }
        return task
    }

    def addClearDataTask(Project project, AbstractRemoteExtension config) {
        def task = createExecuteTask(project, config, CLEAR_DATA_SUFFIX)
        task.description = "Deletes the database and zookeeper files on a ${hostType}"
        task.command = "rm -rf /data/db/1/* /data/geodb/1/* /data/zk/version-2/*"
        return task
    }
    
    void addRemoteProperties(AbstractRemoteTask task, AbstractRemoteExtension config) {
        task.conventionMapping.with {
            host = { config.host }
            port = { config.port }
            user = { config.user }
            password = { config.password }
        }
    }
    
    protected String projectProperty(Project project, String name) {
        if (project.hasProperty(name) || project.ext.has(name)) {
            return project[name]
        }
        else {
            return null
        }
    }
}
