package com.emc.gradle.play

import java.io.File;
import org.gradle.api.Project

class PlayRunner {
    final Project project
    final File baseDir
    final PlayExtension config

    PlayRunner(Project project) {
        this(project, project.projectDir)
    }

    PlayRunner(Project project, File baseDir) {
        this.project = project
        this.baseDir = baseDir
        this.config = project.extensions.play
    }

    /**
     * Runs play in a specific directory.
     * 
     * @param dir the directory.
     * @param args the play arguments.
     */
    String play(File dir, String args) {
        run(dir, "${config.play} ${args}")
    }

    /**
     * Runs play in all modules and in the main application.
     * 
     * @param args the play arguments.
     */
    List<String> playAll(String args) {
        playModules(args) << playApp(args)
    }

    /**
     * Runs play in the main application.
     * 
     * @param args the play arguments.
     */
    String playApp(String args) {
        return play(project.file("${baseDir}/${config.appDir}"), args)
    }

    /**
     * Runs play in all modules.
     * 
     * @param args the play arguments.
     */
    List<String> playModules(String args) {
        config.modules.collect { module ->
            play(project.file("${baseDir}/${module}"), args)
        } as List
    }

    String run(File dir, String command) {
        return run(dir, command, true)
    }

    String run(File dir, String command, boolean failOnError) {
        project.logger.info("${dir}> ${command}")

        StringBuilder output = new StringBuilder()
        def process = createProcessBuilder(command).directory(dir).redirectErrorStream(true).start()
        process.waitForProcessOutput(output, output)
        int exitValue = process.exitValue()
        if (exitValue != 0) {
            project.logger.error("${dir}> ${command} terminated with non-zero value (${exitValue})")
            project.logger.error(output.toString())
            if (failOnError) {
                throw new IllegalStateException("${command} terminated with non-zero value (${exitValue})")
            }
        }
        else {
            project.logger.info(output.toString())
        }
        return output.toString()
    }

    /**
     * Creates a process builder for running the given command using the shell specific to the operating system.
     * 
     * @param command the command to run.
     * @return the process builder.
     */
    ProcessBuilder createProcessBuilder(command) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            return new ProcessBuilder("cmd", "/c", "\"${command}\"");
        }
        else {
            return new ProcessBuilder("sh", "-c", command);
        }
    }
}