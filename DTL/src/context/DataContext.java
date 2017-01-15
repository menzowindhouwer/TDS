package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.Engine;
import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.data.DataList;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.notion.LocalizedNotion;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;
import java.net.URL;

import nu.xom.*;


/**
 * A DataContext contains and gives access to the actual data loaded from a database.
 * It does so by cooperating with a specific Context.
 **/
public class DataContext {

    /* where are we? */
    protected Context context = null;
    protected DataContext parent = null;
    
    /* where are we locally */
    protected Stack stack = new Stack();
    
    /* variables */
    protected java.util.Map variables = new HashMap();

    protected Engine engine = null;
  
    /* the result */
    protected Element base = null;
    protected Element root = null;
  
    /* lookup map for root nodes */
    protected java.util.Map roots = null;
    
    /* loaded data */
    protected Document data = null;
    protected int row = 0;
    protected int rows = -1;
    
    /* fields */
    protected java.util.Map known_fields = null;
    protected java.util.Map fields = null;
    
    /* focus */
    protected Data focus = null;
    
    /* parameter translation */
    protected Stack translate = new Stack();
    
    /* constructor */
    public DataContext(Engine e, Element r) {
        engine = e;
        base = r;
        root = r;
        roots = new HashMap();
    }
    
    public DataContext(Context c, DataContext dc, Element r) {
        parent = dc;
        engine = dc.engine;
        base = dc.base;
        roots = dc.roots;
        root = r;
        context = c;
        data = c.importData(this);
        loadFields();
    }
  
    public DataContext(Context c, DataContext dc) {
        this(c, dc, dc.getRoot());
    }
    
    public Engine getEngine() {
        return engine;
    }
    
    public Element getRoot() {
        return root;
    }
    
    /* manage surroudings */
    public boolean setContext(Context c) {
        boolean res = false;

        if (context == null) {
            context = c;
            data = c.importData(this);
            res = loadFields();
        }
        return res;
    }
    
    public Context getContext() {
        return context;
    }
  
    public void setParent(DataContext p) {
        parent = p;
    }
    
    public DataContext getParent() {
        return parent;
    }
    
    public Scope getScope() {
        DataContext c = this;

        while (c != null) {
            if (c.getContext() instanceof Scope) {
                return (Scope) c.getContext();
            }
            c = c.getParent();
        }
        return null;
    }
    
    /* manage variables */
    public void setVariable(String var, Data val) {
        if (variables.containsKey(var)) {
            Log.warning(DataContext.class,
                    "overwritting value of variable[" + var + "]");
        }
        variables.put(var, val);
    }
    
    public Data getVariable(String var) {
        //System.err.println("!MENZO: trying to get variable[" + var + "] in "+this+(this.context!=null?"/"+this.context:""));
        Data res = null;

        if (variables != null) {
            if (variables.containsKey(var)) {
                res = (Data) variables.get(var);
        	//System.err.println("!MENZO: got variable[" + var + "] in "+this+(this.context!=null?"/"+this.context:""));
            } else if (context.getVariables().containsKey(var)) {
		// JIT instantiation of the variable
            	Expression expr = (Expression) context.getVariables().get(var);
            	//System.err.println("!MENZO:trying to JIT instantiate variable["+var+"]="+expr);
            	if (expr != null) {
               		res = expr.eval(this);
                	if (res == null) {
                    		Log.error(DataContext.class,
                            		"couldn't instantiate variable[" + var
                            		+ "]: its value expression " + expr
                            		+ "couldn't be evaluated");
                	}
            	} else {
               		Log.error(DataContext.class,
                       		"couldn't instantiate variable[" + var
                        	+ "]: there is no value expression");
            	}
	    } else if (parent != null) {
		// go to the parent context in search of the variable
        	//System.err.println("!MENZO: trying to get variable[" + var + "] in parent context "+parent);
                res = parent.getVariable(var);
            }
        }
        if (res == null) {
            Log.error(DataContext.class, "Unknown variable[" + var + "] in "+this+(this.context!=null?"/"+this.context:""));
        }
        return res;
    }
    
    /* load fields and data */
    public boolean hasData() {
        Log.debug(DataContext.class,"hasData():"+((data != null) && (known_fields != null) && (known_fields.size() > 0))+" [data!=null?"+(data != null)+"][known_fields!=null?"+(known_fields != null)+"][known_fields.size()>0?"+((known_fields != null)&&(known_fields.size() > 0))+"]");
        return ((data != null) && (known_fields != null)
                && (known_fields.size() > 0));
    }
    
    public Document getData() {
        return data;
    }
    
    public boolean loadFields() {
        if (data == null) {
            return false;
        }
      
        Nodes l = null;

        try {
            l = nux.xom.xquery.XQueryUtil.xquery(data, "//answer/tuple/field");
        } catch (Exception e) {
            Log.error(DataContext.class, "couldn't execute XPath expression", e);
        }
      
        if ((l == null) || (l.size() == 0)) {
            return false;
        }
      
        known_fields = new HashMap();
        for (int i = 0; i < l.size(); i++) {
            Element f = (Element) l.get(i);
            String field = f.getAttribute("name").getValue();

            if (!known_fields.containsKey(field)) {
                known_fields.put(field, new FieldInfo());
            }
        }
      
        return true;
    }
    
    public boolean loadDataRow() {
        if (data == null) {
            return false;
        }
      
        row++; // get the next row
      
        Nodes l = null;

        try {
            l = nux.xom.xquery.XQueryUtil.xquery(data,
                    "//answer/tuple[@row='" + row + "']");
        } catch (Exception e) {
            Log.error(DataContext.class, "couldn't execute XPath expression", e);
        }
      
        if ((l == null) || (l.size() == 0)) {
            Log.debug(DataContext.class,"row["+row+"] is empty");
            return false;
        }
      
        if (l.size() > 1) {
            Log.error(DataContext.class,
                    "loading row[" + row + "] resulted in multiple answers");
            return false;
        }
      
        Element r = (Element) l.get(0);

        Elements e = r.getChildElements();

        if ((e == null) || (e.size() == 0)) {
            Log.warning(DataContext.class, "row[" + row + "] is an empty row");
            return false;
        }

        resetFieldInfo();
        fields = new HashMap();
        for (int i = 0; i < e.size(); i++) {
            Element c = e.get(i);
            String field = c.getAttribute("name").getValue();
            
            if (context.skip(field)) {
                Log.warning(DataContext.class, "skipped field[" + field + "]");
                continue;
            }
            
            Data d = null;

            if ((c.getAttribute("null") != null)
                    && c.getAttribute("null").getValue().equals("null")) {
                d = new Data(null);
            } else {
                d = new Data(c.getValue());
            }
            d.setNote(
                    "copied from row[" + row
                    + "] from the database import for " + this);
            if (fields.containsKey(field)) {
                Log.warning(DataContext.class,
                        "duplicate field[" + field + "] in row[" + row + "] added to the DataList");
		// add the n-th instantion of the field to the list
                DataList dl = (DataList)fields.get(field);
               	dl.addData(d);
            } else {
            	fields.put(field, new DataList(d));
            }
        }
      
        // preprocess the data
        if (context.hasPreprocess()) {
            for (Iterator steps = context.getPreprocess().iterator(); steps.hasNext();) {
                Preprocess step = (Preprocess) steps.next();

                if (!step.eval(this)) {
                    Log.error(DataContext.class, "preprocessing step failed");
                    return false;
                }
            }
        }

        // instantiate the variables ... maybe we should do this JIT ...
        variables.clear();
        for (Iterator iter = context.getVariables().keySet().iterator(); iter.hasNext();) {
            String var = (String) iter.next();
            Expression expr = (Expression) context.getVariables().get(var);

            //System.err.println("!MENZO:trying to instantiate variable["+var+"]="+expr);
            if (expr != null) {
                Data val = expr.eval(this);

                if (val != null) {
                    //System.err.println("!MENZO:success variable["+var+"]="+val);
                    setVariable(var, val);
                } else {
                    Log.error(DataContext.class,
                            "couldn't instantiate variable[" + var
                            + "]: its value expression " + expr
                            + "couldn't be evaluated");
                    return false;
                }
            } else {
                Log.error(DataContext.class,
                        "couldn't instantiate variable[" + var
                        + "]: there is no value expression");
                return false;
            }
        }
          
        return true;
    }
    
    public void dumpDataRow() {
        for (Iterator iter = fields.keySet().iterator(); iter.hasNext();) {
            String field = (String) iter.next();

            Log.debug(DataContext.class,
                    "FIELD[" + field + "] has value " + fields.get(field));
        }
    }
    
    public int getCurrentRow() {
        return row;
    }
    
    public int getRowCount() {
	if (rows==-1) {
        	Nodes l = null;

        	try {
            		l = nux.xom.xquery.XQueryUtil.xquery(data,
               	     		"count(//answer/tuple)");
        	} catch (Exception e) {
            		Log.error(DataContext.class, "couldn't execute XPath expression", e);
        	}
      
        	if ((l == null) || (l.size() == 0)) {
			rows = 0;
        	} else {
        		Element r = (Element) l.get(0);
			rows = Integer.parseInt(r.getValue());
		}
	}

        return rows;
    }
    
    /* manage fields */
    public boolean addDerivedField(String field, Data value) {
        if (!knownField(field)) {
            FieldInfo info = new FieldInfo();

            info.derived();
            known_fields.put(field, info);
        } else if (!getFieldInfo(field).isDerived()) {
            Log.error(DataContext.class,
                    "couldn't add derived field[" + field
                    + "] it already exists, but as an underived field");
            return false;
        }
        if (fields.containsKey(field)) {
            Log.warning(DataContext.class,
                    "derived field[" + field + "] to " + value
                    + " already has value " + fields.get(field) + " adding this one to the list");
            DataList dl = (DataList)fields.get(field);
            dl.addData(value);
        } else
        	fields.put(field, new DataList(value));
        return true;
    }
    
    public boolean knownField(String field) {
        boolean res = false;

        if ((field != null) && (known_fields != null)) {
            if (known_fields.containsKey(field)) {
                res = true;
            }
        }
        return res;
    }
  
    public FieldInfo getFieldInfo(String field) {
        FieldInfo res = null;

        if ((field != null) && (known_fields != null)) {
            res = (FieldInfo) known_fields.get(field);
        }
        return res;
    }
    
    public void touchField(String field) {
        FieldInfo res = getFieldInfo(field);

        if (res != null) {
            res.touch();
        }
    }
    
    public void resetFieldInfo() {
        if (known_fields != null) {
            for (Iterator iter = known_fields.values().iterator(); iter.hasNext();) {
                ((FieldInfo) iter.next()).reset();
            }
        }
    }
    
    public Set getFields() {
        Set res = null;

        if (known_fields != null) {
            res = new HashSet(); // need to create a copy as during iteration over this set, you might want to call setField(...)
            for (Iterator iter = known_fields.keySet().iterator(); iter.hasNext();) {
		String field = (String)iter.next();
		if (!context.skip(field))
                	res.add(field);
            }
        }
        return res;
    }
  
    public java.util.Map getKnownFields() {
        return known_fields;
    }
    
    public Data getField(String field) {
        Data res = null;

        if (fields != null) {
            if (context.skip(field)) {
                Log.error(DataContext.class, "requesting a field[" + field + "] which has been skipped");
            }
            if (fields.containsKey(field)) {
                res = (Data) fields.get(field);
            } else if (knownField(field)) {
                res = new DataList(new Data(null));
            }
            touchField(field);
        }
        if ((res == null) && (parent != null)) {
            res = parent.getField(field);
        } else if (res == null) {
            Log.error(DataContext.class, "Unknown field[" + field + "]");
        }
        return res;
    }

    public Data setField(String field, Data val) {
        Data old = getField(field);

        if (old != null) {
            fields.put(field, val);
        } else {
            Log.error(DataContext.class,
                    "couldn't set value of field[" + field + "] to " + val);
        }
        return old;
    }
    
    /* manage focus */
    public Data getFocus() {
        return focus;
    }
    
    public DataContext setFocus(Data f) {
        focus = f;
        return this;
    }
    
    public DataContext clearFocus() {
        focus = null;
        return this;
    }

    /* manage the result */
    public Element createElement(String name) {
        return new Element(name);
    }
  
    public Element createElement(Element p, LocalizedNotion n, Data k, int i, int e) {
        Element res = null;

        if ((p != null) && (n != null)) {
            res = createChildElement(p, n.getName());
            res.addAttribute(new Attribute("scope", n.getScope().getName()));
            res.addAttribute(new Attribute("base", "" + n.base()));
            if (n.isRoot()) {
                res.addAttribute(new Attribute("type", "root"));
            } else if (n.isTop()) {
                res.addAttribute(new Attribute("type", "top"));
            } else if (n.isComplete()) {
                res.addAttribute(new Attribute("type", "complete"));
            } else if (n.isAnnotation()) {
                res.addAttribute(new Attribute("type", "annotation"));
            }
            res.addAttribute(new Attribute("id", "" + n.id() + "[" + i + "]"));
            if ((k != null) && (k.getValue() != null)) {
                res.addAttribute(new Attribute("key", "" + k.getValue()));
            }
            res.addAttribute(new Attribute("epoch", "" + e));
            if (n.isAnnotation()) {
                res.addAttribute(new Attribute("general", "" + n.isGeneral()));
            }
            res.addAttribute(new Attribute("srcs", getScope().getName()));
        }
        return res;
    }
  
    public Element createReference(Element from, Element to, int e) {
        Element res = null;

        if ((from != null) && (to != null)) {
            res = createChildElement(from, to.getLocalName());
            res.addAttribute((Attribute) to.getAttribute("scope").copy());
            res.addAttribute((Attribute) to.getAttribute("base").copy());
            res.addAttribute(
                    new Attribute("idref", to.getAttribute("id").getValue()));
            if (to.getAttribute("key") != null) {
                res.addAttribute(
                        new Attribute("ref", to.getAttribute("key").getValue()));
            }
            res.addAttribute(new Attribute("epoch", "" + e));
            res.addAttribute(new Attribute("srcs", getScope().getName()));
        }
        return res;
    }

    public Element createNotion(Element p, LocalizedNotion n, Data k, int i, int e) {
        Element result = null;

        if (n.isRoot()) { // root notion
            if ((k != null) && (k.getValue() != null)) {
                // check if the keyed root notion already exists
                String key = "" + n.base() + "+" + k.getValue();

                if (roots.containsKey(key)) {
                    result = (Element) roots.get(key);
                } else {
                    result = createElement(base, n, k, i, e);
                    roots.put(key, result);
                }
                // create a reference to the root notion from the parent
                if (p != base) {
                    createReference(p, result, e);
                }
		
		// create the inverse, if requested
		if (n.createInverse()) {
			Element inverse = null;
			
			Nodes l = null;
			String xp = "ancestor-or-self::*[@type='root']";
			try {
				l = nux.xom.xquery.XQueryUtil.xquery(p, xp);
			} catch (Exception ex) {
				Log.error(DataContext.class,"couldn't execute XPath expression[" + xp + "]", ex);
			}
			if ((l != null) && l.size() > 0) {
				if (l.size() == 1) {
					inverse = (Element) l.get(0);
				} else {
					Log.fatal(DataContext.class,"lookup for inverse root of " + n + " resulted in more then one match!");
				}
			}
			
			createReference(result,inverse,e);
			//TODO: check what happens/should happen when the tree is retracted
		}
            } else {
                Log.fatal(DataContext.class,
                        "trying to create a root notion without a key!");
            }
        } else if ((k != null) && (k.getValue() != null)) { // keyed notion
            // check if the keyed notion already exists
            Nodes l = null;

            String xp = "*[@base='" + n.base() + "'][@key='"
                    + k.getValue().toString().replace("'", "''") + "']";

            try {
                l = nux.xom.xquery.XQueryUtil.xquery(p, xp);
            } catch (Exception ex) {
                Log.error(DataContext.class,
                        "couldn't execute XPath expression[" + xp + "]", ex);
            }
            if (l.size() > 0) {
                if (l.size() == 1) {
                    result = (Element) l.get(0);
                } else {
                    Log.fatal(DataContext.class,
                            "lookup for " + n + "[key=" + k
                            + "] resulted in more then one match!");
                }
            }
            if (result == null) {
                // keyed notion doesn't exist yet, so create it
                result = createElement(p, n, k, i, e);
            }
        } else { // regular notion
            // check if the notion already exists
            Nodes l = null;

            String xp = "*[@base='" + n.base() + "'][count(@key)=0]";

            try {
                l = nux.xom.xquery.XQueryUtil.xquery(p, xp);
            } catch (Exception ex) {
                Log.error(DataContext.class,
                        "couldn't execute XPath expression[" + xp + "]", ex);
            }
            if ((l != null) && l.size() > 0) {
                if (l.size() == 1) {
                    result = (Element) l.get(0);
                } else {
                    Log.fatal(DataContext.class,
                            "lookup for unkeyed " + n
                            + " resulted in more then one match!");
                }
            }
            if (result == null) {
                // regular notion doesn't exist yet, so create it
                result = createElement(p, n, k, i, e);
            }
        }
        return result;
    }
    
    public Element createValue(Element p, LocalizedNotion n, Data v, int e) {
        Element result = null;

        if ((v!=null) && !v.isEmpty()) { // root notion
            Literal l = null;
            if (v.getValue() != null) {
                if (v.getValue() instanceof Literal) {
                    l = (Literal) v.getValue();
                } // accept the value
                else {
                    l = new Literal(v.getValue());
                } // turn the data into a value

                if (!n.hasValue(l)) { // possibly enumerate this value
                    Annotation a = null;

                    if (n.hasBaseValueAnnotation()) {
                        a = n.getBaseValueAnnotation().eval(this);
                    } else {
                        a = v.getAnnotation();
                    }

		    if (n.getEnum()) { // always enumerate
                       	n.addValue(l, a);
		    } else if (a!=null && !a.isEmpty()) { // only when we have some annotation
                       	n.addValue(l, a);
		    }
                }
            }

            // check if the value already exists
            String xp = "value[@src='"+ getScope().getName() +"']";
            if (l!=null) {
                xp += "[this='"+l.toString().replaceAll("'","''").replaceAll("&","&amp;").replaceAll("<","&lt;")+"']";
            } else {
		xp += "[empty(this)]";
            }

            Nodes nl = null;
            try {
                nl = nux.xom.xquery.XQueryUtil.xquery(p, xp);
            } catch (Exception ex) {
                Log.error(DataContext.class,
                        "couldn't execute XPath expression[" + xp + "]", ex);
            }
            if ((nl != null) && nl.size() > 0) {
                if (nl.size() == 1) {
                    result = (Element) nl.get(0);
                } else {
                    Log.fatal(DataContext.class,
                            "lookup for scope " + getScope().getName() + " value " + v + " for notion " + n + " resulted in more then one match!");
                }
            }

            if (result==null) {
                result = createChildElement(p, "value");

                result.addAttribute(new Attribute("id", "v" + v.getId() + "n" + n.id() ));
                result.addAttribute(new Attribute("epoch", "" + e));
                result.addAttribute(new Attribute("src", getScope().getName()));
                if (l!=null) {
                    addChildElement(result, l.toXML("this"));
                    Log.message(DataContext.class,
                            "value=\"" + v.getValue() + "\"");
                }
            }

            if (v.hasMarkers()) {
                for (Iterator iter = v.getMarkers().iterator(); iter.hasNext();) {
                    String m = "" + ((Marker) iter.next()).getValue();

                    xp = "mark[.='"+m+"']";
                    try {
                        nl = nux.xom.xquery.XQueryUtil.xquery(result, xp);
                    } catch (Exception ex) {
                        Log.error(DataContext.class,
                            "couldn't execute XPath expression[" + xp + "]", ex);
                    }
                    if ((nl != null) && nl.size() > 0) {
                        if (nl.size() > 1) {
                            Log.fatal(DataContext.class,
                               "lookup for marker " + m + " for scope " + getScope().getName() + " value " + v + " for notion " + n + " resulted in more then one match!");
                        }
                    } else
                    	createTextNode(createChildElement(result, "mark"), m);

                    Log.message(DataContext.class, "mark=" + m);
                }
            }
        } else {
            Log.fatal(DataContext.class,
                    "trying to create a value notion without a value!");
        }
        return result;
    }
    
    public Element addChildElement(Element parent, Element child) {
        parent.appendChild(child);
        return child;
    }
    
    public Element createChildElement(Element parent, String name) {
        Element child = new Element(name);

        parent.appendChild(child);
        return child;
    }
    
    public Element createElementNS(String ns, String qname) {
        return new Element(qname, ns);
    }
    
    public void createTextNode(Element parent, String text) {
        parent.appendChild(text);
    }
  
    public boolean forget(Element n, int from) {
        boolean forgotten = false;
        String epoch = "";

        if (n.getAttribute("epoch") != null) {
            epoch = n.getAttribute("epoch").getValue();
        }

        if (!epoch.equals("")) {
            if (Integer.parseInt(epoch) >= from) {
                // detach n from its parent
                n.detach();
                // check if it's a known root
                if (n.getAttribute("key") != null) {
                    String key = n.getAttribute("base").getValue() + "+"
                            + n.getAttribute("key").getValue();

                    if (roots.containsKey(key)) {
                        roots.remove(key);
                    }
                }
                forgotten = true;
            } else {
                // forget descendants
                Elements l = n.getChildElements();

                for (int i = 0; i < l.size(); i++) {
                    if (forget(l.get(i), from)) {
                        forgotten = true;
                    }
                }
            }
        }
        return forgotten;
    }

    /* Access to the local context stack */
    public Object get() {
        return stack.peek();
    }
    
    public String trace() {
        String trace = null;

        if (parent != null) {
            trace = parent.trace();
        } else {
            trace = "/";
        }
        for (Iterator iter = stack.iterator(); iter.hasNext();) {
            trace += (trace.endsWith("/") ? "" : "/") + iter.next().toString();
        }
        return trace;
    }
    
    public void push(Object o) {
        stack.push(o);
    }
    
    public void pop() {
        stack.pop();
    }
    
    /* Manage the parameter translation map */
    public void pushTranslate(java.util.Map t) {
        // Log.message(DataContext.class,"push "+t.size()+" translations");
        java.util.Map cache = new HashMap();

        for (Iterator keys = t.keySet().iterator(); keys.hasNext();) {
            Param p = (Param) keys.next();
            Expression e = (Expression) t.get(p);

            if (e != null) {
                Data d = e.eval(this);

                if (d != null) {
                    cache.put(p, d);
                } else {
                    Log.error(DataContext.class,
                            "The translation expression " + e + " for " + p
                            + " couldn't be evaluated!");
                }
            } else {
                Log.error(DataContext.class,
                        "Coulnd't resolve " + p
                        + ", there is no translation available!");
            }
        }
        translate.push(cache);
    }
    
    public Data getTranslation(Param p) {

        /*
         System.err.println("!MENZO:tanslate param:"+p);
         System.err.println("!MENZO:translations available:");
         for(Iterator iter=translate.iterator();iter.hasNext();) {
         System.err.println("!MENZO:>");
         java.util.Map t = (java.util.Map)iter.next();
         for (Iterator keys=t.keySet().iterator();keys.hasNext();) {
         Object key = keys.next();
         System.err.println("!MENZO: "+key+" -> "+t.get(key));
         }
         System.err.println("!MENZO:<");
         }
         */
        for (Iterator iter = translate.iterator(); iter.hasNext();) {
            java.util.Map t = (java.util.Map) iter.next();

            if (t.containsKey(p)) {
                return (Data) t.get(p);
            }
        }
        return null;
    }
    
    public void popTranslate() {
        translate.pop();
        // Log.message(DataContext.class,"pop translations");
    }
}
