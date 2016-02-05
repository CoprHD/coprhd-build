package com.emc.gradle

import java.io.File
import java.util.Collection
import java.util.Map;

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.result.*
import org.gradle.api.artifacts.maven.MavenPom

public class GradleUtils {
    /**
     * Adds a testCompile dependency for any projects that are compile dependencies.
     * 
     * @param project the project to configure
     */
    public static void addTestCompileDependencies(Project project) {
        project.afterEvaluate {
            getProjectDependencies(project, 'compile').each {
                def dep = project.dependencies.project(path:it.path, configuration:'testRuntime')
                project.dependencies.add('testCompile', dep)
            }
        }
    }

    /**
     * Gets all projects that are declared as dependencies of the project in the named configuration.
     * 
     * @param project the project.
     * @param configurationName the name of the configuration to search.
     * @return the collection of projects that are dependencies.
     */
    public static Collection<Project> getProjectDependencies(Project project, String configurationName) {
        project.configurations[configurationName].allDependencies.findAll { dep ->
            dep instanceof ProjectDependency
        }.collect { dep ->
            project.project(":${dep.dependencyProject.name}")
        }
    }

    /**
     * Searches a number of paths for the first matching file
     */
    public static File findFile(Project project, String... paths) {
        def path = paths.find { 
            try { project.file(it).isFile() } catch (e) {}
        }
        path ? project.file(path).absoluteFile : null
    }

    /**
     * Determines if the operating system is windows.
     * 
     * @return true if the operating system is windows.
     */
    public static boolean isWindows() {
        System.getProperty("os.name").toLowerCase().startsWith("win")
    }
    
    /**
     * Gets a configuration or creates if it doesn't already exist.
     * 
     * @param project
     *        the project.
     * @param configurationName
     *        the name of the configuration.
     * @return the existing or newly created configuration.
     */
    public static Configuration getOrCreateConfiguration(Project project, String configurationName) {
        def config = project.configurations.findByName(configurationName)
        if (config == null) {
            config = project.configurations.create(configurationName)
        }
        return config
    }
    
    /**
     * Removes any unnecessary dependencies from the maven POM.
     * 
     * @param project the project.
     * @param pom the maven POM object.
     */
    public static void removeDuplicateDependencies(Project project, MavenPom pom) {
        def compileDeps = pom.dependencies.findAll { it.scope == 'compile' }.collect{ 
            [it.groupId, it.artifactId]
        }
        // Remove any other scoped dependencies that are already listed as compile dependencies
        pom.dependencies.removeAll { 
            (it.scope == 'test' || it.scope == 'runtime' || it.scope == 'provided') && 
            compileDeps.contains([it.groupId, it.artifactId])
        }
    }
    
    /**
     * Resolves any dynamic versions in the POM file.
     *
     * @param project the project
     * @param pom the maven POM.
     */
    public static void resolvePom(Project project, XmlProvider pom) {
        resolvePom(project, pom, 'runtime')
    }

    public static void resolvePom(Project project, XmlProvider pom, String confName) {
        Map<String, String> versions = resolveVersions(project, confName)
        pom.asNode()?.dependencies?.dependency?.each { Node dep->
            def id = "${dep.groupId.text()}:${dep.artifactId.text()}".toString()
            def version = versions[id]
            if (version != null) {
                dep.version[0].value = version
            }
        }
    }
    
    /**
     * Resolves versions of all dependencies in a given configuration.
     *
     * @param project the project
     * @param confName the configuration name
     * @return the map of artifact->version
     */
    public static Map<String, String> resolveVersions(Project project, String confName) {
        if (project.configurations.findByName(confName) == null) {
            return [:]
        }
        Configuration conf = project.configurations.getByName(confName)
        ResolutionResult resolution = conf.incoming.resolutionResult

        return resolution.allComponents.collectEntries { ResolvedComponentResult version->
            ModuleVersionIdentifier module = version.moduleVersion
            [("${module.group}:${module.name}".toString()): module.version]
        }
    }
    
    /**
     * Adds support for dynamic modules, some closures are made available on the project to make swapping out prebuilt
     * artifacts in place of projects possible.
     * 
     * @param project the project.
     * @param classifiers the extra classifiers to link for the modules.
     */
    public static void configureDynamicModules(Project project, String... classifiers) {
        project.apply {
            delegate = project
            ext.dynamicModuleClassifiers = classifiers
            ext.dynamicModule = { dep ->
                def name = getProjectName(dep)
                if (hasProject(project, name)) {
                    dependencies.project(['path' : ":${name}"])
                }
                else {
                    dependencies.create(dep)
                }
            }

            ext.addDynamicModule = { deps->
                deps?.each { dep->
                    def name = getProjectName(dep)
                    if (hasProject(project, name)) {
                        def projectPath = ":${name}"
                        dependencies.compile dependencies.project(['path': projectPath])
                        dynamicModuleClassifiers?.each { classifier->
                            def compileConfig = "${classifier}Compile"
                            def runtimeConfig = "${classifier}Runtime"
                            def dependency = dependencies.project(['path': projectPath, 'configuration': runtimeConfig])
                            dependencies.add(compileConfig,  dependency)
                        }
                    }
                    else {
                        dependencies.compile dependencies.create(dep)
                        dynamicModule?.each { classifier->
                            def compileConfig = "${classifier}Compile"
                            def dependency = dependencies.create("${dep}:${classifier}")
                            dependencies.add(compileConfig, dependency)
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines if a project exists with the given name.
     * 
     * @param project the project
     * @param name the name of the project to find.
     * @return true if a project exists with the provided name.
     */
    public static boolean hasProject(Project project, String name) {
        try {
            project.project(":${name}")
            return true
        }
        catch (e) {
            return false
        }
    }

    /**
     * Gets the project name from a dependency notation, which is the second part of the notation (artifact name).
     * 
     * @param notation the dependency notation (group:name:version)
     * @return the name of the project
     */
    private static String getProjectName(String notation) {
        return notation?.contains(':') ? notation.split(':')[1] : notation
    }
    
    /**
     * Determines if the versions are compatible.  The current version must be at least the required version
     * or higher.
     *  
     * @param requiredVersion the required version.
     * @param currentVersion the current version.
     * @return true if the current version is compatible, false otherwise.
     */
    public static boolean isCompatibleVersion(String requiredVersion, String currentVersion) {
        def requiredVer = requiredVersion.split("\\.")
        def currentVer = currentVersion.split("\\.")
        // Number of version parts to compare
        def len = Math.min(requiredVer.size(), currentVer.size())
        for (int i = 0; i < requiredVer.size(); i++) {
            // Current version is shorter than required (and not greater than)
            if (i >= currentVer.size()) {
                return false
            }
            // Current number is greater than required
            if (currentVer[i].compareTo(requiredVer[i]) > 0) {
                return true
            }
            // Current number is less than required
            if (currentVer[i].compareTo(requiredVer[i]) < 0) {
                return false
            }
        }
        return true
    }
    
    /**
     * Loads the publish configuration from an external properties file and configures
     * 
     * @param project the project.
     */
    public static void configurePublishing(Project project) {
        String publishConfig = projectProperty(project, 'publishConfig')
        if (publishConfig && project.file(publishConfig).isFile()) {
            project.file(publishConfig).withInputStream {
                def props = new Properties()
                props.load(it)
                if (props.getProperty('publishUrl')) {
                    project.ext.publishUrl = props.getProperty('publishUrl')
                }
                if (props.getProperty('publishUsername')) {
                    project.ext.publishUsername = props.getProperty('publishUsername')
                }
                if (props.getProperty('publishPassword')) {
                    project.ext.publishPassword = props.getProperty('publishPassword')
                }
            }
        }
        String publishUrl = projectProperty(project, 'publishUrl')
        if (publishUrl) {
            // If there is no protocol this is a file path, resolve relative to root project
            if (!publishUrl.contains(':')) {
                publishUrl = project.rootProject.file(publishUrl).toURI().toString()
            }
            project.publishing.repositories {
                maven {
                    name 'publish'
                    url publishUrl
                    credentials {
                        username projectProperty(project, 'publishUsername')
                        password projectProperty(project, 'publishPassword')
                    }
                }
            }
        }
    }
    
    public static String projectProperty(Project project, String name) {
        project.hasProperty(name) ? project.getProperty(name) : null
    }
}