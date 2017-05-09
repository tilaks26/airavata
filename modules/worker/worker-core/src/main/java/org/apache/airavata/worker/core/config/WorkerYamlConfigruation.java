/**
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
 */
package org.apache.airavata.worker.core.config;

import org.apache.airavata.model.appcatalog.computeresource.JobSubmissionProtocol;
import org.apache.airavata.model.appcatalog.computeresource.ResourceJobManagerType;
import org.apache.airavata.model.data.movement.DataMovementProtocol;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkerYamlConfigruation {

	private static final String CONFIG = "config";
	private static final String JOB_SUBMITTERS = "jobSubmitters";
	private static final String SUBMISSIO_PROTOCOL = "submissionProtocol";
	private static final String TASK_CLASS = "taskClass";
	private static final String COMMON_TASKS = "commonTasks";
	private static final String TYPE = "type";
	private static final String FILE_TRANSFER_TASKS = "fileTransferTasks";
	private static final String TRANSFER_PROTOCOL = "transferProtocol";
	private static final String RESOURCES = "resources";
	private static final String JOB_MANAGER_TYPE = "jobManagerType";
	private static final String COMMAND_OUTPUT_PARSER = "commandOutputParser";
	private static final String EMAIL_PARSER = "emailParser";
	private static final String RESOURCE_EMAIL_ADDRESS = "resourceEmailAddresses";
	private static final String PROPERTIES = "properties";
	public static final String TASK_IMPLEMENTATIONS = "taskImplementations";
	private static final String TASK = "task";
	private List<TaskImplementationConfig> taskImplementations = new ArrayList<>();


	private List<JobSubmitterTaskConfig> jobSubmitters = new ArrayList<>();
	private List<DataTransferTaskConfig> fileTransferTasks = new ArrayList<>();
	private List<ResourceConfig> resources = new ArrayList<>();


	public WorkerYamlConfigruation() throws WorkerException {
		InputStream resourceAsStream = WorkerYamlConfigruation.class.getClassLoader().
				getResourceAsStream("worker-config.yaml");
		parse(resourceAsStream);
	}

	private void parse(InputStream resourceAsStream) throws WorkerException {
		if (resourceAsStream == null) {
			throw new WorkerException("Configuration file{gfac-config.yaml} is not fund");
		}
		Yaml yaml = new Yaml();
		Object load = yaml.load(resourceAsStream);
		if (load == null) {
			throw new WorkerException("Yaml configuration object null");
		}

		if (load instanceof Map) {
			Map<String, Object> loadMap = (Map<String, Object>) load;
			String identifier;
			List<Map<String,Object >> jobSubYamls = (List<Map<String, Object>>) loadMap.get(JOB_SUBMITTERS);
			JobSubmitterTaskConfig jobSubmitterTaskConfig;
			if (jobSubYamls != null) {
				for (Map<String, Object> jobSub : jobSubYamls) {
					jobSubmitterTaskConfig = new JobSubmitterTaskConfig();
					identifier = ((String) jobSub.get(SUBMISSIO_PROTOCOL));
					jobSubmitterTaskConfig.setSubmissionProtocol(JobSubmissionProtocol.valueOf(identifier));
					jobSubmitterTaskConfig.setTaskClass(((String) jobSub.get(TASK_CLASS)));
					Object propertiesObj = jobSub.get(PROPERTIES);
					List propertiesList;
					if (propertiesObj instanceof List) {
						propertiesList = ((List) propertiesObj);
						if (propertiesList.size() > 0) {
							Map<String, String> props = (Map<String, String>) propertiesList.get(0);
							jobSubmitterTaskConfig.addProperties(props);
						}
					}
					jobSubmitters.add(jobSubmitterTaskConfig);
				}
			}

			List<Map<String, Object>> fileTransYamls = (List<Map<String, Object>>) loadMap.get(FILE_TRANSFER_TASKS);
			DataTransferTaskConfig dataTransferTaskConfig;
			if (fileTransYamls != null) {
				for (Map<String, Object> fileTransConfig : fileTransYamls) {
					dataTransferTaskConfig = new DataTransferTaskConfig();
					identifier = ((String) fileTransConfig.get(TRANSFER_PROTOCOL));
					dataTransferTaskConfig.setTransferProtocol(DataMovementProtocol.valueOf(identifier));
					dataTransferTaskConfig.setTaskClass(((String) fileTransConfig.get(TASK_CLASS)));
					Object propertiesObj = fileTransConfig.get(PROPERTIES);
					List propertiesList;
					if (propertiesObj instanceof List) {
						propertiesList = (List) propertiesObj;
						if (propertiesList.size() > 0) {
							Map<String, String> props = (Map<String, String>) propertiesList.get(0);
							dataTransferTaskConfig.addProperties(props);
						}
					}
					fileTransferTasks.add(dataTransferTaskConfig);
				}
			}

			List<Map<String, Object>> resourcesYaml = (List<Map<String, Object>>) loadMap.get(RESOURCES);
			ResourceConfig resourceConfig;
			if (resourcesYaml != null) {
				for (Map<String, Object> resource : resourcesYaml) {
					resourceConfig = new ResourceConfig();
					identifier = resource.get(JOB_MANAGER_TYPE).toString();
					resourceConfig.setJobManagerType(ResourceJobManagerType.valueOf(identifier));
					resourceConfig.setCommandOutputParser(resource.get(COMMAND_OUTPUT_PARSER).toString());
                    Object emailParser = resource.get(EMAIL_PARSER);
                    if (emailParser != null){
                        resourceConfig.setEmailParser(emailParser.toString());
                    }
					List<String> emailAddressList = (List<String>) resource.get(RESOURCE_EMAIL_ADDRESS);
					resourceConfig.setResourceEmailAddresses(emailAddressList);
					resources.add(resourceConfig);
				}
			}

			List<Map<String,Object >> taskImplementationYaml = (List<Map<String, Object>>) loadMap.get(TASK_IMPLEMENTATIONS);
			TaskImplementationConfig taskImplementationConfig;
			if (taskImplementationYaml != null) {
				for (Map<String, Object> taskImplementations : taskImplementationYaml) {
					taskImplementationConfig = new TaskImplementationConfig();
					identifier = ((String) taskImplementations.get(TASK));
					taskImplementationConfig.setTaskType(identifier);
					taskImplementationConfig.setImplementationClass(((String) taskImplementations.get(TASK_CLASS)));
					this.taskImplementations.add(taskImplementationConfig);
				}
			}
		}
	}

	public List<JobSubmitterTaskConfig> getJobSbumitters() {
		return jobSubmitters;
	}

	public List<DataTransferTaskConfig> getFileTransferTasks() {
		return fileTransferTasks;
	}

	public List<ResourceConfig> getResourceConfiguration() {
		return resources;
	}

	public List<TaskImplementationConfig> getTaskImplementations() {
		return taskImplementations;
	}

}
