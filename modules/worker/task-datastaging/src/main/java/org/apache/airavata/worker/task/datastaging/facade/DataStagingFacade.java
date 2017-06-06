package org.apache.airavata.worker.task.datastaging.facade;

import org.apache.airavata.model.status.TaskStatus;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.core.context.TaskContext;
import org.apache.airavata.worker.core.task.Task;
import org.apache.airavata.worker.core.task.TaskException;

import java.util.Map;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class DataStagingFacade implements Task {
    @Override
    public void init(Map<String, String> propertyMap) throws TaskException {

    }

    @Override
    public TaskStatus execute(TaskContext taskContext) {
        return null;
    }

    @Override
    public TaskStatus recover(TaskContext taskContext) {
        return null;
    }

    @Override
    public TaskTypes getType() {
        return null;
    }
}
