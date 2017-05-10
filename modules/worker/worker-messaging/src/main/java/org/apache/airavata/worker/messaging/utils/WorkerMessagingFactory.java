package org.apache.airavata.worker.messaging.utils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.jcraft.jsch.Session;
import org.apache.airavata.common.exception.AiravataException;
import org.apache.airavata.common.utils.ServerSettings;
import org.apache.airavata.messaging.core.MessagingFactory;
import org.apache.airavata.messaging.core.Subscriber;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.core.config.ResourceConfig;
import org.apache.airavata.worker.core.config.TaskImplementationConfig;
import org.apache.airavata.worker.core.config.WorkerYamlConfigruation;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.apache.airavata.worker.core.task.Task;
import org.apache.airavata.worker.messaging.handler.WorkerMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ajinkya on 5/2/17.
 */
public class WorkerMessagingFactory {

    private final static Logger log = LoggerFactory.getLogger(WorkerMessagingFactory.class);
    private final static Map<String, Subscriber> SUBSCRIBER_MAP;
    private static boolean isWorkerConfigurationLoaded = false;
    private static Map<TaskTypes, Task> taskImplementations = new HashMap<>();

    static{
        SUBSCRIBER_MAP = new HashMap<>();
    }

    public static void loadConfiguration() throws WorkerException {
        if (!isWorkerConfigurationLoaded) {
            WorkerYamlConfigruation config = new WorkerYamlConfigruation();
            try {
                for (TaskImplementationConfig taskImplementationConfig : config.getTaskImplementations()) {
                    String taskClass = taskImplementationConfig.getImplementationClass();
                    Class<?> aClass = Class.forName(taskClass);
                    Constructor<?> constructor = aClass.getConstructor();
                    Task task = (Task) constructor.newInstance();
                    taskImplementations.put(taskImplementationConfig.getTaskType(), task);
                }
            }catch (Exception e) {
                throw new WorkerException("Worker config issue", e);
            }
            isWorkerConfigurationLoaded =  true;
        }
    }

    public static Task getTaskImplementation(TaskTypes  taskType) {
        return taskImplementations.get(taskType);
    }

    public static Map<String, Subscriber> getSubscriberMap(){
        return SUBSCRIBER_MAP;
    }

    public static final boolean createSubscribers(List<String> tasks) throws AiravataException {

        log.info("Creating RabbitMQ subscribers for : " + tasks.toString());
        for(String task : tasks){

            log.info("Starting subscriber for task : " + task);
            //adding subscriber to map
            SUBSCRIBER_MAP.put(task, MessagingFactory.getWorkerEventSubscriber(new WorkerMessageHandler(), task));

            log.debug("Subscriber started for task : " + task);
        }
        return true;
    }
}
