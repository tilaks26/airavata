package org.apache.airavata.worker.task.datastaging.utils;

import org.apache.airavata.common.exception.ApplicationSettingsException;
import org.apache.airavata.credential.store.store.CredentialStoreException;
import org.apache.airavata.worker.core.authentication.SSHKeyAuthentication;
import org.apache.airavata.worker.core.context.ProcessContext;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.apache.airavata.worker.core.utils.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ajinkya on 4/13/17.
 */
public class DataStagingFactory {

    private static final Logger log = LoggerFactory.getLogger(DataStagingFactory.class);

    public static SSHKeyAuthentication getComputerResourceSSHKeyAuthentication(ProcessContext pc)
            throws WorkerException, CredentialStoreException {
        try {
            return WorkerFactory.getSshKeyAuthentication(pc.getGatewayId(),
                    pc.getComputeResourceLoginUserName(),
                    pc.getComputeResourceCredentialToken());
        } catch (ApplicationSettingsException | IllegalAccessException | InstantiationException e) {
            throw new WorkerException("Couldn't build ssh authentication object", e);
        }
    }

}
