package amazon.dtp.script.reconciler;
import static org.apache.log4j.Logger.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import amazon.platform.types.Currency;
import amazon.platform.types.CurrencyUnit;

public class UnreconciledTransactions {
    RandomAccessFile transactionRAFile;
    
    //Contains info related to reconciler log file name and path.
    final LogFileManager logs;
    
    private static final Logger logger = getLogger(UnreconciledTransactions.class);
    
    //Date format used in reconciler logs.
    private static final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy.MM.dd HH:mm:ss");
    
    /**
     * Retrieve unreconciled transactions for the previous day from log file.
     * @param date
     */
    public UnreconciledTransactions(String fileName, DateTime date) {
        logs = new LogFileManager(date, fileName);
        try {
            File f = new File(logs.getFilePath());
            transactionRAFile = new RandomAccessFile(f, "rw");
            if (transactionRAFile.length() == 0) {
                f.delete();
            }
                
        } catch (FileNotFoundException e) {
        	logger.error(e);
        } catch (IOException e) {
        	logger.error(e);
        }
    }
    
    /**
     * Retrieves unreconciled transactions archived daily from log file.
     * @return Vector<TransactionSummary>
     * @throws IOException
     */
    public TreeMap<String, TransactionSummary> getTransactionSummary() throws IOException {
        Scanner scanner = null;
        String vendorCode = null;
        DateTime transactionDate = null;
        DateTime costDate = null;
        String costId = null;
        String aSIN = null;
        Currency ourPrice = null;
        Currency listPrice = null;
        int age;
        
        final TreeMap<String, TransactionSummary> tMap = new TreeMap<String, TransactionSummary>();
        
        while (transactionRAFile.getFilePointer() < transactionRAFile.length()) {
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            vendorCode = scanner.next();
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            costId = scanner.next();
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            transactionDate = dateFormat.parseDateTime(scanner.nextLine().trim());
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            costDate = dateFormat.parseDateTime(scanner.nextLine().trim());
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            aSIN = scanner.next();
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            ourPrice = parseCurrency(scanner);
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            listPrice = parseCurrency(scanner);
            scanner = new Scanner(transactionRAFile.readLine()); scanner.next();
            age = Integer.parseInt(scanner.next());
            transactionRAFile.readLine();
            
            tMap.put(costId, new TransactionSummary(vendorCode, transactionDate, costDate, costId, aSIN, 
                    ourPrice, listPrice, age));
        }
        return tMap;
    }
    
    /**
     * Parses the Currency information from a log file.
     * @param currencyScanner
     * @return Currency object with retrieved values 
     */
    private Currency parseCurrency(Scanner currencyScanner) {
        String firstToken = currencyScanner.next();
        if (!firstToken.equalsIgnoreCase("null")) {
            BigDecimal bigDecimal = new BigDecimal(firstToken);
            String code = currencyScanner.next();
            String smallestAmount;
            if (code.equalsIgnoreCase("JPY"))
                smallestAmount = "1.00";
            else
                smallestAmount = "0.01";
            CurrencyUnit currencyCode = new CurrencyUnit(code, smallestAmount); 
            return new Currency(bigDecimal, currencyCode);
        }
        else
            return null;
    }
}