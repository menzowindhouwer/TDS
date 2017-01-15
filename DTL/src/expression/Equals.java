package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.*;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * The Equals Expression checks equality of its left and right operands.
 **/
public class Equals extends Expression {
  
    protected Expression left = null;
    protected Expression right = null;

    public Equals(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }
  
    public Data eval(DataContext c) {
        Data res = null;
        Data l = left.eval(c);
        Data r = right.eval(c);

        if ((l != null) && (r != null)) {
            if ((l instanceof DataMarker) || (r instanceof DataMarker)) {
                if (l.hasMarkers(r.getMarkers())) {
                    res = new Data(new Boolean(true)); // <mark> == <mark>
                } else {
                	res = new Data(new Boolean(false)); // <null> != <mark> OR <mark> != <null> OR <mark> != <mark>
		}
            } else if ((l.getValue() == null) && (r.getValue() == null)) {
                res = new Data(new Boolean(true)); // <null> == <null>
            } else if ((l.getValue() == null) || (r.getValue() == null)) {
                res = new Data(new Boolean(false)); // <null> != <right> OR <left> != <null>
            } else if (l.getValue().toString().equals(r.getValue().toString())) { // comparison on the string level to iron out different types
                res = new Data(new Boolean(true));
            } else {
                res = new Data(new Boolean(false));
            }
        }
        if (res != null) {
            res.addMarkedSource(l);
            res.addMarkedSource(r);
            res.setNote("" + l + " = " + r);
        } else {
            Log.error(Equals.class,
                    "" + this + " expression couldn't be evaluated");
        }
        return res;
    }
  
    public boolean parameterized() {
        return ((left != null && left.parameterized())
                || (right != null && right.parameterized()));
    }

    public Expression translate(java.util.Map m) {
        left = left.translate(m);
        right = right.translate(m);
        return this;
    }

    public Equals clone() {
        Equals e = (Equals) super.clone();

        e.left = this.left.clone();
        e.right = this.right.clone();
        return e;
    }
  
    public Element toXML() {
        Element e = new Element("equals");
        Element l = left.toXML();

        if (l != null) {
            e.appendChild(l);
        }
        Element r = right.toXML();

        if (r != null) {
            e.appendChild(r);
        }
        return e;
    }

    public String toString() {
        return ("(" + left + "=" + right + ")");
    }
}
