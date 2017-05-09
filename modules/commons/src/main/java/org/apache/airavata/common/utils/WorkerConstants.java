package org.apache.airavata.common.utils;

import org.apache.airavata.model.dbevent.EntityType;
import org.apache.airavata.model.task.TaskTypes;

/**
 * Created by Ajinkya on 5/2/17.
 */
public class WorkerConstants {

    private final static String QUEUE_SUFFIX = ".queue";
    public final static String WORKER_EVENT_EXCHANGE_NAME = "worker.event.exchange";
    /**
     * Get the queue-name for the task, given service-name as enum
     * @param taskType
     * @return
     */
    public static String getQueueName(TaskTypes taskType) {
        return taskType.toString() + QUEUE_SUFFIX;
    }

    /**
     * Get the queue-name for the task, given task-name as string
     * @param taskType
     * @return
     */
    public static String getQueueName(String taskType) {
        return getQueueName(getTask(taskType));
    }

    /**
     * Get serviceName from EntityType
     * @param entityType
     * @return
     */
    public static String getDbEventServiceName(EntityType entityType) {
        for (DBEventService service : DBEventService.values()) {
            if (service.name().equals(entityType.name())) {
                return service.toString();
            }
        }
        return null;
    }

    /**
     * Get the task as enum, given the task-name as string
     * @param taskType
     * @return
     */
    private static TaskTypes getTask(String taskType) {
        for (TaskTypes task : TaskTypes.values()) {
            if (task.toString().equals(taskType)) {
                return task;
            }
        }
        return null;
    }
}
