package org.apache.airavata.worker.task.datastaging.utils;

import org.apache.airavata.model.application.io.OutputDataObjectType;
import org.apache.airavata.model.data.replica.*;
import org.apache.airavata.model.experiment.ExperimentModel;
import org.apache.airavata.model.process.ProcessModel;
import org.apache.airavata.registry.core.experiment.catalog.impl.RegistryFactory;
import org.apache.airavata.registry.cpi.ExperimentCatalog;
import org.apache.airavata.registry.cpi.ExperimentCatalogModelType;
import org.apache.airavata.registry.cpi.RegistryException;
import org.apache.airavata.registry.cpi.ReplicaCatalog;
import org.apache.airavata.worker.core.context.ProcessContext;
import org.apache.airavata.worker.core.context.TaskContext;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by Ajinkya on 4/13/17.
 */
public class DataStagingUtils {

    private static final Logger log = LoggerFactory.getLogger(DataStagingUtils.class);

    public static URI getDestinationURI(TaskContext taskContext, String hostName, String inputPath, String fileName) throws URISyntaxException {
        String experimentDataDir = taskContext.getParentProcessContext().getProcessModel().getExperimentDataDir();
        String filePath;
        if(experimentDataDir != null && !experimentDataDir.isEmpty()) {
            if(!experimentDataDir.endsWith(File.separator)){
                experimentDataDir += File.separator;
            }
            if (experimentDataDir.startsWith(File.separator)) {
                filePath = experimentDataDir + fileName;
            } else {
                filePath = inputPath + experimentDataDir + fileName;
            }
        } else {
            filePath = inputPath + taskContext.getParentProcessContext().getProcessId() + File.separator + fileName;
        }
        //FIXME
        return new URI("file", taskContext.getParentProcessContext().getStorageResourceLoginUserName(), hostName, 22, filePath, null, null);

    }
    public static void saveExperimentOutput(ProcessContext processContext, String outputName, String outputVal) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            String experimentId = processContext.getExperimentId();
            ExperimentModel experiment = (ExperimentModel)experimentCatalog.get(ExperimentCatalogModelType.EXPERIMENT, experimentId);
            List<OutputDataObjectType> experimentOutputs = experiment.getExperimentOutputs();
            if (experimentOutputs != null && !experimentOutputs.isEmpty()){
                for (OutputDataObjectType expOutput : experimentOutputs){
                    if (expOutput.getName().equals(outputName)){
                        DataProductModel dataProductModel = new DataProductModel();
                        dataProductModel.setGatewayId(processContext.getGatewayId());
                        dataProductModel.setOwnerName(processContext.getProcessModel().getUserName());
                        dataProductModel.setProductName(outputName);
                        dataProductModel.setDataProductType(DataProductType.FILE);

                        DataReplicaLocationModel replicaLocationModel = new DataReplicaLocationModel();
                        replicaLocationModel.setStorageResourceId(processContext.getStorageResource().getStorageResourceId());
                        replicaLocationModel.setReplicaName(outputName + " gateway data store copy");
                        replicaLocationModel.setReplicaLocationCategory(ReplicaLocationCategory.GATEWAY_DATA_STORE);
                        replicaLocationModel.setReplicaPersistentType(ReplicaPersistentType.TRANSIENT);
                        replicaLocationModel.setFilePath(outputVal);
                        dataProductModel.addToReplicaLocations(replicaLocationModel);

                        ReplicaCatalog replicaCatalog = RegistryFactory.getReplicaCatalog();
                        String productUri = replicaCatalog.registerDataProduct(dataProductModel);
                        expOutput.setValue(productUri);
                    }
                }
            }
            experimentCatalog.update(ExperimentCatalogModelType.EXPERIMENT, experiment, experimentId);
        } catch (RegistryException e) {
            String msg = "expId: " + processContext.getExperimentId() + " processId: " + processContext.getProcessId()
                    + " : - Error while updating experiment outputs";
            throw new WorkerException(msg, e);
        }
    }

    public static void saveProcessOutput(ProcessContext processContext, String outputName, String outputVal) throws WorkerException {
        try {
            ExperimentCatalog experimentCatalog = processContext.getExperimentCatalog();
            String processId = processContext.getProcessId();
            List<OutputDataObjectType>  processOutputs = (List<OutputDataObjectType> )experimentCatalog.get(ExperimentCatalogModelType.PROCESS_OUTPUT, processId);
            if (processOutputs != null && !processOutputs.isEmpty()){
                for (OutputDataObjectType processOutput : processOutputs){
                    if (processOutput.getName().equals(outputName)){
                        processOutput.setValue(outputVal);
                    }
                }
            }
            ProcessModel processModel = processContext.getProcessModel();
            processModel.setProcessOutputs(processOutputs);
            experimentCatalog.update(ExperimentCatalogModelType.PROCESS, processModel, processId);
        } catch (RegistryException e) {
            String msg = "expId: " + processContext.getExperimentId() + " processId: " + processContext.getProcessId()
                    + " : - Error while updating experiment outputs";
            throw new WorkerException(msg, e);
        }
    }
}
