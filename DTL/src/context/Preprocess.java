package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.data.DataList;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * The Preprocess class describes the preprocessing needed for (a subset of)
 * the fields loaded in a DataContext through a specific Context.
 **/
public class Preprocess {

    protected String pattern = null;
    protected Expression expr = null;
    protected Set fields = null;
    protected Set except = null;
    protected boolean all = true;
    protected String datatype = null;
    
    public Preprocess(String pattern, Expression expr, Set fields) {
        this.pattern = pattern;
        this.expr = expr;
        this.fields = fields;
        this.all = false;
    }
    
    public Preprocess(String pattern, Expression expr, boolean all) {
        this.pattern = pattern;
        this.expr = expr;
        this.all = all;
    }
    
    public Preprocess(String pattern, Expression expr, boolean all, Set except) {
        this.pattern = pattern;
        this.expr = expr;
        this.all = all;
        this.except = except;
    }
    
    public boolean setDataType(String dt) {
        if (datatype == null) {
            datatype = dt;
        }
        return (datatype == dt);
    }
    
    public String getDataType() {
        return datatype;
    }
    
    public boolean willProcessField(String field) {
        boolean res = false;

        if (all) {
            res = true;
            Log.debug(Preprocess.class,
                    "" + this + " will preprocess all fields, including field["
                    + field + "]");
        }
        if ((fields != null) && fields.contains(field)) {
            res = true;
            Log.debug(Preprocess.class,
                    "" + this + " will preprocess some fields, including field["
                    + field + "]");
        }
        if ((except != null) && except.contains(field)) {
            res = false;
            Log.debug(Preprocess.class,
                    "" + this
                    + " will not preprocess some fields, skipping field["
                    + field + "]");
        }
        return res;
    }
    
    public boolean eval(DataContext c) {
        boolean res = false;

        if (fields == null) {
            fields = c.getFields(); // get all fields
            if ((fields != null) && !all) {
                Set other = new HashSet(); // get all unpreprocessed fields

                for (Iterator iter = fields.iterator(); iter.hasNext();) {
                    String field = (String) iter.next();
                    FieldInfo info = c.getFieldInfo(field);

                    if (!info.isPreprocessed() && !info.isDerived()) {
                        other.add(field);
                    }
                }
                fields = other;
            }
        }
        if (except != null) { // remove the except fields
            fields.removeAll(except);
        }
        if ((fields != null) && (fields.size() > 0)) {
            res = true;
            for (Iterator iter = fields.iterator(); iter.hasNext();) {
                String field = (String) iter.next();

                if (c.knownField(field)) {
                    boolean touched = c.getFieldInfo(field).isTouched();
                    // fields can be instantiated more then once, so loop over the list
                    DataList dl = (DataList)c.getField(field);
                    for (ListIterator liter = dl.getList().listIterator();liter.hasNext();) {
                    	    int  srcidx = liter.nextIndex();
                    	    Data srcval = (Data)liter.next();
                    	    
                    	    Data value = expr.eval(c.setFocus(srcval));

			    if (value != null) {
				value.addMarkedSource(srcval);
				value.setNote(
					"preprocessed field[" + field
					+ "] using expression:" + expr);
				Log.message(Preprocess.class,"preprocessed field["+field+"] with value["+c.getFocus()+"] using expression:"+expr+" resulted in "+value);
				if ((pattern != null) && !pattern.equals("")) {
				    String name = field + pattern;
	
				    if (pattern.contains("%")) {
					name = pattern.replaceAll("%", field);
				    }
				    if (!c.addDerivedField(name, value)) {
					Log.error(Preprocess.class,
						"couldn't create or instantiate derived field["
						+ name + "]"); 
					continue;
				    }
				} else {
					// replace old value by preprocessed value
					liter.set(value);
				}
			    } else {
				Log.error(Preprocess.class,
					"couldn't evaluate preprocessing expression "
					+ expr + " for field[" + field + "]");
				res = false;
			    }
			    c.getFieldInfo(field).preprocessed();
			    if (!touched) // preprocessing shouldn't change the touched state
				c.getFieldInfo(field).untouch();
			    c.clearFocus();
		    }
                } else {
                    Log.error(Preprocess.class,
                            "field[" + field + "] is unknown");
                }
            }
        } else {
            Log.warning(Preprocess.class,
                    "preprocessing step with expression " + expr
                    + " skipped as there are no fields selected");
        }
        return res;
    }
}
