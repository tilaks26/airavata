
package org.apache.airavata.worker.task.jobsubmission.utils;

import org.apache.airavata.model.appcatalog.computeresource.JobSubmissionProtocol;
import org.apache.airavata.model.appcatalog.computeresource.ResourceJobManager;
import org.apache.airavata.worker.core.cluster.OutputParser;
import org.apache.airavata.worker.core.config.JobSubmitterTaskConfig;
import org.apache.airavata.worker.core.config.ResourceConfig;
import org.apache.airavata.worker.core.config.WorkerYamlConfigruation;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.apache.airavata.worker.core.utils.WorkerFactory;
import org.apache.airavata.worker.core.utils.JobManagerConfiguration;
import org.apache.airavata.worker.core.utils.WorkerUtils;
import org.apache.airavata.worker.task.jobsubmission.JobSubmissionTask;
import org.apache.airavata.worker.task.jobsubmission.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by goshenoy on 4/13/17.
 */
public class JobSubmissionFactory {

    private static final Logger log = LoggerFactory.getLogger(JobSubmissionFactory.class);
    private static Map<JobSubmissionProtocol, JobSubmissionTask> jobSubmissionTask = new HashMap<>();

    public static void loadConfiguration() throws WorkerException {
        WorkerYamlConfigruation config = new WorkerYamlConfigruation();
        try {
            // load workerfactory configuration
            WorkerFactory.loadConfiguration();

            for (JobSubmitterTaskConfig jobSubmitterTaskConfig : config.getJobSbumitters()) {
                String taskClass = jobSubmitterTaskConfig.getTaskClass();
                Class<?> aClass = Class.forName(taskClass);
                Constructor<?> constructor = aClass.getConstructor();
                JobSubmissionTask task = (JobSubmissionTask) constructor.newInstance();
                task.init(jobSubmitterTaskConfig.getProperties());
                jobSubmissionTask.put(jobSubmitterTaskConfig.getSubmissionProtocol(), task);
            }
        } catch (Exception e) {
            throw new WorkerException("JobSubmission config issue", e);
        }
    }

    public static JobManagerConfiguration getJobManagerConfiguration(ResourceJobManager resourceJobManager) throws WorkerException {
        if(resourceJobManager == null)
            return null;

        ResourceConfig resourceConfig = WorkerFactory.getResourceConfig(resourceJobManager.getResourceJobManagerType());
        OutputParser outputParser;
        try {
            Class<? extends OutputParser> aClass = Class.forName(resourceConfig.getCommandOutputParser()).asSubclass
                    (OutputParser.class);
            outputParser = aClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new WorkerException("Error while instantiating output parser for " + resourceJobManager
                    .getResourceJobManagerType().name());
        }

        String templateFileName = WorkerUtils.getTemplateFileName(resourceJobManager.getResourceJobManagerType());
        switch (resourceJobManager.getResourceJobManagerType()) {
            case PBS:
                return new PBSJobConfiguration(templateFileName, ".pbs", resourceJobManager.getJobManagerBinPath(),
                        resourceJobManager.getJobManagerCommands(), outputParser);
            case SLURM:
                return new SlurmJobConfiguration(templateFileName, ".slurm", resourceJobManager
                        .getJobManagerBinPath(), resourceJobManager.getJobManagerCommands(), outputParser);
            case LSF:
                return new LSFJobConfiguration(templateFileName, ".lsf", resourceJobManager.getJobManagerBinPath(),
                        resourceJobManager.getJobManagerCommands(), outputParser);
            case UGE:
                return new UGEJobConfiguration(templateFileName, ".pbs", resourceJobManager.getJobManagerBinPath(),
                        resourceJobManager.getJobManagerCommands(), outputParser);
            case FORK:
                return new ForkJobConfiguration(templateFileName, ".sh", resourceJobManager.getJobManagerBinPath(),
                        resourceJobManager.getJobManagerCommands(), outputParser);
            // We don't have a job configuration manager for CLOUD type
            default:
                return null;
        }
    }
}
