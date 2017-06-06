package org.apache.airavata.worker.task.datastaging.config;

import org.apache.airavata.model.data.movement.DataMovementProtocol;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class DataStagingYamlConfig {

    private static final String TASK_CLASS = "taskClass";
    private static final String FILE_TRANSFER_TASKS = "fileTransferTasks";
    private static final String TRANSFER_PROTOCOL = "transferProtocol";
    private static final String PROPERTIES = "properties";
    private List<DataTransferTaskConfig> fileTransferTasks = new ArrayList<>();

    public DataStagingYamlConfig() throws DataStagingConfigException {
        InputStream resourceAsStream = DataStagingYamlConfig.class.getClassLoader().
                getResourceAsStream("data-staging-config.yaml");
        parse(resourceAsStream);
    }

    private void parse(InputStream resourceAsStream) throws DataStagingConfigException {
        if (resourceAsStream == null) {
            throw new DataStagingConfigException("Configuration file{data-staging-config.yaml} is not fund");
        }
        Yaml yaml = new Yaml();
        Object load = yaml.load(resourceAsStream);
        if (load == null) {
            throw new DataStagingConfigException("Yaml configuration object null");
        }

        if (load instanceof Map) {
            Map<String, Object> loadMap = (Map<String, Object>) load;
            String identifier;

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
        }
    }

    public List<DataTransferTaskConfig> getFileTransferTasks() {
        return fileTransferTasks;
    }
}
