package com.emc.gradle.devkit

import com.emc.gradle.remote.AbstractRemoteExtension;
import com.emc.gradle.remote.AbstractRemotePlugin
import org.gradle.api.Project

/**
 * This plugin adds support for controlling the services on a devkit and deploying updated libraries.
 * Only the root project should have this plugin applied.  The root project will be given the service
 * control tasks (<tt>startServices</tt>, <tt>stopServices<tt>), and all projects will be given the
 * <tt>remoteCopy and <tt>deploy</tt> tasks.
 * 
 * Running any deploy tasks will cause the services to be stopped before and started after.  When
 * multiple deploy tasks are run, the stop and start of services will only happen once.
 */
class DevkitPlugin extends AbstractRemotePlugin {
    static final String EXTENSION_NAME = "devkit"
    
    DevkitPlugin() {
        super(EXTENSION_NAME, DevkitExtension)
    }
    
    @Override
    protected void configureExtension(Project project, AbstractRemoteExtension config) {
        String port = projectProperty(project, "devkitPort")
        config.host = projectProperty(project, "devkit")
        config.port = port ? Integer.parseInt(port) : 22
        config.user = projectProperty(project, "devkitUser") ?: "root"
        config.password = projectProperty(project, "devkitPassword") ?: "ChangeMe"
        config.rootDir = "/opt/storageos"
        config.serviceName = "storageos"
        config.confSrc = "src/conf"
        config.sleepAfterStop = 10000
    }
}
