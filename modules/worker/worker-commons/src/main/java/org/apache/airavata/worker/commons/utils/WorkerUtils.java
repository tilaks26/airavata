package org.apache.airavata.worker.commons.utils;

import org.apache.airavata.common.utils.AiravataUtils;
import org.apache.airavata.messaging.core.MessageContext;
import org.apache.airavata.model.appcatalog.computeresource.ResourceJobManagerType;
import org.apache.airavata.model.commons.ErrorModel;
import org.apache.airavata.model.job.JobModel;
import org.apache.airavata.model.messaging.event.*;
import org.apache.airavata.model.status.*;
import org.apache.airavata.registry.cpi.*;
import org.apache.airavata.registry.cpi.utils.Constants;
import org.apache.airavata.worker.commons.context.ProcessContext;
import org.apache.airavata.worker.commons.context.TaskContext;
import org.apache.airavata.worker.commons.exceptions.WorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by goshenoy on 4/12/17.
 */
public class WorkerUtils {

    private static final Logger logger = LoggerFactory.getLogger(WorkerUtils.class);

    public static void saveExperimentError(ProcessContext processContext, ErrorModel errorModel) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            String experimentId = processContext.getExperimentId();
            errorModel.setErrorId(AiravataUtils.getId("EXP_ERROR"));
            experimentCatalog.add(ExpCatChildDataType.EXPERIMENT_ERROR, errorModel, experimentId);
        } catch (RegistryException e) {
            String msg = "expId: " + processContext.getExperimentId() + " processId: " + processContext.getProcessId()
                    + " : - Error while updating experiment errors";
            throw new WorkerException(msg, e);
        }
    }

    public static void saveProcessError(ProcessContext processContext, ErrorModel errorModel) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            errorModel.setErrorId(AiravataUtils.getId("PROCESS_ERROR"));
            experimentCatalog.add(ExpCatChildDataType.PROCESS_ERROR, errorModel, processContext.getProcessId());
        } catch (RegistryException e) {
            String msg = "expId: " + processContext.getExperimentId() + " processId: " + processContext.getProcessId()
                    + " : - Error while updating process errors";
            throw new WorkerException(msg, e);
        }
    }

    public static void saveTaskError(TaskContext taskContext, ErrorModel errorModel) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = taskContext.getParentProcessContext().getExperimentCatalog();
            String taskId = taskContext.getTaskId();
            errorModel.setErrorId(AiravataUtils.getId("TASK_ERROR"));
            experimentCatalog.add(ExpCatChildDataType.TASK_ERROR, errorModel, taskId);
        } catch (RegistryException e) {
            String msg = "expId: " + taskContext.getParentProcessContext().getExperimentId() + " processId: " + taskContext.getParentProcessContext().getProcessId() + " taskId: " + taskContext.getTaskId()
                    + " : - Error while updating task errors";
            throw new WorkerException(msg, e);
        }
    }

    public static void handleProcessInterrupt(ProcessContext processContext) throws WorkerException {
        if (processContext.isCancel()) {
            ProcessStatus pStatus = new ProcessStatus(ProcessState.CANCELLING);
            pStatus.setReason("Process Cancel triggered");
            pStatus.setTimeOfStateChange(AiravataUtils.getCurrentTimestamp().getTime());
            processContext.setProcessStatus(pStatus);
            saveAndPublishProcessStatus(processContext);
            // do cancel operation here

            pStatus.setState(ProcessState.CANCELED);
            processContext.setProcessStatus(pStatus);
            pStatus.setTimeOfStateChange(AiravataUtils.getCurrentTimestamp().getTime());
            saveAndPublishProcessStatus(processContext);
        }else if (processContext.isHandOver()) {

        } else {
            logger.error("expId: {}, processId: {} :- Unknown process interrupt", processContext.getExperimentId(),
                    processContext.getProcessId());
        }
    }

    public static JobModel getJobModel(ProcessContext processContext) throws RegistryException {
        ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
        List<Object> objects = experimentCatalog.get(ExperimentCatalogModelType.JOB,
                Constants.FieldConstants.JobConstants.PROCESS_ID, processContext.getProcessId());
        List<JobModel> jobModels = new ArrayList<>();
        JobModel jobModel = null;
        if (objects != null) {
            for (Object object : objects) {
                jobModel = ((JobModel) object);
                if (jobModel.getJobId() != null || !jobModel.equals("")) {
                    return jobModel;
                }
            }
        }
        return jobModel;
    }

    public static List<String> parseTaskDag(String taskDag) {
        // TODO - parse taskDag and create taskId list
        String[] tasks = taskDag.split(",");
        return Arrays.asList(tasks);
    }

    public static void saveAndPublishTaskStatus(TaskContext taskContext) throws WorkerException {
        try {
            TaskState state = taskContext.getTaskState();
            // first we save job jobModel to the registry for sa and then save the job status.
            ProcessContext processContext = taskContext.getParentProcessContext();
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            TaskStatus status = taskContext.getTaskStatus();
            if (status.getTimeOfStateChange() == 0 || status.getTimeOfStateChange() > 0 ){
                status.setTimeOfStateChange(AiravataUtils.getCurrentTimestamp().getTime());
            }else {
                status.setTimeOfStateChange(status.getTimeOfStateChange());
            }
            experimentCatalog.add(ExpCatChildDataType.TASK_STATUS, status, taskContext.getTaskId());
            TaskIdentifier identifier = new TaskIdentifier(taskContext.getTaskId(),
                    processContext.getProcessId(), processContext.getProcessModel().getExperimentId(),
                    processContext.getGatewayId());
            TaskStatusChangeEvent taskStatusChangeEvent = new TaskStatusChangeEvent(state,
                    identifier);
            MessageContext msgCtx = new MessageContext(taskStatusChangeEvent, MessageType.TASK, AiravataUtils.getId
                    (MessageType.TASK.name()), taskContext.getParentProcessContext().getGatewayId());
            msgCtx.setUpdatedTime(AiravataUtils.getCurrentTimestamp());
            processContext.getStatusPublisher().publish(msgCtx);
        } catch (Exception e) {
            throw new WorkerException("Error persisting task status"
                    + e.getLocalizedMessage(), e);
        }
    }

    public static void saveAndPublishProcessStatus(ProcessContext processContext) throws WorkerException {
        try {
            // first we save job jobModel to the registry for sa and then save the job status.
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            ProcessStatus status = processContext.getProcessStatus();
            if (status.getTimeOfStateChange() == 0 || status.getTimeOfStateChange() > 0 ){
                status.setTimeOfStateChange(AiravataUtils.getCurrentTimestamp().getTime());
            }else {
                status.setTimeOfStateChange(status.getTimeOfStateChange());
            }
            experimentCatalog.add(ExpCatChildDataType.PROCESS_STATUS, status, processContext.getProcessId());
            ProcessIdentifier identifier = new ProcessIdentifier(processContext.getProcessId(),
                    processContext.getProcessModel().getExperimentId(),
                    processContext.getGatewayId());
            ProcessStatusChangeEvent processStatusChangeEvent = new ProcessStatusChangeEvent(status.getState(), identifier);
            MessageContext msgCtx = new MessageContext(processStatusChangeEvent, MessageType.PROCESS,
                    AiravataUtils.getId(MessageType.PROCESS.name()), processContext.getGatewayId());
            msgCtx.setUpdatedTime(AiravataUtils.getCurrentTimestamp());
            processContext.getStatusPublisher().publish(msgCtx);
        } catch (Exception e) {
            throw new WorkerException("Error persisting process status"
                    + e.getLocalizedMessage(), e);
        }
    }

    public static void saveJobStatus(ProcessContext processContext, JobModel jobModel) throws WorkerException {
        try {
            // first we save job jobModel to the registry for sa and then save the job status.
            JobStatus jobStatus = null;
            if(jobModel.getJobStatuses() != null)
                jobStatus = jobModel.getJobStatuses().get(0);

            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            List<JobStatus> statuses = new ArrayList<>();
            statuses.add(jobStatus);
            jobModel.setJobStatuses(statuses);
            if (jobStatus.getTimeOfStateChange() == 0 || jobStatus.getTimeOfStateChange() > 0 ){
                jobStatus.setTimeOfStateChange(AiravataUtils.getCurrentTimestamp().getTime());
            }else {
                jobStatus.setTimeOfStateChange(jobStatus.getTimeOfStateChange());
            }
            CompositeIdentifier ids = new CompositeIdentifier(jobModel.getTaskId(), jobModel.getJobId());
            experimentCatalog.add(ExpCatChildDataType.JOB_STATUS, jobStatus, ids);
            JobIdentifier identifier = new JobIdentifier(jobModel.getJobId(), jobModel.getTaskId(),
                    processContext.getProcessId(), processContext.getProcessModel().getExperimentId(),
                    processContext.getGatewayId());
            JobStatusChangeEvent jobStatusChangeEvent = new JobStatusChangeEvent(jobStatus.getJobState(), identifier);
            MessageContext msgCtx = new MessageContext(jobStatusChangeEvent, MessageType.JOB, AiravataUtils.getId
                    (MessageType.JOB.name()), processContext.getGatewayId());
            msgCtx.setUpdatedTime(AiravataUtils.getCurrentTimestamp());
            processContext.getStatusPublisher().publish(msgCtx);
        } catch (Exception e) {
            throw new WorkerException("Error persisting job status"
                    + e.getLocalizedMessage(), e);
        }
    }

    public static String getTemplateFileName(ResourceJobManagerType resourceJobManagerType) {
        switch (resourceJobManagerType) {
            case FORK:
                return "UGE_Groovy.template";
            case PBS:
                return "PBS_Groovy.template";
            case SLURM:
                return "SLURM_Groovy.template";
            case UGE:
                return "UGE_Groovy.template";
            case LSF:
                return "LSF_Groovy.template";
            case CLOUD:
                return "CLOUD_Groovy.template";
            default:
                return null;
        }
    }
}
