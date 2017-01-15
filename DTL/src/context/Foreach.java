package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.notion.LocalizedNotion;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The Foreach class manages the iteration over a query result.
 **/
public class Foreach {

    protected Query query = null;
    protected List  members = new Vector();
    
    public Foreach(Query q) {
        query = q;
    }

    public void addMember(LocalizedNotion n) {
        members.add(n);
    }

    public void addMember(Foreach q) {
        members.add(q);
    }

    /* the actual transform */
    public int build(DataContext dc, Element p, boolean r) {
        int children = 0;

        if (members.size() == 0) {
            Log.warning(Foreach.class,
                    "skip " + this + " as it doesn't have any members");
            return children;
        }
        Log.push("foreach query", this);
        DataContext c = query.createDataContext(dc, p);

        Log.push("query", query);
        if (query.query != null) {
            Log.message(Foreach.class, "query=" + query.query);
        }
        if (query.command != null) {
            Log.message(Foreach.class, "command=" + query.command);
        }
        if (c.hasData()) {
            while (c.loadDataRow()) {
                Log.debug(Foreach.class,
                        "transform row[" + c.getCurrentRow() + "] for " + this);
                Log.push("row[" + c.getCurrentRow() + "/" + c.getRowCount() + "]", null);
                for (Iterator iter = members.iterator(); iter.hasNext();) {
                    Object member = iter.next();

                    if (member instanceof LocalizedNotion) {
                        if (((LocalizedNotion) member).build(c, c.getRoot(), r)) {
                            children++;
                        } else {
                            Log.warning(Foreach.class,
                                "row[" + c.getCurrentRow() + "] of " + this
                              + " didn't succesfully transform");
                            c.dumpDataRow();
                        }
                    } else if (member instanceof Foreach) {
                        children += ((Foreach) member).build(c, c.getRoot(), r);
                    }
                }
                Log.pop();
                Log.debug(Foreach.class,
                        "transformed row[" + c.getCurrentRow() + "] for " + this);
            }
        }
        Log.pop();
        if (children == 0) {
            Log.warning(Foreach.class,
                    "there were no notions created for " + this);
        }
        query.readDataContext(c);
        Log.pop();
        return children;
    }

    /* Complete semantics */
    public boolean complete() {
        for (Iterator iter = members.iterator(); iter.hasNext();) {
            Object member = iter.next();

            if (member instanceof LocalizedNotion) {
                if (!((LocalizedNotion) member).complete()) {
                    return false;
                }
            } else if (member instanceof Foreach) {
                if (!((Foreach) member).complete()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /* Semantic check */
    public boolean check() {
        if (query != null) {
            if (!query.check()) {
                return false;
            }
        } else {
            Log.error(Foreach.class,
                    "" + this + " isn't associated with any query!");
            return false;
        }
        if (members.size() == 0) {
            Log.warning(Foreach.class, "" + this + " doesn't have any members!");
        }
        for (Iterator iter = members.iterator(); iter.hasNext();) {
            Object member = iter.next();

            if (member instanceof LocalizedNotion) {
                if (!((LocalizedNotion) member).check()) {
                    return false;
                }
            } else if (member instanceof Foreach) {
                if (!((Foreach) member).check()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public String toString() {
        return "FOREACH[" + query + "]";
    }
}
