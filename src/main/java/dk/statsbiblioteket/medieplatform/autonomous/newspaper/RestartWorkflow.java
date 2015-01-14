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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * Class containing main method for a restarting batch workflow.
 */
public class RestartWorkflow {
    public enum Keyword {
        REMOVE,
        ADD,
        RESTART;

        public static Keyword parse(String receivedKeyword) {
            for (Keyword keyword : Keyword.values()) {
                if (keyword.name().equalsIgnoreCase(receivedKeyword.trim())) {
                    return keyword;
                }
            }
            throw new IllegalArgumentException("Keyword '" + receivedKeyword + "' does not match any of the known keywords");
        }
    };

    /**
     * Called when there is an error in the input arguments.
     */
    private static void usage() {
        System.out.println("Usage:" + "\n java dk.statsbiblioteket.medieplatform.autonomous.newspaper.RestartWorkflow <keyword> " + "<config file> <batchId> <roundTrip> <maxAttempts> <waitTime (milliseconds)> [<eventName>] ");
        System.out.println("Keywords being one of: remove, add, restart");
    }

    public static void main(String[] args) {
        try {
            doMain(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to parse arguments; " + e.getMessage());
            usage();
            System.exit(6);
        } catch (CommunicationException e) {
            System.err.println("Failed to initialise; " + e.getMessage());
            System.exit(2);
        } catch (NotFoundException e) {
            System.err.println("No such batch/roundtrip; " + e.getMessage());
            System.exit(4);
        }
    }

    /**
     * Performs one of three related tasks, depending on the first argument received.
     * Either removes a specified event, or adds a specified event, or resets the events registered in DOMS so that the
     * newspaper
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
     * Only the last argument is optional, and only if the keyword is "restart". If the event name is omitted, the
     * workflow is
     * restarted from the earliest failed step.
     *
     * @param args The arguments to the method.
     */
    public static void doMain(String[] args) throws CommunicationException, NotFoundException {

        Keyword keyword;
        String eventName = null;

        if (args.length < 6 || args.length > 7 || (args.length == 6 && !args[0].equals("restart"))) {
            throw new IllegalArgumentException("Argument list is too short/long for given keyword");
        }

        String receivedKeyword = args[0];
        keyword = Keyword.parse(receivedKeyword);

        String configFilePath = args[1];
        File configFile = new File(configFilePath);
        Properties properties;
        try {
            properties = new Properties();
            properties.load(new FileInputStream(configFile));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Configuration file " + configFile.getAbsolutePath() + " does not exist.",
                                                      e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load properties from " + configFile.getAbsolutePath(), e);
        }

        String batchIdString = args[2];
        String roundTripString = args[3];
        String maxAttemptsString = args[4];
        String maxWaitString = args[5];

        int maxAttempts;
        try {
            maxAttempts = Integer.parseInt(maxAttemptsString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("maxAttempts parameter is not a number: " + maxAttemptsString, e);
        }
        int waitTime;
        try {
            waitTime = Integer.parseInt(maxWaitString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("waitTime parameter is not a number: " + maxWaitString, e);
        }


        DomsEventStorageFactory<Batch> domsEventClientFactory = new DomsEventStorageFactory<Batch>();
        domsEventClientFactory.setFedoraLocation(properties.getProperty(ConfigConstants.DOMS_URL));
        domsEventClientFactory.setUsername(properties.getProperty(ConfigConstants.DOMS_USERNAME));
        domsEventClientFactory.setPassword(properties.getProperty(ConfigConstants.DOMS_PASSWORD));
        domsEventClientFactory.setPidGeneratorLocation(properties.getProperty(ConfigConstants.DOMS_PIDGENERATOR_URL));
        domsEventClientFactory.setItemFactory(new BatchItemFactory());
        domsEventClientFactory.setRetries(maxAttempts);
        domsEventClientFactory.setDelayBetweenRetries(waitTime);

        DomsEventStorage<Batch> domsEventClient;
        try {
            domsEventClient = domsEventClientFactory.createDomsEventStorage();
        } catch (Exception e) {
            throw new CommunicationException("Error connecting to DOMS.", e);
        }


        int roundTrip;
        try {
            roundTrip = Integer.parseInt(roundTripString);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("roundTrip parameter is not a number: " + roundTripString, e);
        }

        if (args.length == 7) {
            eventName = args[6];
        }

        switch (keyword) {
            case ADD:
                addEvent(eventName, domsEventClient, batchIdString, roundTrip);
                break;
            case REMOVE:
                removeEvent(eventName, domsEventClient, batchIdString, roundTrip);
                break;
            case RESTART:
                restartWorkflow(eventName, domsEventClient, batchIdString, roundTrip);
                break;
        }
    }

    private static void removeEvent(String eventName, DomsEventStorage<Batch> domsEventClient, String batchId,
                                    int roundTrip) throws CommunicationException, NotFoundException {
        Batch batch = domsEventClient.getItemFromFullID(Batch.formatFullID(batchId, roundTrip));
        domsEventClient.removeEventFromItem(batch, eventName);
    }

    private static void addEvent(String eventName, DomsEventStorage<Batch> domsEventClient, String batchId,
                                 int roundTrip) throws CommunicationException, NotFoundException {
        // Set event to "true" i.e. event was successful
        Batch batch = domsEventClient.getItemFromFullID(Batch.formatFullID(batchId, roundTrip));

        final String agent;
        if (getComponentVersion() != null) {
            agent = getComponentName() + "-" + getComponentVersion();
        } else {
            agent = getComponentName();
        }
        domsEventClient.addEventToItem(batch, agent,
                                              new Date(),
                                              agent,
                                              eventName,
                                              true);
    }

    private static void restartWorkflow(String eventName, DomsEventStorage<Batch> domsEventClient, String batchIdString,
                                        int roundTrip) throws CommunicationException, NotFoundException {
        int eventsRemoved;
        if (eventName == null || eventName.trim().isEmpty()) {
            eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(new Batch(batchIdString, roundTrip));
        } else {
            eventsRemoved = domsEventClient.triggerWorkflowRestartFromFirstFailure(new Batch(batchIdString, roundTrip),
                                                                                          eventName);
        }
        if (eventsRemoved > 0) {
            System.out.println("Removed " + eventsRemoved + " events from DOMS. Workflow will be re-triggered.");
        } else {
            System.out.println("Did not remove any events from DOMS. This operation had no effect.");
        }
    }

    public static String getComponentName() {
        return RestartWorkflow.class.getSimpleName();
    }

    public static String getComponentVersion() {
        return RestartWorkflow.class.getPackage().getImplementationVersion();
    }
}
