package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The Upper operation makes upper case of all characters in the String
 * representation of the value.
 **/
public class Upper extends Value {
  
    protected Expression expr = null;

    public Upper(Expression e) {
        this.expr = e;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Upper.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            res = new Data(e.getValue().toString().toUpperCase());
            res.addMarkedSource(e);
            res.setNote("UPPER(" + e + ")");
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.setNote("UPPER(" + e + ") resulted in a NULL");
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

    public Upper clone() {
        Upper u = (Upper) super.clone();

        u.expr = this.expr.clone();
        return u;
    }

    public String toString() {
        return ("UPPER(" + expr + ")");
    }
}
