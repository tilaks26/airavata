import org.apache.airavata.common.exception.AiravataException;
import org.apache.airavata.model.task.TaskTypes;
import org.apache.airavata.worker.messaging.utils.WorkerMessagingFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ajinkya on 5/3/17.
 */
public class TestWorkerMessaging {

    @Test
    public void testSubscriberCreation(){
        List<String> tasks = new ArrayList<String>(){{add(TaskTypes.ENV_SETUP.toString());add(TaskTypes.DATA_STAGING.toString());}};
        try {

            WorkerMessagingFactory.createSubscribers(tasks);
            Assert.assertEquals(WorkerMessagingFactory.getSubscriberMap().size(), tasks.size());

        } catch (AiravataException e) {
            Assert.fail("Fail to start subscribers.", e);
        }
    }

}
