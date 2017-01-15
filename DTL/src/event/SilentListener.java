package nl.uu.let.languagelink.tds.dtl.event;


/**
 * An implementation of the Listener interface which just ignores
 * all kinds of messages. Usefull as the base class of a class which
 * just wants to listen to one or two kinds of messages.
 **/
public class SilentListener implements Listener {
    
    public void debug(Class c, String m, Exception e) {}
    
    public void message(Class c, String m, Exception e) {}
    
    public void warning(Class c, String m, Exception e) {}
    
    public void error(Class c, String m, Exception e) {}
    
    public void fatal(Class c, String m, Exception e) {}
}
