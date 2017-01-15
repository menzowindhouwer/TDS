package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.map.Map;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * Lookup expression allows to lookup a value in a specific map. Which value
 * to lookup in which map is embedded in the associated UseMap.
 **/
public class Lookup extends Value {

    protected Map map = null;

    public Lookup(Map map) {
        this.map = map;
    }
    
    public Data eval(DataContext c) {
        return map.lookup(c);
    }
    
    public boolean parameterized() {
        return map.parameterized();
    }

    public Expression translate(java.util.Map translate) {
        List p = new Vector();

        for (int i = 0; i < map.getSize(); i++) {
            Param param = map.getParam(i);
            Value val = (Value) translate.get(param);

            if (val == null) {
                Log.error(Lookup.class,
                        "translate map doesn't cover " + param
                        + " of the lookup " + map);
                return null;
            }
            p.add(val);
        }
        map = map.useMap(map.getScope(), p);
        return this;
    }

    public Lookup clone() {
        Lookup l = (Lookup) super.clone();

        return l;
    }

    public String toString() {
        return ("LOOKUP(" + map + ")");
    }
}
