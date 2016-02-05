package com.emc.gradle.remote

import org.gradle.api.*
import org.gradle.api.tasks.*

import com.jcraft.jsch.*

/**
 * Runs an SSH command on a remote host.
 */
class RemoteExecuteTask extends AbstractRemoteTask {
    static final long SLEEP_TIME = 100
    @Input
    String command

    protected void performAction(Session session) {
        logger.lifecycle("Connecting to remote host: ${getHost()}")
        ChannelExec channel = (ChannelExec) session.openChannel("exec")
        try {
            def stdout = new StreamReader("stdout", channel.inputStream)
            def stderr = new StreamReader("stderr", channel.errStream)
            logger.lifecycle("${getUser()}@${getHost()}> ${getCommand()}")

            sendCommand(getCommand(), channel)
            waitForDone(channel)

            stdout.stop()
            stderr.stop()

            if (channel.exitStatus != 0) {
                logger.error("${getCommand()} terminated with ${channel.exitStatus} exit code")
                if (stdout.result) {
                    logger.error("stdout: ${stdout.result}")
                }
                if (stderr.result) {
                    logger.error("stderr: ${stderr.result}")
                }
                throw new GradleException("${getCommand()} terminated with ${channel.exitStatus} exit code")
            }
            else {
                if (stdout.result) {
                    logger.info("stdout: ${stdout.result}")
                }
                if (stderr.result) {
                    logger.info("stderr: ${stderr.result}")
                }
            }
        }
        finally {
            channel.disconnect()
        }
    }

    protected void sendCommand(String command, ChannelExec channel) throws JSchException, IOException {
        channel.setCommand(command)
        channel.connect()
    }

    protected void waitForDone(ChannelExec channel) throws InterruptedException {
        while (!channel.isClosed()) {
            Thread.sleep(SLEEP_TIME)
        }
    }

    static class StreamReader {
        InputStream stream
        Thread thread
        String result

        StreamReader(String name, InputStream stream) {
            this.stream = stream
            this.thread = Thread.start(name) {
                result = stream.getText("UTF-8")
            }
        }

        void stop() {
            thread.interrupt()
            stream.close()
        }
    }
}