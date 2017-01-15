package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The Find operation looks if (a part of) the String representation of a value
 * matches the regular expression.
 **/
public class Find extends Value {
  
    protected Expression expr = null;
    protected Expression regexp = null;

    public Find(Expression e, Expression re) {
        this.expr = e;
        this.regexp = re;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data re = regexp.eval(c);

        if (re == null) {
            Log.error(Find.class,
                    "" + this + " regular expression couldn't be evaluated");
            return null;
        }
        Pattern p = null;

        try { 
            p = Pattern.compile(re.getValue().toString(),
                    Pattern.MULTILINE ^ Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            Log.error(Find.class,
                    "" + this + " evaluated regular expression["
                    + re.getValue().toString() + "] couldn't be compiled",
                    e);
            return null;
        }
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Find.class,
                    "" + this + " match expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            Matcher m = p.matcher(e.getValue().toString());

            if (m.find()) {
                res = new Data(new Boolean(true));
                res.addMarkedSource(e);
                res.addMarkedSource(re);
                res.setNote("FIND(" + e + "," + re + ") resulted in a match");
            }
        }
        if (res == null) {
            // Log.warning(""+this+" resulted in no match for FIND("+e.getValue().toString()+","+re.getValue().toString()+")");
            res = new Data(new Boolean(false));
            res.addMarkedSource(e);
            res.addMarkedSource(re);
            res.setNote("FIND(" + e + "," + re + ") resulted in no match");
        }
        return res;
    }
    
    public boolean parameterized() {
        return ((expr != null && expr.parameterized())
                || (regexp != null && regexp.parameterized()));
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        regexp = regexp.translate(m);
        return this;
    }

    public Find clone() {
        Find f = (Find) super.clone();

        f.expr = this.expr.clone();
        f.regexp = this.regexp.clone();
        return f;
    }

    public String toString() {
        return ("FIND(" + expr + "," + regexp + ")");
    }
}
