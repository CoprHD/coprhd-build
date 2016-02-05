package com.emc.gradle.remote

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import com.jcraft.jsch.Session
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo

/**
 * Base task for operating on a remote host using an SSH session. The SSH session is initialized and passed into the 
 * <tt>performAction(Session)</tt> method.
 */
abstract class AbstractRemoteTask extends DefaultTask {

    @Input
    String host
    @Input
    Integer port
    @Input
    String user
    @Input
    String password

    protected abstract void performAction(Session session) throws JSchException

    @TaskAction
    void performAction() {
        // If multiple hosts are provided, perform the same operation on each
        getHost()?.split("\\s+,\\s+")?.each { 
            Session session = connect(it)
            try {
                performAction(session)
            }
            finally {
                disconnect(session)
            }
        }
    }

    /**
     * Connect to the remote host, returning the SSH session.
     */
    Session connect(String host) throws JSchException {
        Session session = new JSch().getSession(getUser(), host, getPort())
        session.setPassword(getPassword())
        session.setUserInfo(new SSHUserInfo(getPassword()))
        session.connect()
        return session
    }

    /**
     * Disconnects from the remote host.
     */
    void disconnect(Session session) throws JSchException {
        if (session?.isConnected()) {
            session.disconnect()
        }
    }

    /**
     * Provides support for supplying the passsword for a login if the SSH server only allows
     * keyboard interactive logins.
     */
    private static class SSHUserInfo implements UserInfo, UIKeyboardInteractive {
        String password;

        SSHUserInfo(String password) {
            this.password = password;
        }

        @Override
        public String getPassword() {
            return password;
        }
        @Override
        public String getPassphrase() {
            return null;
        }
        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }
        @Override
        public boolean promptPassword(String message) {
            return true;
        }
        @Override
        public boolean promptYesNo(String message) {
            return true;
        }
        @Override
        public void showMessage(String message) {
        }
        @Override
        public String[] promptKeyboardInteractive(String destination, String name, String instruction,
                String[] prompt, boolean[] echo) {
            if ((prompt.length != 1) || (echo[0] != false) || (this.password == null)) {
                return null;
            }
            String[] response = new String[1];
            response[0] = this.password;
            return response;
        }
    }
}