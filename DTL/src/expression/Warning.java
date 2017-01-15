package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * Show an Warnings message
 **/
public class Warning extends Message {

    public Warning(Value v) {
        super(v);
        prefix = "!WARNING";
    }

    public void show(DataContext c) {
        if (hasMessage()) {
            Data m = getMessage(c);

            if (!m.isEmpty()) {
                Log.warning(Warning.class,
                        c.trace() + ":" + m.getValue().toString());
            } else {
                Log.warning(Warning.class, c.trace() + ":" + "empty warning");
            }
        }
    }
}
