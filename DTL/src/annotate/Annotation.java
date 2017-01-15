package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.expression.Literal;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.net.URL;
import java.util.*;

import nu.xom.*;


/**
 * In a DTL specification notions, scopes, etc. can be annotated with a label,
 * a description, notes and links to concepts or other notions. The Annotation
 * class collects those annotations. As the annotations can be scattered in the
 * DTL specification, the class also guards against overwritting existing
 * annotations.
 **/
public class Annotation {

    public Expression    label = null;
    public Expression    description = null;
    public URL           datcat = null;
    public java.util.Map concepts = null;
    public java.util.Map notions = null;
    public List          notes = null;

    public boolean merge(Annotation a) {
        if (a.label != null) {
            if (label != null) {
                if (!label.equals(a.label)) {
                    Log.error(Annotation.class, "label is already defined, and existing label["+label+"] != new label["+a.label+"]");
                    return false;
                }
            } else {
                label = a.label;
            }
        }
        if (a.description != null) {
            if (description != null) {
                if (description != description) {
                    Log.error(Annotation.class, "description is already defined, and existing description["+description+"] != new label["+a.description+"]");
                    return false;
                }
            } else {
                description = a.description;
            }
        }
        if (a.concepts != null) {
            if (concepts != null) {
                for (Iterator iter = a.concepts.keySet().iterator(); iter.hasNext();) {
                    String concept = (String) (iter.next());

                    if (concepts.containsKey(concept)) {
                        Link l = (Link) concepts.get(concept);

                        if (!l.merge((Link) a.concepts.get(concept))) {
                            Log.error(Annotation.class,
                                    "concept[" + concept
                                    + "] has already been linked, and the two links couldn't be merged");
                        }
                    } else {
                        concepts.put(concept, a.concepts.get(concept));
                    }
                }
            } else {
                concepts = a.concepts;
            }
        }
        if (a.notions != null) {
            if (notions != null) {
                for (Iterator iter = a.notions.keySet().iterator(); iter.hasNext();) {
                    String n = (String) (iter.next());

                    if (notions.containsKey(n)) {
                        Link l = (Link) notions.get(n);

                        if (!l.merge((NotionLink) a.notions.get(n))) {
                            Log.error(Annotation.class,
                                    "notion[" + n
                                    + "] has already been linked, and the two links couldn't be merged");
                        }
                    } else {
                        notions.put(n, a.notions.get(n));
                    }
                }
            } else {
                notions = a.notions;
            }
        }
        if (a.datcat != null) {
            if (datcat != null) {
                if (!datcat.equals(a.datcat)) {
                    Log.error(Annotation.class, "datcat is already defined, and existing datcat["+datcat+"] != new datcat["+a.datcat+"]");
                    return false;
                }
            } else {
                datcat = a.datcat;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return ((label == null) && (description == null)
                && ((concepts == null) || (concepts.size() == 0))
                && ((notions == null) || (notions.size() == 0))
                && ((notes == null) || (notes.size() == 0)));
    }
    
    public boolean isDynamic() {
        if ((label != null) && !(label instanceof Literal)) {
            return true;
        }
        if ((description != null) && !(description instanceof Literal)) {
            return true;
        }
        return false;
    }
    
    public Annotation translate(java.util.Map translate) {
        Annotation ann = new Annotation();

        if (label != null) {
            ann.label = label.clone().translate(translate);
        }
        if (description != null) {
            ann.description = description.clone().translate(translate);
        }
        if (concepts != null) {
            ann.concepts = new HashMap();
            ann.concepts.putAll(concepts);
        }
        if (notions != null) {
            ann.notions = new HashMap();
            ann.notions.putAll(notions);
        }
        return ann;
    }
    
    public Annotation eval(DataContext c) {
        Annotation ann = new Annotation();

        if (label != null) {
            if (label instanceof Literal) {
                ann.label = label;
            } else {
                Data l = label.eval(c);

                if (l == null) {
                    Log.error(Annotation.class,
                            "Couldn't evaluate label expression: " + label);
                    return null;
                } else if (l.getValue() != null) {
                    ann.label = new Literal(l.getValue());
                }
            }
        }
        if (description != null) {
            if (description instanceof Literal) {
                ann.description = description;
            } else {
                Data d = description.eval(c);

                if (d == null) {
                    Log.error(Annotation.class,
                            "Couldn't evaluate description expression: "
                            + description);
                    return null;
                } else if (d.getValue() != null) {
                   	ann.description = new Literal(d.getValue());
                }
            }
        }
        if (concepts != null) {
            ann.concepts = new HashMap();
            ann.concepts.putAll(concepts);
        }
        if (notions != null) {
            ann.notions = new HashMap();
            ann.notions.putAll(notions);
        }
        return ann;
    }

    public Element toXML(Element e) {
        if (label != null) {
            if (label instanceof Literal) {
                Element l = ((Literal) label).toXML("label");

                if (l != null) {
                    e.appendChild(l);
                }
            } else {
                Log.warning(Annotation.class,
                        "Request to serialize a non-literal label");
            }
        }
        if (description != null) {
            if (description instanceof Literal) {
                Element d = ((Literal) description).toXML("description");

                if (d != null) {
                    e.appendChild(d);
                }
            } else {
                Log.warning(Annotation.class,
                        "Request to serialize a non-literal description");
            }
        }
        if (notes != null) {
            for (Iterator iter = notes.iterator(); iter.hasNext();) {
                Note n = (Note) iter.next();
                Element note = n.toXML();

                if (note != null) {
                    e.appendChild(note);
                }
            }
        }
        if (concepts != null) {
            for (Iterator iter = concepts.values().iterator(); iter.hasNext();) {
                Link l = (Link) iter.next();
                Element link = l.toXML();

                if (link != null) {
                    e.appendChild(link);
                }
            }
        }
        if (notions != null) {
            for (Iterator iter = notions.values().iterator(); iter.hasNext();) {
                Link l = (Link) iter.next();
                Element link = l.toXML();

                if (link != null) {
                    e.appendChild(link);
                }
            }
        }
        if (datcat != null) {
            Element dc = new Element("datcat");

            dc.appendChild("" + datcat);
            e.appendChild(dc);
        }
        return e;
    }

    public void add(Note n) {
        if (n != null) {
            if (notes == null) {
                notes = new Vector();
            }
            notes.add(n);
        }
    }

    public void debug(String indent, String incr) {
        if (concepts != null) {
            for (Iterator iter = concepts.values().iterator(); iter.hasNext();) {
                Link l = (Link) iter.next();

                l.debug(indent, incr);
            }
        }
        if (notions != null) {
            for (Iterator iter = notions.values().iterator(); iter.hasNext();) {
                Link l = (Link) iter.next();

                l.debug(indent, incr);
            }
        }
        if (label != null) {
            Log.debug(Annotation.class, indent + "- label: " + label);
        }
        if (description != null) {
            Log.debug(Annotation.class, indent + "- description: " + description);
        }
        if (notes != null) {
            for (Iterator iter = notes.iterator(); iter.hasNext();) {
                Note n = (Note) iter.next();

                n.debug(indent, incr);
            }
        }
    }

    public void debug() {
        debug("", "  ");
    }
    
    public String toString() {
        String res = "ANNOTATION[";

        if (isEmpty()) {
            res += "<empty>";
        } else {
            if (label != null) {
                res += "label[" + label + "]";
            }
            if (description != null) {
                res += "description[" + description + "]";
            }
            if (notes != null) {
                res += "notes[" + notes.size() + "]";
            }
            if (concepts != null) {
                res += "concepts[" + concepts.size() + "]";
            }
            if (notions != null) {
                res += "notions[" + notions.size() + "]";
            }
        }
        return (res += "]");
    }
}
