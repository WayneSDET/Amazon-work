/** This was a gui I created on the fly to facilitate
 * efficient execution of the reconciler code. It was never
 * intended to be robust production code, hence the absence
 * of exception handling.
 * /

package amazon.dtp.script.reconciler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import amazon.platform.config.AppConfig;
import amazon.platform.logging.AppConfigLog4jConfigurator;
    
public class ReconcilerAppFrame extends JFrame{
    private static final long serialVersionUID = 3946949123444942865L;

    static JPanel panel;
    static RadioButtonPanel radioButtonPanel;
    static JTextArea textArea;
    static JScrollPane scrollPane;
    
    private ReconcileListener reconcileListener;
    private OpenLogListener openLogListener;
    
    private static final String APP_NAME = "DtpService";
    private static final String APP_GROUP = "tao";
    
    private static final String domain = "test";
    private static final String root = "/rhel5pdi/apollo/env/DigitalTextPlatformScripts";
    
    File openFile;
    JFileChooser fc;
    
    public ReconcilerAppFrame() {
        
        reconcileListener = new ReconcileListener();
        openLogListener = new OpenLogListener();
        fc = new JFileChooser("/apollo/env/DigitalTextPlatformScripts/var/output/logs/");
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new LogFilter());
        
        setTitle("Dvs-Dtps Database Reconciler");
        setMinimumSize(new Dimension(420, 300));
        setMaximizedBounds(new Rectangle(new Dimension(410, 1000)));
        setSize(420, 300);
        setLocation(300, 200);
        
        panel = new JPanel(new BorderLayout());
        radioButtonPanel = new RadioButtonPanel();
        
        radioButtonPanel.reconcile.addActionListener(reconcileListener);
        radioButtonPanel.openLog.addActionListener(openLogListener);
        
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane = new JScrollPane(textArea);
        panel.add(radioButtonPanel, BorderLayout.PAGE_START);
        panel.add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(panel);
        
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
    }
    
    private class ReconcileListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if (radioButtonPanel.daily.isSelected()) {
                runReconcile();
            }
            else if (radioButtonPanel.monthly.isSelected())
                textArea.setText("Montly reconciliation under development.");
        }
    }
    
    private void runReconcile() {
        DateTime queryDate = validDate();
        
        if (queryDate != null) {
            try {
                final DateTime startDate = new DateTime(queryDate.getYear(), queryDate.getMonthOfYear(), queryDate.getDayOfMonth(),
                        0,0,0,0);
                final DateTime endDate = new DateTime(queryDate.getYear(), queryDate.getMonthOfYear(), queryDate.getDayOfMonth(),
                        0,0,0,0).plusDays(1);
                
                initApp(root, domain);
                
                ReconcileDtpDvs r = new ReconcileDtpDvs(startDate, endDate);
                
                r.enact();
                textArea.setText("Finished queries and log reports for:\n" + validDate());
            } catch (Exception e1) {
                System.err.println("Error running reconciliation: " + e1.getMessage());
            }
        }
        else 
            textArea.setText("Invalid Date Entered.");
    }
    
    private DateTime validDate() {
        int month, day, year;
        
        try {
            Scanner dateScanner = new Scanner(radioButtonPanel.dateField.getText());
            dateScanner.useDelimiter("-");
            month = Integer.parseInt(dateScanner.next());
            if (month < 1 || month > 12)
                return null;
            day = Integer.parseInt(dateScanner.next());
            if (day < 1 || day > 31)
                return null;
            year = Integer.parseInt(dateScanner.next());
            if (year < 2000 || year > 2011)
                return null;
            
        } catch (Exception e) {
            return null;
        }
        
        return new DateTime(year, month, day,0,0,0,0);
    }
    
    private class OpenLogListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            int returnVal = fc.showOpenDialog(ReconcilerAppFrame.this);
            
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                openFile = fc.getSelectedFile();
                textArea.setText(openFile.getName() + "\n");
                try {
                    displayLog(openFile);
                } catch (IOException e2) {
                    System.err.println("Error displaying lof file: " + e2.getMessage());
                }
            }
        }
    }
    
    private class LogFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String extension = getExtension(f);
            if (extension != null) {
                if (extension.equals("log"))
                    return true;
                else
                    return false;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Log Files";
        }
        
        private String getExtension(File f) {
            String ext = null;
            String name = f.getName();
            int i = name.lastIndexOf(".");
            
            if (i > 0 && i < name.length() - 1) {
                ext = name.substring(i + 1).toLowerCase();
            }
            return ext;
        }
    }
    
    private void displayLog(File log) throws IOException {
        RandomAccessFile logRAF = new RandomAccessFile(log, "r");
        Scanner scanner;
        while (logRAF.getFilePointer() < logRAF.length()) {
            textArea.append("\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t\t");
            textArea.append(scanner.next() + "\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t");
            textArea.append(scanner.next() + "\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t");
            textArea.append(scanner.next() + " " + scanner.next() + "\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t\t");
            textArea.append(scanner.next() + " " + scanner.next() + "\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t\t");
            textArea.append(scanner.next() + "\n");
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t\t");
            String ourPrice = scanner.next();
            if (ourPrice.equalsIgnoreCase("null")) {
            	textArea.append(ourPrice + "\n");
            }
            else {
            	textArea.append(ourPrice + " " + scanner.next() + "\n");
            }
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t\t");
            String listPrice = scanner.next();
            if (listPrice.equalsIgnoreCase("null")) {
            	textArea.append(listPrice + "\n");
            }
            else {
            	textArea.append(listPrice + " " + scanner.next() + "\n");
            }
            scanner = new Scanner(logRAF.readLine());
            textArea.append(scanner.next() + "\t");
            textArea.append(scanner.next() + "\n");
            textArea.append(logRAF.readLine());
        }
        textArea.setCaretPosition(0);
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
    
    private class RadioButtonPanel extends JPanel {
        private static final long serialVersionUID = 3635609946731064408L;

        JPanel radioPanel = new JPanel();
        JPanel westPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel =  new JPanel();
        JRadioButton daily = new JRadioButton("Daily");
        JRadioButton monthly = new JRadioButton("Monthly");
        ButtonGroup group = new ButtonGroup();
        JButton reconcile = new JButton("Reconcile");
        JButton openLog = new JButton("Open Log");
        DateTime yesterday = new DateTime().minusDays(1);
        JTextField dateField = new JTextField(String.valueOf(yesterday.getMonthOfYear()) + "-" +
                String.valueOf(yesterday.getDayOfMonth()) + "-" + String.valueOf(yesterday.getYear()));
        
        public RadioButtonPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEtchedBorder());
            daily.setSelected(true);
            
            //reconcile.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            reconcile.setPreferredSize(new Dimension(94, 48));
            
            //openLog.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            openLog.setPreferredSize(new Dimension(94, 48));
            
            group.add(daily);
            group.add(monthly);
            
            radioPanel.add(daily);
            radioPanel.add(monthly);
            
            buttonPanel.add(reconcile);
            buttonPanel.add(openLog);
            
            westPanel.add(radioPanel, BorderLayout.CENTER);
            westPanel.add(dateField, BorderLayout.SOUTH);
            
            add(westPanel, BorderLayout.WEST);
            add(Box.createHorizontalStrut(15));
            add(buttonPanel, BorderLayout.EAST);
            
            dateField.setBorder(BorderFactory.createLoweredBevelBorder());
            dateField.setHorizontalAlignment(JTextField.CENTER);
            dateField.setBackground(Color.YELLOW);
            //dateField.setEditable(false);
        }
    }
}
