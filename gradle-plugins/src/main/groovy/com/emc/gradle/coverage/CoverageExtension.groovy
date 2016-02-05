package com.emc.gradle.coverage

import org.gradle.api.Project

class CoverageExtension {
    final Project project
    String jacocoVersion = '0.6.4.201312101107'
    String coverageDir
    
    public CoverageExtension(Project project){
        this.project = project
    }

    String getCoverageDir() {
        coverageDir ?: "${project.buildDir}/coverage"
    }
}