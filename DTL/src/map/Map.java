package nl.uu.let.languagelink.tds.dtl.map;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.data.*;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.context.Scope;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The Map interface.
 **/
public interface Map {
    
    /* General accessors */
    public Scope getScope();
    public String getName();
    public int getLength();

    /* Access the parameters */
    public int getSize();
    public boolean isParam(Variable p);
    public Param getParam(int index);
    public Param getParam(String name);
    
    /* Access the values */
    public java.util.Map getValues();
    public Annotation getAnnotation(Value v);
    
    /* use the map */
    public UseMap useMap(Scope s, List p);
    public UseMap useMap(Scope s, Value val);
    public UseMap useMap(Scope s);
    
    /* import the map into another map */
    public BaseMap importMap(BaseMap m, java.util.Map t);
    
    /* Handle lookup(s) in the map */
    public List lookups(DataContext c);
    public Data lookup(DataContext c);

    /* Get the datatype of the lookup result */
    public String getDataType();
    
    /* Semantic check of the map */
    public boolean parameterized();
    public boolean check();
    
    /* Create an XML dump of the map */
    public Element toXML();

    /* Print debug info about the map */
    public void debug(String indent, String incr);
    public void debug();
}
