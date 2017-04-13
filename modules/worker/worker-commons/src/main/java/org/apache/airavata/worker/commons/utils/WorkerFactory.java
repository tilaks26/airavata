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
package org.apache.airavata.worker.commons.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.jcraft.jsch.Session;
import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.model.appcatalog.computeresource.ResourceJobManagerType;
import org.apache.airavata.worker.commons.config.ResourceConfig;
import org.apache.airavata.worker.commons.config.WorkerYamlConfigruation;
import org.apache.airavata.worker.commons.exceptions.WorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
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
}
