package nl.uu.let.languagelink.tds.dtl.map;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.data.*;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The UseMap manages the parameters to a call to another map.
 **/
public class UseMap implements Map {
    
    /* private variables */
    protected Scope         scope = null;
    protected Map           base = null;
    protected List          params = null;
    protected java.util.Map translate = null;
    
    /* constructor */
    public UseMap(Scope s, Map map, List p) {
        scope = s;
        base = map;
        
        if (base.getSize() == p.size()) {
            
            // create translation lookup map
            translate = new HashMap();
            for (int i = 0; i < base.getSize(); i++) {
                translate.put(base.getParam(i), p.get(i));
            }
            
            // create a list of remainding parameters
            params = new Vector();
            for (Iterator iter = p.iterator(); iter.hasNext();) {
                Value v = (Value) iter.next();

                if (v instanceof Param) {
                    params.add(v);
                }
            }
            
        } else {
            throw new RuntimeException(
                    "Can't translate " + base + " using "
                    + Arrays.toString(p.toArray()) + "!");
        }
    }
    
    /* General accessors */
    public Scope getScope() {
        return scope;
    }
    
    public String getName() {
        return base.getName() + "'";
    }
    
    public int getLength() {
        return base.getLength();
    }

    /* Access the parameters */
    public int getSize() {
        return params.size();
    }
    
    public boolean isParam(Variable p) {
        return params.contains(p);
    }
    
    public Param getParam(int index) {
        if (index < getSize()) {
            return (Param) params.get(index);
        }
        return null;
    }
    
    public Param getParam(String name) {
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Param p = (Param) iter.next();

            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
    
    /* Access the values */
    public java.util.Map getValues() {
        return base.getValues();
    }
    
    public Annotation getAnnotation(Value v) {
        return base.getAnnotation(v);
    }
    
    /* Derive a new Map by translating the parameter(s) */
    public UseMap useMap(Scope s, List p) {
        if (p == null) {
            p = new Vector();
        }
        if (getSize() == p.size()) {
            return new UseMap(s, this, p);
        }
        return null;
    }
    
    public UseMap useMap(Scope s, Value val) {
        List p = null;

        if (val != null) {
            p = new Vector();
            p.add(val);
        }
        return useMap(s, p);
    }
    
    public UseMap useMap(Scope s) {
        if (getSize() != 1) {
            return null;
        }
        List p = new Vector();

        p.add(new FocusVariable());
        return useMap(s, p);
    }
    
    /* import the map into another map */
    public BaseMap importMap(BaseMap m, java.util.Map t) {
        t.putAll(translate);
        return base.importMap(m, t);
    }
    
    /* Handle lookup(s) in the map */
    public List lookups(DataContext c) {
        Log.push("use map", this);
        List res = null;

        c.pushTranslate(translate);
        res = base.lookups(c);
        c.popTranslate();
        Log.pop();
        return res;
    }
    
    public Data lookup(DataContext c) {
        Log.push("use map", this);
        Data res = null;

        c.pushTranslate(translate);
        res = base.lookup(c);
        c.popTranslate();
        Log.pop();
        return res;
    }
    
    /* Get the datatype of the lookup result */
    public String getDataType() {
        return base.getDataType();
    }
    
    /* Semantic check of the map */
    public boolean parameterized() {
        return (getSize() > 0);
    }
    
    public boolean check() {
        if (getSize() > 0) {
            Log.error(UseMap.class,
                    "Not all parameters of " + this + " have been resolved!");
            return false;
        }
        return true;
    }
    
    /* Create an XML dump of the map */
    public Element toXML() {
        return base.toXML();
    }

    /* Print debug info about the map */
    public void debug(String indent, String incr) {
        Log.debug(BaseMap.class,
                indent + "USE MAP: " + getScope() + ":" + getName());
        Log.debug(BaseMap.class, indent + "- size: " + getSize());
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Log.debug(BaseMap.class, indent + "- parameter: " + iter.next());
        }
        base.debug(indent + incr, incr);
    }
    
    public void debug() {
        debug("", "  ");
    }
    
    public String toString() {
        return "USEMAP[" + getName() + "](" + Arrays.toString(params.toArray())
                + ")";
    }
}
