package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A reference to a database field.
 **/
public class FieldVariable extends Variable {

    public FieldVariable(String f) {
        super(f);
    }
    
    public Data eval(DataContext c) {
	Data d = c.getField(var);
        Log.debug(FieldVariable.class, "" + this + " => " + d);
	return d;
    }
    
    public Element toXML() {
        return toXML("field");
    }

    public String toString() {
        return ("VARIABLE[FIELD[" + var + "]]");
    }
}

