package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The focus variable references the database field currently in focus, e.g.
 * by a NOTION IS statement.
 **/
public class FocusVariable extends Variable {

    public FocusVariable() {
        super(null);
    }

    public boolean parameterized() {
        return false;
    }
    
    public Data eval(DataContext c) {
        if (c.getFocus() != null) {
            return c.getFocus();
        }
        Log.error(FocusVariable.class,
                "reference to the default value, but the current context hasn't one");
        return null;
    }
    
    public Element toXML() {
        return toXML("focus");
    }

    public Element toXML(String name) {
        Element v = null;

        v = new Element(name);
        if (!"focus".equals(name)) {
            v.addAttribute(new Attribute("focus", "focus"));
        }
        return v;
    }
    
    public String toString() {
        return ("FOCUS");
    }
}

