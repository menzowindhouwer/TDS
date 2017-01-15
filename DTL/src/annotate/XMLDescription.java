package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.expression.Value;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;

import nu.xom.*;


/**
 * A description in the form of a piece XML, i.e. XHTML with some specific TDS elements.
 **/
public class XMLDescription extends Description {

    protected List docs = null;

    public XMLDescription(Document doc) {
        docs = new Vector();
        docs.add(doc);
    }

    public void add(Document doc) {
        if (doc != null) {
            docs.add(doc);
        }
    }
    
    public Element toXML(String name) {
        Element descr = null;

        descr = new Element(name);
        for (Iterator iter = docs.iterator(); iter.hasNext();) {
            Document item = (Document) iter.next();

            descr.appendChild(item.getRootElement().copy());
        }
        return descr;
    }

    public String toString() {
        String res = "";

        if (!docs.isEmpty()) {
            for (Iterator iter = docs.iterator(); iter.hasNext();) {
                res += ((Document) iter.next()).getValue();
            }
        }
        return res;
    }
    
    public boolean equals(Object v) {
	return (docs == null ? v == null : v.toString().equals(toString()));
    }

    public int hashCode() {
	if (docs!=null)
            return toString().hashCode();
        return super.hashCode();
    }

    public void debug(String indent) {
        if (!docs.isEmpty()) {
            Log.debug(XMLDescription.class, indent + this);
        }
    }
}
