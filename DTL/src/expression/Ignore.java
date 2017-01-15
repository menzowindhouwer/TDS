package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * A literal ignored NULL value.
 **/
public class Ignore extends Null {

    public Ignore() {
        super();
    }
    
    public String toString() {
        return "<ignore>";
    }
}
