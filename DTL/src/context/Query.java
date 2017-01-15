package nl.uu.let.languagelink.tds.dtl.context;

import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;

import nu.xom.*;

/**
 * A Query expand a database specific query, executes it and loads the data into
 * a DataContext.
 **/
public class Query extends Context {

    protected String name = null;
    protected String query = null;
    protected String access = null;

    protected List body = new Vector();
    protected Expression command = null;
    
    public Query(String name,String access) {
        this.name = name;
        this.access = access;
    }

    public String getName() {
        return name;
    }
    
    public String getAccess() {
        return access;
    }

    public void addBody(Expression expr) {
        if (expr != null) {
            body.add(expr);
        }
    }
    
    public boolean hasBody() {
        return ((body != null) && (body.size() > 0));
    }

    protected String resolveBody(DataContext c) {
        String res = "";

        if (hasBody()) {
            for (Iterator iter = body.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();
                Data part = expr.eval(c);

                if (part != null) {
                    if (part.getValue() != null) {
                        res += part.getValue().toString();
                    }
                } else {
                    Log.error(Query.class,
                            "" + this + " " + expr
                            + " of the query body couldn't be evaluated");
                }
            }
        }
        return res;
    }
    
    public void setCommand(Expression c) {
        command = c;
    }
    
    public boolean hasCommand() {
        return (command != null);
    }
    
    protected Document evalCommand(DataContext c) {
        Document res = null;

        if (hasCommand()) {
            Data data = command.eval(c);

            if (data != null) {
                if (data.getValue() != null) {
                    if (data.getValue() instanceof Document) {
                        res = (Document) data.getValue();
                    } else {
                        Log.error(Query.class,
                                "evaluating query command " + command
                                + " didn't result in data");
                    }
                } else {
                    Log.error(Query.class,
                            "evaluating query command " + command
                            + " resulted in a NULL value");
                }
            } else { 
                Log.error(Query.class,
                        "query command " + command + " couldn't be evaluated");
            }
        }
        return res;
    }
    
    protected Document importData(DataContext c) {
        Document doc = null;

        if (hasBody()) {
            query = resolveBody(c);
            if (query != null) {
                doc = scope.getAccessor(getAccess()).execQuery(query);
            }
        } else if (hasCommand()) {
            doc = evalCommand(c);
        } else {
            Log.error(Query.class,
                    "useless query as the command to import the data is not given");
        }
        return doc;
    }
    
    protected boolean check() {
        if (!hasBody() && !hasCommand()) {
            Log.error(Query.class,
                    "" + this + " there is no command to get the data");
            return false;
        }
        if (hasCommand()) {
            if (!command.check()) {
                return false;
            }
        }
        return true;
    }
    
    public String toString() {
        return "QUERY[" + getScope() + ":" + getName() + "@" + getAccess() + "]";
    }
}
