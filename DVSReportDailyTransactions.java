package amazon.dtp.script.reconciler;
import java.util.TreeMap;
import java.util.Vector;

import org.joda.time.DateTime;

import amazon.DigitalVendorPayment.DigitalCostData;
import amazon.DigitalVendorPayment.DigitalTransaction;
import amazon.DigitalVendorPayment.DigitalTransactionsResponse;
import amazon.DigitalVendorPayment.DigitalVendorPaymentService;
import amazon.DigitalVendorPayment.DigitalVendorPaymentServiceClient;
import amazon.DigitalVendorPayment.MissingArgumentException;
import amazon.framework.codigo.client.DependencyFailureException;
import amazon.framework.codigo.client.InvalidRequestException;

public final class DVSReportDailyTransactions {
    
    private static DigitalVendorPaymentService dvs_svc;
    
    public DVSReportDailyTransactions() throws Exception {
        dvs_svc = DigitalVendorPaymentServiceClient.getService();
    }
    
    /**
     * Queries DVS1NA and retrieves all DigitalCostData elements within the specified date range.
     * 
     * @param startDate
     * @param endDate
     * @return Vector<DigitalCostData> sorted by costId
     * @throws DependencyFailureException
     * @throws MissingArgumentException
     * @throws InvalidRequestException
     */
    public TreeMap<String, DigitalCostData> enact(final DateTime startDate, final DateTime endDate) throws DependencyFailureException, MissingArgumentException, InvalidRequestException {

        final DigitalTransactionsResponse dvsTransactionsResponse = dvs_svc.getAllTransactionsByPeriod(startDate.toDate(), endDate.toDate(), null);
        final TreeMap<String, DigitalCostData> tMap = new TreeMap<String, DigitalCostData>(String.CASE_INSENSITIVE_ORDER);
            
        if (dvsTransactionsResponse != null) {
        	final Vector<DigitalTransaction> digitalTransactionSummaries = dvsTransactionsResponse.getDigitalTransactions();

            for (DigitalTransaction transactionSummary: digitalTransactionSummaries) {
                final Vector<DigitalCostData> digitalCostSummaries = transactionSummary.getDigitalCostsData();
                
              //Ensures uniqueness of entries, no duplicate costIds will be stored.
                for (DigitalCostData costSummary: digitalCostSummaries) {
                	if (costSummary.hasCostId()) {
                		tMap.put(costSummary.getCostId(), costSummary);
                	}
                }
            }
        }
        System.out.println("DVS: " + tMap.size());
        return tMap;
    }
}