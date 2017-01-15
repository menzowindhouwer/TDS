package nl.uu.let.languagelink.tds.dtl.context;

import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;

import nu.xom.*;

/**
 * Access an XML document using XQuery statements
 **/
public class XMLimport implements Accessor {
    
    protected Scope    scope  = null;
    protected Document access = null;
    protected String   loc    = null;
    protected Document doc    = null;
    protected String   mode   = null;
    
    public XMLimport() {
        // used by the class loader
    }
    
    public XMLimport(Scope scope,Document access) {
        this.scope  = scope;
        this.access = access;
    }
    
    protected boolean load() {
        boolean res = false;
        if (access!=null) {
            if ((doc == null)) {
                Nodes l = nux.xom.xquery.XQueryUtil.xquery(access,"/xml/file/@href");
                if (l.size()==1) {
                    loc = l.get(0).getValue();
                    try {
                        doc = new Builder().build(new File(loc));
                        res = true;
                    } catch(Exception e) {
                        Log.error(XMLimport.class,"Couldn't load XML file["+loc+"]",e);
                    }
                }
                l = nux.xom.xquery.XQueryUtil.xquery(access,"/xml/file/@mode");
                if (l.size()==1) {
                    mode = l.get(0).getValue();
                } else
                    mode = null;
            } else
                res = true;
        }
        return res;
    }
    
    protected String fieldname(Set fieldnames,String fieldname) {
        int suff = 1;
        String fn = fieldname;
        Nodes reps = nux.xom.xquery.XQueryUtil.xquery(access,"/xml/replace");
        for (int r=0;r<reps.size();r++) {
  	    Element repl = (Element)reps.get(r);
    	    String  from = repl.getAttributeValue("from");
    	    String  to   = repl.getAttributeValue("to");
    	    fn = fn.replaceAll(from,to);
        }
        //while (fieldnames.contains(fn))
        //    fn = fieldname+"_"+(suff++);
        if (!fieldnames.contains(fn))
        	fieldnames.add(fn);
        return fn;
    }
    
    protected Element toField(String prefix,Set fieldnames,Node n) {
        Element field = null;
        if (n instanceof Element) {
            Element e = (Element)n;
            field = new Element("field");
            field.addAttribute(new Attribute("name",fieldname(fieldnames,e.getLocalName())));
            field.appendChild(e.getValue());
        } else if (n instanceof Attribute) {
            Attribute a = (Attribute)n;
            field = new Element("field");
            field.addAttribute(new Attribute("name",fieldname(fieldnames,prefix+a.getLocalName())));
            field.appendChild(a.getValue());
        } else if (n instanceof Text) {
            Text t = (Text)n;
            if (!t.getValue().trim().equals("")) {
                field = new Element("field");
                field.addAttribute(new Attribute("name",fieldname(fieldnames,prefix+"value")));
                field.appendChild(t.getValue());
            }
        } else if (n instanceof Comment) {
            // silently ignore comments
        } else {
            Log.error(XMLimport.class,"don't know how to handle node of class["+n.getClass()+"]");
        }
        return field;
    }
    
    public Document execQuery(String q) {
        Document res = null;
        if ((q!=null) && (!q.trim().equals("")) && load()) {
            Nodes l = nux.xom.xquery.XQueryUtil.xquery(doc,q);
            Log.debug(XMLimport.class,"query["+q+"] resulted in "+l.size()+" rows");
            
            boolean value = true; // the result node is the result field
            for (int i = 0;i<l.size();i++) {
                Node n = l.get(i);
                if ((n instanceof Element) && (((Element)n).getChildElements().size()>0)) {
                    value = false; // the children of the result node are the result fields
                    break;
                }
            }
            
            Element db = new Element("database");
            res = new Document(db);
            Element answer = new Element("answer");
            db.appendChild(answer);
            answer.addAttribute(new Attribute("resource",scope.getName()));
            answer.addAttribute(new Attribute("table",loc));
            for (int i = 0;i<l.size();i++) {
                Node n = l.get(i);
                Element tuple = new Element("tuple");
                answer.appendChild(tuple);
                tuple.addAttribute(new Attribute("row",""+(i+1)));
                String prefix = "xml_";
                if (n instanceof Element)
                    prefix = ((Element)n).getLocalName()+"_";
                Set fields =  new HashSet();
                if (value) {
                    // the result node is the result field
                    Element field = toField(prefix,fields,n);
                    if (field!=null)
                        tuple.appendChild(field);
                    if (n instanceof Element) {
                        Element e = (Element)n;
                        for (int a = 0;a<e.getAttributeCount();a++) {
                            field = toField(prefix,fields,e.getAttribute(a));
                            if (field!=null)
                                tuple.appendChild(field);
                        }
                    }
                } else if ((mode!=null) && (mode.equals("field"))) {
                    // the field child elements of the result node are the result fields, the name is specified in the @name
                    Nodes flds = n.query("field");
                    for (int c = 0;c<flds.size();c++) {
                    	Element e = (Element)flds.get(c);
                        Element field = new Element("field");
                        field.addAttribute(new Attribute("name",fieldname(fields,e.getAttributeValue("name"))));
                        field.appendChild(e.getValue());
                    	tuple.appendChild(field);
                    }
                } else {
                    // the children of the result node are the result fields
                    for (int c = 0;c<n.getChildCount();c++) {
                        Element field = toField(prefix,fields,n.getChild(c));
                        if (field!=null)
                            tuple.appendChild(field);
                    }
                    // also include the attributes
                    if (n instanceof Element) {
                        Element e = (Element)n;
                        for (int a = 0;a<e.getAttributeCount();a++) {
                            Element field = toField(prefix,fields,e.getAttribute(a));
                            if (field!=null)
                                tuple.appendChild(field);
                        }
                    }
                }
            }
            Log.debug(XMLimport.class,"XML["+nux.xom.pool.XOMUtil.toPrettyXML(res)+"]");
        }
        return res;
    }
    
    public boolean hasDefaultQuery() {
        return false;
    }
    
    public Document execDefaultQuery() {
        return null;
    }
    
    public Accessor newInstance(Scope scope,Document access) {
        return new XMLimport(scope,access);
    }
}
