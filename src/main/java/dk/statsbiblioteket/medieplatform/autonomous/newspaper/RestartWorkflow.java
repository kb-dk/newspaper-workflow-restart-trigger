package dk.statsbiblioteket.medieplatform.autonomous.newspaper;

import dk.statsbiblioteket.doms.central.connectors.fedora.pidGenerator.PIDGeneratorException;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClient;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventClientFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

/**
 *
 */
public class RestartWorkflow {

    /**
     * Resets the events registered in DOMS so that the newspaper ingest workflow will be restarted. The method
     * takes the following positional arguments:
     * 1. path to the configuration file
     * 2. batchId
     * 3. roundtrip number
     * 4. maximum attempts
     * 5. wait-time between attempts
     * [6. event name]
     * Only the last argument is optional. If the event name is omitted, the workflow is restarted from the earliest
     * failed step.
     * @param args The arguments to the method.
     */
    public static void main(String[] args) {

        DomsEventClientFactory domsEventClientFactory = new DomsEventClientFactory();
               DomsEventClient domsEventClient = null;

        if (args.length < 5 || args.length > 6) {
            System.out.println("Argument list is too short");
            System.exit(1);
        }

        String configFilePath = args[0];
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            System.out.println("Configuration file " + configFile + " does not exist.");
            System.exit(5);
        }
        Properties properties = null;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(configFile));
        } catch (IOException e) {
            System.out.println("Could not load properties from " + configFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(5);
        }

        String batchIdString = args[1];
        String roundTripString = args[2];
        String maxAttemptsString = args[3];
        String maxWaitString = args[4];

        domsEventClientFactory.setFedoraLocation(properties.getProperty(ConfigConstants.DOMS_URL));
                domsEventClientFactory.setUsername(properties.getProperty(ConfigConstants.DOMS_USERNAME));
                domsEventClientFactory.setPassword(properties.getProperty(ConfigConstants.DOMS_PASSWORD));
                domsEventClientFactory.setPidGeneratorLocation(properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));

        try {
            domsEventClient = domsEventClientFactory.createDomsEventClient();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error connecting to DOMS.");
            System.exit(2);
        }


        int roundTrip;
        roundTrip = Integer.parseInt(roundTripString);
        int maxAttempts;
        maxAttempts = Integer.parseInt(maxAttemptsString);
        long waitTime;
        waitTime = Long.parseLong(maxWaitString);

        int eventsRemoved;
        try {
            if (args.length == 5) {
                eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(batchIdString, roundTrip, maxAttempts, waitTime);
            } else {
                eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(batchIdString, roundTrip, maxAttempts, waitTime, args[5]);
            }
            if (eventsRemoved > 0) {
                System.out.println("Removed " + eventsRemoved + " events from DOMS. Workflow will be re-triggered.");
                return;
            } else {
                System.out.println("Did not remove any events from DOMS. This operation had no effect.");
                return;
            }
        } catch (CommunicationException e) {
            e.printStackTrace();
            System.out.println("Problem communicating with DOMS.");
            System.exit(3);
        } catch (NotFoundException e) {
            e.printStackTrace();
            System.out.println("No such batch/roundtrip.");
            System.exit(4);
        }

    }

}
