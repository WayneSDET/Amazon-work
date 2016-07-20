package amazon.dtp.script.reconciler;
import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import amazon.DTPReportingService.DTPReportingService;
import amazon.DTPReportingService.DTPReportingServiceClient;
import amazon.DTPReportingService.SalesDetailElement;
import amazon.DigitalVendorPayment.DigitalCostData;
import amazon.DigitalVendorPayment.MissingArgumentException;
import amazon.fluxo.BasicFluxoJavaClient;
import amazon.fluxo.FluxoJavaClient;
import amazon.framework.codigo.client.DependencyFailureException;
import amazon.framework.codigo.client.InvalidRequestException;
import amazon.platform.types.Currency;

public class ReconcileDtpDvs {
    private static RandomAccessFile reconciledRAFile;
    
    private static DTPReportDailyTransactions dtp;
    private static DVSReportDailyTransactions dvs;
    private static TreeMap<String,TransactionSummary> yesterdaysTransactionSummary;
    private static TreeMap<String,TransactionSummary> todaysPreviousTransactions;
    
    private static DateTime startDate;
    private static DateTime endDate;
    
    //Date format used in reconciler logs.
    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy.MM.dd HH:mm:ss");
    
    //Contains info related to reconciler log file name and path
    private final LogFileManager logs;
    
    private static final Logger logger = getLogger(ReconcileDtpDvs.class);
    
    public ReconcileDtpDvs(final DateTime start, final DateTime end) throws Exception {
        
        startDate = start;
        endDate = end;
        
        logs = new LogFileManager(startDate, "Unreconciled_Transactions_Report");
        
        dtp = new DTPReportDailyTransactions();
        dvs = new DVSReportDailyTransactions();
        
        //Retrieves old unreconciled transactions from yesterdays log file.
        UnreconciledTransactions yesterdaysTransactions = new UnreconciledTransactions("Unreconciled_Transactions_Report", startDate.minusDays(1));
        yesterdaysTransactionSummary = yesterdaysTransactions.getTransactionSummary();
        
        //Retrieves unreconciled transactions from todays previous executions.
        UnreconciledTransactions todaysTransactions = new UnreconciledTransactions("Unreconciled_Transactions_Report", startDate);
        todaysPreviousTransactions = todaysTransactions.getTransactionSummary();
        
        System.out.println("yesterdays log size: " + yesterdaysTransactionSummary.size());
        
        System.out.println("todays log size: " + todaysPreviousTransactions.size());
        
        //Combine All previously unreconciled transactions.
        yesterdaysTransactionSummary.putAll(todaysPreviousTransactions);
        
        System.out.println("combined log size: " + yesterdaysTransactionSummary.size());
        
        try {
        	//Deletes todays existing log file.  To prevent appending transaction info in case of re-running script.
            File f = new File(logs.getFilePath());
            f.delete();
            reconciledRAFile = new RandomAccessFile(f, "rw");
        } catch (FileNotFoundException e) {
            logger.error("Error opening daily transaction file: " + e);
        }
    }

    /**
     * Managing class for the reconciler
     * @throws DependencyFailureException
     * @throws MissingArgumentException
     * @throws InvalidRequestException
     * @throws IOException 
     * @throws HttpException  
     */
    public void enact() throws DependencyFailureException, MissingArgumentException, InvalidRequestException, IOException {
    	
    	final TreeMap<String,SalesDetailElement> dtpTMap = dtp.enact(startDate, endDate);
    	final TreeMap<String,DigitalCostData> dvsTMap = dvs.enact(startDate, endDate);

        //Compare DVS against DTP transactions, transactions missing from DTP are placed in todaysUnreconciledSummaries.
        final TreeMap<String,TransactionSummary> todaysUnreconciledSummaries = getInconsistencies(dtpTMap, dvsTMap);
        System.out.println("todays unreconciled: " + todaysUnreconciledSummaries.size());
        
        //Old transactions not reconciled are rechecked against DTP, missing transactions are placed in oldUnreconciledSummaries.
        final TreeMap<String,TransactionSummary> oldUnreconciledSummaries = reconcileOldTransactions(new Vector<TransactionSummary>(yesterdaysTransactionSummary.values()));
        System.out.println("old unreconciled: " + oldUnreconciledSummaries.size());
        
        //Combines todays unreconciled transactions with all old unreconciled transactions.
        final Vector<TransactionSummary> unreconciledSummaries = combineSummaries(todaysUnreconciledSummaries, oldUnreconciledSummaries);
        System.out.println("combined unreconciled: " + unreconciledSummaries.size());
        
        //Dumps all unreconciled transactions into log file.
        for (TransactionSummary summary: unreconciledSummaries) {
            generateLogFile(reconciledRAFile, summary.getVendorCode(), new DateTime(summary.getTransactionDate()), new DateTime(summary.getCostDate()),
                    summary.getCostId(), summary.getASIN(), summary.getOurPriceCurrency(), 
                    summary.getListPriceCurrency(), summary.getAge());
        }
        
        //Cutting trouble ticket to include a list of all unreconciled transactions
        if (!unreconciledSummaries.isEmpty()) {
        	cutTicket(generateTransactionString(unreconciledSummaries));
        }
    }
    
    /**
     * Creates the string of all costIds listed in unrecociled transactions.
     * @param summaries
     * @return
     */
    private String generateTransactionString(Vector<TransactionSummary> summaries) {
		String string = "";
		
		for (TransactionSummary summary : summaries) {
			string += (summary.getCostId() + ",  \r\n");
		}
		
		return string;
	}

    /**
     * Creates the Trouble Ticket
     * @param transactionString
     * @throws HttpException
     * @throws IOException
     */
	private void cutTicket(String transactionString) throws HttpException, IOException {
    	BasicFluxoJavaClient client = new BasicFluxoJavaClient("flx-dtp-reports", "flx-dtp-reports", "ticket-api.integ.amazon.com");
    	String ticketId = null;
    	Map<String, String> params = new HashMap<String, String>();
    	params.put(FluxoJavaClient.REQUESTER_LOGIN, "wayb");
    	params.put(FluxoJavaClient.REQUESTER_NAME, "Wayne Bruce");
    	params.put(FluxoJavaClient.SHORT_DESCRIPTION, "Unrenconciled transactions.");
    	params.put(FluxoJavaClient.DETAILS, "The list of missing transactions for the period: " + startDate + " - " + endDate +
    			"; \r\n" + transactionString);
    	params.put(FluxoJavaClient.IMPACT, "5");
    	params.put(FluxoJavaClient.CATEGORY, "Digital Media Technology");
    	params.put(FluxoJavaClient.TYPE, "Text");
    	params.put(FluxoJavaClient.ITEM, "DTP");
    	
    	ticketId = client.createTicket(params);

    	System.out.println("ticketId: " + ticketId + "\n");
	}

	/**
     * Compares the results of the database queries for inconsistencies, then returns those inconsistencies as a Vector<TransactionSummary>.
     * @param dtpTMap
     * @param dvsTMap
     * @return Vector<TransactionSummary>
     */
    protected static TreeMap<String,TransactionSummary> getInconsistencies(TreeMap<String,SalesDetailElement> dtpTMap, TreeMap<String,DigitalCostData> dvsTMap) {
        
        TreeMap<String, TransactionSummary> tMap = new TreeMap<String, TransactionSummary>(String.CASE_INSENSITIVE_ORDER);
        Vector<DigitalCostData> dvsVector = new Vector<DigitalCostData>(dvsTMap.values());
        
        if (dvsVector != null) {
            for (DigitalCostData dvs: dvsVector) {
                boolean isFound = false;
                
                isFound = dtpTMap.containsKey(dvs.getCostId());
                
                if (!isFound) {
                    tMap.put(dvs.getCostId(), new TransactionSummary(dvs.getVendorCode(), new DateTime(dvs.getTransactionDate()),
                            new DateTime(dvs.getCostDate()), dvs.getCostId(), dvs.getTransactionAsin(), dvs.getOurPrice(), 
                            dvs.getListPrice(), 1));
                }
            }
        }
        return tMap;
    }

    /**
     * Compares previous unreconciled transactions with current DTP transactions for reconciliation.
     * @param unreconciledSummaries
     * @param dtpTMap
     * @return Vector<TransactionSummary>
     */
    protected static TreeMap<String,TransactionSummary> reconcileOldTransactions(Vector<TransactionSummary> unreconciledSummaries) {
        
    	DTPReportingService dtp_svc = DTPReportingServiceClient.getService();
    	
        TreeMap<String, TransactionSummary> tMap = new TreeMap<String, TransactionSummary>(String.CASE_INSENSITIVE_ORDER);
        
        for (TransactionSummary unreconciled: unreconciledSummaries) {
            boolean isFound = false;
            
            isFound = dtp_svc.isCostIdFound(unreconciled.getCostId()).isFound();
            
            if (!isFound) {
                tMap.put(unreconciled.getCostId(), new TransactionSummary(unreconciled.getVendorCode(), new DateTime(unreconciled.getTransactionDate()),
                        new DateTime(unreconciled.getCostDate()) ,unreconciled.getCostId(), unreconciled.getASIN(), unreconciled.getOurPriceCurrency(), 
                        unreconciled.getListPriceCurrency(), unreconciled.getAge()+1));
            }
        }
        return tMap;
    }
    
    /**0020055104
     * Combines todays unreconciled transactions with all old unreconciled transactions and sorts
     * them by transaction date.  Duplicate entries do not get stored, ensuring the uniqueness of each entry.
     * @param todaysUnreconciledSummaries
     * @param oldUnreconciledSummaries
     * @return Vector<TransactionSummary>
     */
    protected static Vector<TransactionSummary> combineSummaries(TreeMap<String,TransactionSummary> todaysUnreconciledSummaries,
    		TreeMap<String, TransactionSummary> oldUnreconciledSummaries) {
        
    	todaysUnreconciledSummaries.putAll(oldUnreconciledSummaries);
    	
    	return new Vector<TransactionSummary>(todaysUnreconciledSummaries.values());
    }
    
    /**
     * Creates a log file with the data extracted from the databases.
     * @param logRAFile
     * @param vendorCode
     * @param transactionDate
     * @param costDate
     * @param vendorCostID
     * @param aSIN
     * @param royaltyPlan
     * @param ourPrice
     * @param listPrice
     * @param paymentAmount
     * @param age
     */
    private static void generateLogFile(RandomAccessFile logRAFile, String vendorCode, DateTime transactionDate, DateTime costDate, String vendorCostID,
            String aSIN, Currency ourPrice, Currency listPrice, int age) {
        try {
            logRAFile.writeBytes("Vendor_Code: " + vendorCode + "\n");
            logRAFile.writeBytes("Vendor_Cost_ID: " + vendorCostID + "\n");
            logRAFile.writeBytes("Transaction_Date: " + dateFormat.print(transactionDate) + "\n");
            logRAFile.writeBytes("Cost_Date: " + dateFormat.print(costDate) + "\n");
            logRAFile.writeBytes("ASIN: " + aSIN + "\n");
            logRAFile.writeBytes("Our_Price: " + ourPrice + "\n");
            logRAFile.writeBytes("List_Price: " + listPrice + "\n");
            logRAFile.writeBytes("Cycles_Unreconciled: " + age + "\n\n");
        } catch (IOException e) {
        	logger.error("Error writing to log file: " + e);
        }
    }
}
