package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.context.Query;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import nu.xom.*;
import nu.xom.xslt.*;


/**
 * The Split expression takes an value expression and splits its value based on a regular expression.
 **/
public class Split extends Expression {
  
    protected Expression expr   = null;
    protected Expression regexp = null;
    protected String name = null;

    public Split(Expression expr, Expression regexp,String name) {
        this.expr = expr;
        this.regexp = regexp;
        this.name = name;
    }
  
    public Data eval(DataContext c) {
        Data res = null;
        Data re = regexp.eval(c);

        if (re == null) {
            Log.error(Split.class,
                    "" + this + " regular expression couldn't be evaluated");
            return null;
        }
        
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(Split.class,
                    "" + this + " value expression couldn't be evaluated");
            return null;
        }
        
        Element split = new Element("split");
        Document splitted = new Document(split);
        if (e.getValue() != null) {
            String[] matches = null;
            try { 
                matches = e.getValue().toString().split(re.getValue().toString());
            } catch (PatternSyntaxException ex) {
                Log.error(Split.class,
                        "" + this + " evaluated regular expression["
                        + re.getValue().toString() + "] couldn't be compiled",
                        ex);
                return null;
            }

            if (matches.length==0) {
                Log.warning(Split.class,"" + this + " didn't result in any data");
            } else {
                Log.message(Split.class,"" + this + " resulted in "+matches.length+" "+name+" groups");
                Element answer = new Element("answer");
                split.appendChild(answer);
                    
                for (int i = 0;i<matches.length;i++) {
                    String match = matches[i];
                    Element tuple = new Element("tuple");
                    answer.appendChild(tuple);
                    tuple.addAttribute(new Attribute("row",""+(i+1)));
                    Element field = new Element("field");
                    tuple.appendChild(field);
                    field.addAttribute(new Attribute("name",name));
                    field.appendChild(match);
                }
            }
            
        }
        res = new Data(splitted);
        
        Log.debug(Split.class,"SPLIT result["+splitted.toXML()+"]");
        
        return res;
    }
  
    public boolean parameterized() {
        return false;
    }
  
    public Expression translate(java.util.Map m) {
        return this;
    }
  
    public boolean check() {
        if (expr == null) {
            Log.error(Split.class, "" + this + " hasn't a value expression");
            return false;
        }
        if (regexp == null) {
            Log.error(Split.class,
                    "" + this + " hasn't a regular expression");
            return false;
        }
        return (expr.check() && regexp.check());
    }
  
    public Element toXML() {
        return null;
    }

    public String toString() {
         return ("SPLIT(" + expr + "," + regexp + ")");
   }
}
