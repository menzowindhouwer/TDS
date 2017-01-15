package nl.uu.let.languagelink.tds.dtl.expression;


import java.io.StringReader;

import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import org.ccil.cowan.tagsoup.Parser;
import org.ccil.cowan.tagsoup.XMLWriter;


/**
 * Tidy up HTML code embedded in the String representation of the value
 * and return it as an XML fragment.
 **/
public class Tidy extends Value {
    
    protected Expression in = null;
  
    public Tidy(Expression in) {
        this.in = in;
    }
    
    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (in != null) {
            e = in.eval(c);
        } 
        if (e == null) {
            Log.error(Tidy.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.getValue() != null) {
            String HTML = e.getValue().toString().trim();

            HTML = "<tidy>" + HTML + "</tidy>";
            try {
                Log.debug(Tidy.class, "TIDY:BEGIN:[" + HTML + "]");
                XMLReader tagsoup = new Parser();

                tagsoup.setFeature(Parser.defaultAttributesFeature, false);
                
                Document doc = new Builder(tagsoup).build(new StringReader(HTML));
                
                Log.debug(Tidy.class, "TIDY:     :[" + doc.toXML() + "]");
                
                Element root = doc.getRootElement();

                try {
                    // strip of <html><body>...</body></html> envelop
                    Nodes l = nux.xom.xquery.XQueryUtil.xquery(root,
                            "./html/body/*");

                    if (l.size() > 0) {
                        root.removeChildren();
                        for (int i = 0; i < l.size(); i++) {
                            Node n = l.get(i);

                            n.detach();
                            root.appendChild(n);
                        }
                        Log.debug(Tidy.class, "TIDY:     :[" + doc.toXML() + "]");
                    }
                } catch (Exception ex) {
                    Log.error(Tidy.class, "couldn't execute XPath expression",
                            ex);
                }
                
                XMLLiteral xml = new XMLLiteral(doc);
                
                Log.debug(Tidy.class, "TIDY: END :[" + xml + "]");
                
                res = new Data(xml);
                res.addMarkedSource(e);
                res.setNote("TIDY(" + e + ")");
                
            } catch (Exception ex) {
                Log.error(Tidy.class,
                        "" + this + " failed to tidy HTML[" + HTML + "]:" + ex,
                        ex);
            }
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            res.setNote("TIDY(" + e + ") resulted in a NULL");
        }
        return res;
    }
    
    public boolean parameterized() {
        return (in != null && in.parameterized());
    }

    public Expression translate(java.util.Map m) {
        if (in != null) {
            in = in.translate(m);
        }
        return this;
    }

    public Tidy clone() {
        Tidy t = (Tidy) super.clone();

        if (this.in != null) {
            t.in = this.in.clone();
        }
        return t;
    }

    public String toString() {
        return ("TIDY(" + in + ")");
    }
}
