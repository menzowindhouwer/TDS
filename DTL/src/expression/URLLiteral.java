package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.annotate.URLDescription;
import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.net.*;

import nu.xom.*;


/**
 * A literal URL
 **/
public class URLLiteral extends Literal {
    
    public URLLiteral(URL url) {
        super(new URLDescription(url));
    }

    public Element toXML(String name) {
        URLDescription url = (URLDescription) getValue();
        return url.toXML(name);
    }

}
