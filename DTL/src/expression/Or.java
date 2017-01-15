package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * The Or Expression executes a boolean OR operation on its left and right
 * operands.
 **/
public class Or extends Expression {

    protected Expression left = null;
    protected Expression right = null;

    public Or(Expression left, Expression right) {
        this.left = left;
        this.right = right;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data l = left.eval(c);
       	Data r = null;

		if (l!=null) {
			if (l.getValue()!=null) {
				if (l.getValue() instanceof Boolean) {
					if (((Boolean)l.getValue()).booleanValue()) {
                    	res = new Data(new Boolean(true));
					} else {
        				r = right.eval(c);
						if (r!=null) {
							if (r.getValue()!=null) {
								if (r.getValue() instanceof Boolean) {
									if (((Boolean)r.getValue()).booleanValue()) {
                    					res = new Data(new Boolean(true));
									} else {
                    					res = new Data(new Boolean(false));
									}
								} else {
                    				Log.warning(Or.class,
                         			   "OR:" + this
                           			 + ":right argument isn't a boolean:" + r);
								}
							} else {
                				Log.warning(Or.class,
                        			"OR:" + this
                        			+ ":right argument is a NULL value:" + r);
							}
						} else {
            				Log.error(Or.class,
                 			   "OR:" + this + ":right argument couldn't be evaluated:" + right);
						}
					}
				} else {
                  	Log.warning(Or.class,
                   	   "OR:" + this
                   		+ ":left argument isn't a boolean:" + l);
				}
			} else {
              		Log.warning(Or.class,
               			"OR:" + this
               			+ ":left argument is a NULL value:" + l);
			}
		} else {
          	Log.error(Or.class,
           	   "OR:" + this + ":left argument couldn't be evaluated:" + left);
		}

        if (res != null) {
			if (l!=null)
            	res.addMarkedSource(l);
			if (r!=null)
            	res.addMarkedSource(r);
            res.setNote("" + (l!=null?l:"null") + " OR " + (r!=null?r:"null"));
        } else {
            Log.error(Or.class, "" + this + " expression couldn't be evaluated");
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

    public Or clone() {
        Or e = (Or) super.clone();

        e.left = this.left.clone();
        e.right = this.right.clone();
        return e;
    }
  
    public Element toXML() {
        Element o = new Element("or");
        Element l = left.toXML();

        if (l != null) {
            o.appendChild(l);
        }
        Element r = right.toXML();

        if (r != null) {
            o.appendChild(r);
        }
        return o;
    }
  
    public String toString() {
        return ("(" + left + " OR " + right + ")");
    }
}
