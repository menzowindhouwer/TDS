package nl.uu.let.languagelink.tds.dtl.data;


import nl.uu.let.languagelink.tds.dtl.expression.Marker;
import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * Specific Data subclass to indicate that this is about the Marker, not the value.
 **/
public class DataMarker extends Data {
    public DataMarker(Marker mark) {
        super(null);
        addMarker(mark);
    }
}
