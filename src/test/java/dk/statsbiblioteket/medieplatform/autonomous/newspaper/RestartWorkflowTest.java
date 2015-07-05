package dk.statsbiblioteket.medieplatform.autonomous.newspaper;

import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.sbutil.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.Event;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.NewspaperDomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

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
    }


    public void deleteBatch(Batch batch) throws Exception {
        try {
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
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
        
        int myRoundtrip = roundTrip +1;
        Batch batch = new Batch(batchId,myRoundtrip);
        try {
            deleteBatch(batch);
            String pathToProperties = System.getProperty("integration.test.newspaper.properties");
            domsEventClient.createBatchRoundTrip(batch.getFullID());
              
            domsEventClient.appendEventToItem(batch, "me", new Date(100), "details", "e1", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(200), "details", "e2", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(300), "details", "e3", false);
            domsEventClient.appendEventToItem(batch, "me", new Date(400), "details", "e4", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(500), "details", "e5", false);
            
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            
            RestartWorkflow.main(new String[] {"restart", pathToProperties, batchId, myRoundtrip + ""});
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            assertEquals(batch.getEventList().size(), 2, "Eventlist: " + batch.getEventList());
            
            RestartWorkflow.main(new String[] {"restart", pathToProperties, batchId, myRoundtrip + "", "e1"});
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            assertEquals(batch.getEventList().size(), 0, "Eventlist: " + batch.getEventList());
        } finally {
            deleteBatch(batch);
        }
    }

    @Test(groups = "integrationTest")
    public void testAdd() throws Exception {
        int myRoundtrip = roundTrip +2;
        Batch batch = new Batch(batchId,myRoundtrip);
        try {
            deleteBatch(batch);
            String pathToProperties = System.getProperty("integration.test.newspaper.properties");
            domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, myRoundtrip));
            int eventListStart;
            try {
                batch = domsEventClient.getItemFromFullID(batch.getFullID());
                eventListStart = batch.getEventList().size();
            } catch (NotFoundException e) {
                eventListStart = 0;
            }
    
            RestartWorkflow.main(new String[]{"add", pathToProperties, batchId, myRoundtrip + "", "e1"});
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            assertEquals(batch.getEventList().size(), 1 + eventListStart);
        } finally {
            deleteBatch(batch);
        }    
    }

    @Test(groups = "integrationTest")
    public void testRemove() throws Exception {
        int myRoundtrip = roundTrip + 3;
        Batch batch = new Batch(batchId,myRoundtrip);
        try {
            deleteBatch(batch);        
            String pathToProperties = System.getProperty("integration.test.newspaper.properties");
            domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, myRoundtrip));
    
            domsEventClient.appendEventToItem(batch, "me", new Date(100), "details", "r1", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(200), "details", "r2", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(300), "details", "r1", false);
            domsEventClient.appendEventToItem(batch, "me", new Date(400), "details", "r4", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(500), "details", "r5", false);
    
            RestartWorkflow.main(new String[]{"remove", pathToProperties, batchId, myRoundtrip + "", "r1"});
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            for(Event e : batch.getEventList()) {
                assertFalse(e.getEventID().equals("r1"));
            }
        } finally {
            deleteBatch(batch);
        }
    }
    
    @Test(groups = "integrationTest")
    public void testPrioritize() throws Exception {
        int myRoundtrip = roundTrip + 4;
        Batch batch = new Batch(batchId,myRoundtrip);
        try {
            deleteBatch(batch);
            String pathToProperties = System.getProperty("integration.test.newspaper.properties");
            domsEventClient.createBatchRoundTrip(Batch.formatFullID(batchId, myRoundtrip));
    
            RestartWorkflow.main(new String[]{"remove", pathToProperties, batchId, myRoundtrip + "", RestartWorkflow.PRIORITY_EVENT_NAME});
            domsEventClient.appendEventToItem(batch, "me", new Date(1000), "details", "p1", true);
            domsEventClient.appendEventToItem(batch, "me", new Date(2000), "details", "p2", true);
    
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            int numberOfEventsAfterSetup = batch.getEventList().size();
            
            RestartWorkflow.main(new String[]{"prioritize", pathToProperties, batchId, myRoundtrip + "", "1"});
            batch = domsEventClient.getItemFromFullID(batch.getFullID());
            assertEquals(batch.getEventList().size(), numberOfEventsAfterSetup + 1);
            assertTrue(batch.getEventList().get(0).getEventID().equals(RestartWorkflow.PRIORITY_EVENT_NAME));
        } finally {
            deleteBatch(batch);
        }    
    }
}
