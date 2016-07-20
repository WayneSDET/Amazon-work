package amazon.dtp.script.reconciler;
import org.joda.time.DateTime;

import amazon.platform.jms.AmzId;
import amazon.platform.types.Currency;

public class TransactionSummary {
    
    private String transactionSummaryId;
    private String vendorCode;
    private DateTime transactionDate;
    private DateTime costDate;
    private String costId;
    private String ASIN;
    private Currency ourPriceCurrency;
    private Currency listPriceCurrency;
    //Denotes the number of times the transaction was attempted to reconcile.
    private int age;
    
    public TransactionSummary () {
        transactionSummaryId = new AmzId().asBase32String();
    }
    
    public TransactionSummary (String vendorCode, DateTime transactionDate, DateTime costDate, String costId, 
            String aSIN, Currency ourPrice, Currency listPrice, int oldAge) {
        transactionSummaryId = new AmzId().asBase32String();
        setVendorCode(vendorCode);
        setTransactionDate(transactionDate);
        setCostDate(costDate);
        setCostId(costId);
        setASIN(aSIN);
        setOurPriceCurrency(ourPrice);
        setListPriceCurrency(listPrice);
        setAge(oldAge);
    }

    protected String getTransactionSummaryId() {return transactionSummaryId;}
    
    String getVendorCode() {return vendorCode;}
    private void setVendorCode(String vendorCode) {this.vendorCode = vendorCode;}
    
    DateTime getTransactionDate() {return new DateTime(transactionDate);}
    private void setTransactionDate(DateTime date) {transactionDate = new DateTime(date);}
    
    String getCostId() {return costId;}
    private void setCostId(String costId) {this.costId = costId;}
    
    int getAge() {return age;}
    private void setAge(int newAge) {age = newAge;}
    
    public void setCostDate(DateTime costDate) {this.costDate = costDate;}
    DateTime getCostDate() {return costDate;}

    public void setASIN(String aSIN) {ASIN = aSIN;}
    String getASIN() {return ASIN;}

    public void setOurPriceCurrency(Currency ourPriceCurrency) {this.ourPriceCurrency = ourPriceCurrency;}
    Currency getOurPriceCurrency() {return ourPriceCurrency;}

    public void setListPriceCurrency(Currency listPriceCurrency) {this.listPriceCurrency = listPriceCurrency;}
    Currency getListPriceCurrency() {return listPriceCurrency;}
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("TransactionSummaryId: ").append(getTransactionSummaryId()).append(' ');
        buffer.append("+VendorCode: ").append(getVendorCode()).append(' ');
        buffer.append("+VendorCostId: ").append(getCostId()).append(' ');
        buffer.append("+TransactionDate: ").append(getTransactionDate()).append(' ');
        buffer.append("+CostDate: ").append(getCostDate()).append(' ');
        buffer.append("+ASIN: ").append(getASIN()).append(' ');
        buffer.append("+OurPriceCurrency: ").append(getOurPriceCurrency()).append(' ');
        buffer.append("+ListPriceCurrency: ").append(getListPriceCurrency()).append(' ');
        buffer.append("+Age: ").append(String.valueOf(getAge())).append(' ');

        return buffer.toString();
    }
}