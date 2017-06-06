package org.apache.airavata.worker.task.datastaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class DataStagingConfigException extends Exception {

    private static final Logger log = LoggerFactory.getLogger(DataStagingConfigException.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DataStagingConfigException(String s) {
        super(s);
    }

    public DataStagingConfigException(Exception e) {
        super(e);
        log.error(e.getMessage(),e);
    }

    public DataStagingConfigException(String s, Throwable throwable) {
        super(s, throwable);
        log.error(s,throwable);
    }
}
