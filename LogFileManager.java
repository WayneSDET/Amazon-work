package amazon.dtp.script.reconciler;
import org.joda.time.DateTime;

public class LogFileManager {
    
    final String DIRECTORY = "/apollo/env/DigitalTextPlatformScripts/var/output/logs/";
    
    private DateTime logDate;
    private String dateString;
    private String reportString;
    private String filePath;
    
    /**
     * Generates the log file name for the reconciler app.
     * @param lDate
     * @param rString
     */
    public LogFileManager(DateTime lDate, String rString) {
        setLogDate(lDate);
        setReportString(rString);
        if (logDate.getMonthOfYear() < 10 && logDate.getDayOfMonth() < 10) {
        	setDateString("_" + logDate.getYear() + "_0" + logDate.getMonthOfYear() + "_0" + logDate.getDayOfMonth());
        }
        else if (logDate.getDayOfMonth() < 10) {
            setDateString("_" + logDate.getYear() + "_" + logDate.getMonthOfYear() + "_0" + logDate.getDayOfMonth());
        }
        else if (logDate.getMonthOfYear() < 10) {
            setDateString("_" + logDate.getYear() + "_0" + logDate.getMonthOfYear() + "_" + logDate.getDayOfMonth());
        }
        else {
            setDateString("_" + logDate.getYear() + "_" + logDate.getMonthOfYear() + "_" + logDate.getDayOfMonth());
        }
        setFilePath(DIRECTORY + reportString + dateString + ".log");
    }

    private void setLogDate(DateTime logDate) {this.logDate = logDate;}
    public DateTime getLogDate() {return logDate;}
    
    private void setDateString(String dateString) {this.dateString = dateString;}
    
    private void setReportString(String reportString) {this.reportString = reportString;}
    public String getReportString() {return reportString;}
    
    private void setFilePath(String filePath) {this.filePath = filePath;}
    public String getFilePath() {return filePath;}
}
