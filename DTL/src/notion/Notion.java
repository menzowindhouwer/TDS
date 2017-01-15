package nl.uu.let.languagelink.tds.dtl.notion;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.ID;
import nl.uu.let.languagelink.tds.dtl.context.Scope;
import nl.uu.let.languagelink.tds.dtl.expression.Literal;
import nl.uu.let.languagelink.tds.dtl.expression.Value;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The abstract notion class implements all the basic notion functionality, e.g. nesting.
 **/
public abstract class Notion {

    static final public int ANNOTATION = -1;
    static final public int COMPLETE = 1;
    static final public int TOP = 2;
    static final public int ROOT = 3;

    static protected int       ids = 0;

    protected String           ntp = null; // the notion class type

    protected int			   type = 0;
    protected boolean          optional = false;
    protected boolean          abstr = false;

    protected int              id = 0;
    protected Scope			   scope = null;
    protected String           name = null;

    protected Annotation       ann = null;

    protected String           datatype = null;
    protected java.util.Map    values = new HashMap();
    protected java.util.Map    keys = new HashMap();

    protected Notion           sup = null;
    
    protected Notion           group = null;
    protected List             members = new Vector();
    
    protected Set              scopes = new HashSet(); // the scopes in which this notion is used

    protected java.util.Map    hints = new HashMap();
    
    public Notion(Scope scope, String name) {
        this.id = ++ids;
        this.scope = scope;
        this.name = name;
    }

    public int base() {
        return id();
    }

    public int id() {
        return id;
    }

    public Scope getScope() {
        return scope;
    }

    public String getName() {
        return name;
    }

    public ID getID() {
        return (new ID(getScope(), getName()));
    }
    
    public int setType(int t) {
        int old = getType();

        type = t;
        return old;
    }
    
    public int getType() {
        return type;
    }

    public boolean isRoot() {
        return (getType() >= ROOT);
    }

    public boolean isTop() {
        return (getType() >= TOP);
    }

    public boolean isComplete() {
        return (getType() >= COMPLETE);
    }
    
    public boolean isAnnotation() {
        return (getType() <= ANNOTATION);
    }

    public boolean setOptional(boolean o) {
        boolean old = optional;

        optional = o;
        return old;
    }

    public boolean isOptional() {
        return optional;
    }
    
    public boolean isAbstract() {
        return abstr;
    }

    public boolean checkProperties(int tpe, boolean opt) {
        if (getType() != tpe) {
            return false;
        }
        if (!isOptional()) { // if a node is declared optional, you can refer to it both as optional or not
            if (opt) { // if a node isn't declared otional, you can never refer to it as optional
                return false;
            }
        }
        return true;
    }

    public boolean addAnnotation(Annotation a) {
        if (!a.isEmpty()) {
            if (ann == null) {
                ann = a;
                return true;
            } else { 
                return ann.merge(a);
            }
        }
        return false;
    }

    public Annotation getAnnotation() {
        if ((ann != null) && !ann.isEmpty()) {
            return ann;
        }
        return null;
    }
    
    public boolean setDataType(String t) {
        if (datatype == null) {
            datatype = t;
        }
        return (datatype == t);
    }
    
    public boolean hasDataType() {
        return (datatype != null);
    }
    
    public String getDataType() {
        return datatype;
    }
    
    public boolean getEnum() {
        // enumerate by default
        boolean res = true;

        if (hasDataType()) {
            if (!getScope().getEngine().getRootDataType(getDataType()).equals(
                    "ENUM")) {
                res = false;
            }
        }
        return res;
    }
    
    public boolean addValue(Value v, Annotation a) {
        if (v instanceof Literal) {
            if (v.getValue() != null) {
                if (a == null) {
                    a = new Annotation();
                } else if (a.isDynamic()) {
                    Log.error(Notion.class,
                            "only literal annotations can be used for the meta data, "
                            + a + " is dynamic");
                    return false;
                }
                String s = v.getValue().toString();

                if (values.containsKey(s)) {
                    Annotation va = (Annotation) values.get(s);

                    if (!va.merge(a)) {
                        Log.error(Notion.class,
                                "couldn't merge annotations for " + v + " of "
                                + this);
                        return false;
                    }
                } else {
                    values.put(s, a);
                    Log.debug(Notion.class,"notion["+this+"] added new value["+s+"]["+v+"]");
                }
            } else {
                // Log.warning(Notion.class,"only non-null literals can become annotated values of "+this+", "+v+" is null");
                return true; // we silently ignore nulls
            }
        } else {
            Log.error(Notion.class,
                    "only literals can become annotated values of " + this
                    + ", " + v + " isn't a literal");
            return false;
        }
        return true;
    }

    public boolean hasValue(Value v) {
        if ((v != null) && (v.getValue() != null)) {
            return (values.containsKey(v.getValue().toString()));
        }
        return false;
    }
    
    public boolean hasValues() {
        return !values.isEmpty();
    }
    
    public java.util.Map getValues() {
        return values;
    }

    public boolean addKey(Value k, Annotation a) {
        if (k instanceof Literal) {
            if (k.getValue() != null) {
                if (a == null) {
                    a = new Annotation();
                } else if (a.isDynamic()) {
                    Log.error(Notion.class,
                            "only literal annotations can be used for the meta data, "
                            + a + " is dynamic");
                    return false;
                }
                String s = k.getValue().toString();

                if (keys.containsKey(s)) {
                    Annotation ka = (Annotation) keys.get(s);

                    if (!ka.merge(a)) {
                        Log.error(Notion.class,
                                "couldn't merge annotations for " + k + " of "
                                + this);
                        return false;
                    }
                } else {
                    keys.put(s, a);
                    Log.debug(Notion.class,"notion["+this+"] added new key value["+s+"]["+k+"]");
                }
            } else {
                return true; // we silently ignore nulls
            }
        } else {
            Log.error(Notion.class,
                    "only literals can become annotated keys of " + this + ", "
                    + k + " isn't a literal");
            return false;
        }
        return true;
    }
    
    public boolean hasKeys() {
        return !keys.isEmpty();
    }
    
    public boolean hasKey(Value k) {
        if ((k != null) && (k.getValue() != null)) {
            return (keys.containsKey(k.getValue().toString()));
        }
        return false;
    }
    
    public java.util.Map getKeys() {
        return keys;
    }
    
    public boolean setSuper(Notion sp) {
        if (sup != null) {
            return false;
        }
        sup = sp;
        return true;
    }
    
    public Notion getSuper() {
        return sup;
    }
    
    public boolean hasSuper() {
        return (getSuper() != null);
    }

    public Notion setGroup(Notion g) {
        if (group == g) {
            return g;
        }
        Notion old = null;

        if (!isTop()) {
            old = group;
            group = g;
            if (old != null) {
                old.deleteMember(this);
                Log.warning(Notion.class,
                        "" + this + " is now member of " + group
                        + " instead of " + old);
            }
        } else {
            Log.debug(Notion.class,
                    "" + this
                    + " is a top notion, so we don't keep track of the groups it appears in");
        }
        return old;
    }

    public Notion getGroup() {
        return group;
    }

    protected boolean addMember(Notion n) {
        Log.debug(Notion.class,
                "Notion(" + this + ").addMember(Notion=" + n + ")");
        if (!hasMember(n.getID())) {
            members.add(n);
            n.setGroup(this);
        } else { 
            Log.debug(Notion.class,
                    "" + n + " is already a member of the group " + this
                    + ", so its addition is skipped");
        }
        return true;
    }

    public boolean deleteMember(Notion n) {
        if (members.remove(n)) {
            return true;
        }
        Log.error(Notion.class,
                "tried to remove " + n + " as member from " + this
                + ", but it wasn't a member at all");
        return false;
    }

    public Notion getMember(ID id) {
        Notion res = null;

        Log.debug(Notion.class, "Notion(" + this + ").getMember(ID=" + id + ")");
        if (sup != null) {
            res = sup.getMember(id);
        }
        if (res == null) {
            for (Iterator iter = members.iterator(); iter.hasNext();) {
                Notion n = (Notion) iter.next();

                if (id.hasScope()) {
                    if (n.getScope() != id.scope) {
                        continue;
                    }
                }
                if (n.getName().equals(id.name)) {
                    Log.debug(Notion.class,
                            "Notion(" + this + ").getMember(ID=" + id
                            + "):Notion(" + n
                            + ") has same scope and name, so a match");
                    res = n;
                    break;
                }
            }
        }
        if (res != null) {
            Log.debug(Notion.class,
                    "Notion(" + this + ").getMember(ID=" + id
                    + "):was not found");
        }
        return res;
    }

    public boolean hasMember(ID id) {
        return (getMember(id) != null);
    }

    public boolean hasMembers() {
        return !members.isEmpty();
    }
    
    public List getMembers() {
        return members;
    }
    
    public List getAllMembers() {
        return getAllMembers(new HashSet(), "");
    }
    
    public List getAllMembers(Set optionals, String ind) {
        Log.debug(Notion.class, ind + "BEGIN:" + this + ".getAllMembers()");
        optionals.add(this);
        List res = new Vector();

        if (sup != null) {
            res = sup.getAllMembers();
        }
        for (Iterator members = getMembers().iterator(); members.hasNext();) {
            Notion member = (Notion) members.next();
            List cand = new Vector();

            cand.add(member);
            if (member.isOptional()) {
                if (!optionals.contains(member)) {
                    Log.debug(Notion.class,
                            ind + "  " + member
                            + " is OPTIONAL so hoist all its members");
                    List desc = member.getAllMembers(optionals, ind + "  ");

                    for (Iterator descendants = desc.iterator(); descendants.hasNext();) {
                        cand.add(descendants.next());
                    }
                } else {
                    Log.debug(Notion.class,
                            ind + "  " + member
                            + " is OPTIONAL, but already on the stack");
                }
            }
            int added = 0;

            for (Iterator candidates = cand.iterator(); candidates.hasNext();) {
                Notion candidate = (Notion) candidates.next();
                boolean add = true;

                for (Iterator results = res.iterator(); results.hasNext();) {
                    Notion result = (Notion) results.next();

                    if (result.getScope() == candidate.getScope()) {
                        if (result.getName().equals(candidate.getName())) {
                            add = false;
                        }
                    }
                }
                if (add) {
                    res.add(candidate);
                    added++;
                }
            }
            if (member.isOptional() && !optionals.contains(member)) {
                Log.debug(Notion.class,
                        ind + "  " + member + " is OPTIONAL so hoisted "
                        + (added - 1) + " of its members");
            }
        }
        Log.debug(Notion.class,
                ind + " END :" + this + ".getAllMembers().size[" + res.size()
                + "]");
        return res;
    }

    public Notion findNotion(ID id) {
        if (getName().equals(id.name) && (getScope() == id.scope)) {
            return this;
        }
        for (Iterator iter = getMembers().iterator(); iter.hasNext();) {
            Notion n = (Notion) iter.next();
	    if (!n.isTop())
	    	n = n.findNotion(id);
	    else
		n = null;

            if (n != null) {
                return n;
            }
        }
        return null;
    }
    
    public void addScope(Scope s) {
        scopes.add(s);
    }

    public void addHint(String h, Literal l) {
        hints.put(h, l);
    }

    public Element toXML() {
        return toXML(true);
    }
    
    public Element toXML(boolean main) {
        Element notion = new Element("notion");

        if (id() != base()) {
            notion.addAttribute(new Attribute("base", "" + base()));
        }
        notion.addAttribute(new Attribute("scope", getScope().getName()));
        notion.addAttribute(new Attribute("name", getName()));
        if (isRoot()) {
            notion.addAttribute(new Attribute("type", "root"));
        } else if (isTop()) {
            notion.addAttribute(new Attribute("type", "top"));
        } else if (isComplete()) {
            notion.addAttribute(new Attribute("type", "complete"));
        } else if (isAnnotation()) {
            notion.addAttribute(new Attribute("type", "annotation"));
        }
        if (isOptional()) {
            notion.addAttribute(new Attribute("optional", "true"));
        } else {
            notion.addAttribute(new Attribute("optional", "false"));
        }
        if (isAbstract()) {
            notion.addAttribute(new Attribute("abstract", "true"));
        } else {
            notion.addAttribute(new Attribute("abstract", "false"));
        }
        if (hasDataType()) {
            notion.addAttribute(new Attribute("datatype", getDataType()));
        }
        if (getEnum()) {
            notion.addAttribute(new Attribute("enum", "true"));
        } else {
            notion.addAttribute(new Attribute("enum", "false"));
        }
        if (hasSuper()) {
            notion.addAttribute(new Attribute("super", "" + getSuper().id()));
        }
        if (scopes.size() > 0) {
            String s = "";

            for (Iterator iter = scopes.iterator(); iter.hasNext();) {
                s += ((Scope) iter.next()).getName();
                if (iter.hasNext()) {
                    s += " ";
                }
            }
            notion.addAttribute(new Attribute("scopes", s));
        }
        if (isTop() && !main) {
            notion.addAttribute(new Attribute("ref", "" + id()));
        } else {
            notion.addAttribute(new Attribute("id", "" + id()));
            if (getAnnotation() != null) {
                getAnnotation().toXML(notion);
            }
            for (Iterator iter = hints.keySet().iterator(); iter.hasNext();) {
                String h = (String) iter.next();
                Literal l = (Literal) hints.get(h);
                Element hint = new Element("hint");

                hint.addAttribute(new Attribute("name", h));
                hint.addAttribute(
                        new Attribute("type",
                        getScope().getEngine().getHintType(h)));
                hint.addAttribute(
                        new Attribute("value", l.getValue().toString()));
                notion.appendChild(hint);
            }
            if (hasValues()) {
                int i = 0;

                for (Iterator iter = getValues().keySet().iterator(); iter.hasNext();) {
                    Object v = iter.next();
                    Annotation a = (Annotation) getValues().get(v);
                    Element value = Value.toXML("value", v);

                    if ((value != null) && (a != null)) {
                        value.addAttribute(new Attribute("id", "" + (++i)));
                        a.toXML(value);
                        notion.appendChild(value);
                    }
                }
            }
            if (hasKeys()) {
                int i = 0;

                for (Iterator iter = getKeys().keySet().iterator(); iter.hasNext();) {
                    Object k = iter.next();
                    Annotation a = (Annotation) getKeys().get(k);
                    Element key = Value.toXML("key", k);

                    if ((key != null) && (a != null)) {
                        key.addAttribute(new Attribute("id", "" + (++i)));
                        a.toXML(key);
                        notion.appendChild(key);
                    }
                }
            }
            if (hasMembers()) {
                for (Iterator iter = getAllMembers().iterator(); iter.hasNext();) {
                    Notion n = (Notion) iter.next();

                    if (n.getScope().enabled()) {
                        Element member = n.toXML(false);

                        if (member != null) {
                            notion.appendChild(member);
                        }
                    }
                }
            }
        }
        return notion;
    }

    public void debug(String indent, String incr) {
        Log.debug(Notion.class,
                indent + (isOptional() ? "OPTIONAL " : "")
                + (isAnnotation() ? "ANNOTATION " : "")
                + (isRoot() ? "ROOT " : (isTop() ? "TOP " : "")) + this);
        if (group != null) {
            Log.debug(Notion.class, indent + "- member of: " + group.getID());
        }
        if (ann != null) {
            ann.debug(indent, incr);
        }
        if (values != null) {
            Log.debug(Notion.class, indent + "- values:");
            for (Iterator iter = values.keySet().iterator(); iter.hasNext();) {
                Value v = (Value) iter.next();
                Annotation a = (Annotation) values.get(v);

                v.debug(indent + incr, incr);
                a.debug(indent + incr + incr, incr);
            }
        }
        if (hasMembers()) {
            Log.debug(Notion.class, indent + "- members:");
            for (Iterator iter = getMembers().iterator(); iter.hasNext();) {
                Notion n = (Notion) iter.next();

                n.debug(indent + incr, incr);
            }
        }
    }

    public void debug() {
        debug("", "  ");
    }

    public String toString() {
        String res = "";

        if (Log.debug()) {
            if (isOptional()) {
                res += "OPTIONAL ";
            }
            if (isRoot()) {
                res += "ROOT ";
            } else if (isTop()) {
                res += "TOP ";
            } else if (isComplete()) {
                res += "COMPLETE ";
            } else if (isAnnotation()) {
                res += "ANNOTATION ";
            }
            if (ntp != null) {
                res += ntp + " ";
            }
            res += "NOTION[" + getID() + "," + id() + "]";
        } else {
            res += getID();
        }
        return res;
    }
}
