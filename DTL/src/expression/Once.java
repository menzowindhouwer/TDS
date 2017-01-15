package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * The Once operation maintains a set of named lookup tables. Only the first
 * time a value is added it's returned, subsequent calls with the same value will
 * return a null.
 **/
public class Once extends Value {
  
    static protected HashMap maps = new HashMap();
    protected String         name = null;
    protected Expression     expr = null;
    
    public Once(String name, Expression expr) {
        this.name = name;
        this.expr = expr;
        
        if (!maps.containsKey(name))
            maps.put(name,new HashSet());
    }
    
    public Data eval(DataContext c) {
        Data res = null;
        
        Data val = c.getFocus();
        if (expr!=null)
            val = expr.eval(c);

        if (val != null) {
			String str = null;
            if (val.isEmpty()) {
                Log.warning(Once.class,
                    "" + this
                    + " value expression resulted in an empty value");
            } else
				str = val.getValue().toString();
            if (((Set)maps.get(name)).add(str)) {
                Log.message(Once.class,
                    "" + this
                    + " keep " + val);
                res = val;
            } else {
                Log.message(Once.class,
                    "" + this
                    + " skip " + val);
                res = new Data(null);
                res.setNote(
                    "ONCE(" + name + ") skipped known value");
            }
        } else {
            Log.error(Once.class,
                "" + this + " value expression couldn't be evaluated");
        }
        return res;
    }
    
    public boolean parameterized() {
        if (expr!=null)
            return expr.parameterized();
        return false;
    }

    public Expression translate(java.util.Map m) {
        if (expr!=null)
            expr = expr.translate(m);
        return this;
    }

    public Once clone() {
        Once o = (Once) super.clone();
        if (expr!=null)
            o.expr = expr.clone();
        return o;
    }

    public String toString() {
        return ("ONCE(" + name + "," + (expr!=null?expr:"FOCUS") + ")");
    }
}
