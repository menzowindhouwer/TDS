package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nl.uu.let.languagelink.tds.ipa.Conversion;


/**
 * The Convert operation takes the String representation of a value and
 * asks the IPA converter to apply a specific font transformation map
 * to it (possibly with a value drop).
 **/
public class Convert extends Value {
  
    protected Expression map = null;
    protected Expression val = null;
    protected Expression drp = null;
    
    static Conversion conversion = new Conversion(false);

    public Convert(Expression map, Expression drp, Expression val) {
        this.map = map;
        this.drp = drp;
        this.val = val;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data m = map.eval(c);

        if (m == null) {
            Log.error(Conversion.class,
                    "" + this + " conversion map name couldn't be evaluated");
            return null;
        }
        Data v = c.getFocus();

        if (val != null) {
            v = val.eval(c);
        } 
        if (v == null) {
            Log.error(Conversion.class,
                    "" + this + " value expression couldn't be evaluated");
            return null;
        }
        boolean drop = true;

        if (drp != null) {
            Data d = drp.eval(c);

            if (d != null) {
                if (d.getValue() instanceof Boolean) {
                    drop = ((Boolean) d.getValue()).booleanValue();
                }
            } else {
                Log.error(Conversion.class,
                        "" + this + " drop expression couldn't be evaluated");
            }
        }
        if (v.getValue() != null) {
            String converted = conversion.convert(m.getValue().toString(),
                    v.getValue().toString(), drop);

            if (converted != null) {
                res = new Data(converted);
                res.addMarkedSource(v);
                res.setNote("" + this + " succeeded");
            }
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(v);
            res.setNote("" + this + " failed: no data to convert");
        }
        return res;
    }
    
    public boolean parameterized() {
        return ((map != null && map.parameterized())
                || (val != null && val.parameterized()));
    }

    public Expression translate(java.util.Map m) {
        map = map.translate(m);
        val = val.translate(m);
        return this;
    }

    public Convert clone() {
        Convert c = (Convert) super.clone();

        c.map = this.map.clone();
        c.val = this.val.clone();
        return c;
    }

    public String toString() {
        return ("CONVERT(" + map + "," + drp + "," + val + ")");
    }
}
