package org.apache.airavata.worker.task.envsetup.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ajinkya on 6/1/17.
 */
public class EnvironmentSetupConfigException extends Exception {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentSetupConfigException.class);

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public EnvironmentSetupConfigException(String s) {
        super(s);
    }

    public EnvironmentSetupConfigException(Exception e) {
        super(e);
        log.error(e.getMessage(),e);
    }

    public EnvironmentSetupConfigException(String s, Throwable throwable) {
        super(s, throwable);
        log.error(s,throwable);
    }
}
