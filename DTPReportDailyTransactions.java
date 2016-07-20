package amazon.dtp.script.reconciler;
import java.util.TreeMap;
import java.util.Vector;

import org.joda.time.DateTime;

import amazon.DTPReportingService.DTPReportingService;
import amazon.DTPReportingService.DTPReportingServiceClient;
import amazon.DTPReportingService.SalesDetailByPeriodResponse;
import amazon.DTPReportingService.SalesDetailElement;
import amazon.DTPReportingService.SalesSummaryElementV2;
import amazon.DTPReportingService.SalesSummaryResponseV2;
import amazon.DigitalOpenPublishing.util.AmzId;
import amazon.framework.codigo.client.DependencyFailureException;

public final class DTPReportDailyTransactions {

    private static DTPReportingService dtp_svc;

    public DTPReportDailyTransactions() {
        dtp_svc = DTPReportingServiceClient.getService();
    }

    /**
     * Queries DTP-RS and retrieves all SalesDetailElement elements within a specified date range.
     * 
     * @param startDate
     * @param endDate
     * @return Vector<SalesDetailElement> sorted by costId
     * @throws DependencyFailureException
     */
    public TreeMap<String, SalesDetailElement> enact(final DateTime startDate, final DateTime endDate)
            throws DependencyFailureException {

        final TreeMap<String, SalesDetailElement> tMap = new TreeMap<String, SalesDetailElement>(
                String.CASE_INSENSITIVE_ORDER);

        SalesDetailByPeriodResponse salesDetailByPeriodResponse = null;

        final Vector<SalesDetailElement> dtpSummaries;

            salesDetailByPeriodResponse = dtp_svc.getSalesDetailByPeriod(startDate.toDate(), endDate.toDate());
        
        if (salesDetailByPeriodResponse != null) {
            dtpSummaries = salesDetailByPeriodResponse.getElements();
            
            for (SalesDetailElement summary : dtpSummaries) {
                if (summary.hasVendorCostId()) {
                	tMap.put(summary.getVendorCostId(), summary);
                }
            }
        }
        System.out.println("DTP: " + tMap.size());
        return tMap;
    }
}