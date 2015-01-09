package dk.statsbiblioteket.medieplatform.autonomous.newspaper;

import dk.statsbiblioteket.medieplatform.autonomous.Batch;
import dk.statsbiblioteket.medieplatform.autonomous.BatchItemFactory;
import dk.statsbiblioteket.medieplatform.autonomous.CommunicationException;
import dk.statsbiblioteket.medieplatform.autonomous.ConfigConstants;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorage;
import dk.statsbiblioteket.medieplatform.autonomous.DomsEventStorageFactory;
import dk.statsbiblioteket.medieplatform.autonomous.NotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Class containing main method for a restarting batch workflow.
 */
public class RestartWorkflow {
    public enum Keyword {
        REMOVE, ADD, RESTART
    };

    /**
     * Called when there is an error in the input arguments.
     */
    private static void usage() {
        System.out.println("Usage:" + "\n java dk.statsbiblioteket.medieplatform.autonomous.newspaper.RestartWorkflow <keyword> "
                + "<config file> <batchId> <roundTrip> <maxAttempts> <waitTime (milliseconds> [<eventName>] ");
        System.out.println("Keywords being one of: remove, add, restart");
        System.exit(1);
    }

    /**
     * Performs one of three related tasks, depending on the first argument received.
     * Either removes a specified event, or adds a specified event, or resets the events registered in DOMS so that the newspaper
     * ingest workflow will be restarted. The method takes the following positional arguments:
     *
     * 1. keyword (remove, add, or restart)
     * 2. path to the configuration file
     * 3. batchId
     * 4. roundtrip number
     * 5. maximum attempts
     * 6. wait-time between attempts
     * [7. event name]
     *
     * Only the last argument is optional, and only if the keyword is "restart". If the event name is omitted, the workflow is
     * restarted from the earliest failed step.
     *
     * @param args The arguments to the method.
     */
    public static void main(String[] args) {
        DomsEventStorageFactory<Batch> domsEventClientFactory = new DomsEventStorageFactory<Batch>();
        DomsEventStorage<Batch> domsEventClient = null;
        Keyword keyword = null;
        String eventName = "";

        if (args.length < 6 || args.length > 7 || (args.length == 6 && !args[0].equals("restart"))) {
            System.out.println("Argument list is too short/long for given keyword");
            usage();
        }

        String receivedKeyword = args[0];
        if (receivedKeyword.equals("remove")) {
            keyword = Keyword.REMOVE;
        } else if (receivedKeyword.equals("add")) {
            keyword = Keyword.ADD;
        } else if (receivedKeyword.equals("restart")) {
            keyword = Keyword.RESTART;
        } else {
            System.out.println("Invalid keyword");
            System.exit(6);
        }

        String configFilePath = args[1];
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            System.out.println("Configuration file " + configFile + " does not exist.");
            System.exit(6);
        }
        Properties properties = null;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(configFile));
        } catch (IOException e) {
            System.out.println("Could not load properties from " + configFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(6);
        }

        String batchIdString = args[2];
        String roundTripString = args[3];
        String maxAttemptsString = args[4];
        String maxWaitString = args[5];

        domsEventClientFactory.setFedoraLocation(properties.getProperty(ConfigConstants.DOMS_URL));
        domsEventClientFactory.setUsername(properties.getProperty(ConfigConstants.DOMS_USERNAME));
        domsEventClientFactory.setPassword(properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        domsEventClientFactory.setPidGeneratorLocation(properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
        domsEventClientFactory.setItemFactory(new BatchItemFactory());

        try {
            domsEventClient = domsEventClientFactory.createDomsEventStorage();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error connecting to DOMS.");
            System.exit(2);
        }


        int roundTrip = 0;
        try {
            roundTrip = Integer.parseInt(roundTripString);
        } catch (NumberFormatException e) {
            System.out.println("roundTrip parameter is not a number: " + roundTripString);
            usage();
        }
        int maxAttempts = 0;
        try {
            maxAttempts = Integer.parseInt(maxAttemptsString);
        } catch (NumberFormatException e) {
            System.out.println("maxAttempts paramter is not a number: " + maxAttemptsString);
            usage();
        }
        long waitTime = 0L;
        try {
            waitTime = Long.parseLong(maxWaitString);
        } catch (NumberFormatException e) {
            System.out.println("waitTime parameter is not a number: " + maxWaitString);
            usage();
        }

        if (args.length == 7) {
            eventName = args[6];
        }

        if (keyword.equals(Keyword.REMOVE)) {
            removeEvent(args, domsEventClient, batchIdString, roundTrip, maxAttempts, waitTime);
        } else if (keyword.equals(Keyword.ADD)) {
            addEvent(eventName, domsEventClient, batchIdString, roundTrip);
        } else if (keyword.equals(Keyword.RESTART)) {
            restartWorkflow(eventName, domsEventClient, batchIdString, roundTrip, maxAttempts, waitTime);
        }
    }

    private static void removeEvent(String[] args, DomsEventStorage<Batch> domsEventClient, String batchIdString, int roundTrip,
                                    int maxAttempts, long waitTime) {
        // TODO
    }

    private static void addEvent(String eventName, DomsEventStorage<Batch> domsEventClient, String batchIdString, int roundTrip) {
        // Set event to "true" i.e. event was successful
        try {
            Batch batch = domsEventClient.getItemFromFullID(Batch.formatFullID(batchIdString, roundTrip));

            // TODO Insert proper agent name below
            domsEventClient.addEventToItem(batch, "RestartWorkflow.addEvent", new Date(), "agent-name", eventName, true);
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

    private static void restartWorkflow(String eventName, DomsEventStorage<Batch> domsEventClient, String batchIdString,
                                        int roundTrip, int maxAttempts, long waitTime) {
        int eventsRemoved;
        try {
            if (eventName.isEmpty()) {
                eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(new Batch(
                        batchIdString,
                        roundTrip),
                        maxAttempts,
                        waitTime);
            } else {
                eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(new Batch(
                        batchIdString,
                        roundTrip),
                        maxAttempts,
                        waitTime,
                        eventName);
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
