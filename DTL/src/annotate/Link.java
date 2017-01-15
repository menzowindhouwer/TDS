package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A link to something.
 **/
public abstract class Link {

    public boolean merge(Link l) {
        return false;
    }

    public Element toXML() {
        return null;
    }

    public void debug(String indent, String incr) {
        Log.debug(Link.class, indent + "- EMPTY LINK");
    }

    public void debug() {
        debug("", "  ");
    }
}
