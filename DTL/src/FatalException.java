package nl.uu.let.languagelink.tds.dtl;


/**
 * Our own Fatal exception, to be catched by the Engine.
 **/
public class FatalException extends RuntimeException {
    
    public FatalException(String m, Exception e) {
        super(m, e);
    }
}
