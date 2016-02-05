package com.emc.gradle.remote

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*

import com.jcraft.jsch.*

/**
 * Copies a number of files to a remote host using the SFTP protocol.
 */
class RemoteCopyTask extends AbstractRemoteTask {
    @Input
    String destDir
    @InputFiles
    FileCollection files
    @Input
    @Optional
    private Closure renameAction

    public void rename(Closure closure) {
        this.renameAction = closure 
    }

    protected void performAction(Session session) {
        logger.lifecycle("Connecting to remote host: ${getHost()}")
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp")
        try {
            channel.connect()
            getFiles().each { file ->
                def filename = getFilename(file)
                copyFile(channel, file, "${getDestDir()}/${filename}")
            }
        }
        finally {
            channel.disconnect();
        }
    }
    
    protected String getFilename(File file) {
        if (renameAction) {
            return renameAction(file.name) ?: file.name
        }
        else {
            return file.name
        }
    }
    
    protected void copyFile(ChannelSftp channel, File source, String targetPath) {
        logger.lifecycle("Copying ${source.name} to ${getUser()}@${getHost()}:${targetPath} (${source.length()} bytes)")
        channel.put(source.absolutePath, targetPath)
    }
}