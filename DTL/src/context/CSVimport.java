package nl.uu.let.languagelink.tds.dtl.context;

import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;
import java.util.*;

import nu.xom.*;
import nu.xom.xslt.*;

import com.oreilly.javaxslt.util.*;

/**
 * Access an CSV document using XQuery statements
 **/
public class CSVimport implements Accessor {
    
    protected Scope    scope  = null;
    protected Document access = null;

    protected Map      docs   = null;
    
    public CSVimport() {
        // used by the class loader
    }
    
    public CSVimport(Scope scope,Document access) {
        this.scope  = scope;
        this.access = access;
    }
    
    protected Document load(String name) {
        Document doc = null;
        if (access!=null) {
		if (docs==null)
			docs = new HashMap();
		if (!docs.containsKey(name)) {
			Nodes l = nux.xom.xquery.XQueryUtil.xquery(access,"/csv/file[@table='"+name+"']");
			if (l.size()==1) {
			    Node n = l.get(0);
			    String loc = nux.xom.xquery.XQueryUtil.xquery(n,"@href").get(0).getValue();
			    String del = null;
			    l = nux.xom.xquery.XQueryUtil.xquery(n,"@delimeter");
			    if (l.size()==1)
				del = l.get(0).getValue();
			    String header = "true";
			    l = nux.xom.xquery.XQueryUtil.xquery(n,"@header");
			    if (l.size()==1)
				header = l.get(0).getValue();
			    try {
				Document csv = (new Builder(new CSVXMLReader(del))).build(new File(loc));
				Log.debug(CSVimport.class,"loaded CSV table["+name+"] file["+loc+"] to XML[\n"+nux.xom.pool.XOMUtil.toPrettyXML(csv)+"\n]");
				// run through XSL
				nux.xom.pool.XSLTransformFactory factory = new nux.xom.pool.XSLTransformFactory() {
				    protected String[] getPreferredTransformerFactories() {
					return new String[] {
					    "net.sf.saxon.TransformerFactoryImpl",
					};
				    }
				}; 
				XSLTransform xslt = factory.createTransform(
					CSVimport.class.getResource("/CSVconvert.xsl").openStream(),
					new URI(CSVimport.class.getResource("/CSVconvert.xsl").toString()));
		
				xslt.setParameter("header", header);
				xslt.setParameter("resource",scope.getName());
				xslt.setParameter("table", name);
				Nodes nodes = xslt.transform(csv);
				if ((nodes.size()==1) && (nodes.get(0) instanceof Element)) {
				    doc = new Document((Element)nodes.get(0));
				    Log.debug(CSVimport.class,"loaded and converted CSV table["+name+"] file["+loc+"] to XML[\n"+nux.xom.pool.XOMUtil.toPrettyXML(doc)+"\n]");
				    docs.put(name,doc);
				} else
				    Log.error(CSVimport.class,"Couldn't convert CSV table["+name+"] file["+loc+"] to XML");
			    } catch(Exception e) {
				Log.error(CSVimport.class,"Couldn't load CSV table["+name+"] file["+loc+"]",e);
			    }
			} else
			    Log.error(CSVimport.class,"Couldn't load CSV table["+name+"]");
		} else {
		    doc = (Document)docs.get(name);
		}
        }
        return doc;
    }
    
    protected String fieldname(Set fieldnames,String fieldname) {
        int suff = 1;
        String fn = fieldname;
        while (fieldnames.contains(fn))
            fn = fieldname+"_"+(suff++);
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
        } else {
            Log.error(CSVimport.class,"don't know how to handle node of class["+n.getClass()+"]");
        }
        return field;
    }
    
    public Document execQuery(String q) {
        Document res = null;
        if ((q!=null) && (!q.trim().equals(""))) {
            String name  = q;
            String query = "/database/answer/tuple";
            String pair[] = q.split(":",2);
            if (pair.length==2) {
                name  = pair[0];
                query = pair[1];
            }
			if ((name!=null) && (!name.trim().equals(""))) {
                Document doc = load(name);
                if (doc!=null) {
                    Nodes l = nux.xom.xquery.XQueryUtil.xquery(doc,query);
                    Log.debug(CSVimport.class,"table["+name+"] query["+query+"] resulted in "+l.size()+" rows");
                    
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
                    answer.addAttribute(new Attribute("table",name));
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
                    Log.debug(CSVimport.class,"XML["+nux.xom.pool.XOMUtil.toPrettyXML(res)+"]");
                } else
                    Log.error(CSVimport.class,"CSV table["+name+"] isn't loaded");
            } else
                Log.error(CSVimport.class,"don't know which CSV table to use");
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
        return new CSVimport(scope,access);
    }
}
