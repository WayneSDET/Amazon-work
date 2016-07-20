package amazon.dtp.script.reconciler;

import java.io.IOException;
import java.rmi.RemoteException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import amazon.DigitalOpenPublishing.util.exceptions.InvalidRequestException;
import amazon.DigitalOpenPublishing.util.service.faults.CorruptArgumentFault;
import amazon.DigitalOpenPublishing.util.service.faults.PersistFault;
import amazon.VendorMaster.BadArgumentException;
import amazon.VendorMaster.NotFoundException;
import amazon.VendorMaster.SystemException;
import amazon.platform.config.AppConfig;
import amazon.platform.logging.AppConfigLog4jConfigurator;

import com.amazon.rtip.BasicTypesService.ServiceFault;

public class ReconcilerMain {
	private static final String APP_NAME = "DtpService";
    private static final String APP_GROUP = "tao";
    private static Logger logger;
	
     /**
     * @param args
     * 
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
    	try {
            final ReconcilerMain main = new ReconcilerMain();
            main.runMain(args);

        } catch (Exception exception) {
            logger.info(exception.getMessage(), exception);
        }  
    }

    private void runMain(final String[] args) throws ParseException, CorruptArgumentFault,PersistFault,
    	BadArgumentException, NotFoundException, SystemException, ServiceFault, RemoteException, IOException, 
    	IllegalArgumentException, InvalidRequestException, InterruptedException {

    	logger = Logger.getLogger(ReconcilerMain.class);
    	final CommandLine line = parse(args);

    	initApp(line.getOptionValue(CommandLineOptions.Root.name),
    			line.getOptionValue(CommandLineOptions.Domain.name));
    	
    	DateTime interval = new DateTime();
        
    	final DateTime startDate = interval.minusHours(2);
        final DateTime endDate = interval;
    	
        /*final DateTime startDate = new DateTime(2010, 12, 5,
                0,0,0,0);
        final DateTime endDate = new DateTime(2010, 12, 6,
                0,0,0,0);*/
        
		try {
			final ReconcileDtpDvs r = new ReconcileDtpDvs(startDate, endDate);
			r.enact();
		} catch (Exception exception) {
            System.err.println(exception);
			
			logger.info(exception.getMessage(), exception);
		}
    }
    
    private static void initApp(final String root, final String domain) {
        if (AppConfig.isInitialized()) {
            return;
        }

        String[] config = { "--root", root, "--domain",
                domain, "--realm", "USAmazon", "--logLevel", "info",
                "--log4j.appender.performanceFile.File", "/var/tmp/Reconciler",
                "--log4j.appender.application.File", "/var/tmp/Reconciler",
                "--log4j.redirectStdoutToLogDestination", "false" };

        final Logger rootLogger = Logger.getRootLogger();
    	rootLogger.removeAllAppenders();
    	AppConfigLog4jConfigurator.configureForBootstrap();

    	AppConfig.initialize(APP_NAME, APP_GROUP, config);
    	AppConfigLog4jConfigurator.configureFromAppConfig();
    }
    
    public static CommandLine parse(final String[] args) throws ParseException {

        final Options options = new Options();
        for (final CommandLineOptions option : CommandLineOptions.values()) {
            options.addOption(option.name, true, "");
        }

        final CommandLineParser parser = new PosixParser();
        final CommandLine line = parser.parse(options, args, true);
        return line;
    }
    
    private static enum CommandLineOptions {
        Root("root"), Domain("domain");

        final String name;

        CommandLineOptions(final String name) {
            this.name = name;
        }
    }
}