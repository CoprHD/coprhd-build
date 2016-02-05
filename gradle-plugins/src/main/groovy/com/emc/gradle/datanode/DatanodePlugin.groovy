package com.emc.gradle.datanode

import org.gradle.api.Project;

import com.emc.gradle.remote.AbstractRemoteExtension;
import com.emc.gradle.remote.AbstractRemotePlugin

/**
 * This plugin adds support for controlling the services on a datanode and deploying updated libraries.
 * Only the root project should have this plugin applied.
 * 
 * Running any deploy tasks will cause the services to be stopped before and started after.  When
 * multiple deploy tasks are run, the stop and start of services will only happen once.
 */
class DatanodePlugin extends AbstractRemotePlugin {
    static final String EXTENSION_NAME = "datanode"
    
    DatanodePlugin() {
        super(EXTENSION_NAME, DatanodeExtension)
    }
    
    @Override
    protected void configureExtension(Project project, AbstractRemoteExtension config) {
        String port = projectProperty(project, "datanodePort")
        config.host = projectProperty(project, "datanode")
        config.port = port ? Integer.parseInt(port) : 22
        config.user = projectProperty(project, "datanodeUser") ?: "root"
        config.password = projectProperty(project, "datanodePassword") ?: "ChangeMe"
        config.rootDir = "/opt/storageos"
        config.serviceName = "storageos-dataservice"
        config.confSrc = "src/conf"
        config.sleepAfterStop = 10000
    }
}
