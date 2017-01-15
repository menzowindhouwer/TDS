package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * The And Expression executes a boolean AND operation on its left and right
 * operands.
 **/
public class And extends Expression {

    protected Expression left = null;
    protected Expression right = null;

    public And(Expression left, Expression right) {
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
                    				Log.warning(And.class,
                         			   "AND:" + this
                           			 + ":right argument isn't a boolean:" + r);
								}
							} else {
                				Log.warning(And.class,
                        			"AND:" + this
                        			+ ":right argument is a NULL value:" + r);
							}
						} else {
            				Log.error(And.class,
                 			   "AND:" + this + ":right argument couldn't be evaluated:" + right);
						}
					} else {
                    	res = new Data(new Boolean(false));
					}
				} else {
                  	Log.warning(And.class,
                   	   "AND:" + this
                   		+ ":left argument isn't a boolean:" + l);
				}
			} else {
              		Log.warning(And.class,
               			"AND:" + this
               			+ ":left argument is a NULL value:" + l);
			}
		} else {
          	Log.error(And.class,
           	   "AND:" + this + ":left argument couldn't be evaluated:" + left);
		}

        if (res != null) {
			if (l!=null)
            	res.addMarkedSource(l);
			if (r!=null)
            	res.addMarkedSource(r);
            res.setNote("" + (l!=null?l:"null") + " AND " + (r!=null?r:"null"));
        } else {
            Log.error(And.class, "" + this + " expression couldn't be evaluated");
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
  
    public Element toXML() {
        Element a = new Element("and");
        Element l = left.toXML();

        if (l != null) {
            a.appendChild(l);
        }
        Element r = right.toXML();

        if (r != null) {
            a.appendChild(r);
        }
        return a;
    }

    public And clone() {
        And e = (And) super.clone();

        e.left = this.left.clone();
        e.right = this.right.clone();
        return e;
    }

    public String toString() {
        return ("(" + left + " AND " + right + ")");
    }
}
