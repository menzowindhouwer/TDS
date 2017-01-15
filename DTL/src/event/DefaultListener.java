package nl.uu.let.languagelink.tds.dtl.event;


import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * Default implementation of the Listener and Tracker interfaces. Outputs
 * the messages on the console.
 **/
public class DefaultListener implements Listener, Tracker {
    
    protected boolean st = true;
    protected int level = 0;
    
    public void stackTrace(boolean st) {
        this.st = st;
    }
    
    protected void out(String prefix, String m, Exception e) {
        for (int i = 0; i < level; i++) {
            System.err.print("  ");
        }
        System.err.println(
                prefix + ":" + (m != null ? m + (e != null ? ":" : "") : "")
                + (e != null ? e : ""));
        if (st && (e != null)) {
            e.printStackTrace(System.err);
        }
    }
    
    public void debug(Class c, String m, Exception e) {
        out("?DEBUG  ", m, e);
    }
    
    public void message(Class c, String m, Exception e) {
        out("?MESSAGE", m, e);
    }
    
    public void warning(Class c, String m, Exception e) {
        out("!WARNING", m, e);
    }
    
    public void error(Class c, String m, Exception e) {
        out("!ERROR  ", m, e);
    }
    
    public void fatal(Class c, String m, Exception e) {
        out("!FATAL  ", m, e);
        level = 0;
    }
    
    public void push(String c, Object o) {
        for (int i = 0; i < level; i++) {
            System.err.print("  ");
        }
        System.err.println(
                "CONTEXT:" + c + (o != null ? ":" + o.toString() : ""));
        level++;
    }
    
    public void pop() {
        level--;
    }
}
