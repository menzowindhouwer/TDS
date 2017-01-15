package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A link to a concept. These links can be refined by a linking type, and
 * the amount of resemblance. Linking types can be added within a DTL spec,
 * resemblance (overlapping, strict, etc.) are predefined in the DTL grammar.
 **/
public class ConceptLink extends Link {

    protected String type = null;
    protected String resemblance = null;
    protected String concept = null;

    public ConceptLink(String t, String r, String c) {
        type = t;
        resemblance = r;
        concept = c;
    }

    public boolean merge(Link l) {
        if (l instanceof ConceptLink) {
            ConceptLink cl = (ConceptLink) l;

            if (!concept.equals(cl.concept)) {
                Log.error(ConceptLink.class,
                        "can't merge links to two different concepts: concept["
                        + concept + "] and concept[" + cl.concept + "]");
                return false;
            }
            if (type != null) {
                if (cl.type != null) {
                    if (!type.equals(cl.type)) {
                        Log.error(ConceptLink.class,
                                "can't merge two links to concept[" + concept
                                + "]: one has link type[" + type
                                + "] while and the other has link type["
                                + cl.type + "]");
                        return false;
                    }
                }
            } else {
                type = cl.type;
            }
            if (resemblance != null) {
                if (cl.resemblance != null) {
                    if (!resemblance.equals(cl.resemblance)) {
                        Log.error(ConceptLink.class,
                                "can't merge two links to concept[" + concept
                                + "]: one has resemblance type[" + resemblance
                                + "] while and the other has resemblance     type["
                                + cl.resemblance + "]");
                        return false;
                    }
                }
            } else {
                resemblance = cl.resemblance;
            }
            return true;
        }
        return false;
    }

    public Element toXML() {
        Element link = new Element("link");

        link.addAttribute(new Attribute("concept", concept));
        if (type != null) {
            link.addAttribute(new Attribute("type", type));
        }
        if (resemblance != null) {
            link.addAttribute(new Attribute("resemblance", resemblance));
        }
        return link;
    }
    
    public void debug(String indent, String incr) {
        Log.debug(ConceptLink.class, indent + "- LINK TO CONCEPT: " + concept);
        if (type != null) {
            Log.debug(ConceptLink.class, indent + incr + "- type: " + type);
        }
        if (resemblance != null) {
            Log.debug(ConceptLink.class,
                    indent + incr + "- resemblance: " + resemblance);
        }
    }

    public void debug() {
        debug("", "  ");
    }
}
