package nl.uu.let.languagelink.tds.dtl.context;

import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;

import nu.xom.*;

/**
 * Access a data source using DTLimport
 **/
public class DTLimport implements Accessor {
    
    protected Scope scope = null;
    protected Document access = null;
    
    public DTLimport() {
        // used by the class loader
    }
    
    public DTLimport(Scope scope,Document access) {
        this.scope = scope;
        this.access = access;
    }
    
    protected Document exec(String query) {
        Document doc = null;

        // TODO: should be a dump of this.access document
        File conf = scope.getEngine().dumpConfXML();
        if (conf != null) {
            try {
                String cmd[] = new String[4];

                cmd[0] = "DTLimport";
                cmd[1] = "--catalog=" + conf.getCanonicalPath();
                if (query!=null) {
                    cmd[2] = "--query";
                    cmd[3] = scope.getName() + ":" + query;
                } else {
                    cmd[2] = "--dump";
                    cmd[3] = scope.getName();
                }
                Process importer = Runtime.getRuntime().exec(cmd);

                doc = new Builder().build(importer.getInputStream());
                if (importer.waitFor() != 0) {
                    Log.error(DTLimport.class,
                        "DTLimport resulted in an error");
                }
            } catch (Exception exp) {
                Log.error(DTLimport.class,
                    "couldn't import data for scope[" + scope.getName() + "]:"
                    + exp);
            }
        } else {
            Log.error(DTLimport.class,
                "couldn't get access to the configuration");
        }

        return doc;
    }
    
    public Document execQuery(String q) {
        Document doc = null;
        if (scope!=null) {
            doc = exec(q);
        }
        return doc;
    }
    
    public boolean hasDefaultQuery() {
        Document access = scope.getAccess();
        String type = access.getRootElement().getLocalName();
        if (type.equals("csv")) {
            Nodes l = nux.xom.xquery.XQueryUtil.xquery(access.getRootElement(),"/csv/file[exists(@table)]");
            return (l.size()>0);
        } else if (type.equals("odbc")) {
            Nodes l = nux.xom.xquery.XQueryUtil.xquery(access.getRootElement(),"/odbc/table[exists(name)]");
            return (l.size()>0);
        } else if (type.equals("odbtp")) {
            Nodes l = nux.xom.xquery.XQueryUtil.xquery(access.getRootElement(),"/odbtp/table[exists(name)]");
            if (l.size()>0)
                return true;
            l = nux.xom.xquery.XQueryUtil.xquery(access.getRootElement(),"/odbtp/query");
            return (l.size()>0);
        }
        return false;
    }
    
    public Document execDefaultQuery() {
        Document doc = null;
        if (scope!=null) {
            doc = exec(null);
        }
        return doc;
    }
    
    public Accessor newInstance(Scope scope,Document access) {
        return new DTLimport(scope,access);
    }
}
