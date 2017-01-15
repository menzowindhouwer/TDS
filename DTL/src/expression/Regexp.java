package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.regex.*;


/**
 * The regexp operation allows to extract part of a String representation of
 * a value using a regular expression. Notice that only one group is returned,
 * i.e. the first one.
 **/
public class Regexp extends Value {
  
    protected Expression expr = null;
    protected Expression regexp = null;

    public Regexp(Expression e, Expression re) {
        this.expr = e;
        this.regexp = re;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data re = regexp.eval(c);

        if (re == null) {
            Log.error(Regexp.class,
                    "" + this + " regular expression couldn't be evaluated");
            return null;
        }
        Pattern p = null;

        try { 
            p = Pattern.compile(re.getValue().toString(),
                    Pattern.MULTILINE ^ Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
            Log.error(Regexp.class,
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
            Log.error(Regexp.class,
                    "" + this + " match expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            Matcher m = p.matcher(e.getValue().toString());

            if (m.find()) {
                MatchResult matches = m.toMatchResult();

                if (matches.groupCount() > 0) {
                    if (matches.groupCount() > 1) {
                        Log.warning(Regexp.class,
                                "" + this
                                + " resulted in multiple groups, only the first one is returned");
                    }
                    res = new Data(matches.group(1));
                    res.addMarkedSource(e);
                    res.addMarkedSource(re);
                    res.setNote(
                            "REGEXP(" + e + "," + re
                            + ") resulted in multiple groups, returned the first one");
                } else {
                    res = new Data(matches.group(0));
                    res.addMarkedSource(e);
                    res.addMarkedSource(re);
                    res.setNote(
                            "REGEXP(" + e + "," + re + ") resulted in a match");
                }
            }
        }
        if (res == null) {
            // Log.warning(""+this+" resulted in no match for REGEXP("+e.getValue().toString()+","+re.getValue().toString()+")");
            res = new Data(null);
            res.addMarkedSource(e);
            res.addMarkedSource(re);
            res.setNote("REGEXP(" + e + "," + re + ") resulted in no match");
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

    public Regexp clone() {
        Regexp re = (Regexp) super.clone();

        re.expr = this.expr.clone();
        re.regexp = this.regexp.clone();
        return re;
    }

    public String toString() {
        return ("REGEXP(" + expr + "," + regexp + ")");
    }
}
