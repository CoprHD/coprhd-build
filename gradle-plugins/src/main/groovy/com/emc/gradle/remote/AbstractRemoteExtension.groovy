package com.emc.gradle.remote

import org.gradle.api.Project

/**
 * Base class for remote operation extensions.
 */
abstract class AbstractRemoteExtension {
    String host
    int port
    String user
    String password
    String rootDir
    String confSrc
    String serviceName
    long sleepAfterStop
}