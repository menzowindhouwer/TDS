package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The regsub operation allows to replace part of a String representation of
 * a value using a regular expression.
 **/
public class Regsub extends Value {
  
    protected Expression expr = null;
    protected Expression regexp = null;
    protected Expression replace = null;

    public Regsub(Expression e, Expression re, Expression r) {
        this.expr = e;
        this.regexp = re;
        this.replace = r;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Regsub.class,
                    "" + this + " match expression (" + expr
                    + ") couldn't be evaluated");
            return null;
        }
        Data re = regexp.eval(c);

        if (re == null) {
            Log.error(Regsub.class,
                    "" + this + " regular expression (" + regexp
                    + ") couldn't be evaluated");
            return null;
        }
        if (re.getValue() == null) {
            Log.error(Regsub.class,
                    "" + this + " regular expression (" + re
                    + ") resulted in a NULL value");
        }
        Data r = replace.eval(c);

        if (r == null) {
            Log.error(Regsub.class,
                    "" + this + " replacement expression (" + replace
                    + ") couldn't be evaluated");
            return null;
        }
        if (r.getValue() == null) {
            Log.warning(Regsub.class,
                    "" + this + " replacement expression (" + replace
                    + ") resulted in a NULL value");
        }
        if (e.getValue() != null) {
            if (re.getValue() != null) {
                try {
                    res = new Data(
                            e.getValue().toString().replaceAll(
                                    re.getValue().toString(),
                                    (r.getValue() != null
                                            ? r.getValue().toString()
                                            : "")));
                } catch (PatternSyntaxException ex) {
                    Log.error(Regsub.class,
                            "" + this + " evaluated regular expression["
                            + re.getValue().toString()
                            + "] couldn't be compiled",
                            ex);
                    return null;
                }
            } else {
                res = e;
            }
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.addMarkedSource(re);
            res.setNote(
                    "REGSUB(" + e + "," + re + "," + r + ") resulted in a NULL");
        }
        return res;
    }
    
    public boolean parameterized() {
        return ((expr != null && expr.parameterized())
                || (regexp != null && regexp.parameterized())
                || (replace != null && replace.parameterized()));
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        regexp = regexp.translate(m);
        replace = replace.translate(m);
        return this;
    }

    public Regsub clone() {
        Regsub rs = (Regsub) super.clone();

        rs.expr = this.expr.clone();
        rs.regexp = this.regexp.clone();
        rs.replace = this.replace.clone();
        return rs;
    }

    public String toString() {
        return ("REGSUB(" + expr + "," + regexp + "," + replace + ")");
    }
}
