package org.apache.airavata.worker.messaging.handler;

import org.apache.airavata.common.utils.ThriftUtils;
import org.apache.airavata.messaging.core.MessageContext;
import org.apache.airavata.messaging.core.MessageHandler;
import org.apache.airavata.worker.core.context.TaskContext;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ajinkya on 5/2/17.
 */
public class WorkerMessageHandler implements MessageHandler {

    private final static Logger log = LoggerFactory.getLogger(WorkerMessageHandler.class);

    @Override
    public void onMessage(MessageContext messageContext) {
        log.info("Worker received message. Message Id : " +  messageContext.getMessageId());
        try {
            TBase messageEvent = messageContext.getEvent();
            byte[] bytes = new byte[0];
            bytes = ThriftUtils.serializeThriftObject(messageEvent);
            //ThriftUtils.createThriftFromBytes(bytes, event);
            TaskContext taskContext = new TaskContext();

            switch (taskContext.getTaskType()){

                case ENV_SETUP: //TODO: call environment setup
                    break;

                case DATA_STAGING: //TODO: call data staging
                    break;

                case JOB_SUBMISSION: //TODO: call job submission
                    break;

                case MONITORING: //TODO: call monitoring
                    break;
            }

        } catch (TException e) {
            log.error("Error processing messaging. Message Id : " +  messageContext.getMessageId());
        }
    }
}
