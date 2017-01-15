package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The Lower operation makes lower case of all characters in the String
 * representation of the value.
 **/
public class Lower extends Value {
  
    protected Expression expr = null;

    public Lower(Expression e) {
        this.expr = e;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Lower.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            res = new Data(e.getValue().toString().toLowerCase());
            res.addMarkedSource(e);
            res.setNote("LOWER(" + e + ")");
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.setNote("LOWER(" + e + ") resulted in a NULL");
        }
        return res;
    }
    
    public boolean parameterized() {
        return (expr != null && expr.parameterized());
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        return this;
    }

    public Lower clone() {
        Lower l = (Lower) super.clone();

        l.expr = this.expr.clone();
        return l;
    }

    public String toString() {
        return ("LOWER(" + expr + ")");
    }
}
