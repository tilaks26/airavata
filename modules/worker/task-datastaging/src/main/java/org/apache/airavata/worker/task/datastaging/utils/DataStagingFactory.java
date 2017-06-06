package org.apache.airavata.worker.task.datastaging.utils;

import org.apache.airavata.common.exception.ApplicationSettingsException;
import org.apache.airavata.credential.store.store.CredentialStoreException;
import org.apache.airavata.model.data.movement.DataMovementProtocol;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.core.authentication.SSHKeyAuthentication;
import org.apache.airavata.worker.core.config.TaskImplementationConfig;
import org.apache.airavata.worker.core.config.WorkerYamlConfigruation;
import org.apache.airavata.worker.core.context.ProcessContext;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.apache.airavata.worker.core.task.Task;
import org.apache.airavata.worker.core.utils.WorkerFactory;
import org.apache.airavata.worker.task.datastaging.config.DataStagingConfigException;
import org.apache.airavata.worker.task.datastaging.config.DataStagingYamlConfig;
import org.apache.airavata.worker.task.datastaging.config.DataTransferTaskConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ajinkya on 4/13/17.
 */
public class DataStagingFactory {

    private static final Logger log = LoggerFactory.getLogger(DataStagingFactory.class);

    private static boolean isDataStagingConfigurationLoaded = false;
    private static Map<DataMovementProtocol, Task> dataMovementTask = new HashMap<>();

    public static void loadConfiguration() throws DataStagingConfigException {
        if (!isDataStagingConfigurationLoaded) {
            DataStagingYamlConfig config = new DataStagingYamlConfig();
            try {
                for (DataTransferTaskConfig dataTransferTaskConfig : config.getFileTransferTasks()) {
                    String taskClass = dataTransferTaskConfig.getTaskClass();
                    Class<?> aClass = Class.forName(taskClass);
                    Constructor<?> constructor = aClass.getConstructor();
                    Task task = (Task) constructor.newInstance();
                    task.init(dataTransferTaskConfig.getProperties());
                    dataMovementTask.put(dataTransferTaskConfig.getTransferProtocol(), task);
                }
            }catch (Exception e) {
                throw new DataStagingConfigException("Data staging config issue", e);
            }
            isDataStagingConfigurationLoaded =  true;
        }
    }

    public static SSHKeyAuthentication getComputerResourceSSHKeyAuthentication(ProcessContext pc)
            throws WorkerException, CredentialStoreException {
        try {
            return WorkerFactory.getSshKeyAuthentication(pc.getGatewayId(),
                    pc.getComputeResourceLoginUserName(),
                    pc.getComputeResourceCredentialToken());
        } catch (ApplicationSettingsException | IllegalAccessException | InstantiationException e) {
            throw new WorkerException("Couldn't build ssh authentication object", e);
        }
    }

}
