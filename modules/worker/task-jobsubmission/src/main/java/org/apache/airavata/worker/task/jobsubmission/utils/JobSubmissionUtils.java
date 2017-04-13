package org.apache.airavata.worker.task.jobsubmission.utils;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;
import org.apache.airavata.common.exception.ApplicationSettingsException;
import org.apache.airavata.common.utils.ApplicationSettings;
import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.model.appcatalog.appdeployment.ApplicationDeploymentDescription;
import org.apache.airavata.model.appcatalog.appdeployment.CommandObject;
import org.apache.airavata.model.appcatalog.computeresource.*;
import org.apache.airavata.model.appcatalog.gatewayprofile.ComputeResourcePreference;
import org.apache.airavata.model.application.io.DataType;
import org.apache.airavata.model.application.io.InputDataObjectType;
import org.apache.airavata.model.application.io.OutputDataObjectType;
import org.apache.airavata.model.job.JobModel;
import org.apache.airavata.model.parallelism.ApplicationParallelismType;
import org.apache.airavata.model.process.ProcessModel;
import org.apache.airavata.model.scheduling.ComputationalResourceSchedulingModel;
import org.apache.airavata.model.task.JobSubmissionTaskModel;
import org.apache.airavata.registry.core.experiment.catalog.impl.RegistryFactory;
import org.apache.airavata.registry.cpi.*;
import org.apache.airavata.worker.commons.cluster.OutputParser;
import org.apache.airavata.worker.commons.config.ResourceConfig;
import org.apache.airavata.worker.commons.context.ProcessContext;
import org.apache.airavata.worker.commons.context.TaskContext;
import org.apache.airavata.worker.commons.exceptions.WorkerException;
import org.apache.airavata.worker.commons.utils.JobManagerConfiguration;
import org.apache.airavata.worker.commons.utils.WorkerConstants;
import org.apache.airavata.worker.commons.utils.WorkerUtils;
import org.apache.airavata.worker.task.jobsubmission.config.*;
import org.apache.commons.io.FileUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by goshenoy on 4/12/17.
 */
public class JobSubmissionUtils {

    private static final Logger log = LoggerFactory.getLogger(JobSubmissionUtils.class);
    private static Map<ResourceJobManagerType, ResourceConfig> resources = new HashMap<>();

    public static GroovyMap createGroovyMap(ProcessContext processContext, TaskContext taskContext)
            throws WorkerException, AppCatalogException, ApplicationSettingsException {

        GroovyMap groovyMap = new GroovyMap();
        ProcessModel processModel = processContext.getProcessModel();
        ResourceJobManager resourceJobManager = getResourceJobManager(processContext);
        setMailAddresses(processContext, groovyMap); // set email options and addresses

        groovyMap.add(Script.INPUT_DIR, processContext.getInputDir());
        groovyMap.add(Script.OUTPUT_DIR, processContext.getOutputDir());
        groovyMap.add(Script.EXECUTABLE_PATH, processContext.getApplicationDeploymentDescription().getExecutablePath());
        groovyMap.add(Script.STANDARD_OUT_FILE, processContext.getStdoutLocation());
        groovyMap.add(Script.STANDARD_ERROR_FILE, processContext.getStderrLocation());
        groovyMap.add(Script.SCRATCH_LOCATION, processContext.getScratchLocation());
        groovyMap.add(Script.GATEWAY_ID, processContext.getGatewayId());
        groovyMap.add(Script.GATEWAY_USER_NAME, processContext.getProcessModel().getUserName());
        groovyMap.add(Script.APPLICATION_NAME, processContext.getApplicationInterfaceDescription().getApplicationName());

        groovyMap.add(Script.ACCOUNT_STRING, processContext.getAllocationProjectNumber());
        groovyMap.add(Script.RESERVATION, processContext.getReservation());

        // To make job name alpha numeric
        groovyMap.add(Script.JOB_NAME, "A" + String.valueOf(generateJobName()));
        groovyMap.add(Script.WORKING_DIR, processContext.getWorkingDir());

        List<String> inputValues = getProcessInputValues(processModel.getProcessInputs());
        inputValues.addAll(getProcessOutputValues(processModel.getProcessOutputs()));
        groovyMap.add(Script.INPUTS, inputValues);

        groovyMap.add(Script.USER_NAME, processContext.getJobSubmissionRemoteCluster().getServerInfo().getUserName());
        groovyMap.add(Script.SHELL_NAME, "/bin/bash");
        // get walltime
        if (taskContext != null) {
            try {
                JobSubmissionTaskModel jobSubmissionTaskModel = ((JobSubmissionTaskModel) taskContext.getSubTaskModel());
                if (jobSubmissionTaskModel.getWallTime() > 0) {
                    groovyMap.add(Script.MAX_WALL_TIME,
                            JobSubmissionUtils.maxWallTimeCalculator(jobSubmissionTaskModel.getWallTime()));
                }
            } catch (TException e) {
                log.error("Error while getting job submission sub task model", e);
            }
        }

        // NOTE: Give precedence to data comes with experiment
        // qos per queue
        String qoS = getQoS(processContext.getQualityOfService(), processContext.getQueueName());
        if (qoS != null) {
            groovyMap.add(Script.QUALITY_OF_SERVICE, qoS);
        }
        ComputationalResourceSchedulingModel scheduling = processModel.getProcessResourceSchedule();
        if (scheduling != null) {
            int totalNodeCount = scheduling.getNodeCount();
            int totalCPUCount = scheduling.getTotalCPUCount();

            if (isValid(scheduling.getQueueName())) {
                groovyMap.add(Script.QUEUE_NAME, scheduling.getQueueName());
            }
            if (totalNodeCount > 0) {
                groovyMap.add(Script.NODES, totalNodeCount);
            }
            if (totalCPUCount > 0) {
                int ppn = totalCPUCount / totalNodeCount;
                groovyMap.add(Script.PROCESS_PER_NODE, ppn);
                groovyMap.add(Script.CPU_COUNT, totalCPUCount);
            }
            // max wall time may be set before this level if jobsubmission task has wall time configured to this job,
            // if so we ignore scheduling configuration.
            if (scheduling.getWallTimeLimit() > 0 && groovyMap.get(Script.MAX_WALL_TIME) == null) {
                groovyMap.add(Script.MAX_WALL_TIME,
                        JobSubmissionUtils.maxWallTimeCalculator(scheduling.getWallTimeLimit()));
                if (resourceJobManager != null) {
                    if (resourceJobManager.getResourceJobManagerType().equals(ResourceJobManagerType.LSF)) {
                        groovyMap.add(Script.MAX_WALL_TIME,
                                JobSubmissionUtils.maxWallTimeCalculator(scheduling.getWallTimeLimit()));
                    }
                }
            }
            if (scheduling.getTotalPhysicalMemory() > 0) {
                groovyMap.add(Script.USED_MEM, scheduling.getTotalPhysicalMemory());
            }
            if (isValid(scheduling.getOverrideLoginUserName())) {
                groovyMap.add(Script.USER_NAME, scheduling.getOverrideLoginUserName());
            }
            if (isValid(scheduling.getOverrideAllocationProjectNumber())) {
                groovyMap.add(Script.ACCOUNT_STRING, scheduling.getOverrideAllocationProjectNumber());
            }
            if (isValid(scheduling.getStaticWorkingDir())) {
                groovyMap.add(Script.WORKING_DIR, scheduling.getStaticWorkingDir());
            }
        } else {
            log.error("Task scheduling cannot be null at this point..");
        }

        ApplicationDeploymentDescription appDepDescription = processContext.getApplicationDeploymentDescription();
        List<CommandObject> moduleCmds = appDepDescription.getModuleLoadCmds();
        if (moduleCmds != null) {
            List<String> modulesCmdCollect = moduleCmds.stream()
                    .sorted((e1, e2) -> e1.getCommandOrder() - e2.getCommandOrder())
                    .map(map -> map.getCommand())
                    .collect(Collectors.toList());
            groovyMap.add(Script.MODULE_COMMANDS, modulesCmdCollect);
        }

        List<CommandObject> preJobCommands = appDepDescription.getPreJobCommands();
        if (preJobCommands != null) {
            List<String> preJobCmdCollect = preJobCommands.stream()
                    .sorted((e1, e2) -> e1.getCommandOrder() - e2.getCommandOrder())
                    .map(map -> parseCommands(map.getCommand(), groovyMap))
                    .collect(Collectors.toList());
            groovyMap.add(Script.PRE_JOB_COMMANDS, preJobCmdCollect);
        }

        List<CommandObject> postJobCommands = appDepDescription.getPostJobCommands();
        if (postJobCommands != null) {
            List<String> postJobCmdCollect = postJobCommands.stream()
                    .sorted((e1, e2) -> e1.getCommandOrder() - e2.getCommandOrder())
                    .map(map -> parseCommands(map.getCommand(), groovyMap))
                    .collect(Collectors.toList());
            groovyMap.add(Script.POST_JOB_COMMANDS, postJobCmdCollect);
        }

        ApplicationParallelismType parallelism = appDepDescription.getParallelism();
        if (parallelism != null) {
            if (parallelism != ApplicationParallelismType.SERIAL) {
                Map<ApplicationParallelismType, String> parallelismPrefix = processContext.getResourceJobManager().getParallelismPrefix();
                if (parallelismPrefix != null){
                    String parallelismCommand = parallelismPrefix.get(parallelism);
                    if (parallelismCommand != null){
                        groovyMap.add(Script.JOB_SUBMITTER_COMMAND, parallelismCommand);
                    }else {
                        throw new WorkerException("Parallelism prefix is not defined for given parallelism type " + parallelism + ".. Please define the parallelism prefix at App Catalog");
                    }
                }
            }
        }
        return groovyMap;
    }

    public static ResourceJobManager getResourceJobManager(ProcessContext processContext) {
        try {
            JobSubmissionProtocol submissionProtocol = getPreferredJobSubmissionProtocol(processContext);
            JobSubmissionInterface jobSubmissionInterface = getPreferredJobSubmissionInterface(processContext);
            if (submissionProtocol == JobSubmissionProtocol.SSH ) {
                SSHJobSubmission sshJobSubmission = JobSubmissionUtils.getSSHJobSubmission(jobSubmissionInterface.getJobSubmissionInterfaceId());
                if (sshJobSubmission != null) {
                    return sshJobSubmission.getResourceJobManager();
                }
            } else if (submissionProtocol == JobSubmissionProtocol.LOCAL) {
                LOCALSubmission localJobSubmission = JobSubmissionUtils.getLocalJobSubmission(jobSubmissionInterface.getJobSubmissionInterfaceId());
                if (localJobSubmission != null) {
                    return localJobSubmission.getResourceJobManager();
                }
            } else if (submissionProtocol == JobSubmissionProtocol.SSH_FORK){
                SSHJobSubmission sshJobSubmission = JobSubmissionUtils.getSSHJobSubmission(jobSubmissionInterface.getJobSubmissionInterfaceId());
                if (sshJobSubmission != null) {
                    return sshJobSubmission.getResourceJobManager();
                }
            }
        } catch (AppCatalogException e) {
            log.error("Error occured while retrieving resource job manager", e);
        }
        return null;
    }

    private static JobSubmissionProtocol getPreferredJobSubmissionProtocol(ProcessContext context) throws AppCatalogException {
        try {
            GwyResourceProfile gatewayProfile = context.getAppCatalog().getGatewayProfile();
            String resourceHostId = context.getComputeResourceDescription().getComputeResourceId();
            ComputeResourcePreference preference = gatewayProfile.getComputeResourcePreference(context.getGatewayId()
                    , resourceHostId);
            return preference.getPreferredJobSubmissionProtocol();
        } catch (AppCatalogException e) {
            log.error("Error occurred while initializing app catalog", e);
            throw new AppCatalogException("Error occurred while initializing app catalog", e);
        }
    }

    public static File createJobFile(GroovyMap groovyMap, TaskContext tc, JobManagerConfiguration jMC)
            throws WorkerException {
        try {
            int number = new SecureRandom().nextInt();
            number = (number < 0 ? -number : number);
            File tempJobFile = new File(JobSubmissionUtils.getLocalDataDir(tc), "job_" + Integer.toString(number) + jMC.getScriptExtension());
            FileUtils.writeStringToFile(tempJobFile, generateScript(groovyMap, jMC.getJobDescriptionTemplateName()));
            return tempJobFile;
        } catch (IOException e) {
            throw new WorkerException("Error while writing script content to temp file");
        }
    }

    private static String generateScript(GroovyMap groovyMap, String templateName) throws WorkerException {
        URL templateUrl = ApplicationSettings.loadFile(templateName);
        if (templateUrl == null) {
            String error = "Template file '" + templateName + "' not found";
            throw new WorkerException(error);
        }
        File template = new File(templateUrl.getPath());
        TemplateEngine engine = new GStringTemplateEngine();
        Writable make;
        try {
            make = engine.createTemplate(template).make(groovyMap);
        } catch (Exception e) {
            throw new WorkerException("Error while generating script using groovy map");
        }
        return make.toString();
    }

    private static boolean isEmailBasedJobMonitor(ProcessContext processContext) throws WorkerException, AppCatalogException {
        JobSubmissionProtocol jobSubmissionProtocol = getPreferredJobSubmissionProtocol(processContext);
        JobSubmissionInterface jobSubmissionInterface = getPreferredJobSubmissionInterface(processContext);
        if (jobSubmissionProtocol == JobSubmissionProtocol.SSH) {
            String jobSubmissionInterfaceId = jobSubmissionInterface.getJobSubmissionInterfaceId();
            SSHJobSubmission sshJobSubmission = processContext.getAppCatalog().getComputeResource().getSSHJobSubmission(jobSubmissionInterfaceId);
            MonitorMode monitorMode = sshJobSubmission.getMonitorMode();
            return monitorMode != null && monitorMode == MonitorMode.JOB_EMAIL_NOTIFICATION_MONITOR;
        } else {
            return false;
        }
    }

    public static void saveJobModel(ProcessContext processContext, JobModel jobModel) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            experimentCatalog.add(ExpCatChildDataType.JOB, jobModel, processContext.getProcessId());
        } catch (RegistryException e) {
            String msg = "expId: " + processContext.getExperimentId() + " processId: " + processContext.getProcessId()
                    + " jobId: " + jobModel.getJobId() + " : - Error while saving Job Model";
            throw new WorkerException(msg, e);
        }
    }

    private static JobSubmissionInterface getPreferredJobSubmissionInterface(ProcessContext processContext) throws AppCatalogException {
        try {
            String resourceHostId = processContext.getComputeResourceDescription().getComputeResourceId();
            JobSubmissionProtocol preferredJobSubmissionProtocol = processContext.getPreferredJobSubmissionProtocol();
            ComputeResourceDescription resourceDescription = processContext.getAppCatalog().getComputeResource().getComputeResource(resourceHostId);
            List<JobSubmissionInterface> jobSubmissionInterfaces = resourceDescription.getJobSubmissionInterfaces();
            Map<JobSubmissionProtocol, List<JobSubmissionInterface>> orderedInterfaces = new HashMap<>();
            List<JobSubmissionInterface> interfaces = new ArrayList<>();
            if (jobSubmissionInterfaces != null && !jobSubmissionInterfaces.isEmpty()) {
                for (JobSubmissionInterface submissionInterface : jobSubmissionInterfaces){

                    if (preferredJobSubmissionProtocol != null){
                        if (preferredJobSubmissionProtocol.toString().equals(submissionInterface.getJobSubmissionProtocol().toString())){
                            if (orderedInterfaces.containsKey(submissionInterface.getJobSubmissionProtocol())){
                                List<JobSubmissionInterface> interfaceList = orderedInterfaces.get(submissionInterface.getJobSubmissionProtocol());
                                interfaceList.add(submissionInterface);
                            }else {
                                interfaces.add(submissionInterface);
                                orderedInterfaces.put(submissionInterface.getJobSubmissionProtocol(), interfaces);
                            }
                        }
                    }else {
                        Collections.sort(jobSubmissionInterfaces, new Comparator<JobSubmissionInterface>() {
                            @Override
                            public int compare(JobSubmissionInterface jobSubmissionInterface, JobSubmissionInterface jobSubmissionInterface2) {
                                return jobSubmissionInterface.getPriorityOrder() - jobSubmissionInterface2.getPriorityOrder();
                            }
                        });
                    }
                }
                interfaces = orderedInterfaces.get(preferredJobSubmissionProtocol);
                Collections.sort(interfaces, new Comparator<JobSubmissionInterface>() {
                    @Override
                    public int compare(JobSubmissionInterface jobSubmissionInterface, JobSubmissionInterface jobSubmissionInterface2) {
                        return jobSubmissionInterface.getPriorityOrder() - jobSubmissionInterface2.getPriorityOrder();
                    }
                });
            } else {
                throw new AppCatalogException("Compute resource should have at least one job submission interface defined...");
            }
            return interfaces.get(0);
        } catch (AppCatalogException e) {
            throw new AppCatalogException("Error occurred while retrieving data from app catalog", e);
        }
    }

    private static LOCALSubmission getLocalJobSubmission(String submissionId) throws AppCatalogException {
        try {
            AppCatalog appCatalog = RegistryFactory.getAppCatalog();
            return appCatalog.getComputeResource().getLocalJobSubmission(submissionId);
        } catch (Exception e) {
            String errorMsg = "Error while retrieving local job submission with submission id : " + submissionId;
            log.error(errorMsg, e);
            throw new AppCatalogException(errorMsg, e);
        }
    }

    public static UnicoreJobSubmission getUnicoreJobSubmission(String submissionId) throws AppCatalogException {
        try {
            AppCatalog appCatalog = RegistryFactory.getAppCatalog();
            return appCatalog.getComputeResource().getUNICOREJobSubmission(submissionId);
        } catch (Exception e) {
            String errorMsg = "Error while retrieving UNICORE job submission with submission id : " + submissionId;
            log.error(errorMsg, e);
            throw new AppCatalogException(errorMsg, e);
        }
    }

    public static SSHJobSubmission getSSHJobSubmission(String submissionId) throws AppCatalogException {
        try {
            AppCatalog appCatalog = RegistryFactory.getAppCatalog();
            return appCatalog.getComputeResource().getSSHJobSubmission(submissionId);
        } catch (Exception e) {
            String errorMsg = "Error while retrieving SSH job submission with submission id : " + submissionId;
            log.error(errorMsg, e);
            throw new AppCatalogException(errorMsg, e);
        }
    }

    public static CloudJobSubmission getCloudJobSubmission(String submissionId) throws RegistryException {
        try {
            AppCatalog appCatalog = RegistryFactory.getAppCatalog();
            return appCatalog.getComputeResource().getCloudJobSubmission(submissionId);
        } catch (Exception e) {
            String errorMsg = "Error while retrieving SSH job submission with submission id : " + submissionId;
            log.error(errorMsg, e);
            throw new RegistryException(errorMsg, e);
        }
    }

    private static String listToCsv(List<String> listOfStrings, char separator) {
        StringBuilder sb = new StringBuilder();

        // all but last
        for (int i = 0; i < listOfStrings.size() - 1; i++) {
            sb.append(listOfStrings.get(i));
            sb.append(separator);
        }

        // last string, no separator
        if (listOfStrings.size() > 0) {
            sb.append(listOfStrings.get(listOfStrings.size() - 1));
        }

        return sb.toString();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    private static boolean isValid(String str) {
        return str != null && !str.isEmpty();
    }

    private static void setMailAddresses(ProcessContext processContext, GroovyMap groovyMap)
            throws WorkerException, AppCatalogException, ApplicationSettingsException {

        ProcessModel processModel =  processContext.getProcessModel();
        String emailIds = null;
        if (isEmailBasedJobMonitor(processContext)) {
            emailIds = ServerSettings.getEmailBasedMonitorAddress();
        }
        if (ServerSettings.getSetting(ServerSettings.JOB_NOTIFICATION_ENABLE).equalsIgnoreCase("true")) {
            String userJobNotifEmailIds = ServerSettings.getSetting(ServerSettings.JOB_NOTIFICATION_EMAILIDS);
            if (userJobNotifEmailIds != null && !userJobNotifEmailIds.isEmpty()) {
                if (emailIds != null && !emailIds.isEmpty()) {
                    emailIds += ("," + userJobNotifEmailIds);
                } else {
                    emailIds = userJobNotifEmailIds;
                }
            }
            if (processModel.isEnableEmailNotification()) {
                List<String> emailList = processModel.getEmailAddresses();
                String elist = JobSubmissionUtils.listToCsv(emailList, ',');
                if (elist != null && !elist.isEmpty()) {
                    if (emailIds != null && !emailIds.isEmpty()) {
                        emailIds = emailIds + "," + elist;
                    } else {
                        emailIds = elist;
                    }
                }
            }
        }
        if (emailIds != null && !emailIds.isEmpty()) {
            log.info("Email list: " + emailIds);
            groovyMap.add(Script.MAIL_ADDRESS, emailIds);
        }
    }

    private static List<String> getProcessOutputValues(List<OutputDataObjectType> processOutputs) {
        List<String> inputValues = new ArrayList<>();
        if (processOutputs != null) {
            for (OutputDataObjectType output : processOutputs) {
                if (output.getApplicationArgument() != null
                        && !output.getApplicationArgument().equals("")) {
                    inputValues.add(output.getApplicationArgument());
                }
                if (output.getValue() != null && !output.getValue().equals("") && output.isRequiredToAddedToCommandLine()) {
                    if (output.getType() == DataType.URI) {
                        String filePath = output.getValue();
                        filePath = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.length());
                        inputValues.add(filePath);
                    }
                }
            }
        }
        return inputValues;
    }

    private static List<String> getProcessInputValues(List<InputDataObjectType> processInputs) {
        List<String> inputValues = new ArrayList<String>();
        if (processInputs != null) {

            // sort the inputs first and then build the command ListR
            Comparator<InputDataObjectType> inputOrderComparator = new Comparator<InputDataObjectType>() {
                @Override
                public int compare(InputDataObjectType inputDataObjectType, InputDataObjectType t1) {
                    return inputDataObjectType.getInputOrder() - t1.getInputOrder();
                }
            };
            Set<InputDataObjectType> sortedInputSet = new TreeSet<InputDataObjectType>(inputOrderComparator);
            sortedInputSet.addAll(processInputs);

            for (InputDataObjectType inputDataObjectType : sortedInputSet) {
                if (!inputDataObjectType.isRequiredToAddedToCommandLine()) {
                    continue;
                }
                if (inputDataObjectType.getApplicationArgument() != null
                        && !inputDataObjectType.getApplicationArgument().equals("")) {
                    inputValues.add(inputDataObjectType.getApplicationArgument());
                }

                if (inputDataObjectType.getValue() != null
                        && !inputDataObjectType.getValue().equals("")) {
                    if (inputDataObjectType.getType() == DataType.URI) {
                        // set only the relative path
                        String filePath = inputDataObjectType.getValue();
                        filePath = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.length());
                        inputValues.add(filePath);
                    } else if (inputDataObjectType.getType() == DataType.URI_COLLECTION) {
                        String filePaths = inputDataObjectType.getValue();
                        String[] paths = filePaths.split(WorkerConstants.MULTIPLE_INPUTS_SPLITTER);
                        String filePath;
                        String inputs = "";
                        int i = 0;
                        for (; i < paths.length - 1; i++) {
                            filePath = paths[i];
                            filePath = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.length());
                            // File names separate by a space
                            inputs += filePath + " ";
                        }
                        inputs += paths[i];
                        inputValues.add(inputs);
                    } else {
                        inputValues.add(inputDataObjectType.getValue());
                    }

                }
            }
        }
        return inputValues;
    }

    private static String getQoS(String qualityOfService, String preferredBatchQueue) {
        if(preferredBatchQueue == null  || preferredBatchQueue.isEmpty()
                ||  qualityOfService == null  || qualityOfService.isEmpty()) return null;
        final String qos = "qos";
        Pattern pattern = Pattern.compile(preferredBatchQueue + "=(?<" + qos + ">[^,]*)");
        Matcher matcher = pattern.matcher(qualityOfService);
        if (matcher.find()) {
            return matcher.group(qos);
        }
        return null;
    }

    private static int generateJobName() {
        Random random = new Random();
        int i = random.nextInt(Integer.MAX_VALUE);
        i = i + 99999999;
        if (i < 0) {
            i = i * (-1);
        }
        return i;
    }

    private static String parseCommands(String value, GroovyMap bindMap) {
        TemplateEngine templateEngine = new GStringTemplateEngine();
        try {
            return templateEngine.createTemplate(value).make(bindMap).toString();
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalArgumentException("Error while parsing command " + value
                    + " , Invalid command or incomplete bind map");
        }
    }

    private static String maxWallTimeCalculator(int maxWalltime) {
        if (maxWalltime < 60) {
            return "00:" + maxWalltime + ":00";
        } else {
            int minutes = maxWalltime % 60;
            int hours = maxWalltime / 60;
            return hours + ":" + minutes + ":00";
        }
    }

    private static File getLocalDataDir(TaskContext taskContext) {
        String outputPath = ServerSettings.getLocalDataLocation();
        outputPath = (outputPath.endsWith(File.separator) ? outputPath : outputPath + File.separator);
        return new File(outputPath + taskContext.getParentProcessContext() .getProcessId());
    }
}
