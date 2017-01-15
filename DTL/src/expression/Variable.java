package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A reference to a variable.
 **/
public class Variable extends Value {

    protected String var = null;

    public Variable(String v) {
        super(null);
        var = v;
    }

    public String getName() {
        return var;
    }

    public Data eval(DataContext c) {
        return c.getVariable(var);
    }
    
    public Element toXML() {
        return toXML("variable");
    }
    
    public Element toXML(String name) {
        Element v = null;

        if (var != null) {
            v = new Element(name);
            v.addAttribute(new Attribute("name", var));
        }
        return v;
    }
    
    public String toString() {
        return ("VARIABLE[" + var + "]");
    }
}

