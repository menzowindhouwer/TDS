package nl.uu.let.languagelink.tds.ipa;

import javax.swing.DefaultListModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

class LogOutputStream extends ByteArrayOutputStream {
    
    protected DefaultListModel log = null;
    protected PrintStream      out = null;
    
    public LogOutputStream(DefaultListModel log,PrintStream out) {
        super();
        this.log = log;
        this.out = out;
    }
    
    public void flush() throws IOException {

        String record = null;
        synchronized(this) {
            super.flush();
            record = this.toString();
            super.reset();
        }
        
        if (log!=null) {
            String[] entries = record.trim().split("\n");
            for (int i=0;i<entries.length;i++)
                log.addElement(entries[i]);
        }
        
        if (out!=null)
            out.print(record);
    }
}