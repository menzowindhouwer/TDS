package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * The Not Expression executes a boolean NOT operation on its boolean operand.
 **/
public class Not extends Expression {
  
    protected Expression expr = null;

    public Not(Expression expr) {
        this.expr = expr;
    }
  
    public Data eval(DataContext c) {
        Data res = null;
        Data e = expr.eval(c);

        if ((e != null) && (e.getValue() != null)
                && (e.getValue() instanceof Boolean)) {
            res = new Data(new Boolean(!((Boolean) e.getValue()).booleanValue()));
        }
        if (res != null) {
            res.addMarkedSource(e);
            res.setNote("!" + e);
        } else {
            Log.error(Not.class, "" + this + " expression couldn't be evaluated");
        }
        return res;
    }
  
    public boolean parameterized() {
        return (expr != null && expr.parameterized());
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        return this;
    }

    public Not clone() {
        Not n = (Not) super.clone();

        n.expr = this.expr.clone();
        return n;
    }

    public Element toXML() {
        Element n = new Element("not");
        Element e = expr.toXML();

        if (e != null) {
            n.appendChild(e);
        }
        return n;
    }
  
    public String toString() {
        return ("NOT(" + expr + ")");
    }
}
