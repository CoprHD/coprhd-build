package com.emc.gradle.remote

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*

import com.jcraft.jsch.*

/**
 * General task using an SFTP channel.
 */
class SFTP extends AbstractRemoteTask {
    @Input
    private Closure closure

    public SFTP() {
        // Force the task to always run
        outputs.upToDateWhen { false }
    }

    public void action(Closure closure) {
        this.closure = closure
    }

    protected void performAction(Session session) {
        logger.lifecycle("Connecting to remote host: ${getHost()}")
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp")
        try {
            channel.connect()
            closure(channel)
        }
        finally {
            channel.disconnect();
        }
    }
}