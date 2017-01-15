package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import java.net.URL;

import nu.xom.*;


/**
 * A description using a URL to point to an external description.
 **/
public class URLDescription extends Description {

    protected URL url = null;

    public URLDescription(URL url) {
        this.url = url;
    }

    public Element toXML(String name) {
        Element descr = new Element(name);

        descr.addAttribute(new Attribute("href", "" + url));
        return descr;
    }
    
    public String toString() {
        return "" + url;
    }

    public void debug(String indent) {
        Log.debug(URLDescription.class, indent + url);
    }
}
