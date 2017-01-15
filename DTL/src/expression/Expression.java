package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * Abstract Expression class.
 **/
public abstract class Expression implements Cloneable {

    public boolean parameterized() {
        return false;
    }
    
    public boolean check() {
        return true;
    }

    public Expression translate(java.util.Map m) {
        return this;
    }
    ;
    
    public abstract Data eval(DataContext c);
    
    public abstract Element toXML();

    public Expression clone() {
        try {
            return (Expression) super.clone();
        } catch (CloneNotSupportedException e) {
            Log.error(Expression.class, "couldn't clone expression", e);
        }
        return null;
    }
}
