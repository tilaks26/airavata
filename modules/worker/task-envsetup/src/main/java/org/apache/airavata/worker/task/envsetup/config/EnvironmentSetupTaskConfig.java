package org.apache.airavata.worker.task.envsetup.config;

import org.apache.airavata.model.task.TaskTypes;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class EnvironmentSetupTaskConfig {
    private TaskTypes taskType;
    private String taskClass;

    public TaskTypes getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskTypes taskType) {
        this.taskType = taskType;
    }

    public String getTaskClass() {
        return taskClass;
    }

    public void setTaskClass(String taskClass) {
        this.taskClass = taskClass;
    }

}
