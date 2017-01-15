package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.notion.Notion;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A reference to a notion, i.e. its value.
 **/
public class NotionVariable extends Variable {
  
    protected Notion notion = null;

    public NotionVariable(Notion n) {
        super("" + n.getID());
        notion = n;
    }
  
    public Notion getNotion() {
        return notion;
    }
  
    public Data eval(DataContext c) {
        Log.warning(NotionVariable.class,
                "not implemented yet, should use the Notion ID to find the correct instantiation(s)");
        return null;
    }
  
    public Element toXML() {
        return toXML("notion");
    }

    public String toString() {
        return ("VARIABLE[" + notion + "]");
    }
}

