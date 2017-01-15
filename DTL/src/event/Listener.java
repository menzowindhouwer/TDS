package nl.uu.let.languagelink.tds.dtl.event;


/**
 * The listener interface allows to listen for messages of various kinds.
 **/
public interface Listener {

    public void debug(Class c, String m, Exception e);
    
    public void message(Class c, String m, Exception e);
    
    public void warning(Class c, String m, Exception e);
    
    public void error(Class c, String m, Exception e);
    
    public void fatal(Class c, String m, Exception e);

}
