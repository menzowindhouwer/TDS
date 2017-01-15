package nl.uu.let.languagelink.tds.dtl;


import nl.uu.let.languagelink.tds.dtl.annotate.*;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.event.*;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.map.*;
import nl.uu.let.languagelink.tds.dtl.notion.*;

import java.io.*;
import java.net.*;
import java.util.*;

import antlr.CommonAST;
import antlr.collections.AST;
import antlr.debug.misc.ASTFrame;
import antlr.TokenStream;
import antlr.TokenStreamSelector;

import org.apache.commons.cli.*;

import nu.xom.*;


/**
 * The Engine class is the main public DTL accessor. It allows parsing and checking
 * of a DTL spec. And also executes the actual transform leading to an XML document
 * containing the integrated (meta) data.
 **/
public class Engine {

    // the input names the engine
    protected String              name = null;

    protected DTLLexer            lexer = null;
    protected DTLParser           parser = null;
    protected TokenStreamSelector selector = null;

    protected boolean             checkOntology = true;
    protected Ontology            ontology = null;
   
    protected java.util.Map       scopes = new HashMap();
    protected Stack               in_scope = new Stack();

    protected Stack               queries = new Stack();
    protected Stack               contexts = new Stack();
   
    public java.util.Map          markers = new HashMap();
    public java.util.Map          linkingTypes = new HashMap();
    protected java.util.Map       hints = new HashMap();
    public java.util.Map          datatypes = new HashMap();
    
    protected java.util.Map       accessors = new HashMap();

    protected Variable            focus = null;

    protected BaseMap             map = null;

    protected Stack               declarationPath = new Stack();
    protected Stack               localizationPath = new Stack();
    
    protected StatisticsListener  validListener = new StatisticsListener();

    protected boolean             valid = true;
    protected boolean             debug = false;

    protected Resolver            resolver = new DefaultResolver();
    protected Includer            includer = null;
    
    protected Set                 enabled_scopes = new HashSet();
    protected Set                 disabled_scopes = new HashSet();
    
    protected Set                 excludes = new HashSet();

	protected String              stamp = ""+new Date();

    /* Main constructor */

    public Engine(Reader r, String name) {
        // name the input of the engine
        this.name = name;
        if (name == null) {
            name = "<stdin>";
        }
        
        // default listener signals validity
        Log.addListener(validListener);
        
        // default types
        datatypes.put("FREE", null);
        datatypes.put("ENUM", null);
        
        // default accessors
        accessors.put("odbc", new nl.uu.let.languagelink.tds.dtl.context.DTLimport());
        accessors.put("odbtp",new nl.uu.let.languagelink.tds.dtl.context.DTLimport());
        accessors.put("xml",  new nl.uu.let.languagelink.tds.dtl.context.XMLimport());
        accessors.put("csv",  new nl.uu.let.languagelink.tds.dtl.context.CSVimport());
        accessors.put("jdbc", new nl.uu.let.languagelink.tds.dtl.context.JDBCimport());

        // try to open the input
        try {
            lexer = new DTLLexer(r, this);
            lexer.setFilename(name);

            selector = new TokenStreamSelector();
            selector.addInputStream(lexer, name);
            selector.push(name);

            parser = new DTLParser(selector, this);
            parser.setFilename(name);
        } catch (Exception e) {
            Log.error(Engine.class, e);
        }
    }

    public Engine(String name) throws FileNotFoundException, UnsupportedEncodingException {
        this(
                new BufferedReader(
                        new InputStreamReader(new FileInputStream(name),"UTF-8")),
                        name);
    }

    /* parse the DTL and returns its validity */
    public boolean parse() {
        validListener.reset();
        try {
            try {
                parser.dtlspec();
                complete();
            } catch (Exception e) {
                Log.error(Engine.class, e);
            }
        } catch (FatalException e) {// all listeners have been notified, including the validListener
        }
        close();
        return ((validListener.error() == 0) && (validListener.fatal() == 0));
    }

    /* Handling files/URLs */
    public void setResolver(Resolver r) {
        resolver = r;
    }

    public void setIncluder(Includer i) {
        includer = i;
    }

    Reader resolve(String rel) {
        return resolve(name, rel);
    }

    Reader resolve(String base, String rel) {
        if (resolver == null) {
            resolver = new DefaultResolver();
        }
        return resolver.resolve(base, rel);
    }

    /* Close all files */
    void close() {
        TokenStream ts = selector.getCurrentStream();

        while (ts != null) {
            if (ts instanceof DTLLexer) {
                try {
                    ((DTLLexer) ts).reader.close(); // close the reader
                } catch (IOException e) {
                    Log.error(Engine.class, "Couldn't close a stream: " + e);
                }
            }
            ts = selector.pop();
        }
    }

    /* Manage markers */
    void addMarker(String m, Annotation a) {
        markers.put(m, a);
    }

    boolean isMarker(String m) {
        return markers.containsKey(m);
    }

    /* Manage linking types */
    void addLinkingType(String lt, Annotation a) {
        linkingTypes.put(lt, a);
    }

    boolean isLinkingType(String lt) {
        return linkingTypes.containsKey(lt);
    }

    /* Manage hints */
    void addHint(String h, String dt) {
        hints.put(h, dt);
    }

    boolean isHint(String h) {
        return hints.containsKey(h);
    }

    public String getHintType(String h) {
        return (String) hints.get(h);
    }

    boolean allowedHintValue(String h, Literal l) {
        boolean result = false;
        String dt = (String) hints.get(h);

        if ((dt != null) && (l != null)) {
            Object o = l.getValue();

            if (dt.equals("str") && o instanceof String) {
                result = true;
            } else if (dt.equals("int") && o instanceof Integer) {
                result = true;
            } else if (dt.equals("flt")
                    && (o instanceof Integer || o instanceof Float)) {
                result = true;
            } else if (dt.equals("url") && o instanceof URL) {
                result = true;
            } else if (dt.equals("bit") && o instanceof Boolean) {
                result = true;
            }
        }
        return result;
    }

    /* Manage data types */
    boolean addDataType(String nt, String bt) {
        boolean res = false;
        String t = nt.toUpperCase();
        String b = bt.toUpperCase();

        if (hasDataType(b) && !hasDataType(t)) {
            datatypes.put(t, b);
            res = true;
        }
        return res;
    }
    
    boolean hasDataType(String t) {
        return datatypes.containsKey(t.toUpperCase());
    }
    
    String getBaseDataType(String t) {
        return (String) datatypes.get(t.toUpperCase());
    }
    
    public String getRootDataType(String t) {
        String b = (String) datatypes.get(t.toUpperCase());

        if (b != null) {
            return getRootDataType(b);
        }
        return t;
    }
    
    /* Manage accessors */
    public boolean hasAccessor(String type) {
        return accessors.containsKey(type);
    }
    
    public Accessor getAccessor(Scope scope,Document access) {
        String type = access.getRootElement().getLocalName();
        Accessor accessor = null;
        if ((type!=null) && accessors.containsKey(type)) {
            accessor = ((Accessor)accessors.get(type)).newInstance(scope,access);
        } else
          Log.error(Engine.class,"accessor["+type+"] isn't known");
        return accessor;
    }

    /* Manage the ontology */
    public boolean checkOntology() {
        return checkOntology;
    }
    
    public void checkOntology(boolean checkOntology) {
        this.checkOntology = checkOntology;
    }

    void loadOntology(String location) {
        if (checkOntology()) {
            Reader onto = resolve(location);

            if (onto != null) {
                ontology = new Ontology(onto, location);
            } else {
                Log.error(Engine.class, "couldn't open ontology: " + location);
            }
        }
    }

    boolean isConcept(String concept) {
        if (!checkOntology()) {
            return true;
        }
        
        if (ontology != null) {
            return ontology.isConcept(concept);
        }
        return false;
    }

    /* Manage scopes */
    Scope newScope(String scope) {
        Scope s = (Scope) scopes.get(scope);

        if (s == null) {
            s = new Scope(this, getCurrentScope(), scope);
            scopes.put(scope, s);
        }
        return s;
    }

    boolean pushScope(Scope s) {
        if (s == null || in_scope.contains(s)) {
            return false;
        }
        in_scope.push(s);
        pushContext(s);
        return true;
    }

    void popScope() {
        if (!in_scope.empty()) {
            in_scope.pop();
        } else {
            Log.warning(Engine.class,
                    "request for scope popping ignored: the scope stack is empty");
        }
        popContext();
    }

    Scope getWarehouse() {
        Scope wh = null;

        for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
            Scope s = (Scope) iter.next();
            String t = s.getType();

            if ((t != null) && t.equals("warehouse")) {
                wh = s;
                break;
            }
        }
        return wh;
    }

    Scope getScope(String name) {
        Scope s = (Scope) scopes.get(name);

        return s;
    }

    Scope getCurrentScope() {
        if (!in_scope.empty()) {
            return (Scope) in_scope.peek();
        }
        return null;
    }
    
    public boolean inScope(Scope s) {
        return in_scope.contains(s);
    }

    /* manage queries */
    Query addQuery(String name,String access) {
        Query query = getCurrentScope().addQuery(name,access);

        return query;
    }

    Query getQuery(ID id) {
        if (!id.hasScope()) {
            id.scope = getCurrentScope();
        }
        return id.scope.getQuery(id.name);
    }

    boolean pushQuery(Query q) {
        if (q == null || queries.contains(q)) {
            return false;
        }
        queries.push(q);
        pushContext(q);
        return true;
    }

    void popQuery() {
        if (!queries.empty()) {
            queries.pop();
        } else {
            Log.warning(Engine.class,
                    "request for query popping ignored: the query stack is empty");
        }
        popContext();
    }

    Query getCurrentQuery() {
        if (!queries.empty()) {
            return (Query) queries.peek();
        }
        return null;
    }

    /* manage contexts */
    boolean pushContext(Context c) {
        if (c == null || contexts.contains(c)) {
            return false;
        }
        c.setParent(getCurrentContext());
        c.setScope(getCurrentScope());
        contexts.push(c);
        return true;
    }

    void popContext() {
        if (!contexts.empty()) {
            contexts.pop();
        } else {
            Log.warning(Engine.class,
                    "request for context popping ignored: the context stack is empty");
        }
    }

    Context getCurrentContext() {
        if (!contexts.empty()) {
            return (Context) contexts.peek();
        }
        return null;
    }

    /* Manage maps */
    BaseMap addMap(String name, List params) {
        map = getCurrentScope().addMap(name, params);
        return map;
    }

    BaseMap addMap(String name) {
        return addMap(name, null);
    }
    
    BaseMap getCurrentMap() {
        return map;
    }
    
    BaseMap clearCurrentMap() {
        BaseMap old = map;

        map = null;
        return old;
    }

    BaseMap getMap(ID id) {
        if (!id.hasScope()) {
            for (int i = in_scope.size(); i > 0; i--) {
                Scope s = (Scope) in_scope.get(i - 1);
                BaseMap   m = s.getMap(id.name);

                if (m != null) {
                    return m;
                }
            }
        } else {
            return id.scope.getMap(id.name);
        }
        return null;
    }
    
    BaseMap replaceMap(String name, BaseMap m) {
        return getCurrentScope().replaceMap(name, m);
    }

    /* Manage focus */
    void setFocus(Variable var) {
        focus = var;
    }
    
    void clearFocus() {
        focus = null;
    }
    
    Variable getFocus() {
        return focus;
    }
    
    boolean hasFocus() {
        return (focus != null);
    }

    /* Manage variables */
    Variable addVariable(String name, Expression expr) {
        return getCurrentScope().addVariable(name, expr);
    }

    Variable getVariable(ID id) {
        for (int i = contexts.size(); i > 0; i--) { // try to find it in the Context stack
            Context c = (Context) contexts.get(i - 1);

            if (id.hasScope() && id.scope != c.getScope()) {
                continue;
            }
            Variable v = c.getVariable(id.name);

            if (v != null) {
                return v;
            }
        }
        if (id.hasScope()) {
            return id.scope.getVariable(id.name);
        } // try to find it in the Scope (will find only variables from the Scope Context, not from sub queries)
        return null;
    }

    /* Manage declared notions */
    boolean pushDeclaredNotion(DeclaredNotion n) {
        if (declarationPath.contains(n)) {
            return false;
        }
        declarationPath.push(n);
        Log.debug(Engine.class,
                "PUSH declared notion:" + (n != null ? n : "<NULL>"));
        return true;
    }

    void popDeclaredNotion() {
        DeclaredNotion n = getCurrentDeclaredNotion();

        if (!declarationPath.empty()) {
            declarationPath.pop();
        } else {
            Log.warning(Engine.class,
                    "request for declared notion popping ignored: the declared notion stack is empty");
        }
        Log.debug(Engine.class,
                "POP declared notion:" + (n != null ? n : "<NULL>"));
    }

    DeclaredNotion getCurrentDeclaredNotion() {
        DeclaredNotion c = null;

        if (!declarationPath.empty()) {
            c = (DeclaredNotion) declarationPath.peek();
        }
        Log.debug(Engine.class,
                "current declared notion:" + (c != null ? c : "<NULL>"));
        return c;
    }

    List getDeclarationPath() {
        return declarationPath;
    }

    DeclaredNotion getDeclaredNotion(int type, ID id) {
        DeclaredNotion d = null;
        DeclaredNotion c = getCurrentDeclaredNotion();

        if ((type < Notion.TOP) && (c != null)) {
            d = c.getMember(id);
        }
        if ((d == null) && id.hasScope()) {
            d = id.scope.getDeclarationRoot(id.name);
        }
        Log.debug(Engine.class,
                "get declared notion[" + id + "]:" + (d != null ? d : "<NULL>"));
        return d;
    }

    DeclaredNotion addDeclaredNotion(DeclaredNotion d) {
        if (d != null) {
            DeclaredNotion c = getCurrentDeclaredNotion();

            if (!d.isTop() && (c != null)) {
                if (!c.addMember(d)) {
                    return null;
                }
            } else {
                if (!d.getScope().addDeclarationRoot(d)) {
                    return null;
                }
            }
        }
        Log.debug(Engine.class,
                "add declared notion[" + d + "]:" + (d != null ? d : "<NULL>"));
        return d;
    }

    DeclaredNotion addDeclaredNotion(int type, ID id) {
        DeclaredNotion d = null;
        DeclaredNotion c = getCurrentDeclaredNotion();

        if ((type < Notion.TOP) && (c != null)) {
            d = c.addMember(id);
        } else if (id.hasScope()) {
            d = id.scope.addDeclarationRoot(id.name);
        }
        d.setType(type);
        Log.debug(Engine.class,
                "add declared notion[" + id + "]:" + (d != null ? d : "<NULL>"));
        // System.err.println("!MENZO:add declared notion["+id+"]:"+(d!=null?d:"<NULL>")+((c!=null)?" as member to "+c:" as root"));
        return d;
    }

    DeclaredNotion findDeclaredNotion(ID id, boolean top) {
        DeclaredNotion d = null;

        if (top) { // this is a top notion, try to find it
            if (id.hasScope()) {
                d = id.scope.getDeclarationRoot(id.name);
            } else {
                d = getCurrentScope().getDeclarationRoot(id.name);
            }
        }
        
        if (d == null) { // this is a member notion, try to find it
            InstantiatedNotion c = getCurrentInstantiatedNotion();

            if (c != null) {
                d = c.getDeclarationContext();
                if (d != null) {
                    d = d.getMember(id);
                }
            }
        }
        
        if (d == null) { // this is an independent defined notion, try to find it
            if (id.hasScope()) {
                d = id.scope.getDeclarationRoot(id.name);
                // System.err.println("!MENZO:"+id.scope+".getDeclarationRoot("+id.name+"):"+((d!=null)?d:"<null>"));
            } else {
                d = getCurrentScope().getDeclarationRoot(id.name);
                // System.err.println("!MENZO:"+getCurrentScope()+".getDeclarationRoot("+id.name+"):"+((d!=null)?d:"<null>"));
            }
        }
        
        Log.debug(Engine.class,
                "find declared notion[" + id + "]:" + (d != null ? d : "<NULL>"));
        // System.err.println("!MENZO:find declared notion["+id+"]:"+(d!=null?d:"<NULL>"));
        return d;
    }

    /* Manage instantiated notions */
    InstantiatedNotion getCurrentInstantiatedNotion() {
        InstantiatedNotion n = null;
        LocalizedNotion l = getCurrentLocalizedNotion();

        if (l != null) {
            n = l.getInstantiationContext();
        }
        Log.debug(Engine.class,
                "current instantiated notion:" + (n != null ? n : "<NULL>"));
        return n;
    }

    InstantiatedNotion getInstantiatedNotion(ID id, boolean top) {
        InstantiatedNotion n = null;

        if (top) {
            if (id.hasScope()) {
                n = id.scope.getInstantiationRoot(id.name);
            } else {
                n = getCurrentScope().getInstantiationRoot(id.name);
            }
        } else {
            n = getCurrentInstantiatedNotion();
            if (n != null) {
                n = n.getMember(id);
            }
        }
        Log.debug(Engine.class,
                "get instantiated notion[" + id + "]:"
                + (n != null ? n : "<NULL>"));
        return n;
    }

    InstantiatedNotion addInstantiatedNotion(ID id, boolean top) {
        InstantiatedNotion n = getCurrentInstantiatedNotion();

        if (n != null) {
            n = n.addMember(id);
            if ((n != null) && top) {
                if (!n.isTop()) {
                    n.setType(Notion.TOP);
                }
                if (!id.scope.addInstantiationRoot(n)) {
                    Log.error(Engine.class,
                            "coulnd't set " + n + " as instantiation root");
                }
            }
        } else if (top) {
            if (id.hasScope()) {
                n = id.scope.addInstantiationRoot(id);
            } else {
                n = getCurrentScope().addInstantiationRoot(id);
            }
        }
        Log.debug(Engine.class,
                "add instantiated notion[" + id + "]:"
                + (n != null ? n : "<NULL>"));
        return n;
    }

    InstantiatedNotion addInstantiatedNotion(DeclaredNotion dn, boolean top) {
        InstantiatedNotion n = getCurrentInstantiatedNotion();

        if (n != null) {
            n = n.addMember(dn);
            if ((n != null) && top) {
                if (!n.getScope().addInstantiationRoot(n)) {
                    Log.error(Engine.class,
                            "coulnd't set " + n + " as instantiation root");
                }
            }
        } else if (top) {
            n = dn.getScope().addInstantiationRoot(dn);
        }
        Log.debug(Engine.class,
                "add instantiated notion[" + dn + "]:"
                + (n != null ? n : "<NULL>"));
        return n;
    }
    
    InstantiatedNotion addInstantiatedNotion(ID id, DeclaredNotion sup) {
        InstantiatedNotion n = getCurrentInstantiatedNotion();

        InstantiatedNotion r = null;
        if (id.hasScope()) {
            r = id.scope.addInstantiationRoot(id, sup);
        } else {
            r = getCurrentScope().addInstantiationRoot(id, sup);
        }
        n.addMember(r);
        
        return r;
    }

    boolean addInstantiatedNotion(InstantiatedNotion n) {
        InstantiatedNotion c = getCurrentInstantiatedNotion();

        if (c != null) {
            return c.addMember(n);
        }
        return true;
    }

    /* Manage localized notions */
    boolean pushLocalizedNotion(LocalizedNotion n) {
        if (localizationPath.contains(n)) {
            return false;
        }
        localizationPath.push(n);
        Log.debug(Engine.class,
                "PUSH localized notion:" + (n != null ? n : "<NULL>"));
        return true;
    }

    void popLocalizedNotion() {
        LocalizedNotion n = getCurrentLocalizedNotion();

        if (!localizationPath.empty()) {
            localizationPath.pop();
        } else {
            Log.warning(Engine.class,
                    "request for localized notion popping ignored: the localized notion stack is empty");
        }
        Log.debug(Engine.class,
                "POP localized notion:" + (n != null ? n : "<NULL>"));
    }

    List getLocalizationPath() {
        return localizationPath;
    }

    LocalizedNotion getCurrentLocalizedNotion() {
        LocalizedNotion n = null;

        if (!localizationPath.empty()) {
            n = (LocalizedNotion) localizationPath.peek();
        }
        Log.debug(Engine.class,
                "current localized notion:" + (n != null ? n : "<NULL>"));
        return n;
    }

    LocalizedNotion addLocalizedNotion(InstantiatedNotion in) {
        LocalizedNotion n = getCurrentLocalizedNotion();

        if (in != null) {
            if (n != null) {
                n = n.addMember(getCurrentScope(), in);
            } else {
                Log.debug(Engine.class,
                        "set LocalizationRoot of " + getCurrentScope()
                        + " to a localized version of " + in);
                n = getCurrentScope().addLocalizationRoot(in);
            }
        }
        if (n != null) {
            n.setContext(getCurrentContext());
        }
        Log.debug(Engine.class,
                "add localized notion[" + in + "]:" + (n != null ? n : "<NULL>"));
        return n;
    }

    /* Find a notion */
    DeclaredNotion findRootNotion(ID id) {
        if (!id.hasScope()) {
            id.scope = getCurrentScope();
        }
        return id.scope.findRootNotion(id);
    }
    
    Notion findNotion(ID id) {
        if (!id.hasScope()) {
            id.scope = getCurrentScope();
        }
        return id.scope.findNotion(id);
        // TODO: should check if multiple notions can be found, if so that's an error and the selection should be narrowed
    }

    /* Generate the meta information */
    void getMetaXML(Element root) {
        Element meta = new Element("meta");
		meta.addAttribute(new Attribute("stamp",stamp));
        Element dict = new Element("dictionary");

        for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
            Scope scope = (Scope) iter.next();

            scope.getMetaXML(meta);
            if (scope.enabled()) {
                scope.getDictXML(dict);
            }
        }
        if (dict.getChildCount() > 0) {
            meta.appendChild(dict);
        }
        if (meta.getChildCount() > 0) {
            root.appendChild(meta);
        }
    }

    public Document getMetaXML() {
        Document doc = null;

        try {
            Element root = new Element("warehouse");

            doc = new Document(root);
            getMetaXML(root);
        } catch (Exception e) {
            Log.error(Engine.class, "couldn't create the meta document", e);
            return null;
        }
        return doc;
    }
    
    /* Generate DB configuration XML document */
    Document getConfXML() {
        Document doc = null;

        try {
            Element root = new Element("warehouse");

            doc = new Document(root);
            for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
                Scope scope = (Scope) iter.next();

                if (scope.enabled()) {
                    scope.getConfXML(root);
                }
            }
        } catch (Exception e) {
            Log.error(Engine.class, "couldn't create the configuration document",
                    e);
            return null;
        }
        return doc;
    }
    
    public File dumpConfXML() {
        File conf = null;
        Document doc = getConfXML();

        if (doc != null) {
            conf = new File(
                    System.getProperty("java.io.tmpdir")
                            + System.getProperty("file.separator") + "DTL."
                            + System.currentTimeMillis() + ".conf.xml");
            try {
                OutputStream out = new FileOutputStream(conf);
                Serializer ser = new Serializer(out);

                ser.write(doc);
                out.close(); 
                conf.deleteOnExit();
                Log.debug(Engine.class,
                        "saved the configuration document to:" + conf);
            } catch (Exception exp) {
                Log.error(Engine.class,
                        "couldn't save the configuration document", exp);
            }
        }
        return conf;
    }
    
    /* execute the actual transformation */
    void transform(Element root) {
        Element data = new Element("data");
		data.addAttribute(new Attribute("stamp",stamp));

        root.appendChild(data);
            
        DataContext dc = new DataContext(this, data);

        for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
            Scope scope = (Scope) iter.next();

            if (scope.enabled()) {
                scope.build(dc);
            }
        }
        for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
            Scope scope = (Scope) iter.next();

            if (scope.enabled()) {
                scope.report();
            }
        }
    }

    public Document transform(boolean meta) {
        Document doc = null;

        try {
            try {
                Element root = new Element("warehouse");

                doc = new Document(root);
            
                transform(root);
            
                if (meta) {
                    getMetaXML(root);
                }
            } catch (FatalException e) {// all listeners are notified just return the (partial) (meta) data and log
            }
        } catch (Exception e) {
            Log.error(Engine.class, "couldn't create the result document", e);
        }
        return doc;
    }

    /* Parse an XML string */
    Document toXML(String xml) {
        try {
            return nux.xom.pool.XOMUtil.toDocument(xml);
        } catch (Exception e) {
            Log.error(Engine.class,
                    "string["+xml+"] doesn't contain wellformed (and valid) XML", e);
        }
        return null;
    }
    
    /* Complete the internal structures */
    public boolean complete() {
        try {
            for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
                if (!((Scope) iter.next()).complete()) {
                    return false;
                }
            }
            return true;
        } catch (FatalException e) {// all listeners have been notified just return the failure
        }
        return false;
    }

    /* Check the internal structures */
    public boolean check() {
        try {
            for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
                if (!((Scope) iter.next()).check()) {
                    return false;
                }
            }
            return true;
        } catch (FatalException e) {// all listeners have been notified just return the failure
        }
        return false;
    }

    /* Debug info */
    public void debug(boolean debug) {
        this.debug = debug;
        Log.debug(debug);
    }

    public void debug() {
        Log.debug(Engine.class, "DTL: " + name);
        for (Iterator iter = scopes.values().iterator(); iter.hasNext();) {
            ((Scope) iter.next()).debug("  ", "  ");
        }
    }
    
    /* enable or disable scopes */
    public void enableScopes(String[] scopes) {
        if ((scopes != null) && (scopes.length > 0)) {
            for (int i = 0; i < scopes.length; i++) {
                enabled_scopes.add(scopes[i]);
            }
        }
    }
    
    public void disableScopes(String[] scopes) {
        if ((scopes != null) && (scopes.length > 0)) {
            for (int i = 0; i < scopes.length; i++) {
                disabled_scopes.add(scopes[i]);
            }
        }
    }
    
    public boolean isEnabledScope(String name) {
        boolean res = true;

        if (enabled_scopes.contains(name)) {
            res = true;
        } // explicitly enabled
        else if (enabled_scopes.size() > 0) {
            res = false;
        } // implicitly disabled
        else if (disabled_scopes.contains(name)) {
            res = false;
        } // explicitly disabled
        else if (disabled_scopes.size() > 0) {
            res = true;
        } // implicitly enabled
        return res;
    }
    
    public boolean isDisabledScope(String name) {
        return !(isEnabledScope(name));
    }
    
    /* disable includes */
    public void excludeIncludes(String[] includes) {
        if ((includes != null) && (includes.length > 0)) {
            for (int i = 0; i < includes.length; i++) {
                excludes.add(includes[i]);
            }
        }
    }

    protected boolean exclude(String include) {
        boolean res = false;

        if (excludes.contains(include)) {
            res = true;
        }
        return res;
    }
    
    public boolean include(String include) {
        boolean res = !(exclude(include));

        if (res && (includer != null)) {
            res = includer.include(include);
        }
        return res;
    }
    
    /* Finalize an Engine object */
    protected void finalize() {
        Log.delListener(validListener);
    }
    
    /* Main method */
    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("DTLEngine [options] [DTL file]",
                "Where possible options are:", options,
                "When there is no DTL file specified on the commandline, the specification is read from standard input");
    }

    public static void main(String[] args) {
        // Command line options
        Options options = new Options();

        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(new Option("g", "debug", false, "show debug messages"));

        options.addOption(
                new Option("x", "exclude", true, "don't include this file"));

        options.addOption(
                new Option("e", "enable", true, "enable database scope"));
        options.addOption(
                new Option("d", "disable", true, "disable database scope"));
        
        options.addOption(new Option("m", "meta", false, "dump the meta data"));
        options.addOption(
                new Option("c", "configuration", false,
                "dump the configuration data"));
        options.addOption(
                new Option("t", "transform", false, "transform the data"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;

        try {
            // parse the command line arguments
            cmdline = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            Log.error(Engine.class, "Couldn't parse command line options", exp);
            usage(options);
            System.exit(1);
        }

        if (cmdline.hasOption("h")) {
            usage(options);
            System.exit(0);
        }
        
        String file = null;
        InputStream in = System.in;

        if (cmdline.getArgs().length > 0) {
            if (cmdline.getArgs().length > 1) {
                Log.error(Engine.class,
                        "You should give only one DTL file on the command line");
                usage(options);
                System.exit(1);
            }
            file = cmdline.getArgs()[0];
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException exp) {
                Log.error(Engine.class, "Couldn't open the DTL file: " + file);
                System.exit(1);
            }
        }
        
        DefaultListener dl = new DefaultListener();

        Log.addListener(dl);
        Log.addTracker(dl);

        Engine e = null;;
        try {
            Reader r = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            e = new Engine(r, file);
        } catch (UnsupportedEncodingException ex) {
            Log.error(Engine.class, "Couldn't open the DTL file: " + file + " is it encoded in UTF-8?");
            System.exit(1);
        }

        e.debug(cmdline.hasOption("g"));
        
        e.enableScopes(cmdline.getOptionValues('e'));
        e.disableScopes(cmdline.getOptionValues('d'));
        
        e.excludeIncludes(cmdline.getOptionValues('x'));

        if (!e.parse()) {
            Log.fatal(Engine.class, "the DTL couldn't be parsed");
            System.exit(1);
        }
        if (!e.check()) {
            Log.fatal(Engine.class,
                    "the internal memo structure build from this DTL isn't semantically consistent");
            System.exit(1);
        }
        if (cmdline.hasOption("d")) {
            e.debug();
        }
        if (cmdline.hasOption("m") && !cmdline.hasOption("t")) {
            Document doc = e.getMetaXML();

            if (doc != null) {
                try {
                    new Serializer(System.out).write(doc);
                } catch (Exception exp) {
                    Log.error(Engine.class, exp);
                }
            }
        }
        if (cmdline.hasOption("c")) {
            Document doc = e.getConfXML();

            if (doc != null) {
                try {
                    new Serializer(System.out).write(doc);
                } catch (Exception exp) {
                    Log.error(Engine.class, exp);
                }
            }
        }
        if (cmdline.hasOption("t")) {
            Document doc = e.transform(cmdline.hasOption("m"));

            if (doc != null) {
                try {
                    new Serializer(System.out).write(doc);
                } catch (Exception exp) {
                    Log.error(Engine.class, exp);
                }
            }
        }
    }
}
