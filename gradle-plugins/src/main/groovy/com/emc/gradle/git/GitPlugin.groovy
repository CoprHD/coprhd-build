package com.emc.gradle.git

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import com.emc.gradle.GradleUtils
import org.apache.commons.lang.StringUtils

/**
 * Simple plugin that will make available some GIT information on the project.
 */
class GitPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        String git = findGit(project)

        String version = getVersion(project, git)
        String branch = getBranch(project, git)
        String hash = getHash(project, git)
        String commits = getCommits(project, git)
        String diffStat = getDiffStat(project, git)
        String userName = getUserName(project, git)
        String userEmail = getUserEmail(project, git)

        project.ext {
            gitVersion = version
            gitBranch = branch
            gitHash = hash
            gitShortHash = hash?.substring(0, 7)
            gitCommits = commits
            gitDiffStat = diffStat
            gitUserName = userName
            gitUserEmail = userEmail
        }
        project.allprojects { p->
            tasks.withType(Jar.class) {
                manifest.attributes (
                    "Implementation-Title": '',
                    "Implementation-Version": '',
                    "Implementation-Vendor": '',
                    "Build-Version": '',
                    "Git-Version": (version ?: '-'),
                    "Git-Branch": (branch ?: '-'),
                    "Git-Hash": (hash ?: '-')
                )
                
                p.afterEvaluate {
                    def implementationTitle = "${p.group}:${p.name}"
                    def implementationVersion = "${p.version} ${branch}"
                    def implementationVendor = "EMC"
                    if (diffStat) {
                        // Mark the version with an asterisk if modified
                        implementationVersion += '*'
                        // Include the git user information for the vendor
                        implementationVendor = userName
                        if (userEmail) {
                            implementationVendor += " <${userEmail}>"
                        }
                    }
                    manifest.attributes (
                        "Build-Version": (p.version ?: ''),
                        "Implementation-Title": implementationTitle,
                        "Implementation-Version": implementationVersion,
                        "Implementation-Vendor": implementationVendor
                    )
                }
            }
        }
    }

    private String findGit(Project project) {
        def git = null
        if (project.hasProperty('git')) {
            git = project.git
        }
        else {
            // Search for git in known locations
            git = [
                "/usr/libexec/git-core/git",
                "/usr/local/bin/git",
                "/usr/bin/git",
                "C:\\Program Files (x86)\\Git\\bin\\git.exe",
                "C:\\Program Files\\Git\\bin\\git.exe",
            ].find {
                try { project.file(it).exists() } catch (e) { }
            }
        }

        if (!git) {
            project.logger.warn("GIT could not be found")
            // Fallback on attempting to use git on the PATH
            git = 'git'
        }
        // Quote the command on windows
        if (GradleUtils.isWindows()) {
            git = "\"${git}\""
        }
        return git
    }

    private String getVersion(Project project, String git) {
        return exec(project, "${git} describe --always")
    }

    private String getHash(Project project, String git) {
        return exec(project, "${git} rev-parse HEAD")
    }

    private String getBranch(Project project, String git) {
        def branch = exec(project, "${git} rev-parse --abbrev-ref HEAD")
        // Happens when a commit is checked out in a detached state
        if (branch == "HEAD") {
            // It is possible that this commit exists in multiple branches
            def branches = exec(project, "${git} branch --contains HEAD")?.readLines()?.findAll{
                 !it.contains("detached from")
            }?.collect{ it.trim() }
            branch = branches?.join(',')
        }
        return branch
    }

    private String getCommits(Project project, String git) {
        return exec(project, "${git} rev-list HEAD --count")
    }

    private String getUserName(Project project, String git) {
        return exec(project, "${git} config --get user.name")
    }

    private String getUserEmail(Project project, String git) {
        return exec(project, "${git} config --get user.email")
    }

    private String getDiffStat(Project project, String git) {
        return exec(project, "${git} diff --shortstat HEAD")
    }

    private String exec(Project project, String command) {
        try {
            return command.execute(null, project.projectDir)?.text.trim()
        }
        catch (e) {
            project.logger.debug("Failed to execute command: ${command}", e)
            return null
        }
    }
}