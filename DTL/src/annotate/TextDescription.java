package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A description which is just a string.
 **/
public class TextDescription extends Description {

    protected String text = null;

    public TextDescription(String text) {
        this.text = text;
    }

    public Element toXML(String name) {
        Element descr = new Element(name);

        descr.appendChild(text);
        return descr;
    }
    
    public String toString() {
        return "" + text;
    }

    public void debug(String indent) {
        Log.debug(TextDescription.class, indent + text);
    }
}
