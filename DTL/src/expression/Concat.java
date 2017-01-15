package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.data.DataList;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * The Concat class executes a concatenation of the String representations
 * of all its parameters.
 **/
public class Concat extends Value {
  
    protected List params = null;
    
    public Concat(List l) {
        params = l;
    }

    public Data eval(DataContext c) {
        Set concats = new HashSet();
        concats.add("");

        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Expression expr = (Expression) iter.next();
            Data part = expr.eval(c);

            if (part != null) {
            	    DataList parts = null;
            	    if (part instanceof DataList) {
            	    	    parts = (DataList)part;
            	    } else {
            	    	    parts = new DataList(part);
            	    }
            	    // create the new result set by adding the values from the part list to the existing result set
            	    Set newConcats = new HashSet();
            	    for (Iterator piter = parts.getList().iterator();piter.hasNext();) {
            	    	    Data spart = (Data)piter.next();
			    for (Iterator citer = concats.iterator();citer.hasNext();) {
				    String concat = citer.next().toString();
				    newConcats.add(concat+(spart.getValue()!=null?spart.getValue().toString():""));
			    }
            	    }
            	    concats = newConcats;
            } else {
                Log.error(Concat.class,
                        "" + this + " " + expr
                        + " of the concatanation parameters couldn't be evaluated");
            }
        }
        
        DataList res = new DataList();
        for (Iterator iter = concats.iterator();iter.hasNext();)
        	res.addValue(iter.next());
        
        return res;
    }
    
    public boolean parameterized() {
        if (params != null) {
            for (Iterator iter = params.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                if (expr.parameterized()) {
                    return true;
                }
            }
        }
        return false;
    }

    public Expression translate(java.util.Map m) {
        if (params != null) {
            List p = new Vector();

            for (Iterator iter = params.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                p.add(expr.translate(m));
            }
            params = p;
        }
        return this;
    }

    public Concat clone() {
        Concat c = (Concat) super.clone();

        if (params != null) {
            c.params = new Vector();
            for (Iterator iter = params.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                c.params.add(expr.clone());
            }
        }
        return c;
    }

    public String toString() {
        return ("CONCAT(...)");
    }
}
