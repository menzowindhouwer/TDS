package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * Abstract description class
 **/
public abstract class Description {

    public Element toXML(String name) {
        return null;
    }

    public void debug(String indent) {}

    public void debug() {
        debug("");
    }
}
