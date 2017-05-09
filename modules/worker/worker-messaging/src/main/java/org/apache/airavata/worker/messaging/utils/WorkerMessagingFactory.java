package org.apache.airavata.worker.messaging.utils;

import org.apache.airavata.common.exception.AiravataException;
import org.apache.airavata.messaging.core.MessagingFactory;
import org.apache.airavata.messaging.core.Subscriber;
import org.apache.airavata.worker.messaging.handler.WorkerMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ajinkya on 5/2/17.
 */
public class WorkerMessagingFactory {

    private final static Logger log = LoggerFactory.getLogger(WorkerMessagingFactory.class);
    private final static Map<String, Subscriber> SUBSCRIBER_MAP;

    static{
        SUBSCRIBER_MAP = new HashMap<>();
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
