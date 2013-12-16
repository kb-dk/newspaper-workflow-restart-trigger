package dk.statsbiblioteket.medieplatform.autonomous.newspaper;

import dk.statsbiblioteket.doms.central.connectors.BackendInvalidCredsException;
import dk.statsbiblioteket.doms.central.connectors.BackendInvalidResourceException;
import dk.statsbiblioteket.doms.central.connectors.BackendMethodFailedException;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedora;
import dk.statsbiblioteket.doms.central.connectors.EnhancedFedoraImpl;
import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.doms.webservices.authentication.Credentials;
import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClient;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClientFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

/**
 *
 */
public class RestartWorkflowTest {

    private DomsEventClient domsEventClient;
    private EnhancedFedora fedora;
    private String batchId = "123";
    private int roundTrip = 12;

    @BeforeTest
    public void setUp() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(pathToProperties));
        DomsEventClientFactory domsEventClientFactory = new DomsEventClientFactory();
        domsEventClientFactory.setFedoraLocation(properties.getProperty(ConfigConstants.DOMS_URL));
        domsEventClientFactory.setUsername(properties.getProperty(ConfigConstants.DOMS_USERNAME));
        domsEventClientFactory.setPassword(properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        domsEventClientFactory.setPidGeneratorLocation(properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
        domsEventClient = domsEventClientFactory.createDomsEventClient();
        Credentials creds = new Credentials(properties.getProperty(ConfigConstants.DOMS_USERNAME), properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        fedora =
                new EnhancedFedoraImpl(creds,
                        properties.getProperty(ConfigConstants.DOMS_URL).replaceFirst("/(objects)?/?$", ""),
                        properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL),
                        null);
        deleteBatch();
    }

    @AfterTest
    public void tearDown() throws Exception {
        deleteBatch();
    }

    public void deleteBatch() throws Exception {
        Batch batch = domsEventClient.getBatch(batchId, roundTrip);
        List<String> pids = fedora.findObjectFromDCIdentifier(batch.getFullID());
        if (!pids.isEmpty()) {
            fedora.deleteObject(pids.get(0), "Deleted in test.");
        }
    }

    @Test(groups = "integrationTest")
    public void testMain() throws Exception {
        String pathToProperties = System.getProperty("integration.test.newspaper.properties");
        domsEventClient.createBatchRoundTrip("batchID", roundTrip);
        domsEventClient.addEventToBatch("batchID", roundTrip, "me", new Date(100), "details", "e1", true);
        domsEventClient.addEventToBatch("batchID", roundTrip, "me", new Date(200), "details", "e2", true);
        domsEventClient.addEventToBatch("batchID", roundTrip, "me", new Date(300), "details", "e3", false);
        domsEventClient.addEventToBatch("batchID", roundTrip, "me", new Date(400), "details", "e4", true);
        domsEventClient.addEventToBatch("batchID", roundTrip, "me", new Date(500), "details", "e5", false);
        RestartWorkflow.main(new String[] {pathToProperties, "batchID", roundTrip + "", "10", "100"});
        Batch batch = domsEventClient.getBatch("batchID", roundTrip);
        assertEquals(batch.getEventList().size(), 2);
        RestartWorkflow.main(new String[] {pathToProperties, "batchID", roundTrip + "", "10", "100", "e1"});
        batch = domsEventClient.getBatch("batchID", roundTrip);
        assertEquals(batch.getEventList().size(), 0);
    }
}
