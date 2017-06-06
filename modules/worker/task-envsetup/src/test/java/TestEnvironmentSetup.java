import org.testng.Assert;
import org.testng.annotations.*;

/**
 * Created by Ajinkya on 4/17/17.
 */
public class TestEnvironmentSetup {

    @BeforeClass
    public void beforeClass(){
        System.out.println("@BeforeClass");
    }

    @AfterClass
    public void afterClass(){
        System.out.println("@AfterClass");
    }

    @BeforeMethod
    public void beforeMethod(){
        System.out.println("@BeforeMethod");
    }

    @AfterMethod
    public void AfterMethod(){
        System.out.println("@AfterMethod");
    }

    @DataProvider(name = "DataProvider")

    public static Object[][] credentials() {

        return new Object[][] { { "myName1"}, {"myName2"}, {"myName3"}};

    }

    @Test(invocationCount = 3, threadPoolSize = 3, description = "test", suiteName = "Suit1", dataProvider = "DataProvider", groups = { "fast" })
    public void test(String myName){
        System.out.println(myName);
        System.out.println(Thread.currentThread().getId());
        Assert.assertEquals("1", "1");
    }

    @Test(invocationCount = 1, threadPoolSize = 3, description = "test2", timeOut = 2000, groups = { "slow" })
    public void test2() throws InterruptedException {
        Thread.sleep(1000);
        Assert.assertEquals("hello", "hello");
    }

}
