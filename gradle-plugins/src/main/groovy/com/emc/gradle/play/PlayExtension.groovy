package com.emc.gradle.play

import org.gradle.api.*
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection

class PlayExtension {
    private final Project project
    String play
    String installDir
    String compileDir
    String compileArgs
    String testArgs
    String version
    String appDir
    List<String> modules
    String logFile
    String libDir
    CopySpec distribution
    String mavenRepo
    String contributedModulesDescriptor
    String contributedModulesArtifact
    File ivyHome
    FileCollection ivyCacheFiles
    String baseLauncherJvmArgs
    NamedDomainObjectContainer launchers
    
    PlayExtension(Project project) {
        this.project = project
        this.installDir = "${project.buildDir}/play"
        this.compileDir = "${project.buildDir}/compile"
        this.compileArgs = ""
        this.testArgs = ""
        this.version = "1.2.5"
        this.appDir = "."
        this.modules = [] as List
        this.libDir = "lib"
        this.logFile = "logs/application.log"
        this.distribution = project.copySpec {}
        this.ivyHome = project.rootProject.file(".ivy2").absoluteFile
        this.ivyCacheFiles = project.fileTree("${ivyHome}/cache") {
            include "play-*"
            include "resolved-play-*"
        }

        this.baseLauncherJvmArgs = "-server -Xms128M -Xmx768m -XX:PermSize=128m -XX:MaxPermSize=256m " +
            "-XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -Dplay.debug=yes -Dapple.awt.UIElement=true"
        this.launchers = project.container(LaunchConfiguration)
        this.launchers.create(project.name)
    }
    
    public void launchers(Closure config) {
        this.launchers.configure(config)
    }
    
    static class LaunchConfiguration {
        final String name
        String launcherName
        String jvmArgs = ""
        
        public LaunchConfiguration(String name) {
            this.name = name
        }
        
        String getLauncherName() {
            return launcherName ?: name
        }
    }
}