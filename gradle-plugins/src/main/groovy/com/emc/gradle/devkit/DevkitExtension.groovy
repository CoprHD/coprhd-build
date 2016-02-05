package com.emc.gradle.devkit

import com.emc.gradle.remote.AbstractRemoteExtension
import org.gradle.api.Project

/**
 * This extension class holds all the relevant devkit information, such as the connection
 * details.
 * 
 * Since most developers use a single devkit, the easiest way to configure this is by adding the 
 * devkit properties to the ${userHome}/.gradle/gradle.properties file
 */
class DevkitExtension extends AbstractRemoteExtension {
}