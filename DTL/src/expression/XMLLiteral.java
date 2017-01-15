package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.annotate.XMLDescription;
import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A piece of literal XML.
 **/
public class XMLLiteral extends Literal {
    
    public XMLLiteral(Document doc) {
        super(new XMLDescription(doc));
    }

    public void add(Document doc) {
        ((XMLDescription) value).add(doc);
    }
    
    public Element toXML(String name) {
        XMLDescription xml = (XMLDescription) getValue();

        return xml.toXML(name);
    }
  
}
