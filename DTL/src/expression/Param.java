package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * A Map parameter.
 **/
public class Param extends Variable {

    public Param(String v) {
        super(v);
    }

    public boolean parameterized() {
        return true;
    }
    
    public Expression translate(java.util.Map m) {
        Expression e = null;

        for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
            Variable var = (Variable) iter.next();

            if (var.getName().equals(this.var)) {
                e = (Expression) m.get(var);
            }
        }
        if (e == null) {
            e = this;
        }
        return e;
    }
    
    public boolean equals(Object o) {
        if (o instanceof Param) {
            Param p = (Param) o;

            // Log.debug(this.toString()+"=?="+p.toString()+" -> "+(var.equals(p.var)));
            return (var.equals(p.var));
        }
        // Log.debug(this.toString()+"=?=object["+o.toString()+"] -> false");
        return false;
    }
    
    public Data eval(DataContext c) {
        return c.getTranslation(this);
    }

    public String toString() {
        return ("PARAM[" + super.toString() + "]");
    }
}

