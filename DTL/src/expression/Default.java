package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * The Default operation allows to provide an alternative for a NULL value.
 **/
public class Default extends Value {
  
    protected Expression expr = null;
    protected Expression def = null;

    public Default(Expression e, Expression d) {
        this.expr = e;
        this.def = d;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Lower.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() == null) {
            Data d = c.getFocus();

            if (def != null) {
                d = def.eval(c);
            } 
            if (d == null) {
                Log.error(Lower.class,
                        "" + this + " default expression couldn't be evaluated");
                return null;
            }
            res = new Data(d.getValue());
            res.addSource(e);
            res.addAnnotatedMarkedSource(d);
            res.setNote("DEFAULT(" + e + "," + d + ")");
        } else {
            res = e;
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.setNote("DEFAULT(" + expr + "," + def + ") resulted in a NULL");
        }
        return res;
    }
    
    public boolean parameterized() {
        return ((expr != null && expr.parameterized())
                && ((def != null && def.parameterized())));
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        def = def.translate(m);
        return this;
    }

    public Default clone() {
        Default d = (Default) super.clone();

        d.expr = this.expr.clone();
        d.def = this.def.clone();
        return d;
    }

    public String toString() {
        return ("DEFAULT(" + expr + "," + def + ")");
    }
}
