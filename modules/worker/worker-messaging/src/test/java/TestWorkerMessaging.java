import org.apache.airavata.common.exception.AiravataException;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.core.exceptions.WorkerException;
import org.apache.airavata.worker.core.task.Task;
import org.apache.airavata.worker.core.utils.WorkerFactory;
import org.apache.airavata.worker.messaging.utils.WorkerMessagingFactory;
import org.apache.airavata.worker.task.datastaging.impl.DataStageTask;
import org.apache.airavata.worker.task.datastaging.utils.DataStagingFactory;
import org.apache.airavata.worker.task.envsetup.impl.EnvironmentSetupTask;
import org.apache.airavata.worker.task.jobsubmission.impl.DefaultJobSubmissionTask;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ajinkya on 5/3/17.
 */
public class TestWorkerMessaging {

//    @Test
//    public void testSubscriberCreation(){
//        List<String> tasks = new ArrayList<String>(){{add(TaskTypes.ENV_SETUP.toString());add(TaskTypes.DATA_STAGING.toString());}};
//        try {
//
//            WorkerMessagingFactory.createSubscribers(tasks);
//            Assert.assertEquals(WorkerMessagingFactory.getSubscriberMap().size(), tasks.size());
//
//        } catch (AiravataException e) {
//            Assert.fail("Fail to start subscribers.", e);
//        }
//    }

    @Test
    public void testTaskImplementationFactory(){

        try {
            WorkerMessagingFactory.loadConfiguration();
            Task task = WorkerMessagingFactory.getTaskImplementation(TaskTypes.ENV_SETUP);
            Assert.assertEquals(task instanceof EnvironmentSetupTask, true);

            task = WorkerMessagingFactory.getTaskImplementation(TaskTypes.DATA_STAGING);
            Assert.assertEquals(task instanceof DataStageTask, true);

            task = WorkerMessagingFactory.getTaskImplementation(TaskTypes.JOB_SUBMISSION);
            Assert.assertEquals(task instanceof DefaultJobSubmissionTask, true);
        } catch (WorkerException e) {
            Assert.fail("Failed to load worker configuration.", e);
        }

    }

}
