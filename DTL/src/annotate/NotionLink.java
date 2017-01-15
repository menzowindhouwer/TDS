package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.notion.Notion;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A link to a notion.
 **/
public class NotionLink extends Link {

    protected Notion notion = null;

    public NotionLink(Notion n) {
        notion = n;
    }

    public boolean merge(NotionLink l) {
        if (!notion.equals(l.notion)) {
            Log.error(NotionLink.class,
                    "can't merge links to two different notions: " + notion
                    + " and " + l.notion);
            return false;
        }
        return true;
    }

    public Element toXML() {
        Element link = new Element("link");

        link.addAttribute(new Attribute("notion", "" + notion.getID()));
        return link;
    }
    
    public void debug(String indent, String incr) {
        Log.debug(NotionLink.class, indent + "- LINK TO NOTION: " + notion);
    }

    public void debug() {
        debug("", "  ");
    }
}
