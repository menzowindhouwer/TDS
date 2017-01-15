package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The Match operation looks if the String representation of a value
 * matches the regular expression.
 **/
public class Match extends Value {
  
    protected Expression expr = null;
    protected Expression regexp = null;

    public Match(Expression e, Expression re) {
        this.expr = e;
        this.regexp = re;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data re = regexp.eval(c);

        if (re == null) {
            Log.error(Match.class,
                    "" + this + " regular expression couldn't be evaluated");
            return null;
        }
        Pattern p = null;

        try { 
            p = Pattern.compile(re.getValue().toString(),
                    Pattern.MULTILINE ^ Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            Log.error(Match.class,
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
            Log.error(Match.class,
                    "" + this + " match expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            Matcher m = p.matcher(e.getValue().toString());

            if (m.matches()) {
                res = new Data(new Boolean(true));
                res.addMarkedSource(e);
                res.addMarkedSource(re);
                res.setNote("MATCH(" + e + "," + re + ") resulted in a match");
            }
        }
        if (res == null) {
            Log.debug(Match.class,""+this+" resulted in no match for MATCH("+e.getValue()+","+re.getValue()+")");
            res = new Data(new Boolean(false));
            res.addMarkedSource(e);
            res.addMarkedSource(re);
            res.setNote("MATCH(" + e + "," + re + ") resulted in no match");
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

    public Match clone() {
        Match m = (Match) super.clone();

        m.expr = this.expr.clone();
        m.regexp = this.regexp.clone();
        return m;
    }

    public String toString() {
        return ("MATCH(" + expr + "," + regexp + ")");
    }
}
