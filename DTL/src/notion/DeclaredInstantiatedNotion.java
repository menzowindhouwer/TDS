package nl.uu.let.languagelink.tds.dtl.notion;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.expression.Literal;
import nl.uu.let.languagelink.tds.dtl.expression.Value;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * An instantiation of a previously declared notion. It wraps and absorbs
 * (e.g. values) the declared notion.
 **/
public class DeclaredInstantiatedNotion extends InstantiatedNotion {

    protected DeclaredNotion	notion = null;

    public DeclaredInstantiatedNotion(DeclaredNotion n) {
        // super(n.getScope(),n.getName()+"-"+(ids+1));
        super(null, null);
        this.ntp = "DECLARED INSTANTIATED";
        this.notion = n;
        for (Iterator iter = n.hints.keySet().iterator(); iter.hasNext();) {
            String  h = (String) iter.next();
            Literal l = (Literal) n.hints.get(h);

            addHint(h, l);
        }
        if (notion.hasValues()) {
            java.util.Map m = notion.getValues();

            for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
                Object o = iter.next();
                Annotation a = (Annotation) m.get(o);

                if (!addValue(new Literal(o), a)) {
                    Log.error(DeclaredInstantiatedNotion.class,
                            "couldn't import " + o + " value from " + notion
                            + " to " + this);
                }
            }
        }
        if (notion.hasKeys()) {
            java.util.Map m = notion.getKeys();

            for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
                Object o = iter.next();
                Annotation a = (Annotation) m.get(o);

                if (!addKey(new Literal(o), a)) {
                    Log.error(DeclaredInstantiatedNotion.class,
                            "couldn't import " + o + " key from " + notion
                            + " to " + this);
                }
            }
        }
    }

    public DeclaredInstantiatedNotion(ID id, DeclaredNotion n) {
        super(id.scope, id.name);
        this.ntp = "DECLARED INSTANTIATED";
        this.notion = n;
        if (notion.hasValues()) {
            java.util.Map m = notion.getValues();

            for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
                Object o = iter.next();
                Annotation a = (Annotation) m.get(o);

                if (!addValue(new Literal(o), a)) {
                    Log.error(DeclaredInstantiatedNotion.class,
                            "couldn't import " + o + " value from " + notion
                            + " to " + this);
                }
            }
        }
        if (notion.hasKeys()) {
            java.util.Map m = notion.getKeys();

            for (Iterator iter = m.keySet().iterator(); iter.hasNext();) {
                Object o = iter.next();
                Annotation a = (Annotation) m.get(o);

                if (!addKey(new Literal(o), a)) {
                    Log.error(DeclaredInstantiatedNotion.class,
                            "couldn't import " + o + " key from " + notion
                            + " to " + this);
                }
            }
        }
    }

    public int base() {
        return notion.base();
    }

    public Scope getScope() {
        if (scope == null) {
            return notion.getScope();
        }
        return scope;
    }

    public String getName() {
        if (name == null) {
            return notion.getName();
        }
        return name;
    }

    public DeclaredNotion getDeclarationContext() {
        return notion;
    }
    
    public int setType(int t) {
        Log.error(DeclaredInstantiatedNotion.class,
                "can't set the type of " + this);
        return getType();
    }
    
    public int getType() {
        return notion.getType();
    }

    public boolean setOptional(boolean o) {
        Log.error(DeclaredInstantiatedNotion.class,
                "can't set the optional flag of " + this);
        return notion.isOptional();
    }

    public boolean isOptional() {
        return notion.isOptional();
    }

    public boolean addAnnotation(Annotation a) {
        Log.error(DeclaredInstantiatedNotion.class,
                "can't set an annotation property of " + this);
        return false;
    }

    public Annotation getAnnotation() {
        return notion.getAnnotation();
    }
    
    public boolean setDataType(String t) {
        if (!hasDataType()) {
            return super.setDataType(t);
        }
        return false;
    }
    
    public String getDataType() {
        if (notion.hasDataType()) {
            return notion.getDataType();
        }
        return super.getDataType();
    }
    
    public boolean hasDataType() {
        if (!notion.hasDataType()) {
            return super.hasDataType();
        }
        return true;
    }
    
    public boolean getEnum() {
        if (notion.hasDataType()) {
            return notion.getEnum();
        }
        return super.getEnum();
    }
    
    public boolean setSuper(Notion sp) {
        if (notion.getSuper() == sp) {
            return true;
        }
        return false;
    }
    
    public Notion getSuper() {
        return notion.getSuper();
    }

    public boolean addMember(InstantiatedNotion n) {
        Log.debug(DeclaredInstantiatedNotion.class,
                "DeclaredInstantiatedNotion(" + this
                + ").addMember(InstantiatedNotion=" + n + ")");
        return addMember((Notion) n);
    }
    
    public void addScope(Scope s) {
        super.addScope(s);
        notion.addScope(s);
    }
}
