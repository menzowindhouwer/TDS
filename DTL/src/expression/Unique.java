package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * The Unique operation returns an unique number.
 **/
public class Unique extends Value {
  
    protected static int id = 0;

    public Unique() {}

    public Data eval(DataContext c) {
        return new Data(c.getScope().getName()+"-"+(++id));
    }
    
    public String toString() {
        return ("UNIQUE()");
    }
}
