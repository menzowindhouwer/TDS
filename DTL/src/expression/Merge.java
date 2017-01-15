package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.context.Query;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;
import java.util.*;

import nu.xom.*;
import nu.xom.xslt.*;


/**
 * The Merge expression takes a set of query results and merges them based on
 * a set of key fields.
 **/
public class Merge extends Expression {
  
    protected Set queries = null;
    protected Set keys = null;
    protected Set ignore = null;

    public Merge(Set queries, Set keys, Set ignore) {
        this.queries = queries;
        this.keys = keys;
        this.ignore = ignore;
    }
  
    public Data eval(DataContext dc) {
        Data res = null;

        if ((queries != null) && (queries.size() > 0) && (keys != null)
                && (keys.size() > 0)) {
            try {
                Element root = new Element("merge");
                Document merge = new Document(root);

                for (Iterator iter = queries.iterator(); iter.hasNext();) {
                    boolean load = true;
                    Query q = (Query) iter.next();
                    DataContext c = q.createDataContext(dc, root);

                    if (c.hasData()) {
                        for (Iterator iter2 = keys.iterator(); iter2.hasNext();) {
                            String key = (String) iter2.next();

                            if (!c.knownField(key)) {
                                Log.warning(Merge.class,
                                        "" + this + " " + q
                                        + " doesn't contain the key field["
                                        + key + "]");
                                load = false;
                            }
                        }
                        if (load) {
                            Document qdata = c.getData();
                            Element qroot = new Element("query");

                            qroot.addAttribute(new Attribute("id", q.getName()));
                            qroot.appendChild(qdata.getRootElement().copy());
                            root.appendChild(qroot);
                        } else {
                            Log.warning(Merge.class,
                                    "" + this + " " + q + " isn't loaded");
                        }
                    } else {
                        Log.warning(Merge.class,
                                "" + this + " " + q
                                + " didn't result in any data");
                    }
                }

                String keystr = "";

                for (Iterator iter = keys.iterator(); iter.hasNext();) {
                    keystr += (String) iter.next();
                    if (iter.hasNext()) {
                        keystr += " ";
                    }
                }

                String ignorestr = "";

		if (ignore!=null) {
             	  for (Iterator iter = ignore.iterator(); iter.hasNext();) {
                    ignorestr += (String) iter.next();
                    if (iter.hasNext()) {
                      keystr += " ";
                    }
                  }
                }

                nux.xom.pool.XSLTransformFactory factory = new nux.xom.pool.XSLTransformFactory() {
                    protected String[] getPreferredTransformerFactories() {
                        return new String[] {
                            "net.sf.saxon.TransformerFactoryImpl",
                        };
                    }
                }; 
                XSLTransform xslt = factory.createTransform(
                        Merge.class.getResource("/merge.xsl").openStream(),
                        new URI(Merge.class.getResource("/merge.xsl").toString()));

                xslt.setParameter("keystr", keystr);
                xslt.setParameter("ignorestr", ignorestr);
                Nodes nodes = xslt.transform(merge);

                Element answer = new Element("merge");
                Document merged = new Document(answer);
                
                for (int i = 0; i < nodes.size(); i++) {
                    answer.appendChild(nodes.get(i));
                }

                res = new Data(merged);
            } catch (Exception e) {
                Log.error(Merge.class, "" + this + " couldn't merge the queries",
                        e);
            }
        }
        return res;
    }
  
    public boolean parameterized() {
        return false;
    }
  
    public Expression translate(java.util.Map m) {
        return this;
    }
  
    public boolean check() {
        if ((queries == null) || (queries.size() == 0)) {
            Log.error(Merge.class, "" + this + " hasn't any queries to merge");
            return false;
        }
        if ((keys == null) || (keys.size() == 0)) {
            Log.error(Merge.class,
                    "" + this + " doesn't know the key(s) to use during merging");
            return false;
        }
        return true;
    }
  
    public Element toXML() {
        return null;
    }

    public String toString() {
        return ("MERGE(...)");
    }
}
