package org.apache.airavata.worker.core.config;

import org.apache.airavata.model.task.TaskTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ajinkya on 5/8/17.
 */
public class TaskImplementationConfig {

    private String taskType;
    private String implementationClass;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(String implementationClass) {
        this.implementationClass = implementationClass;
    }

}
