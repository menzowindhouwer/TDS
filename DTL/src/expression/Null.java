package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * A literal NULL value.
 **/
public class Null extends Literal {

    public Null() {
        super(null);
    }
    
    public String toString() {
        return "<null>";
    }
}
