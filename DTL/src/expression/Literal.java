package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.annotate.Description;
import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A literal (marked) value.
 **/
public class Literal extends Value {

    protected Marker mark = null;

    public Literal(Object l) {
        super(l);
    }
  
    public void setMarker(Marker mark) {
        this.mark = mark;
    }
  
    public Marker getMarker() {
        return this.mark;
    }
  
    public Data eval(DataContext c) {
        Data res = new Data(value);
        
        res.addMarker(mark);
        res.setNote("literal");
        
        return res;
    }
  
    public Element toXML(String name) {
        Element n = null;

		if (value != null) {
			if  (value instanceof Description) {
				n = ((Description)value).toXML(name);
			} else if  (value instanceof Literal) {
				n = ((Literal)value).toXML(name);
			} else {
            	n = new Element(name);
            	n.appendChild(value.toString());
			}
            if (mark != null) {
                n.addAttribute(new Attribute("mark", "" + mark));
            }
        }

        return n;
    }
  
    public Literal clone() {
        Literal l = (Literal) super.clone();

        if (mark != null) {
            l.mark = (Marker) this.mark.clone();
        }
        return l;
    }
}
