package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * Show an Error message
 **/
public class Error extends Warning {

    public Error(Value v) {
        super(v);
        prefix = "!ERROR";
    }
    
    public void show(DataContext c) {
        if (hasMessage()) {
            Data m = getMessage(c);

            if (!m.isEmpty()) {
                Log.error(Error.class, c.trace() + ":" + m.getValue().toString());
            } else {
                Log.error(Error.class, c.trace() + ":" + "empty error");
            }
        }
    }
}
