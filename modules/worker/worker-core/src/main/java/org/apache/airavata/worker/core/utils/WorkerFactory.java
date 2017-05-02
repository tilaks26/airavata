/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.airavata.worker.core.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.jcraft.jsch.*;
import org.apache.airavata.common.exception.ApplicationSettingsException;
import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.credential.store.credential.Credential;
import org.apache.airavata.credential.store.store.CredentialReader;
import org.apache.airavata.credential.store.store.CredentialStoreException;
import org.apache.airavata.model.appcatalog.computeresource.ResourceJobManagerType;
import org.apache.airavata.credential.store.credential.impl.ssh.SSHCredential;
import org.apache.airavata.worker.core.authentication.AuthenticationInfo;
import org.apache.airavata.worker.core.authentication.SSHKeyAuthentication;
import org.apache.airavata.worker.core.cluster.ServerInfo;
import org.apache.airavata.worker.core.config.ResourceConfig;
import org.apache.airavata.worker.core.config.WorkerYamlConfigruation;
import org.apache.airavata.worker.core.context.ProcessContext;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by goshenoy on 4/13/17.
 */
public class WorkerFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkerFactory.class);

    private static boolean isWorkerConfigurationLoaded = false;
    private static Map<ResourceJobManagerType, ResourceConfig> resources = new HashMap<>();
    private static Cache<String,Session> sessionCache;

    public static void loadConfiguration() throws WorkerException {
        if (!isWorkerConfigurationLoaded) {
            WorkerYamlConfigruation config = new WorkerYamlConfigruation();
            try {
                for (ResourceConfig resourceConfig : config.getResourceConfiguration()) {
                    resources.put(resourceConfig.getJobManagerType(), resourceConfig);
                }
            } catch (Exception e) {
                throw new WorkerException("Worker config issue", e);
            }

            sessionCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(ServerSettings.getSessionCacheAccessTimeout(), TimeUnit.MINUTES)
                    .removalListener((RemovalListener<String, Session>) removalNotification -> {
                        if (removalNotification.getValue().isConnected()) {
                            log.info("Disconnecting ssh session with key: " + removalNotification.getKey());
                            removalNotification.getValue().disconnect();
                        }
                        log.info("Removed ssh session with key: " + removalNotification.getKey());
                    })
                    .build();

            isWorkerConfigurationLoaded =  true;
        }
    }

    public static Map<ResourceJobManagerType, ResourceConfig> getResourceConfig() {
        return resources;
    }

    public static ResourceConfig getResourceConfig(ResourceJobManagerType resourceJobManagerType) {
        return resources.get(resourceJobManagerType);
    }

    public static SSHKeyAuthentication getStorageSSHKeyAuthentication(ProcessContext pc)
            throws WorkerException, CredentialStoreException {
        try {
            return getSshKeyAuthentication(pc.getGatewayId(),
                    pc.getStorageResourceLoginUserName(),
                    pc.getStorageResourceCredentialToken());
        }  catch (ApplicationSettingsException | IllegalAccessException | InstantiationException e) {
            throw new WorkerException("Couldn't build ssh authentication object", e);
        }
    }

    public static SSHKeyAuthentication getSshKeyAuthentication(String gatewayId,
                                                                String loginUserName,
                                                                String credentialStoreToken)
            throws ApplicationSettingsException, IllegalAccessException, InstantiationException,
            CredentialStoreException, WorkerException {

        SSHKeyAuthentication sshKA;CredentialReader credentialReader = WorkerUtils.getCredentialReader();
        Credential credential = credentialReader.getCredential(gatewayId, credentialStoreToken);
        if (credential instanceof SSHCredential) {
            sshKA = new SSHKeyAuthentication();
            sshKA.setUserName(loginUserName);
            SSHCredential sshCredential = (SSHCredential) credential;
            sshKA.setPublicKey(sshCredential.getPublicKey());
            sshKA.setPrivateKey(sshCredential.getPrivateKey());
            sshKA.setPassphrase(sshCredential.getPassphrase());
            sshKA.setStrictHostKeyChecking("no");
/*            sshKA.setStrictHostKeyChecking(ServerSettings.getSetting("ssh.strict.hostKey.checking", "no"));
            sshKA.setKnownHostsFilePath(ServerSettings.getSetting("ssh.known.hosts.file", null));
            if (sshKA.getStrictHostKeyChecking().equals("yes") && sshKA.getKnownHostsFilePath() == null) {
                throw new ApplicationSettingsException("If ssh strict hostkey checking property is set to yes, you must " +
                        "provide known host file path");
            }*/
            return sshKA;
        } else {
            String msg = "Provided credential store token is not valid. Please provide the correct credential store token";
            log.error(msg);
            throw new CredentialStoreException("Invalid credential store token:" + credentialStoreToken);
        }
    }

    public static synchronized Session getSSHSession(AuthenticationInfo authenticationInfo,
                                                     ServerInfo serverInfo) throws WorkerException {
        if (authenticationInfo == null
                || serverInfo == null) {

            throw new IllegalArgumentException("Can't create ssh session, argument should be valid (not null)");
        }
        SSHKeyAuthentication authentication;
        if (authenticationInfo instanceof SSHKeyAuthentication) {
            authentication = (SSHKeyAuthentication) authenticationInfo;
        } else {
            throw new WorkerException("Support ssh key authentication only");
        }
        String key = buildKey(serverInfo);
        Session session = sessionCache.getIfPresent(key);
        boolean valid = isValidSession(session);
        // FIXME - move following info logs to debug
        if (valid) {
            log.info("SSH Session validation succeeded, key :" + key);
            valid = testChannelCreation(session);
            if (valid) {
                log.info("Channel creation test succeeded, key :" + key);
            } else {
                log.info("Channel creation test failed, key :" + key);
            }
        } else {
            log.info("Session validation failed, key :" + key);
        }

        if (!valid) {
            if (session != null) {
                log.info("Reinitialize a new SSH session for :" + key);
            } else {
                log.info("Initialize a new SSH session for :" + key);
            }
            try {

                JSch jSch = new JSch();
                jSch.addIdentity(UUID.randomUUID().toString(), authentication.getPrivateKey(), authentication.getPublicKey(),
                        authentication.getPassphrase().getBytes());
                session = jSch.getSession(serverInfo.getUserName(), serverInfo.getHost(),
                        serverInfo.getPort());
                session.setUserInfo(new DefaultUserInfo(serverInfo.getUserName(), null, authentication.getPassphrase()));
                if (authentication.getStrictHostKeyChecking().equals("yes")) {
                    jSch.setKnownHosts(authentication.getKnownHostsFilePath());
                } else {
                    session.setConfig("StrictHostKeyChecking", "no");
                }
                session.connect(); // 0 connection timeout
                sessionCache.put(key, session);
            } catch (JSchException e) {
                throw new WorkerException("JSch initialization error ", e);
            }
        } else {
            // FIXME - move following info log to debug
            log.info("Reuse SSH session for :" + key);
        }
        return session;

    }

    private static boolean testChannelCreation(Session session) {

        String command = "pwd ";
        Channel channel = null;
        try {
            channel = session.openChannel("exec");
            StandardOutReader stdOutReader = new StandardOutReader();
            ((ChannelExec) channel).setCommand(command);
            ((ChannelExec) channel).setErrStream(stdOutReader.getStandardError());
            channel.connect();
            stdOutReader.onOutput(channel);
        } catch (JSchException e) {
            log.error("Test Channel creation failed.", e);
            return false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return true;
    }

    private static boolean isValidSession(Session session) {
        return session != null && session.isConnected();
    }

    private static String buildKey(ServerInfo serverInfo) {
        return serverInfo.getUserName() +
                "_" +
                serverInfo.getHost() +
                "_" +
                serverInfo.getPort() +
                "_" +
                serverInfo.getCredentialToken();
    }

    private static class DefaultUserInfo implements UserInfo {

        private String userName;
        private String password;
        private String passphrase;

        public DefaultUserInfo(String userName, String password, String passphrase) {
            this.userName = userName;
            this.password = password;
            this.passphrase = passphrase;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptPassword(String s) {
            return false;
        }

        @Override
        public boolean promptPassphrase(String s) {
            return false;
        }

        @Override
        public boolean promptYesNo(String s) {
            return false;
        }

        @Override
        public void showMessage(String s) {

        }
    }
}
