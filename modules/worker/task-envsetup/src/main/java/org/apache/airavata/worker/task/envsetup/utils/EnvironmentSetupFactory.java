package org.apache.airavata.worker.task.envsetup.utils;

import org.apache.airavata.model.data.movement.DataMovementProtocol;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.core.task.Task;
import org.apache.airavata.worker.task.envsetup.config.EnvironmentSetupConfigException;
import org.apache.airavata.worker.task.envsetup.config.EnvironmentSetupTaskConfig;
import org.apache.airavata.worker.task.envsetup.config.EnvironmentSetupYamlConfig;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class EnvironmentSetupFactory {

    private static boolean isEnvSetupConfigurationLoaded = false;
    private static Map<TaskTypes, Task> envSetupTask = new HashMap<>();

    public static void loadConfiguration() throws EnvironmentSetupConfigException {
        if (!isEnvSetupConfigurationLoaded) {
            EnvironmentSetupYamlConfig config = new EnvironmentSetupYamlConfig();
            try {
                for (EnvironmentSetupTaskConfig envSetupTaskConfig : config.getEnvironmentSetupTasks()) {
                    String taskClass = envSetupTaskConfig.getTaskClass();
                    Class<?> aClass = Class.forName(taskClass);
                    Constructor<?> constructor = aClass.getConstructor();
                    Task task = (Task) constructor.newInstance();
                    envSetupTask.put(envSetupTaskConfig.getTaskType(), task);
                }
            }catch (Exception e) {
                throw new EnvironmentSetupConfigException("Data staging config issue", e);
            }
            isEnvSetupConfigurationLoaded =  true;
        }
    }
}
