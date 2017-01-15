package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The Trim operation left and right trims the String representation of a value.
 **/
public class Trim extends Value {
  
    protected Expression expr = null;

    public Trim(Expression e) {
        this.expr = e;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Trim.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            res = new Data(e.getValue().toString().trim());
            res.addMarkedSource(e);
            res.setNote("TRIM(" + e + ")");
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.setNote("TRIM(" + e + ") resulted in a NULL");
        }
        return res;
    }
    
    public boolean parameterized() {
        return (expr != null && expr.parameterized());
    }

    public Expression translate(java.util.Map m) {
        if (expr != null) {
            expr = expr.translate(m);
        }
        return this;
    }

    public Trim clone() {
        Trim t = (Trim) super.clone();

        if (this.expr != null) {
            t.expr = this.expr.clone();
        }
        return t;
    }

    public String toString() {
        return ("TRIM(" + expr + ")");
    }
}
