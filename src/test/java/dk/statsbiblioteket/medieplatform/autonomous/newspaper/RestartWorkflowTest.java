package dk.statsbiblioteket.medieplatform.autonomous.newspaper;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

/**
 *  Test class for RestartWorkflow
 */
public class RestartWorkflowTest {

    private NewspaperDomsEventStorage domsEventClient;
    private EnhancedFedora fedora;
    private String batchId = "123";
    private int roundTrip = 12;

    @BeforeTest(groups = "integrationTest")
    public void setUp() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(pathToProperties));
        NewspaperDomsEventStorageFactory domsEventClientFactory = new NewspaperDomsEventStorageFactory();
        domsEventClientFactory.setFedoraLocation(properties.getProperty(ConfigConstants.DOMS_URL));
        domsEventClientFactory.setUsername(properties.getProperty(ConfigConstants.DOMS_USERNAME));
        domsEventClientFactory.setPassword(properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        domsEventClientFactory.setPidGeneratorLocation(properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
        domsEventClient = domsEventClientFactory.createDomsEventStorage();
        Credentials creds = new Credentials(properties.getProperty(ConfigConstants.DOMS_USERNAME),
                properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        fedora =
                new EnhancedFedoraImpl(creds,
                        properties.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                        properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                        null);
        deleteBatch();
    }

    @AfterTest(groups = "integrationTest")
    public void tearDown() throws Exception {
        deleteBatch();
    }

    public void deleteBatch() throws Exception {
        Batch batch = null;
        try {
            batch = domsEventClient.getItemFromFullID(Batch.formatFullID(batchId, roundTrip));
            List<String> pids = fedora.findObjectFromDCIdentifier(batch.getFullID());
            for (String pid : pids) {
                fedora.deleteObject(pid, "Deleted in test.");
            }
        } catch (NotFoundException e) {
            //fine, its gone
        }
    }

    @Test(groups = "integrationTest")
    public void testRestart() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Batch batch = new Batch(batchId,roundTrip);
        domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, roundTrip));
        domsEventClient.addEventToItem(batch, "me", new Date(100), "details", "e1", true);
        domsEventClient.addEventToItem(batch, "me", new Date(200), "details", "e2", true);
        domsEventClient.addEventToItem(batch, "me", new Date(300), "details", "e3", false);
        domsEventClient.addEventToItem(batch, "me", new Date(400), "details", "e4", true);
        domsEventClient.addEventToItem(batch, "me", new Date(500), "details", "e5", false);

        RestartWorkflow.main(new String[] {"restart", pathToProperties, batchId, roundTrip + "", "10", "100"});
        batch = domsEventClient.getItemFromFullID(batch.getFullID());
        assertEquals(batch.getEventList().size(), 2);

        RestartWorkflow.main(new String[] {"restart", pathToProperties, batchId, roundTrip + "", "10", "100", "e1"});
        batch = domsEventClient.getItemFromFullID(batch.getFullID());
        assertEquals(batch.getEventList().size(), 0);
    }

    @Test(groups = "integrationTest")
    public void testAdd() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Batch batch = new Batch(batchId, roundTrip);
        domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, roundTrip));
        int eventListStart;
        try {
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            eventListStart = batch.getEventList().size();
        } catch (NotFoundException e) {
            eventListStart = 0;
        }

        RestartWorkflow.main(new String[]{"add", pathToProperties, batchId, roundTrip + "", "10", "100", "e1"});
        batch = domsEventClient.getItemFromFullID(batch.getFullID());
        assertEquals(batch.getEventList().size(), 1 + eventListStart);
    }

    @Test(groups = "integrationTest")
    public void testRemove() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Batch batch = new Batch(batchId, roundTrip);
        domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, roundTrip));
        int eventListStart;
        try {
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            eventListStart = batch.getEventList().size();
        } catch (NotFoundException e) {
            eventListStart = 0;
        }
        System.out.println(eventListStart);

        domsEventClient.addEventToItem(batch, "me", new Date(100), "details", "r1", true);
        domsEventClient.addEventToItem(batch, "me", new Date(200), "details", "r2", true);
        domsEventClient.addEventToItem(batch, "me", new Date(300), "details", "r1", false);
        domsEventClient.addEventToItem(batch, "me", new Date(400), "details", "r4", true);
        domsEventClient.addEventToItem(batch, "me", new Date(500), "details", "r5", false);

        RestartWorkflow.main(new String[]{"remove", pathToProperties, batchId, roundTrip + "", "10", "100", "r1"});
        batch = domsEventClient.getItemFromFullID(batch.getFullID());
        assertEquals(batch.getEventList().size(), 5 + eventListStart - 2);
    }
}
