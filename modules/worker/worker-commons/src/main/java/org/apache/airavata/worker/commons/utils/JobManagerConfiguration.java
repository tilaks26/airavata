package org.apache.airavata.worker.commons.utils;

import org.apache.airavata.worker.commons.cluster.OutputParser;
import org.apache.airavata.worker.commons.cluster.RawCommandInfo;

/**
 * Created by goshenoy on 4/12/17.
 */
public interface JobManagerConfiguration {

    public RawCommandInfo getCancelCommand(String jobID);

    public String getJobDescriptionTemplateName();

    public RawCommandInfo getMonitorCommand(String jobID);

    public RawCommandInfo getUserBasedMonitorCommand(String userName);

    public RawCommandInfo getJobIdMonitorCommand(String jobName, String userName);

    public String getScriptExtension();

    public RawCommandInfo getSubmitCommand(String workingDirectory, String pbsFilePath);

    public OutputParser getParser();

    public String getInstalledPath();

    public String getBaseCancelCommand();

    public String getBaseMonitorCommand();

    public String getBaseSubmitCommand();
}
