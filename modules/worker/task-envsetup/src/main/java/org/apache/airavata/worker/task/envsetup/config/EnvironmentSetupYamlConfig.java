package org.apache.airavata.worker.task.envsetup.config;

import org.apache.airavata.model.task.TaskTypes;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class EnvironmentSetupYamlConfig {

    private static final String TASK_CLASS = "taskClass";
    private static final String ENV_SETUP_TASKS = "envSetupTasks";
    private static final String TASK_TYPE = "type";
    private List<EnvironmentSetupTaskConfig> envSetupTasks = new ArrayList<>();

    public EnvironmentSetupYamlConfig() throws EnvironmentSetupConfigException {
        InputStream resourceAsStream = EnvironmentSetupYamlConfig.class.getClassLoader().
                getResourceAsStream("env-setup-config.yaml");
        parse(resourceAsStream);
    }

    private void parse(InputStream resourceAsStream) throws EnvironmentSetupConfigException {
        if (resourceAsStream == null) {
            throw new EnvironmentSetupConfigException("Configuration file{env-setup-config.yaml} is not fund");
        }
        Yaml yaml = new Yaml();
        Object load = yaml.load(resourceAsStream);
        if (load == null) {
            throw new EnvironmentSetupConfigException("Yaml configuration object null");
        }

        if (load instanceof Map) {
            Map<String, Object> loadMap = (Map<String, Object>) load;
            String identifier;

            List<Map<String, Object>> envSetupYamls = (List<Map<String, Object>>) loadMap.get(ENV_SETUP_TASKS);
            EnvironmentSetupTaskConfig environmentSetupTaskConfig;
            if (envSetupYamls != null) {
                for (Map<String, Object> envSetupConfig : envSetupYamls) {
                    environmentSetupTaskConfig = new EnvironmentSetupTaskConfig();
                    identifier = ((String) envSetupConfig.get(TASK_TYPE));
                    environmentSetupTaskConfig.setTaskType(TaskTypes.valueOf(identifier));
                    environmentSetupTaskConfig.setTaskClass(((String) envSetupConfig.get(TASK_CLASS)));
                    envSetupTasks.add(environmentSetupTaskConfig);
                }
            }
        }
    }

    public List<EnvironmentSetupTaskConfig> getEnvironmentSetupTasks() {
        return envSetupTasks;
    }
}
