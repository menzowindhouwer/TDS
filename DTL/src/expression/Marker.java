package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.*;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * A literal Marker
 **/
public class Marker extends Value {

    public Marker(String name) {
        super(name);
    }
  
    public Data eval(DataContext c) {
        Data res = new DataMarker(this);

        res.setNote("literal marker");
        return res;
    }
  
    public boolean equals(Object o) {
        return false;
    }

    public boolean equals(Marker o) {
        return o.value.equals(this.value);
    }
  
    public String toString() {
        return ("MARKER[" + this.value + "]");
    }
}
