package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.annotate.*;
import nl.uu.let.languagelink.tds.dtl.map.BaseMap;
import nl.uu.let.languagelink.tds.dtl.map.Map;
import nl.uu.let.languagelink.tds.dtl.notion.*;
import nl.uu.let.languagelink.tds.dtl.Engine;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;
import java.net.URL;

import nu.xom.*;


/**
 * A scope in a DTL specification.
 **/
public class Scope extends Context {

    protected Engine            engine = null;

    protected String			name = null;
    protected String			type = null;
    
    protected Scope             scope_parent = null;
    protected List              scope_children = new Vector();

    /* in- and output info */
    protected String			namespace = null;
    protected java.util.Map     accesses = new HashMap();
    protected java.util.Map     accessors = new HashMap();

    /* the meta data */
    protected String			abbreviation = null;
    protected String			fullname = null;
    protected Description		description = null;
    protected Description       notes = null;
    protected URL				website = null;
    protected List				researchers = new Vector();
    protected List				contacts = new Vector();
    protected String            datatype = null;
    
    /* maps */
    protected java.util.Map		maps = new HashMap();

    /* queries */
    protected java.util.Map		queries = new HashMap();

    /* declared notions */
    protected List				declaredNotions = new Vector();
    protected java.util.Map		declarationRoots = new HashMap();

    /* instantiated notions */
    protected java.util.Map	    instantiationRoots = new HashMap();

    /* queries */
    protected Stack             query_stack = new Stack();
    
    /* transformation roots */
    protected List              roots = new Vector();

    /* Main constructor */

    public Scope(Engine engine, Scope parent, String name) {
        this.engine = engine;
        this.scope_parent = parent;
        this.name = name;
        if (scope_parent != null) {
            scope_parent.addScope(this);
        }
    }
    
    public Engine getEngine() {
        return engine;
    }
    
    public String getName() {
        return name;
    }
    
    public Scope getScope() {
        return scope_parent;
    }
    
    public void addScope(Scope s) {
        if (!scope_children.contains(s)) {
            scope_children.add(s);
        }
    }
    
    /* is this scope enabled or disabled */
    
    public boolean enabled() {
        return engine.isEnabledScope(name);
    }

    public boolean disabled() {
        return engine.isDisabledScope(name);
    }
    
    /* is this scope valid or not */
    public boolean valid() {
        return engine.inScope(this);
    }
    
    public boolean invalid() {
        return (!valid());
    }

    /* what's the type of this scope? */

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /* manage in- and output info */

    public boolean setNamespace(String ns) {
        if (namespace != null) {
            return false;
        }
        namespace = ns;
        return true;
    }
    
    public String getNamespace() {
        if (namespace != null) {
            return namespace;
        }
        return ("http://languagelink.let.uu.nl/tds/ns/TDS/D-" + getName() + "-x");
    }

    public boolean setAccess(String id,Document access) {
        if (accessors.containsKey(id))
          return false;
        Accessor accessor = getEngine().getAccessor(this,access);
        if (accessor==null)
            return false;
        accesses.put(id,access);
        accessors.put(id,accessor);
        return true;
    }
    
    public Document getAccess(String id) {
        return (Document)accesses.get(id);
    }
    
    public boolean hasAccessor(String id) {
        return accessors.containsKey(id);
    }
    
    public Accessor getAccessor(String id) {
        return (Accessor)accessors.get(id);
    }

    public Document getAccess() {
        return (Document)accesses.get(getName());
    }

    public boolean hasAccessor() {
        return accessors.containsKey(getName());
    }
    
    public Accessor getAccessor() {
        return (Accessor)accessors.get(getName());
    }

    /* manage meta data */
    public boolean setAbbreviation(String abbreviation) {
        if (this.abbreviation != null) {
            return false;
        }
        this.abbreviation = abbreviation;
        return true;
    }

    public boolean setFullname(String fullname) {
        if (this.fullname != null) {
            return false;
        }
        this.fullname = fullname;
        return true;
    }

    public boolean setDescription(Description descr) {
        if (description != null) {
            return false;
        }
        description = descr;
        return true;
    }

    public boolean setNotes(Description notes) {
        if (this.notes != null) {
            return false;
        }
        this.notes = notes;
        return true;
    }
    
    public boolean setWebsite(URL site) {
        if (website != null) {
            return false;
        }
        website = site;
        return true;
    }

    public boolean addResearcher(String researcher) {
        researchers.add(researcher);
        return true;
    }

    public boolean addContact(String contact) {
        contacts.add(contact);
        return true;
    }
    
    /* manage default data type */
    
    public boolean setDefaultDataType(String t) {
        boolean res = true;

        if (!hasDefaultDataType()) {
            datatype = t;
        } else {
            res = false;
        }
        return res;
    }
    
    public boolean hasDefaultDataType() {
        return (datatype != null);
    }
    
    public String getDefaultDataType() {
        return datatype;
    }

    /* manage maps */

    public BaseMap addMap(String name, List params) {
        if (name == null) {
            return null;
        }
        BaseMap m = (BaseMap) maps.get(name);

        if (m == null) {
            m = new BaseMap(this, name, params);
            maps.put(name, m);
        } else {
            m = null;
        }
        return m;
    }

    public BaseMap getMap(String name) {
        return (BaseMap) maps.get(name);
    }
    
    public BaseMap replaceMap(String name, BaseMap m) {
        return (BaseMap) maps.put(name, m);
    }

    /* manage queries */
    public Query addQuery(String name,String access) {
        if (name == null) {
            return null;
        }
        
        if (access==null)
            access = getName();
        
        if (!hasAccessor(access)) {
            Log.error(Scope.class,"couldn't create query["+name+"]: "+this+" doesn't have data accessor["+access+"]");
            return null;
        }

        Query q = (Query) queries.get(name);

        if (q == null) {
            q = new Query(name,access);
            queries.put(name, q);
        } else {
            q = null;
        }
        return q;
    }

    public Query getQuery(String name) {
        return (Query) queries.get(name);
    }
    
    /* manage declared notions */

    public DeclaredNotion declareNotion(String name) {
        DeclaredNotion n = new DeclaredNotion(this, name);

        declaredNotions.add(n);
        return n;
    }

    public DeclaredNotion getDeclarationRoot(String name) {
        return (DeclaredNotion) declarationRoots.get(name);
    }

    public boolean addDeclarationRoot(DeclaredNotion n) {
        if (n.getScope() != this) {
            return false;
        }
        if (getDeclarationRoot(n.getName()) != null) {
            return false;
        }
        if (n.getGroup() != null) {
            return false;
        }
        declarationRoots.put(n.getName(), n);
        return true;
    }

    public DeclaredNotion addDeclarationRoot(String name) {
        DeclaredNotion n = getDeclarationRoot(name);

        if (n == null) {
            n = declareNotion(name);
            declarationRoots.put(name, n);
        }
        // System.err.println("!MENZO:"+this+".addDeclarationRoot("+name+"):"+(n!=null?n:"<null>"));
        return n;
    }

    /* manage instatiated notions */

    public InstantiatedNotion getInstantiationRoot(String name) {
        return (InstantiatedNotion) instantiationRoots.get(name);
    }

    public boolean addInstantiationRoot(InstantiatedNotion n) {
        if (n.getScope() != this) {
            Log.error(Scope.class, "" + n + " isn't from this scope");
            return false;
        }
        if (!n.isTop()) {
            Log.error(Scope.class, "" + n + " isn't a top notion");
            return false;
        }
        InstantiatedNotion r = getInstantiationRoot(n.getName());

        if (r != null) {
            if (r != n) {
                Log.error(Scope.class,
                        "there is already a different instantiation root with the same name: "
                        + r + "!=" + n);
            }
            return true;
        }
        instantiationRoots.put(n.getName(), n);
        return true;
    }
    
    public InstantiatedNotion addInstantiationRoot(DeclaredNotion d) {
        InstantiatedNotion n = null;

        if (d != null) {
            n = getInstantiationRoot(d.getName());
            if ((n == null) && (d.isTop())) {
                addInstantiationRoot(new DeclaredInstantiatedNotion(d));
            }
            n = getInstantiationRoot(d.getName());
        }
        return n;
    }
    
    public InstantiatedNotion addInstantiationRoot(ID id) {
        InstantiatedNotion n = id.scope.getInstantiationRoot(id.name);

        if (n == null) {
            DeclaredNotion d = id.scope.getDeclarationRoot(id.name);

            if (d != null) {
                addInstantiationRoot(d);
            } else {
                n = new InstantiatedNotion((id.hasScope() ? id.scope : this),
                        id.name);
                n.setType(Notion.TOP);
                addInstantiationRoot(n);
            }
            n = id.scope.getInstantiationRoot(id.name);
        }
        return n;
    }
    
    public InstantiatedNotion addInstantiationRoot(ID id, DeclaredNotion sup) {
  	InstantiatedNotion n = addInstantiationRoot(id);
    	if (n != null) {
    		n.setSuper(sup);
    		n.setType(sup.getType());
    	}
        return n;
    }

    /* handle query stack */
    public void pushQuery(Foreach q) {
        if (currentQuery() != null) {
            currentQuery().addMember(q);
        } else {
            roots.add(q);
        }
        query_stack.push(q);
    }
    
    public void popQuery() {
        if (!query_stack.empty()) {
            query_stack.pop();
        } else {
            Log.warning(Scope.class,
                    "request for query popping ignored: the query stack is empty");
        }
    }
    
    public Foreach currentQuery() {
        if (!query_stack.empty()) {
            return (Foreach) query_stack.peek();
        }
        return null;
    }

    /* manage transformation roots */
    public LocalizedNotion addLocalizationRoot(LocalizedNotion l) {
        if (currentQuery() != null) {
            currentQuery().addMember(l);
        } else {
            roots.add(l);
        }
        return l;
    }

    public LocalizedNotion addLocalizationRoot(InstantiatedNotion i) {
        return addLocalizationRoot(new LocalizedNotion(this, i));
    }
    
    /* find a notion */
    public DeclaredNotion findRootNotion(ID id) {
        DeclaredNotion n = getDeclarationRoot(id.name);

        if ((n != null) && n.isRoot()) {
            return n;
        }
        return null;
    }

    public Notion findNotion(ID id) {
        for (Iterator iter = instantiationRoots.values().iterator(); iter.hasNext();) {
            Notion n = ((Notion) iter.next()).findNotion(id);

            if (n != null) {
                return n;
            }
        }
        return null;
    }

    /* Generate the meta documentation */

    public void getMetaXML(Element meta) {
        Element sc = new Element("scope");

        try {
            sc.addAttribute(
                    new Attribute("xml:id",
                    "http://www.w3.org/XML/1998/namespace", name));
            if (type != null) {
                sc.addAttribute(new Attribute("type", type));
            }
            if (abbreviation != null) {
                Element abrv = new Element("abbreviation");

                abrv.appendChild(abbreviation);
                sc.appendChild(abrv);
            }
            if (fullname != null) {
                Element fn = new Element("name");

                fn.appendChild(fullname);
                sc.appendChild(fn);
            }
            if (description != null) {
                Element descr = description.toXML("description");

                if (descr != null) {
                    sc.appendChild(descr);
                }
            }
            if (notes != null) {
                Element n = notes.toXML("notes");

                if (n != null) {
                    sc.appendChild(n);
                }
            }
            if (website != null) {
                Element site = new Element("website");

                site.appendChild("" + website);
                sc.appendChild(site);
            }
            for (Iterator iter = researchers.iterator(); iter.hasNext();) {
                Element researcher = new Element("researcher");

                researcher.appendChild("" + iter.next());
                sc.appendChild(researcher);
            }
            for (Iterator iter = contacts.iterator(); iter.hasNext();) {
                Element contact = new Element("contact");

                contact.appendChild("" + iter.next());
                sc.appendChild(contact);
            }
            if (namespace != null) {
                Element ns = new Element("namespace");

                ns.appendChild("" + namespace);
                sc.appendChild(ns);
            }
            if ((type != null) && type.equals("warehouse")) {
                for (Iterator iter = engine.datatypes.keySet().iterator(); iter.hasNext();) {
                    String t = (String) iter.next();
                    String b = (String) engine.datatypes.get(t);
                    Element dt = new Element("datatype");

                    dt.addAttribute(new Attribute("id", t));
                    if (b != null) {
                        dt.addAttribute(new Attribute("base", b));
                    }
                    sc.appendChild(dt);
                }
                for (Iterator iter = engine.markers.keySet().iterator(); iter.hasNext();) {
                    String     m = (String) iter.next();
                    Annotation a = (Annotation) engine.markers.get(m);
                    Element marker = new Element("marker");

                    marker.addAttribute(new Attribute("id", m));
                    if (a != null) {
                        a.toXML(marker);
                    }
                    sc.appendChild(marker);
                }
                for (Iterator iter = engine.linkingTypes.keySet().iterator(); iter.hasNext();) {
                    String     l = (String) iter.next();
                    Annotation a = (Annotation) engine.markers.get(l);
                    Element link = new Element("linktype");

                    link.addAttribute(new Attribute("id", l));
                    if (a != null) {
                        a.toXML(link);
                    }
                    sc.appendChild(link);
                }
            }
            for (Iterator iter = scope_children.iterator(); iter.hasNext();) {
                Element child = new Element("scope");

                child.addAttribute(
                        new Attribute("ref", ((Scope) iter.next()).getName()));
                sc.appendChild(child);
            }
        } catch (Exception exp) {
            Log.error(Scope.class,
                    "couldn't create XML meta data for scope[" + name + "]", exp);
        }
        if (sc.getChildCount() > 0) {
            meta.appendChild(sc);
        }
    }

    public void getDictXML(Element dict) {
        if (!instantiationRoots.isEmpty()) {
            dict.appendChild(new Comment("list roots for " + this));
            for (Iterator iter = instantiationRoots.values().iterator(); iter.hasNext();) {
                Element notion = ((Notion) iter.next()).toXML();

                if (notion != null) {
                    dict.appendChild(notion);
                }
            }
        }
        if (!declarationRoots.isEmpty()) {
            dict.appendChild(new Comment("list abstract roots for " + this));
            for (Iterator iter = declarationRoots.values().iterator(); iter.hasNext();) {
                Notion n = (Notion) iter.next();

                if (n.isAbstract()) {
                    Element notion = n.toXML();

                    if (notion != null) {
                        dict.appendChild(notion);
                    }
                }
            }
        }
    }
    
    /* Generate the configuration documentation */

    public void getConfXML(Element root) {
        Element sc = new Element("scope");

        try {
            sc.addAttribute(
                    new Attribute("xml:id",
                    "http://www.w3.org/XML/1998/namespace", name));
            if (hasAccessor()) {
                Element a = new Element("access");

                a.appendChild(getAccess().getRootElement().copy());
                sc.appendChild(a);
            }
        } catch (Exception exp) {
            Log.error(Scope.class,
                    "couldn't create XML configuration data for scope[" + name
                    + "]",
                    exp);
        }
        if (sc.getChildCount() > 0) {
            root.appendChild(sc);
        }
    }
    
    /* import the actual data from the database */
    public Document importData(DataContext c) {
        Document doc = null;
        if (getAccessor().hasDefaultQuery()) {
            doc = getAccessor().execDefaultQuery();
        }
        return doc;
    }
    
    /* execute the actual transformation */
    public boolean build(DataContext dc) {
        boolean res = false;

        Log.push("scope", this);
        if (hasAccessor() && (roots != null) && (roots.size() > 0)) {
            if (getAccessor().hasDefaultQuery()) {
                Log.push("default query", this);
                DataContext c = createDataContext(dc);
                if (c.hasData()) {
                    Log.debug(Scope.class, "transform data for " + this);
                    while (c.loadDataRow()) {
                        Log.debug(Scope.class,
                                "transform row[" + c.getCurrentRow() + "] for "
                                + this);
                        Log.push("row=" + c.getCurrentRow() + "/" + c.getRowCount(), null);
                        boolean build = false;
    
                        for (Iterator iter = roots.iterator(); iter.hasNext();) {
                            Object root = iter.next();
    
                            if (root instanceof LocalizedNotion) {
                                if (((LocalizedNotion) root).build(c, c.getRoot(),
                                        false)) {
                                    build = true;
                                }
                            } else if (root instanceof Foreach) {
                                if (((Foreach) root).build(c, c.getRoot(), false)
                                        > 0) {
                                    build = true;
                                }
                            }
                        }
                        if (!build) {
                            Log.warning(Scope.class,
                                    "row[" + c.getCurrentRow() + "] of " + this
                                    + " didn't succesfully transform");
                            c.dumpDataRow();
                        } else {
                            res = true;
                        }
                        Log.debug(Scope.class,
                                "transformed row[" + c.getCurrentRow() + "] for "
                                + this);
                        Log.pop();
                    }
                    Log.debug(Scope.class,
                            "transformed data from the default query for " + this);
                    readDataContext(c);
                    Log.pop();
                }
            } else if ((roots != null) && (roots.size() > 0)) {
                DataContext c = createDataContext(dc);
                for (Iterator iter = roots.iterator(); iter.hasNext();) {
                    Object root = iter.next();

                    if (root instanceof LocalizedNotion) {
                        if (((LocalizedNotion) root).build(c, c.getRoot(), false)) {
                            res = true;
                        }
                    } else if (root instanceof Foreach) {
                        if (((Foreach) root).build(c, c.getRoot(), false) > 0) {
                            res = true;
                        }
                    }
                }
            } else {
                // there are no nested notions, so this is a non-data scope
                res = true;
            }
        }
        if (!res && (roots != null) && (roots.size() > 0)) {
            Log.error(Scope.class, "there were no notions created for " + this);
        }
        Log.pop();
        return res;
    }
  
    public void report() {
        Log.push("context", this);
        untouchedFields();
        for (Iterator iter = queries.values().iterator(); iter.hasNext();) {
            Query q = (Query) iter.next();

            Log.push("context", q);
            q.untouchedFields();
            Log.pop();
        }
        Log.pop();
    }
    
    /* Complete semantics */
    public boolean complete() {
        for (Iterator iter = roots.iterator(); iter.hasNext();) {
            Object root = iter.next();

            if (root instanceof LocalizedNotion) {
                if (!((LocalizedNotion) root).complete()) {
                    Log.error(Scope.class,
                            "" + this + ": couldn't complete semantics for "
                            + root);
                    return false;
                }
            } else if (root instanceof Foreach) {
                if (!((Foreach) root).complete()) {
                    Log.error(Scope.class,
                            "" + this + ": couldn't complete semantics for "
                            + root);
                    return false;
                }
            }
        }
        return true;
    }
    
    /* Semantic check */
    public boolean check() {
        for (Iterator iter = roots.iterator(); iter.hasNext();) {
            Object root = iter.next();

            if (root instanceof LocalizedNotion) {
                if (!((LocalizedNotion) root).check()) {
                    Log.error(Scope.class,
                            "" + this + ": the localized root is inconsistent: "
                            + root);
                    return false;
                }
            } else if (root instanceof Foreach) {
                if (!((Foreach) root).check()) {
                    Log.error(Scope.class,
                            "" + this + ": the foreach is inconsistent: " + root);
                    return false;
                }
            }
        }
        return true;
    }
    
    /* Debug info */

    public void debug(String indent, String incr) {
        Log.debug(Scope.class, indent + "SCOPE: " + name);
        if (type != null) {
            Log.debug(Scope.class, indent + "- type       : " + type);
        }
        if (namespace != null) {
            Log.debug(Scope.class, indent + "- namespace  : " + namespace);
        }
        if (hasAccessor()) {
            Log.debug(Scope.class, indent + "- access     : <xml/>");
        }
        if (fullname != null) {
            Log.debug(Scope.class, indent + "- name       : " + fullname);
        }
        if (description != null) {
            Log.debug(Scope.class, indent + "- description: ");
            description.debug();
        }
        if (notes != null) {
            Log.debug(Scope.class, indent + "- notes      : ");
            notes.debug();
        }
        if (website != null) {
            Log.debug(Scope.class, indent + "- website    : " + website);
        }
        for (Iterator iter = researchers.iterator(); iter.hasNext();) {
            Log.debug(Scope.class, indent + "- researcher : " + iter.next());
        }
        for (Iterator iter = contacts.iterator(); iter.hasNext();) {
            Log.debug(Scope.class, indent + "- contact    : " + iter.next());
        }
        for (Iterator iter = maps.values().iterator(); iter.hasNext();) {
            ((Map) iter.next()).debug(indent + incr, incr);
        }
        for (Iterator iter = declarationRoots.values().iterator(); iter.hasNext();) {
            ((Notion) iter.next()).debug(indent + incr, incr);
        }
        for (Iterator iter = instantiationRoots.values().iterator(); iter.hasNext();) {
            ((Notion) iter.next()).debug(indent + incr, incr);
        }
    }

    public void debug() {
        debug("", "  ");
    }

    public String toString() {
        return ("" + (type != null ? type + " " : "") + "SCOPE[" + name + "]");
    }
}
