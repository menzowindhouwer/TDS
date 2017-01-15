package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.expression.Variable;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;
import java.net.URL;

import nu.xom.*;


/**
 * A processing context contains information about fields to skip, how to
 * preprocess fields, and specific variables.
 **/
public abstract class Context {

    /* where are we? */
    protected Scope scope = null;
    protected Context parent = null;
    
    /* preprocessing steps */
    protected Set skiplist = new HashSet();
    protected List preprocess = new Vector();
    
    /* variables */
    protected java.util.Map variables = new HashMap();
  
    /* known fields */
    protected java.util.Map known_fields = new HashMap();

    /* manage surroudings */
    public void setScope(Scope scope) {
        this.scope = scope;
    }
    
    public Scope getScope() {
        return scope;
    }
    
    public void setParent(Context parent) {
        this.parent = parent;
    }
    
    public Context getParent() {
        return parent;
    }
    
    /* manage preprocessing */
    public boolean addSkipList(Set fields) {
        return skiplist.addAll(fields);
    }
    
    public boolean skip(String field) {
        return skiplist.contains(field);
    }
    
    public boolean addPreprocess(Preprocess p) {
        preprocess.add(p);
        return true;
    }
    
    public boolean hasPreprocess() {
        return ((preprocess != null) && (preprocess.size() > 0));
    }
    
    public List getPreprocess() {
        return preprocess;
    }

    /* manage variables */
    public Variable addVariable(String name, Expression expr) {
        Variable v = null;

        if (!variables.containsKey(name)) {
            variables.put(name, expr);
            v = new Variable(name);
            //System.err.println("!MENZO: variable[" + name + "] created in " + this);
        } else {
            Log.error(Context.class,
                    "variable[" + name + "] already exists in " + this);
        }
        return v;
    }

    public Variable getVariable(String name) {
        Variable v = null;

        if (variables.containsKey(name)) {
            v = new Variable(name);
	}
        return v;
    }
    
    public java.util.Map getVariables() {
        return variables;
    }

    /* abstract methods */
    protected abstract Document importData(DataContext c);
    
    /* instantiate the context */
    public DataContext createDataContext(DataContext dc, Element root) {
        DataContext c = new DataContext(this, dc, root);

        return c;
    }
  
    public DataContext createDataContext(DataContext dc) {
        return createDataContext(dc, dc.getRoot());
    }
  
    public void readDataContext(DataContext dc) {
        java.util.Map fields = dc.getKnownFields();

        if (fields != null) {
            for (Iterator iter = fields.keySet().iterator(); iter.hasNext();) {
                String field = (String) iter.next();
                FieldInfo info = (FieldInfo) fields.get(field);

                if (known_fields.containsKey(field)) {
                    if (!((FieldInfo) known_fields.get(field)).merge(info)) {
                        Log.error(Context.class,
                                "" + this
                                + ":couldn't merge field info for field["
                                + field + "]");
                    }
                } else {
                    known_fields.put(field, info.copy());
                }
            }
        }
    }
  
    public boolean untouchedFields() {
        boolean res = false;

        for (Iterator iter = known_fields.keySet().iterator(); iter.hasNext();) {
            String field = (String) iter.next();
            FieldInfo info = (FieldInfo) known_fields.get(field);

            if (!info.isTouched() && !skip(field)) {
                //Log.error(Context.class, "field[" + field + "] is never touched");
                Log.warning(Context.class, "field[" + field + "] is never touched");
                res = true;
            }
        }
        return res;
    }
}
