package nl.uu.let.languagelink.tds.dtl.event;


/**
 * Specific implementation of the Listener interface which counts
 * how many messages of a certain kind are issued.
 **/
public class StatisticsListener implements Listener {
    
    protected int debug = 0;
    protected int message = 0;
    protected int warning = 0;
    protected int error = 0;
    protected int fatal = 0;
    
    public void reset() {
        debug = 0;
        message = 0;
        warning = 0;
        error = 0;
        fatal = 0;
    }
    
    public int debug() {
        return debug;
    }
    
    public void debug(Class c, String m, Exception e) {
        debug++;
    }
    
    public int message() {
        return message;
    }
    
    public void message(Class c, String m, Exception e) {
        message++;
    }
    
    public int warning() {
        return warning;
    }
    
    public void warning(Class c, String m, Exception e) {
        warning++;
    }
    
    public int error() {
        return error;
    }
    
    public void error(Class c, String m, Exception e) {
        error++;
    }
    
    public int fatal() {
        return fatal;
    }
    
    public void fatal(Class c, String m, Exception e) {
        fatal++;
    }
}
