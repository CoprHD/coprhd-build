package com.emc.gradle.coverage

import org.gradle.api.Project

class CoverageExtension {
    final Project project
    String jacocoVersion = '0.7.5.201505241946'
    String coverageDir
    
    public CoverageExtension(Project project){
        this.project = project
    }

    String getCoverageDir() {
        coverageDir ?: "${project.buildDir}/coverage"
    }
}