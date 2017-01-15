package nl.uu.let.languagelink.tds.dtl.context;

import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import nu.xom.*;
import nu.xom.xslt.*;

/**
 * Access an JDBC accessible datasource
 **/
public class JDBCimport implements Accessor {
    
    protected Scope    scope  = null;
    protected Document access = null;
    
    protected Connection conn = null;

    public JDBCimport() {
        // used by the class loader
    }
    
    public JDBCimport(Scope scope,Document access) {
        this.scope  = scope;
        this.access = access;
        
        // open a JDBC connection to the database
        String driver = null;
        Nodes l = nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/driver");
        if (l.size()>0)
        	driver = l.get(0).getValue();
        else
        	Log.fatal(JDBCimport.class,"no driver specified for the JDBC connection");
        
        String url = null;
        l = nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/url");
        if (l.size()>0)
        	url = l.get(0).getValue();
        else
        	Log.fatal(JDBCimport.class,"no connection URL for the JDBC connection");

        String login = "";
        l = nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/login");
        if (l.size()>0)
        	login = l.get(0).getValue();

        String password = "";
        l = nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/password");
        if (l.size()>0)
        	password = l.get(0).getValue();

        try {
        	Class.forName(driver);
        } catch(java.lang.ClassNotFoundException e) {
		Log.fatal(JDBCimport.class,"couldn't load the JDBC driver["+driver+"]",e);
	}
        
        try {
        	if (!login.equals(""))
        		this.conn = DriverManager.getConnection(url,login,password);
        	else
        		this.conn = DriverManager.getConnection(url);
        } catch(SQLException e) {
		Log.fatal(JDBCimport.class,"couldn't connect to the DBMS["+url+"]",e);
	}
    }
    
    private String fieldname(String field) {
    	    String res = field;
    	    Nodes l = nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/replace");
    	    for (int i = 0;i<l.size();i++) {
    	    	    Node n = l.get(i);
    	    	    String from = nux.xom.xquery.XQueryUtil.xquery(n,"@from").get(0).getValue();
    	    	    String to   = nux.xom.xquery.XQueryUtil.xquery(n,"@to").get(0).getValue();
    	    	    res = res.replaceAll(from,to);
    	    }
    	    return res;
    }
        
    public Document execQuery(String q) {
    	if (this.conn == null)
    	    	    Log.fatal(JDBCimport.class,"no JDBC connection to execute the SQL query["+q+"]");
    	// execute an SQL query through the JDBC connection 
	Document res = null;
        if ((q!=null) && (!q.trim().equals(""))) {
        	
        	try {
        		Statement stmt       = this.conn.createStatement();
        		ResultSet rs         = stmt.executeQuery(q);
        		ResultSetMetaData md = rs.getMetaData();
        		
        		Element db = new Element("database");
        		res = new Document(db);
        		Element answer = new Element("answer");
        		db.appendChild(answer);
        		answer.addAttribute(new Attribute("resource",scope.getName()));
        		answer.addAttribute(new Attribute("table","JDBC query"));
        		
        		int i = 0;
        		while (rs.next()) {
        			Element tuple = new Element("tuple");
        			answer.appendChild(tuple);
        			tuple.addAttribute(new Attribute("row",""+(++i)));
        			
        			for (int c = 1;c <= md.getColumnCount();c++) {
        				String val = rs.getString(c);
        				if (val != null) {
        					Element field = new Element("field");
        					field.addAttribute(new Attribute("name",fieldname(md.getColumnName(c))));
        					field.appendChild(val);
        					tuple.appendChild(field);
        				}
        		        }
        		}
        	} catch(SQLException e) {
        		Log.fatal(JDBCimport.class,"couldn't execute the SQL query["+q+"]",e);
        	}
        	
        }
        return res;
    }
    
    public boolean hasDefaultQuery() {
        return (nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/query").size()>0);
    }
    
    public Document execDefaultQuery() {
    	    return execQuery(nux.xom.xquery.XQueryUtil.xquery(access,"/jdbc/query").get(0).getValue());
    }
    
    public Accessor newInstance(Scope scope,Document access) {
        return new JDBCimport(scope,access);
    }
    
    public void finalize() {
    	    if (this.conn != null) {
    	    	    try {
    	    	    	    this.conn.close();
    	    	    	    this.conn = null;
    	    	    } catch(SQLException e) {
    	    	    	    Log.fatal(JDBCimport.class,"couldn't close the JDBC connection",e);
    	    	    }
    	    }
    }
    
}
