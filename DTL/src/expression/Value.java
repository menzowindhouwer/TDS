package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.annotate.Description;
import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * Abstract value class.
 **/
public abstract class Value extends Expression {

    protected static int id = 0;
    protected int my_id = id++;
    protected Object value = null;
    
    public Value() {
        value = null;
    }

    public Value(Object v) {
        value = v;
    }
    
    public Object getValue() {
        return value;
    }
    
    public Element toXML() {
        return toXML("value");
    }
    
    public Element toXML(String name) {
        return toXML(name, value);
    }
    
    public static Element toXML(String name, Object val) {
        Element v = null;

        if (val != null) {
            v = new Element(name);

            Element t = null;
            if (val instanceof Description) {
                t = ((Description)val).toXML("this");
            } else if (val instanceof Literal) {
                t = ((Literal)val).toXML("this");
            } else {
                t = new Element("this");
                t.appendChild(val.toString());
            }

            v.appendChild(t);
        }
        return v;
    }

    public boolean equals(Value v) {
        if (v != null) {
            if (my_id == v.my_id) {
                return true;
            } else if (v.value != null && value != null) {
                return v.value.equals(value);
            }
        }
        return false;
    }
    
    public int hashCode() {
        if (value != null) {
            return value.hashCode();
        }
        return super.hashCode();
    }

    public void debug(String indent, String incr) {
        Log.debug(Value.class, indent + "VALUE[" + this + "]");
    }

    public void debug() {
        debug("", "  ");
    }

    public String toString() {
        if (value != null) {
            return value.toString();
        }
        return "<null>";
    }
}
