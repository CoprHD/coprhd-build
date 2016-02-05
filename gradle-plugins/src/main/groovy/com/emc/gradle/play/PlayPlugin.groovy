package com.emc.gradle.play

import static org.gradle.api.plugins.BasePlugin.ASSEMBLE_TASK_NAME

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.publish.maven.MavenPublication
import com.emc.gradle.GradleUtils

import de.undercouch.gradle.tasks.download.Download

/**
 * This plugin adds support for compling Play applications.  By default, the playframework
 * will be downloaded and installed into the build directory before any other play related
 * tasks.  When doing a full build, the play framework will always be downloaded for
 * packaging into the final distribution.  For most normal use, a local play installation
 * can be used. Setting the installDir property of the play extension (or by setting the
 * 'playDir' project property) will either use an existing installation, or will download 
 * the play framework into the specified directory for later reuse.
 * 
 * Most developers will want to set the playDir property in the 
 * ${user.home}/.gradle/gradle.properties file so that play is not continually unpacked
 * and setup in order to run any of the tasks.
 */
class PlayPlugin implements Plugin<Project> {
    static final String PLAY_GROUP = 'play'
    static final String ECLIPSE_TASK_NAME = 'eclipse'
    static final String IDEA_TASK_NAME = 'idea'
    static final String COMPILE_TASK_NAME = 'compile'
    static final String TEST_TASK_NAME = 'test'
    static final String BUILD_TASK_NAME = 'build'
    static final String PLAY_DOWNLOAD_TASK_NAME = 'playDownload'
    static final String PLAY_INSTALL_TASK_NAME = 'playInstall'
    static final String PLAY_CONFIGURE_TASK_NAME = 'playConfigure'
    static final String PLAY_CLEAN_IVY_TASK_NAME = 'playCleanIvy'
    static final String PLAY_PREPARE_TASK_NAME = 'playPrepare'
    static final String PLAY_COMPILE_TASK_NAME = 'playCompile'
    static final String PLAY_TEST_TASK_NAME = 'playTest'
    static final String PLAY_ASSEMBLE_TASK_NAME = 'playAssemble'
    static final String PLAY_ARCHIVE_TASK_NAME = 'playArchive'
    
    static final String PLAY_LOCAL_TASK_NAME = 'playLocal'
    static final String PLAY_SETUP_TASK_NAME = 'playSetup'
    static final String PLAY_DEPS_TASK_NAME = 'playDeps'
    static final String PLAY_LIBS_TASK_NAME = 'playLibs'
    static final String PLAY_RUN_TASK_NAME = 'playRun'

    void apply(Project project) {
        project.plugins.apply('java')
        project.plugins.apply('eclipse')
        project.plugins.apply('idea')
        project.plugins.apply('maven-publish')

        project.extensions.create('play', PlayExtension, project)

        project.afterEvaluate {
                project.plugins.apply('de.undercouch.download')
        }

        configureJava(project)
        configureDistribution(project)
        addConfigurations(project)
        addTasks(project)
        configureIdea(project)
        configureEclipse(project)
        GradleUtils.addTestCompileDependencies(project)
    }
    
    void configureJava(Project project) {
        disableTask(project, 'compileJava')
        disableTask(project, 'compileTestJava')
        disableTask(project, 'processResources')
        disableTask(project, 'processTestResources')
        disableTask(project, 'classes')
        disableTask(project, 'testClasses')
        disableTask(project, 'jar')
        disableTask(project, 'javadoc')
        disableTask(project, 'test')
        
        // Remove the 'jar' archive from the runtime/archives artifacts 
        project.configurations.runtime.with {
            artifacts.remove artifacts.find { it.archiveTask.is project.tasks.jar }
        }
        project.configurations.archives.with {
            artifacts.remove artifacts.find { it.archiveTask.is project.tasks.jar }
        }
        project.afterEvaluate {
            def play = project.extensions.play
            project.sourceSets {
                main {
                    java {
                        srcDirs = [ project.file("${play.appDir}/app") ]
                        play.modules.each {
                            srcDir project.file("${it}/app")
                        }
                        exclude "views/**"
                    }
                }
            }
        }
    }

    void disableTask(Project project, String taskName) {
        def task = project.tasks.findByName(taskName)
        if (task) {
            task.enabled = false
        }
    }
    
    void configureDistribution(Project project) {
        def playExt = project.extensions.play
        playExt.distribution.with {
            from { "${playExt.compileDir}/${playExt.appDir}" } {
                include 'app/**'
                include 'conf/**'
                include 'lib/**'
                include 'modules/**'
                include 'precompiled/**'
                include 'public/**'
                exclude '**/*.java'
            }
        }
    }

    void addConfigurations(Project project) {
        addConfiguration(project, 'playframework')
        addConfiguration(project, 'play')
        addConfiguration(project, 'playModules')
    }

    void addConfiguration(Project project, String name) {
        GradleUtils.getOrCreateConfiguration(project, name)
    }

    void addTasks(Project project) {
        addPlayCleanIvyTask(project)
        addPlayInstallTask(project)
        addPlayConfigureTask(project)
        addPlayCompileTask(project)
        addPlayTestTask(project)
        addPlayAssembleTask(project)
        addPlayArchiveTask(project)
        addPlaySetupTask(project)
        addPlayDepsTask(project)
        addPlayLibsTask(project)
        addPlayRunTask(project)
    }

    void addPlayCleanIvyTask(Project project) {
        def task = project.task(PLAY_CLEAN_IVY_TASK_NAME, type:Delete) {
            group = PLAY_GROUP
            delete { project.extensions.play.ivyCacheFiles }
        }
    }
    
    void addPlayInstallTask(Project project) {
        def playExt = project.extensions.play

        project.afterEvaluate {
                project.dependencies { playframework "play:play:${playExt.version}@zip" }
        }

        def task = project.task(PLAY_INSTALL_TASK_NAME, type:Copy) {
            group = PLAY_GROUP
            project.afterEvaluate {
                    from { project.zipTree(project.configurations.playframework.singleFile) }
                	into playExt.installDir
            }

            doLast {
                project.file("${project.buildDir}/play/play1-${playExt.version}").renameTo("${project.buildDir}/play/play-${playExt.version}")

                def playDir = "${project.buildDir}/play/play-${playExt.version}"

                // needed for 1.3.1
                project.file("${playDir}/play").setExecutable(true, false)

                playExt.play = project.file("${playDir}/play").absolutePath
                def depsFile = project.file("${playDir}/framework/dependencies.yml")
                updateDependenciesConfig(project, depsFile)
            }
        }
        project.tasks[ASSEMBLE_TASK_NAME].dependsOn(PLAY_INSTALL_TASK_NAME)
    }
    
    /**
     * Rewrites the dependendencies.yml file to point the to configured maven repo/modules repo (if provided).
     */
    void updateDependenciesConfig(Project project, File depsFile) {
        def playExt = project.extensions.play
        if (playExt.mavenRepo || playExt.contributedModulesDescriptor || playExt.contributedModulesArtifact) {
            def dependencies = depsFile.text
            def descExpr = ""
            def artifExpr = ""

            descExpr = "( +)descriptor:( *)\"https://www.playframework.com/modules.*"
            artifExpr = "( +)artifact:( *)\"https://www.playframework.com/modules.*"

            // Using a different repository as the main maven central repository
            if (playExt.mavenRepo) {
                dependencies = dependencies.replaceAll(
                        "( +)type: *iBiblio",
                        "\$1type: iBiblio\n\$1root: \"${playExt.mavenRepo}\"")
            }
            // When using an alternate modules repository change the descriptor pattern
            if (playExt.contributedModulesDescriptor) {
                dependencies = dependencies.replaceAll(
                        descExpr,
                        "\$1descriptor:\$2\"${playExt.contributedModulesDescriptor}\"")
            }
            // When using an alternate modules repository change the artifact pattern
            if (playExt.contributedModulesArtifact) {
                dependencies = dependencies.replaceAll(
                        artifExpr,
                        "\$1artifact:\$2\"${playExt.contributedModulesArtifact}\"")
            }
            depsFile.text = dependencies
        }
    }
    
    void addPlayConfigureTask(Project project) {
        def playExt = project.extensions.play
        def task = project.task(PLAY_CONFIGURE_TASK_NAME, dependsOn:PLAY_INSTALL_TASK_NAME) {
            doLast {
                def playDir = "${playExt.installDir}/play-${playExt.version}"
                playExt.play = project.file("${playDir}/play").absolutePath
                if (playExt.play.contains(" ")) {
                    playExt.play = "\"${playExt.play}\""
                }
            }
        }
    }
    
    void addPlayCompileTask(Project project) {
        // Copies the play application into the compile directory
        def prepare = project.task(PLAY_PREPARE_TASK_NAME)
        prepare.dependsOn PLAY_CLEAN_IVY_TASK_NAME, PLAY_CONFIGURE_TASK_NAME
        prepare.dependsOn project.configurations.runtime.buildDependencies
        project.afterEvaluate {
            prepare.with {
                ext.config = project.extensions.play
                ext.compileDir = project.file(config.compileDir)
                ext.srcFiles = project.fileTree(project.projectDir) {
                    if (config.appDir != '.') {
                       include "${config.appDir}/**"
                    }
                    config.modules.each { module ->
                        include "${module}/**"
                        exclude "${module}/lib"
                    }
                    exclude 'build/**'
                    exclude 'lib/**'
                    exclude '**/modules/**'
                    exclude '**/eclipse/**'
                    exclude '**/tmp/**'
                    exclude '**/precompiled/**'
                    exclude '**/commands.pyc'
                    exclude '**/.classpath'
                    exclude '**/.project'
                    exclude '**/.settings'
                    exclude '**/*.iml'
                }
                ext.destFiles = project.fileTree(compileDir) {
                    exclude '**/tmp/**'
                    exclude '**/precompiled/**'
                    exclude '**/commands.pyc'
                }
                
                inputs.files srcFiles
                outputs.files destFiles
                
                doLast {
                    compileDir.mkdirs()
                    project.copy {
                        from srcFiles
                        into compileDir
                    }
                    new PlayRunner(project, compileDir).playAll("deps --forProd --sync --forceCopy -Divy.home=${config.ivyHome}")
                    project.copy {
                        from project.configurations.runtime
                        from project.configurations.runtime.allArtifacts.files
                        if (config.appDir != '.') {
                            into "${config.compileDir}/${config.appDir}/lib"
                        }
                        else {
                            into "${config.compileDir}/lib"
                        }
                    }

                    // to avoid conflict when using play-1.3.1 which needs asm5
                    def playExt = project.extensions.play
                    ant.delete(file: "${compileDir}/lib/asm-3.1.jar")
                   
                }
            }
        }
        
        // Precompiles play application
        def task = project.task(PLAY_COMPILE_TASK_NAME, dependsOn: prepare) {
            group = PLAY_GROUP
            ext.config = project.extensions.play
            ext.compileDir = project.file(config.compileDir)
            ext.srcFiles = project.fileTree(compileDir) {
                exclude '**/tmp/**'
                exclude '**/precompiled/**'
                exclude '**/commands.pyc'
            }
            ext.destDir = project.file("${compileDir}/${config.appDir}/precompiled")
            
            inputs.files srcFiles
            outputs.dir destDir
            doFirst {
                ant.delete(dir:destDir.path)
                ext.logFile = project.file("${project.buildDir}/${->config.logFile}")
                logFile.delete();
            }
            doLast {
                try {
                    new PlayRunner(project, compileDir).playApp("precompile --%prod ${config.compileArgs}")
                }
                catch (e) {
                    if (logFile.isFile()) {
                        project.logger.error(logFile.text)
                    }
                    throw e
                }
            }
        }
        project.task(COMPILE_TASK_NAME).dependsOn(PLAY_COMPILE_TASK_NAME)
    }

    void addPlayTestTask(Project project) {
        // Runs all play tests
        def task = project.task(PLAY_TEST_TASK_NAME, dependsOn:PLAY_COMPILE_TASK_NAME) {
            group = PLAY_GROUP
            ext.config = project.extensions.play
            ext.compileDir = project.file(config.compileDir)
            
            doLast {
                new PlayRunner(project, compileDir).playApp("auto-test ${config.testArgs}")
            }
        }
        project.tasks[TEST_TASK_NAME].dependsOn(PLAY_TEST_TASK_NAME)
    }

    void addPlayAssembleTask(Project project) {
        // Creates an archive of the play application
        def task = project.task(PLAY_ASSEMBLE_TASK_NAME, type:Copy, dependsOn:COMPILE_TASK_NAME) {
            group = PLAY_GROUP
            description = "Assembles the Play application"
            
            with project.extensions.play.distribution
            into project.file("${project.buildDir}/dist")
        }
        project.tasks[ASSEMBLE_TASK_NAME].dependsOn(PLAY_ASSEMBLE_TASK_NAME)
    }

    void addPlayArchiveTask(Project project) {
        // Creates an archive of the play application
        def task = project.task(PLAY_ARCHIVE_TASK_NAME, type:Zip, dependsOn:PLAY_ASSEMBLE_TASK_NAME) {
            group = PLAY_GROUP
            version = project.version
            from project.file("${project.buildDir}/dist")
        }
        project.tasks[ASSEMBLE_TASK_NAME].dependsOn(PLAY_ARCHIVE_TASK_NAME)

        project.artifacts { archives task }
        project.publishing.publications {
            maven(MavenPublication) {
                artifact task

                pom.withXml { xml->
                    GradleUtils.resolvePom(project, xml)
                }
            }
        }
    }

    /**
     * Gets the local play dir.  If the playDir property is set it will be used, otherwise it will go into the 
     * gradle user home directory.    
     */
    String getLocalPlayDir(Project project) {
        def playExt = project.extensions.play
        def gradleUserHome = project.gradle.gradleUserHomeDir
        project.ext.has('playDir') ? project.playDir : "${gradleUserHome}/play-${playExt.version}"
    }
    
    /**
     * Runs a local play command against the application
     */
    String playLocalApp(Project project, String command) {
        def playExt = project.extensions.play
        def playRunner = new PlayRunner(project)
         
        playRunner.run(project.file(playExt.appDir), "${project.localPlay} ${command}")
    }
    
    /**
     * Runs a local play command against the modules.
     */
    List<String> playLocalModules(Project project, String command) {
        def playExt = project.extensions.play
        def playRunner = new PlayRunner(project)
         
        playExt.modules.collect { module ->
            playRunner.run(project.file(module), "${project.localPlay} ${command}")
        } as List
    }

    /**
     * Runs a local play command against all modules
     */
    List<String> playLocalAll(Project project, String command) {
        playLocalModules(project, command) << playLocalApp(project, command)
    }
    
    void addPlaySetupTask(Project project) {
        // Sets up the local play installation
        def setupTask = project.task(PLAY_SETUP_TASK_NAME) {
            doLast {
                def playDir = getLocalPlayDir(project)
                project.ext.localPlay = project.file("${playDir}/play").absolutePath
                if (project.localPlay.contains(" ")) {
                    project.localPlay = "\"${project.localPlay}\""
                }
            }
        }
        
        // Determine if local install is required
        project.afterEvaluate {
            def playExt = project.extensions.play
            def playDir = getLocalPlayDir(project)
            
            // Add all play framework libraries to the play configuration
            project.dependencies {
                play project.fileTree(playDir) {
                    include "framework/play-*.jar"
                    include "framework/lib/*.jar"
                }
            }

            if (!project.file("${playDir}/play").isFile()) {
                def localInstall = project.task(PLAY_LOCAL_TASK_NAME, type:Copy, dependsOn:PLAY_INSTALL_TASK_NAME) {
                    from { "${playExt.installDir}/play-${playExt.version}" }
                    into { playDir }
                }
                setupTask.dependsOn(localInstall)
            }
            
            // Add the sourcepath for the play jar
            project.eclipse.classpath.file {
                whenMerged { classpath->
                    classpath.entries.findAll { entry ->
                        entry.path.endsWith("play-${playExt.version}.jar")
                    }.each { entry->
                        entry.sourcePath = fileReferenceFactory.fromFile(
                            project.file("${playDir}/framework/src"))
                    }
                }
            }
        }
    }
    
    void addPlayDepsTask(Project project) {
        def playExt = project.extensions.play
        def task = project.task(PLAY_DEPS_TASK_NAME, dependsOn:PLAY_SETUP_TASK_NAME) {
            doLast {
                playLocalAll(project, "clean")
                def outputs = playLocalAll(project, "deps --sync  -Divy.home=${playExt.ivyHome}")
                // Check the output for a missing dependencies message
                outputs.each { output ->
                    if (output?.contains("Some dependencies are still missing")) {
                        project.logger.error(output)
                        throw new GradleException("Missing module dependencies")
                    }
                }
                // Configure source directories for modules, add module libs to dependencies
                def moduleDirs = project.file("${playExt.appDir}/modules").listFiles()?.findAll { it.isDirectory() }
                moduleDirs.each { module ->
                    project.sourceSets.main.java {
                        srcDir "${module.absolutePath}/app"
                    }
                    project.dependencies {
                        playModules project.fileTree("${module.absolutePath}/lib") {
                            include "*.jar"
                        }
                    }
                }
            }
        }
    }
    
    void addPlayLibsTask(Project project) {
        def task =  project.task(PLAY_LIBS_TASK_NAME, type: Copy, dependsOn:PLAY_DEPS_TASK_NAME) {
            group = PLAY_GROUP
            from { project.configurations.runtime }
            from { project.configurations.runtime.allArtifacts.files }
            into { project.extensions.play.libDir }
        }
    }
    
    void addPlayRunTask(Project project) {
        // Runs play application
        def task = project.task(PLAY_RUN_TASK_NAME, dependsOn:PLAY_LIBS_TASK_NAME) {
            group = PLAY_GROUP
            doLast {
                if (project.ext.has('playId')) {
                    playLocalApp(project, "run --%${project.playId}")
                }
                else {
                    playLocalApp(project, "run")
                }
            }
        }
    }
    
    void configureIdea(Project project) {
        project.afterEvaluate {
            project.idea {
                module {
                    scopes.COMPILE.plus += [project.configurations.play, project.configurations.playModules]
                }
            }

            project.rootProject.idea.project.ipr.withXml { provider ->
                def playExt = project.extensions.play
                def playDir = project.file(getLocalPlayDir(project))
                def playJar = project.fileTree(dir: playDir, include: "framework/play-*.jar").singleFile
                def appDir = "${project.projectDir.absolutePath}/${playExt.appDir}"
                def component = provider.node.find { it.attributes()["name"] == "ProjectRunConfigurationManager" } ?:
                                provider.node.appendNode('component', [name: "ProjectRunConfigurationManager"])

                project.play.launchers.all { launcher->
                    def builder = new groovy.util.NodeBuilder()
                    component.append builder.configuration(name: "${launcher.getLauncherName()}", type: "Application", factoryName: "Application", singleton: true) {
                        option(name: "MAIN_CLASS_NAME", value: "play.server.Server")
                        option(name: "VM_PARAMETERS", value: "${playExt.baseLauncherJvmArgs} " +
                                "-javaagent:\"${playJar}\" -Dapplication.path=\"${appDir}\" "+
                                "-Djava.endorsed.dirs=\"${playDir}/framework/endorsed\" ${launcher.jvmArgs}")
                        option(name: "WORKING_DIRECTORY", value: "file://${appDir}")
                        module(name: project.name)
                        method() {
                            option(name: "Make", enabled: false)
                        }
                    }
                }
            }
        }

        project.rootProject.tasks['ideaProject'].dependsOn(":${project.name}:${PLAY_DEPS_TASK_NAME}")
        project.tasks['ideaModule'].dependsOn(PLAY_DEPS_TASK_NAME)
    }
    
    void configureEclipse(Project project) {
        def playExt = project.extensions.play
        project.afterEvaluate {
            project.eclipse {
                classpath {
                    plusConfigurations += [project.configurations.play, project.configurations.playModules]
                    defaultOutputDir = project.file("${playExt.appDir}/eclipse/classes")
                }
            }
        }
        playExt.launchers.all { launcher->
            def task = project.task("${launcher.name}Launcher")
            task.doLast {
                def applicationPath = '${workspace_loc:'+project.name+'}'
                if (playExt.appDir != '.') {
                    applicationPath += '/'+playExt.appDir
                }
                def playDir = project.file(getLocalPlayDir(project))
                def playJar = project.fileTree(dir:playDir, include:"framework/play-*.jar").singleFile
                def jvmArgs = "${playExt.baseLauncherJvmArgs} "+
                    "-javaagent:&quot;${playJar}&quot; " +
                    "-Dapplication.path=&quot;${applicationPath}&quot; "+
                    "-Djava.endorsed.dirs=&quot;${playDir}/framework/endorsed&quot; "+
                    "${launcher.jvmArgs}"
                def launchConfiguration = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.jdt.launching.localJavaApplication">
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
<listEntry value="/${project.name}"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
<listEntry value="4"/>
</listAttribute>
<booleanAttribute key="org.eclipse.debug.core.appendEnvironmentVariables" value="true"/>
<booleanAttribute key="org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD" value="true"/>
<listAttribute key="org.eclipse.jdt.launching.CLASSPATH">
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry containerPath=&quot;org.eclipse.jdt.launching.JRE_CONTAINER&quot; javaProject=&quot;Portal&quot; path=&quot;1&quot; type=&quot;4&quot;/&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry internalArchive=&quot;/${project.name}/${playExt.appDir}/conf&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#10;"/>
<listEntry value="&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;&#10;&lt;runtimeClasspathEntry id=&quot;org.eclipse.jdt.launching.classpathentry.defaultClasspath&quot;&gt;&#10;&lt;memento exportedEntriesOnly=&quot;false&quot; project=&quot;${project.name}&quot;/&gt;&#10;&lt;/runtimeClasspathEntry&gt;&#10;"/>
</listAttribute>
<booleanAttribute key="org.eclipse.jdt.launching.DEFAULT_CLASSPATH" value="false"/>
<stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="play.server.Server"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="${project.name}"/>
<stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="${jvmArgs}"/>
<stringAttribute key="org.eclipse.jdt.launching.WORKING_DIRECTORY" value="${applicationPath}"/>
</launchConfiguration>
"""
                def launcherFile = project.file("${playExt.appDir}/eclipse/${launcher.getLauncherName()}.launch")
                launcherFile.parentFile.mkdirs()
                launcherFile.text = launchConfiguration
            }
            project.tasks['eclipse'].finalizedBy(task)
        }
        project.tasks['eclipseClasspath'].dependsOn(PLAY_DEPS_TASK_NAME)
    }
}
