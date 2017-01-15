package nl.uu.let.languagelink.tds.dtl.map;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.data.*;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.expression.Error;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A BaseMap contains the actual transformation rules, and thus also executes
 * the actual value lookup operation.
 **/
public class BaseMap implements Map {
    
    /* private variables */

    protected Scope         scope = null;
    protected String        name = null;
    protected List			params = new Vector();

    protected java.util.Map oparams = null;

    protected java.util.Map	map = new HashMap();
    protected java.util.Map	values = new HashMap();
    
    protected List          checks = new Vector();
    protected List          messages = new Vector();

    protected Expression	fallback = null;
    protected Message		verbose = new Error(new Literal("undescribed value"));

    protected Message		clash = new Error(new Literal("multiple matches"));
    
    protected Message       nomatch = new Warning(new Literal("no match found"));
    protected Message       empty = new Warning(
            new Literal("unmarked null value is ignored"));
    protected Message       marked = new Message(
            new Literal("marked null value is returned"));
            
    protected String        datatype = null;
    
    /* constructor */
    public BaseMap(Scope scope, String name, List params) {
        this.scope = scope;
        this.name = name;
        if (params != null) {
            this.params = params;
        }
    }
    
    /* general accessors */
    public Scope  getScope() {
        return scope;
    }
    
    public String getName() {
        return name;
    }

    public int getLength() {
        return map.size();
    }
    
    /* parameter accessors */
    public int getSize() {
        return params.size();
    }

    public boolean isParam(Variable p) {
        return params.contains(p);
    }

    public Param getParam(int index) {
        if (index < getSize()) {
            return (Param) params.get(index);
        }
        return null;
    }

    public Param getParam(String name) {
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Param p = (Param) iter.next();

            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
    
    /* add mapping expressions */
    
    public void addCheck(Message msg, Expression check) {
        checks.add(check);
        messages.add(msg);
    }
    
    public boolean addMapping(Value v, Expression e) {
        return addMapping(v, e, null);
    }
    
    public boolean addMapping(Value v, Expression e, Annotation a) {
        boolean res = addAnnotation(v, a);

        if (res) {
            if (map.containsKey(v)) {
                e = new Or((Expression) map.get(v), e);
            }
            map.put(v, e);
        }
        return res;
    }
    
    /* value accessors */
    protected boolean addAnnotation(Value v, Annotation a) {
        boolean res = false;

        if (a == null) {
            a = new Annotation();
        }
        if (values.containsKey(v)) {
            res = ((Annotation) values.get(v)).merge(a);
        } else {
            values.put(v, a);
            res = true;
        }
        return res;
    }

    public Annotation getAnnotation(Value v) {
        return (Annotation) values.get(v);
    }
    
    public java.util.Map getAllValues() {
        return values;
    }
    
    public java.util.Map getValues() {
        java.util.Map res = new HashMap();

        for (Iterator iter = values.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();

            if (v instanceof Literal) {
                Annotation a = getAnnotation(v);

                if (!a.isDynamic()) {
                    res.put(v, a);
                }
                // TODO:derive an annotation with the static parts ...
            }
        }
        return res;
    }
    
    /* otherwise accessors */
    public boolean setOtherwise(Message m) {
        return setOtherwise(null, null, m);
    }

    public boolean setOtherwise(Expression fb, Annotation a) {
        return setOtherwise(fb, a, null);
    }

    public boolean setOtherwise(Expression fb, Annotation a, Message m) {
        this.fallback = fb;
        this.verbose = m;
        if (fb != null && (fb instanceof Value) && a != null) {
            return addAnnotation((Value) fb, a);
        } else if (a != null) {
			return false;
		}
        return true;
    }

    /* clash accessors */
    public boolean setClash(Message m) {
        this.clash = m;
        return true;
    }
    
    /* datatype accessors */
    public boolean setDataType(String type) {
        if (datatype == null) {
            datatype = type;
        }
        return (datatype!=null?datatype.equals(type):false);
    }
    
    public String getDataType() {
        if (datatype == null) {
            return "ENUM";
        }
        return datatype;
    }
    
    /* derive a new map from an old map */
    // use the translate map to translate the expressions in this map, and add them to the other map
    public BaseMap importMap(BaseMap m, java.util.Map translate) {
        for (ListIterator iter = checks.listIterator(); iter.hasNext();) {
            Message msg = (Message) messages.get(iter.nextIndex());
            Expression chk = (Expression) iter.next();

            msg = (Message) msg.clone().translate(translate);
            chk = chk.clone().translate(translate);
            m.addCheck(msg, chk);
        }

        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();
            Expression e = (Expression) map.get(v);
            Annotation a = getAnnotation(v);

            if (v instanceof Param) {
                v = (Value) translate.get(v);
            }
            e = e.clone().translate(translate);
            a = a.translate(translate);
            m.addMapping(v, e, a);
        }
      
        Log.debug(BaseMap.class,
                "translated and imported " + this + " into " + m);
        debug();
        Log.debug(BaseMap.class, "using the following  translation map:");
        for (Iterator iter = translate.keySet().iterator(); iter.hasNext();) {
            Object key = iter.next();

            Log.debug(BaseMap.class,
                    "translate " + key + " into " + translate.get(key));
        }
        Log.debug(BaseMap.class, "which resulted in " + m);
        m.debug();
      
        return m;
    }

    public BaseMap translateMap(List p) {
        BaseMap m = null;

        if (getSize() == p.size()) {
            List params = new Vector();

            for (Iterator iter = p.iterator(); iter.hasNext();) {
                Value v = (Value) iter.next();

                if (v instanceof Param) {
                    params.add(v);
                }
            }
            m = new BaseMap(getScope(), getName() + "'", params);
            
            java.util.Map translate = new HashMap();

            for (int i = 0; i < getSize(); i++) {
                translate.put(getParam(i), p.get(i));
            }
            
            m.track(oparams, translate); // keep track of what happens to the original parameters
            m = importMap(m, translate);
            
            if (m != null) {
                Expression fb = null;
                Annotation ab = null;
                Message    vb = null;

                if (fallback != null) {
                    fb = (Expression) fallback.translate(translate);
                    ab = getAnnotation((Value) fallback);
                    if (ab != null) {
                        ab = ab.translate(translate);
                    }
                }
                if (verbose != null) {
                    vb = (Message) verbose.translate(translate);
                }
                m.setOtherwise(fb, ab, vb);
                m.setClash(clash);
		m.setDataType(datatype);
            }
        }
          
        if (m == null) {
            String pl = "(";

            for (Iterator iter = p.iterator(); iter.hasNext();) {
                pl += iter.next();
                if (iter.hasNext()) {
                    pl += ",";
                }
            }
            pl += ")";
            Log.error(BaseMap.class,
                    "couldn't translate " + this + " using " + pl);
        } else {}
        return m;
    }

    /* use the map */
    public UseMap useMap(Scope s, List p) {
        if (p == null) {
            p = new Vector();
        }
        if (getSize() == p.size()) {
            return new UseMap(s, this, p);
        } else {
            Log.error(BaseMap.class,
                    "The " + this + " was wrongly referenced: expected "
                    + getSize() + " parameters, but got " + p.size() + ".");
        }
        return null;
    }

    public UseMap useMap(Scope s, Value val) {
        List p = null;

        if (val != null) {
            p = new Vector();
            p.add(val);
        }
        return useMap(s, p);
    }
    
    public UseMap useMap(Scope s) {
        if (getSize() != 1) {
            return null;
        }
        List p = new Vector();

        p.add(new FocusVariable());
        return useMap(s, p);
    }
    
    /* import a map into this map */
    public boolean importMap(Map m) {
        boolean err = false;
      
        // the import parameters should all be known in the current map
        for (int i = 0; i < m.getSize(); i++) {
            Param p = m.getParam(i);

            if (!this.isParam(p)) {
                Log.error(BaseMap.class,
                        "the import " + p + " is unknown in " + this);
                err = true;
                break;
            }
        }

        // translate the map and import the expressions
        if (!err) {
            java.util.Map translate = new HashMap();

            for (int i = 0; i < m.getSize(); i++) {
                translate.put(m.getParam(i), getParam(m.getParam(i).getName()));
            }
            m.importMap(this, translate);
        }

        return (!err);
    }
    
    /* keep track of the original parameters and what happened to them */
    void track(java.util.Map oparams, java.util.Map translate) {
        if (oparams == null) {
            this.oparams = translate;
        } else {
            this.oparams = new HashMap();
            for (Iterator iter = oparams.keySet().iterator(); iter.hasNext();) {
                Param oparam = (Param) iter.next();
                Value ovalue = (Value) oparams.get(oparam);

                if (translate.containsKey(oparam)) {
                    ovalue = (Value) translate.get(oparam);
                }
                this.oparams.put(oparam, ovalue);
            }
        }
    }
    
    protected void track(DataContext c) {
        if (c.getFocus() != null) {
            Log.message(BaseMap.class,
                    "focus=\"" + c.getFocus().getValue() + "\"");
        }
      
        if (oparams != null) {
            for (Iterator iter = oparams.keySet().iterator(); iter.hasNext();) {
                Param p = (Param) iter.next();
                Expression e = (Expression) oparams.get(p);
                Data d = e.eval(c);

                Log.message(BaseMap.class,
                        p.getName() + "=" + e + "="
                        + ((d!=null)?((d.getValue()!=null)?"\""+d.getValue()+"\"":"<null>"):"<NULL>"));
            }
        }
    }
    
    /* handle lookup(s) in the map */
    public List lookups(DataContext c) {
        Log.push("map", this);
		if (Log.debug())
			track(c);
      
        List matches = new Vector(); // CHECK: should this be a set, i.e. each matching value is only allowed once?
        Set markers = new HashSet();
      
        for (ListIterator iter = checks.listIterator(); iter.hasNext();) {
            Message msg = (Message) messages.get(iter.nextIndex());
            Expression chk = (Expression) iter.next();
        
            Data proof = chk.eval(c);

            if (proof == null) {
                track(c);
                Log.error(BaseMap.class, "" + chk + " couldn't be evaluated");
                Log.pop();
                return matches;
            }

            boolean check = false;

            if (proof.getValue() == null) {
                check = false;
            } else if (proof.getValue() instanceof Boolean) {
                check = ((Boolean) proof.getValue()).booleanValue();
            } else {
                check = true;
            }
        
            if (check) {
                if (msg != null) {
                    track(c);
                    msg.show(c);
                }
                if (msg instanceof Error) {
                    track(c);
                    Log.pop();
                    return matches;
                }
            }
        }
      
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();
            Expression e = (Expression) map.get(v);
            Annotation a = getAnnotation(v);
            Data value = v.eval(c);

            if (value == null) {
                track(c);
                Log.error(BaseMap.class, "" + v + " couldn't be evaluated");
                Log.pop();
                return matches;
            }
            
            Data decide = e.eval(c);

            if (decide == null) {
                track(c);
                Log.error(BaseMap.class, "" + e + " couldn't be evaluated");
                Log.pop();
                return matches;
            }
            
            boolean match = false;

            if (decide.getValue() == null) {
                match = false;
            } else if (decide.getValue() instanceof Boolean) {
                match = ((Boolean) decide.getValue()).booleanValue();
            } else {
                match = true;
            }
        
            if (match) {
                if (value instanceof DataMarker) {
                    markers.addAll(value.getMarkers());
                } else if (!value.isEmpty() || (v instanceof Ignore)) {
                    Data m = new Data(value.getValue());

                    m.addAnnotatedMarkedSource(value);
                    m.addMarkedSource(decide);
                    m.setNote("lookup in " + this);
            
                    if (a != null) {
                        a = a.eval(c);
                        if ((a != null) && !a.isEmpty()) {
                            m.setAnnotation(a);
                        }
                    }
            
                    matches.add(m);
                }
            }
        }
      
        if (markers.size() > 0) {
            if (matches.size() == 0) {
                Data m = new Data(null);

                m.addMarkers(markers);
                m.setNote("lookup in " + this);
                matches.add(m); // the result of the map is only the explicit requested markers
            } else {
                for (Iterator iter = matches.iterator(); iter.hasNext();) {
                    ((Data) iter.next()).addMarkers(markers);
                }
            }// add the explicit requested markers
        }
      
        // System.err.println("!MENZO:lookup in "+this+" resulted in "+matches.size()+" matches");

        if (matches.size() == 0) {
            if (fallback != null) {
                Data fb = fallback.eval(c);

                if (fb == null) {
                    track(c);
                    Log.error(BaseMap.class,
                            "" + fallback + " couldn't be evaluated");
                    Log.pop();
                    return matches;
                }
                Data m = new Data(fb.getValue());

                m.addAnnotatedMarkedSource(fb);
                m.setNote("fallback for lookup in " + this);
              
                if (fallback instanceof Value) {
                    Annotation a = getAnnotation((Value) fallback);

                    if (a != null) {
                        a = a.eval(c);
                        if ((a != null) && !a.isEmpty()) {
                            m.setAnnotation(a);
                        } else
							Log.warning(BaseMap.class,"empty fallback annotation");
                    }
                }
              
                matches.add(m);
                if (verbose != null) {
                    if (verbose instanceof Error) {
                        matches.clear();
                    }
                    track(c);
                    verbose.show(c);
                }
            } else if (c.getFocus() != null) {
                Data d = c.getFocus();

                if (d.getValue() == null) {
                    if (d.isEmpty()) {
                        if (empty != null) {
                            track(c);
                            empty.show(c);
                        }
                    } else {
                        if (marked != null) {
                            track(c);
                            marked.show(c);
                        }
                    }
                    Data m = new Data(d.getValue());

                    m.addMarkedSource(d);
                    m.setNote("lookup in " + this);
                    matches.add(m);
                    if (Log.debug()) {
                        track(c);
                    }
                    Log.debug(BaseMap.class,
                            "no match: returned the (marked) NULL value of the focus: "
                            + m);
                } else {
                    if (verbose != null) {
                        track(c);
                        verbose.show(c);
                    }
                }
            } else if ((oparams != null) && (oparams.size() == 1)) {
                Expression e = (Expression) oparams.values().toArray()[0];
                Data d = e.eval(c);

                if (d.getValue() == null) {
                    if (d.isEmpty()) {
                        if (empty != null) {
                            track(c);
                            empty.show(c);
                        }
                    } else {
                        if (marked != null) {
                            track(c);
                            marked.show(c);
                        }
                    }
                    Data m = new Data(d.getValue());

                    m.addMarkedSource(d);
                    m.setNote("lookup in " + this);
                    matches.add(m);
                    if (Log.debug()) {
                        track(c);
                    }
                    Log.debug(BaseMap.class,
                            "no match: returned the (marked) NULL value of the first original parameter: "
                            + m);
                } else {
                    if (verbose != null) {
                        track(c);
                        verbose.show(c);
                    }
                }
            } else {
                if (nomatch != null) {
                    track(c);
                    nomatch.show(c);
                }
            }
        }
        if (matches.size() > 1) {
            if (clash != null) {
                if (clash instanceof Error) {
                    matches.clear();
                }
                track(c);
                clash.show(c);
            }
        }
      
        // add the match annotations
        for (Iterator iter = matches.iterator(); iter.hasNext();) {
            Data m = (Data) iter.next();

            // System.err.println("!MENZO:"+this+" match:"+m);
            if (m.hasAnnotation()) {
                Annotation a = m.getAnnotation();

                if (!a.isEmpty()) {
                    addAnnotation(new Literal(m.getValue()), a);
                }
            }
        }
      
        Log.pop();
        return matches;
    }
    
    public Data lookup(DataContext c) {
        Data res = new Data(null);
        List matches = lookups(c);

        if (matches.size() > 1) {
            Log.error(BaseMap.class,
                    c.trace() + ":lookup in " + this
                    + " resulted in multiple matches, but there was only one expected");
        } else if (matches.size() == 1) {
            res = (Data) matches.get(0);
        }
        return res;
    }
    
    /* semantic checks */
    public boolean parameterized() {
        if (getSize() > 0) {
            return true;
        }
        for (ListIterator iter = checks.listIterator(); iter.hasNext();) {
            Message msg = (Message) messages.get(iter.nextIndex());
            Expression chk = (Expression) iter.next();

            if (msg.parameterized() || chk.parameterized()) {
                return true;
            }
        }
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();

            if (v.parameterized()) {
                return true;
            }
            Expression e = (Expression) map.get(v);

            if (e.parameterized()) {
                return true;
            }
        }
        if (fallback != null) {
            if (fallback.parameterized()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean check() {
        if (getSize() > 0) {
            Log.error(BaseMap.class,
                    "Not all parameters of " + this + " have been resolved!");
            return false;
        }
        for (ListIterator iter = checks.listIterator(); iter.hasNext();) {
            Message msg = (Message) messages.get(iter.nextIndex());
            Expression chk = (Expression) iter.next();

            if (msg.parameterized()) {
                Log.error(BaseMap.class,
                        "" + this
                        + " still contains parameters in a message expression: "
                        + msg);
                return false;
            }
            if (chk.parameterized()) {
                Log.error(BaseMap.class,
                        "" + this
                        + " still contains parameters in a check expression: "
                        + chk);
                return false;
            }
        }
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();

            if (v.parameterized()) {
                Log.error(BaseMap.class,
                        "" + this + " still contains parameters in a value: "
                        + v);
                return false;
            }
            Expression e = (Expression) map.get(v);

            if (e.parameterized()) {
                Log.error(BaseMap.class,
                        "" + this
                        + " still contains parameters in an expression: " + e);
                return false;
            }
        }
        if (fallback != null) {
            if (fallback.parameterized()) {
                Log.error(BaseMap.class,
                        "" + this
                        + " still contains parameters in the fallback expression: "
                        + fallback);
                return false;
            }
        }
        return true;
    }
    
    /* dump the map as XML */
    public Element toXML() {
        Element m = new Element("map");

        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Element e = new Element("entry");
            Value val = (Value) iter.next();
            Element v = val.toXML("value");

            if (v != null) {
                e.appendChild(v);
                Element w = new Element("when");
                Element x = ((Expression) map.get(val)).toXML();

                if (x != null) {
                    w.appendChild(x);
                    e.appendChild(w);
                }
            }
            if (e.getChildCount() > 0) {
                m.appendChild(e);
            }
        }
        return m;
    }
    
    /* print debug info */
    public void debug(String indent, String incr) {
        Log.debug(BaseMap.class, indent + "MAP: " + name);
        Log.debug(BaseMap.class, indent + "- size: " + getSize());
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            Log.debug(BaseMap.class, indent + "- parameter: " + iter.next());
        }
        int countdown = 10; 

        for (Iterator iter = map.keySet().iterator(); (countdown-- > 0)
                && iter.hasNext();) {
            Value v = (Value) iter.next();

            Log.debug(BaseMap.class,
                    indent + "- entry: " + v + " <- " + map.get(v));
            if (values.containsKey(v)) {
                Annotation a = (Annotation) values.get(v);

                if (a != null) {
                    a.debug(indent + incr, incr);
                }
            }
            if (iter.hasNext() && (countdown == 1)) {
                Log.debug(BaseMap.class, indent + "...");
            }
        }
        Log.debug(BaseMap.class, indent + "- otherwise: ");
        if (fallback != null) {
            Log.debug(BaseMap.class, indent + incr + "- fallback: " + fallback);
            if (values.containsKey(fallback)) {
                Annotation a = (Annotation) values.get(fallback);

                a.debug(indent + incr, incr);
            }
        }
        if (verbose != null) {
            Log.debug(BaseMap.class, indent + incr + "- message: " + verbose);
        }
    }

    public void debug() {
        debug("", "  ");
    }

    public String toString() {
        String res = "MAP[" + scope.getName() + ":" + name + "]";

        res += "(";
        for (Iterator iter = params.iterator(); iter.hasNext();) {
            res += iter.next();
            if (iter.hasNext()) {
                res += ",";
            }
        }
        res += ")";
        return res;
    }
}
