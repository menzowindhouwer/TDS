package nl.uu.let.languagelink.tds.dtl.notion;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.data.DataList;
import nl.uu.let.languagelink.tds.dtl.expression.*;
import nl.uu.let.languagelink.tds.dtl.map.Map;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * A localized notion always wraps an instantiated notion. While the instantiated
 * notion manages and collects the (semantic) structure of a notion and thus forms
 * the backbone of the meta data, the localized notion describes how a notion is
 * related to a specific database field. It thus forms the backbone of the
 * transformation process and manages the data.
 **/
public class LocalizedNotion extends Notion {

    protected InstantiatedNotion  notion = null;
    
    protected boolean	          inverse = false;

    protected Expression          key = null;
    protected Value		  base = null;

    protected Annotation          key_ann = null;
    protected Annotation          val_ann = null;

    protected Map                 keymap = null;

    protected Expression	  precond = null;
    protected Map		  valuemap = null;
    protected boolean             check_members = false;

    protected List                groups = new Vector();
    protected List                grpconds = new Vector();

    protected Stack               queries = new Stack();

    protected int                 instantiations = 0;

    protected static int          now = 0;
    protected int                 epoch = 0;

    protected Boolean             optional = null;
    protected boolean             general = false;

    protected Scope               local_scope = null;
    protected Context             context = null;

    public LocalizedNotion(Scope s, InstantiatedNotion n) {
        super(n.getScope(), n.getName() + "-" + (ids + 1));
        this.ntp = "LOCALIZED";
        this.notion = n;
        this.local_scope = s;
        n.addLocalization(this);
    }

    // wrap around the InstantiationContext
    
    public int base() {
        return notion.id();
    }

    public Scope getScope() {
        return notion.getScope();
    }
    
    public Scope getLocalScope() {
        return local_scope;
    }

    public String getName() {
        return notion.getName();
    }

    public InstantiatedNotion getInstantiationContext() {
        return notion;
    }
    
    public void createInverse(boolean inverse) {
	    this.inverse = inverse;
    }
    
    public boolean createInverse() {
	    return this.inverse;
    }

    public int setType(int t) {
        Log.error(LocalizedNotion.class, "can't set the type of " + this);
        return getType();
    }
    
    public int getType() {
        return notion.getType();
    }

    public boolean setOptional(boolean o) {
        boolean old = isOptional();

        if (isOptional()) {
            optional = new Boolean(o);
        }
        return old;
    }

    public boolean isOptional() {
        if (optional != null) {
            return optional.booleanValue();
        }
        return notion.isOptional();
    }
    
    public boolean setAbstract(boolean a) {
        if (a) {
            Log.error(LocalizedNotion.class,
                    "localized notions can never be abstract");
        }
        return false;
    }

    public boolean setGeneral(boolean g) {
        boolean old = general;

        general = g;
        return old;
    }

    public boolean isGeneral() {
        return general;
    }
    
    public boolean checkProperties(int tpe, boolean opt, boolean gen) {
        if (checkProperties(tpe, opt)) {
            return (isGeneral() == gen);
        }
        return false;
    }

    public boolean addAnnotation(Annotation a) {
        return notion.addAnnotation(a);
    }

    public Annotation getAnnotation() {
        return notion.getAnnotation();
    }
    
    public boolean setDataType(String t) {
        return notion.setDataType(t);
    }
    
    public String getDataType() {
        return notion.getDataType();
    }
    
    public boolean hasDataType() {
        return notion.hasDataType();
    }

    public boolean getEnum() {
        return notion.getEnum();
    }
    
    public boolean addValue(Value v, Annotation a) {
        return notion.addValue(v, a);
    }
    
    public boolean hasValues() {
        return notion.hasValues();
    }
    
    public boolean hasValue(Value v) {
        return notion.hasValue(v);
    }

    public java.util.Map getValues() {
        return notion.getValues();
    }

    public boolean addKey(Value k, Annotation a) {
        return notion.addKey(k, a);
    }
    
    public boolean hasKeys() {
        return notion.hasKeys();
    }

    public java.util.Map getKeys() {
        return notion.getKeys();
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
    
    public boolean setContext(Context c) {
        if (context == null) {
            context = c;
        }
        return (context == c);
    }
    
    public Context getContext() {
        return context;
    }

    // handle groups
    
    public List addGroup() {
        List group = new Vector();

        groups.add(group);
        grpconds.add(null);
        return group;
    }
    
    public boolean hasGroups() {
        return (groups.size() > 0);
    }
    
    public List currentGroup() {
        if (hasGroups()) {
            return (List) groups.get(groups.size() - 1);
        }
        return null;
    }
    
    public boolean setGroupCondition(Expression expr) {
        if (hasGroups()) {
            grpconds.set(grpconds.size() - 1, expr);
            return true;
        }
        return false;
    }
    
    // handle queries
    
    public boolean pushQuery(Foreach q) {
        boolean res = true;

        if (currentQuery() != null) {
            currentQuery().addMember(q);
        } else if (currentGroup() != null) {
            currentGroup().add(q);
        } else {
            res = false;
        }
        if (res) {
            queries.push(q);
        }
        return res;
    }
    
    public void popQuery() {
        if (!queries.empty()) {
            queries.pop();
        } else {
            Log.warning(LocalizedNotion.class,
                    "request for query popping ignored: the query stack is empty");
        }
    }
    
    public Foreach currentQuery() {
        if (!queries.empty()) {
            return (Foreach) queries.peek();
        }
        return null;
    }

    // handle members
    
    public boolean addMember(LocalizedNotion n) {
        boolean res = true;

        if (currentQuery() != null) {
            currentQuery().addMember(n);
        } else if (currentGroup() != null) {
            currentGroup().add(n);
        } else {
            res = false;
        }
        return res;
    }

    public LocalizedNotion addMember(Scope s, InstantiatedNotion in) {
        LocalizedNotion n = null;

        if (notion.hasMember(in.getID())) {
            n = new LocalizedNotion(s, in);
            if (!addMember(n)) {
                n = null;
            }
        } else {
            Log.error(LocalizedNotion.class,
                    "can't add a LocalizedNotion[" + in.getID()
                    + "] without a counter part in the wrapped " + notion);
        }
        return n;
    }

    public LocalizedNotion getMember(ID id) {
        Log.fatal(LocalizedNotion.class,
                "" + this + ".getMember(" + id
                + ") shouldn't be called the localized structure is currently too complex to find members");

        /*
         Log.warning(""+this+" could contain the same notion["+id+"] in multiple groups, returning the first one found.");
         for (Iterator iter=groups.iterator();iter.hasNext();) {
         List group=(List)iter.next();
         for (Iterator members = group.iterator(); members.hasNext();) {
         LocalizedNotion n = (LocalizedNotion)members.next();
         if (id.hasScope()) {
         if (n.getScope()!=id.scope)
         continue;
         }
         if (n.getName().equals(id.name))
         return n;
         }
         }
         */
        return null;
    }
    
    // set the key expression
    public boolean setKey(Expression k, Annotation a) {
        if (k == null) {
            return false;
        }
        if (key != null) {
            Log.error(LocalizedNotion.class,
                    "key expression is already declared for " + this);
            return false;
        }
        key = k;
        key_ann = a;
        if ((k instanceof Literal) && ((a == null) || !a.isDynamic())) {
            addKey((Literal) k, a);
        }
        return true;
    }
    
    public boolean hasKey() {
        return (key != null);
    }
    
    // set the pre condition
    public boolean setPreCondition(Expression expr) {
        if (precond == null) {
            precond = expr;
        }
        return (precond == expr);
    }
    
    // enable/disable the check for members
    public boolean checkMembers(boolean enable) {
        boolean res = true;

        if (enable) {
            check_members = enable;
        } else if (check_members) {
            res = false;
        }
        return res;
    }
    
    // set the base value
    public boolean setBaseValue(Value v) {
        return setBaseValue(v, null);
    }
    
    public boolean setBaseValue(Value v, Annotation a) {
        if (base == null) {
            base = v;
            val_ann = a;
            if ((v instanceof Literal) && ((a == null) || !a.isDynamic())) {
                addValue((Literal) v, a);
            }
        }
        return (base == v);
    }

    public boolean hasBaseValue() {
        return (base != null);
    }
    
    public Value getBaseValue() {
        return base;
    }

    // get the base value annotation
    public boolean hasBaseValueAnnotation() {
        return (val_ann != null);
    }

    public Annotation getBaseValueAnnotation() {
        return val_ann;
    }

    // set the value map
    public boolean setValueMap(Map m) {
        if (m == null) {
            return false;
        }
        if (valuemap != null) {
            Log.error(LocalizedNotion.class,
                    "" + this + " has already a value " + valuemap);
            return false;
        }
        if (m.parameterized()) {
            Log.error(LocalizedNotion.class,
                    "value " + m + " for " + this
                    + " has still parameters, these should all have been resolved");
            return false;
        }
        java.util.Map values = m.getValues();

        for (Iterator iter = values.keySet().iterator(); iter.hasNext();) {
            Value v = (Value) iter.next();
            Annotation a = (Annotation) values.get(v);

            if (!addValue(v, a)) {
                Log.error(LocalizedNotion.class,
                        "couldn't import " + v + " from " + m + " to " + this);
                return false;
            }
        }
        valuemap = m;
        return true;
    }

    public boolean hasValueMap() {
        if (valuemap == null) {
            return false;
        }
        return true;
    }

    public Map getValueMap() {
        return valuemap;
    }

    // set the key map
    public boolean setKeyMap(Map m) {
        if (m == null) {
            return false;
        }
        if (keymap != null) {
            Log.error(LocalizedNotion.class,
                    "" + this + " has already a key " + keymap);
            return false;
        }
        if (m.parameterized()) {
            Log.error(LocalizedNotion.class,
                    "value " + m + " for " + this
                    + " has still parameters, these should all have been resolved");
            return false;
        }
        java.util.Map keys = m.getValues();

        for (Iterator iter = keys.keySet().iterator(); iter.hasNext();) {
            Value k = (Value) iter.next();
            Annotation a = (Annotation) keys.get(k);

            if (!addKey(k, a)) {
                Log.error(LocalizedNotion.class,
                        "couldn't import " + k + " from " + m + " to " + this);
                return false;
            }
        }
        keymap = m;
        return true;
    }

    public boolean hasKeyMap() {
        if (keymap == null) {
            return false;
        }
        return true;
    }

    public Map getKeyMap() {
        return keymap;
    }

    // when the value map is translated we need to replace the old one with the new one
    // TODO: check that the new value map is derived from the previous one
    protected Map replaceValueMap(Map m) {

        if (valuemap == null) {
            Log.warning(LocalizedNotion.class,
                    "replace unset value map with " + m + " for " + this);
        }
        Map old = valuemap;

        valuemap = m;
        return old;
    }

    /* add a scope in which this notion appears */
    public void addScope(Scope s) {
        super.addScope(s);
        notion.addScope(s);
    }
    
    /* tell that the notion has data */
    public void hasData(boolean data) {
        notion.hasData(data);
    }
    
    /* add an instantiation */
    public int addInstantiation() {
	notion.addInstantiation();
	return ++instantiations;
    }

    /* delete an instantiation */
    public int delInstantiation() {
	notion.delInstantiation();
	return --instantiations;
    }

    /* build this notion from an actual data context */
    public boolean build(DataContext c, Element p, Data v, Data k, boolean root) {
        boolean proceed = true;
        boolean hoist = false;
      
        Log.debug(LocalizedNotion.class,
                "" + this + ".build(" + (v != null ? v : "<null>") + "," + (k != null ? k : "<null>") + ")");
      
        // check the when expression
        Boolean when = null;

        if (proceed) {
            if (precond != null) {
                Data w = precond.eval(c.setFocus(v));

                if (w == null) {
                    Log.error(LocalizedNotion.class,
                            "" + this + ":when expression " + precond
                            + " couldn't be evaluated");
                    proceed = false;
                } else if (!(w.getValue() instanceof Boolean)) {
                    Log.error(LocalizedNotion.class,
                            "" + this + ":when expression " + precond
                            + " didn't result in a boolean value");
                    proceed = false;
                } else {
                    Log.debug(LocalizedNotion.class, "" + this + ":when=" + w);
                    Log.message(LocalizedNotion.class,
                            "when=\"" + w.getValue() + "\"");
                    when = (Boolean) w.getValue();
                    if (when.booleanValue() != true) {
                        proceed = false;
                    }
                }
            } else {
                if ((getBaseValue() != null) || (valuemap != null)) { // we're expecting a value
                    if (v == null) { // but there isn't one
                        proceed = false;
                        Log.debug(LocalizedNotion.class,
                                "" + this
                                + ":skip instantiation a value was expected, but not found");
                    }
                } else if ((k == null) && isOptional()) { // we're not expecting a value and we don't have a key, but we're still optional, so skip this notion
                    proceed = false;
                }
            }
        }
      
        // only complain about an empty key when the when expression has been checked
        if (proceed && (k == null) && isRoot()) {
            Log.error(LocalizedNotion.class,
                    "" + this
                    + ":doesn't have a key, which a root should always have");
            proceed = false;
        }
      
        // are we allowed to instantiate this notion or not?
        if (!proceed && isOptional()) {
            if (v != null) {
                Log.error(LocalizedNotion.class,
                        "" + this
                        + ":can only hoist members up, not values like " + v);
                proceed = false;
            } else {
                hoist = true;
                proceed = true;
                Log.debug(LocalizedNotion.class,
                        "" + this
                        + ":may instantiate members as the notion is optional");
            }
        } else if (proceed && isRoot()) {
            root = true;
        } else if (proceed && !root) {
            Log.error(LocalizedNotion.class,
                    "" + this
                    + ":couldn't instantiate, there is no root to attach it to");
            proceed = false;
        }
      
        if (proceed) {
            Element n = p; // by default hoist n
          
            if (!hoist) {
                // create or lookup the node
                epoch = now++;
                addInstantiation();
                n = c.createNotion(p, this, k, instantiations, epoch);
            }
              
            // create the value
            if ((v != null) && !v.isEmpty()) {
                Log.debug(LocalizedNotion.class, "" + this + ":value=" + v);
                Element val = c.createValue(n, this, v , epoch);
            }
      
            // loop over the groups
            int children = 0;

            if (hasGroups()) {
                for (ListIterator iter = groups.listIterator(); iter.hasNext();) {
                    int grp = iter.nextIndex();
                    Expression grpcond = (Expression) grpconds.get(grp);
                    List group = (List) iter.next();
              
                    // check the group condition
                    boolean enter = true;

                    if (proceed && (grpcond != null)) {
                        Data w = grpcond.eval(c.setFocus(v));

                        if (w == null) {
                            Log.error(LocalizedNotion.class,
                                    "" + this + ":group[" + grp
                                    + "] when expression " + grpcond
                                    + " couldn't be evaluated");
                            enter = false;
                        } else if (!(w.getValue() instanceof Boolean)) {
                            Log.error(LocalizedNotion.class,
                                    "" + this + ":group[" + grp
                                    + "] when expression " + grpcond
                                    + " didn't result in a boolean value");
                            enter = false;
                        } else {
                            Log.debug(LocalizedNotion.class,
                                    "" + this + ":group[" + grp + "] when=" + w);
                            if (((Boolean) w.getValue()).booleanValue() != true) {
                                enter = false;
                            }
                        }
                    }
              
                    // create the group members
                    if (enter) {
                        Log.debug(LocalizedNotion.class,
                                "" + this + ":enter group[" + grp + "]:"
                                + children);
                        for (Iterator members = group.iterator(); members.hasNext();) {
                            Object member = members.next();

                            if (member instanceof LocalizedNotion) {
                                LocalizedNotion ln = (LocalizedNotion) member;

                                if (ln.build(c, n, root)) {
                                    children++;
                                }
                            } else if (member instanceof Foreach) {
                                Foreach q = (Foreach) member;

                                children += q.build(c, n, root);
                            } else {
                                Log.fatal(LocalizedNotion.class,
                                        "don't know how to handle " + member);
                            }
                        }
                        Log.debug(LocalizedNotion.class,
                                "" + this + ":leave group[" + grp + "]:"
                                + children);
                    }
                }
            }

            Log.debug(LocalizedNotion.class,
                    "" + this + ":key=" + k + " value=" + v + " children="
                    + children + " when=" + (when != null ? when : "<null>")
                    + " check_members=" + check_members);
            if ((k == null) && (v == null) && (children == 0)
                    && ((when == null) || (when.booleanValue() == false))) {
                proceed = false; // the node doesn't carry any info, so forget about it
            } else if (check_members && (children == 0)) {
                Log.warning(LocalizedNotion.class,
                        "" + this + ":no children so forget it");
                proceed = false; // the node should only be created when it has members
            }
          
            if (!proceed) {
                // forget this notion instantiation
                c.forget(n, epoch);
                // forget references to this notion instantiation
                c.forget(p, epoch);
                delInstantiation();
            } else {
                String src = c.getScope().getName();

                if (Arrays.binarySearch(
                        n.getAttribute("srcs").getValue().split(" +"), src)
                                < 0) {
                    n.addAttribute(
                            new Attribute("srcs",
                            n.getAttribute("srcs").getValue() + " " + src));
                }
                // tell this notion it has data
                hasData((k != null) || (v != null));
            }
        }
      
        Log.debug(LocalizedNotion.class,
                "" + this + ".build(" + (v != null ? v : "<null>") + "," + (k != null ? k : "<null>") + "):"
                + proceed);
        return proceed;
    }
    
    public boolean build(DataContext c, Element p, Data v, boolean root) {
        boolean result = false;
        
        boolean proceed = true;
      
        Log.debug(LocalizedNotion.class,
                "" + this + ".build(" + (v != null ? v : "<null>") + ")");
      
        if ((v != null) && v.isEmpty()) {
            Log.warning(LocalizedNotion.class,
                    "" + this + ":empty value is turned into a <null> value");
            v = null;
        }

        // get the key
        Data k = null;
        if (proceed && (key != null)) {
            k = key.eval(c.setFocus(v));
            if (k == null) {
                Log.error(LocalizedNotion.class,
                        "" + this + ":key value expression " + key
                        + " couldn't be evaluated");
                proceed = false;
            }
        }
        Log.debug(LocalizedNotion.class, "" + this + ":key=" + k);

        // turn the key value into a list
        DataList keyList = null;
        if (k != null) {
        	if (k instanceof DataList) {
        		keyList = (DataList)k;
        	} else {
        		keyList = new DataList(k);
        	}
        }
        Log.debug(LocalizedNotion.class, "" + this + ":key list=" + keyList);
        
        // complete the keys
        if (proceed && (keyList!=null)) {
        	for (ListIterator kiter = keyList.getList().listIterator();kiter.hasNext();) {
        		int  keyidx = kiter.nextIndex();
        		Data keyval = (Data)kiter.next();

        		if (keyval.getValue() != null) {
        			
        			// complete the key annotation
        			Annotation a = null;
        			if (key_ann != null) {
        				a = key_ann.eval(c);
        			} else {
        				a = keyval.getAnnotation();
        			}
        			
        			// add the key value to the key enum
        			addKey(new Literal(keyval.getValue()), a);
        		}
        	}
        }
        Log.debug(LocalizedNotion.class, "" + this + ":completed key list=" + keyList);
        
        // for each possible key value try to build the complete notion
        if (proceed) {
        	if (keyList != null) {
			for (ListIterator kiter = keyList.getList().listIterator();kiter.hasNext();) {
				int  keyidx = kiter.nextIndex();
				Data keyval = (Data)kiter.next();
				
				if (keyval.getValue() != null) {
					Log.debug(LocalizedNotion.class, "" + this + ":key=" + keyval);
					Log.message(LocalizedNotion.class, "key=\"" + keyval.getValue() + "\"");
				} else {
					Log.debug(LocalizedNotion.class, "" + this + ":key is null");
					keyval = null;
				}
				if (build(c, p, v, keyval, root)) {
					result = true;
				}
			}
		} else {
			if (build(c, p, v, null, root)) {
				result = true;
			}
		}
        }
        
        Log.debug(LocalizedNotion.class,
                "" + this + ".build(" + (v != null ? v : "<null>") + "):"
                + result);
        return result;
    }
    
    public boolean build(DataContext c, Element p, boolean root) {
        boolean result = false;

        Log.push("notion", this);
        c.push(this);
        Log.debug(LocalizedNotion.class, "" + this + ".build()");
        
        // get the base value(s)
        Data value = null;
        if (getBaseValue() != null) {
            value = getBaseValue().eval(c);
            if (value == null) {
                Log.error(LocalizedNotion.class,
                        "" + this + ":base value expression " + getBaseValue()
                        + " couldn't be evaluated");
            } else {
                Log.debug(LocalizedNotion.class, "" + this + ":base=" + value);
                Log.message(LocalizedNotion.class,
                        "base=\"" + value.getValue() + "\"");
            }
        }
        
        // turn the base value into a list
        DataList valueList = null;
        if (value != null) {
        	if (value instanceof DataList) {
        		valueList = (DataList)value;
        	} else {
        		valueList = new DataList(value);
        	}
        }
      
        // we're going to transform the base value(s) in a set
        Set values = new HashSet();

        // look the value up in the value map
        if (valuemap != null) {
        	if (valueList!=null) {
        		for (Iterator iter = valueList.getList().iterator();iter.hasNext();) {
        			List vals = valuemap.lookups(c.setFocus((Data)iter.next()));
        			// add the found values to the set
        			values.addAll(vals);
        		}
        	} else
        		values.addAll(valuemap.lookups(c.clearFocus()));
        } else if (valueList != null) {
        	values.addAll(valueList.getList());
        }
      
        // for each possible value try to build the complete notion
        if ((values != null) && (values.size() > 0)) {
            for (Iterator iter = values.iterator(); iter.hasNext();) {
                if (build(c, p, (Data) iter.next(), root)) {
                    result = true;
                }
            }
        } else if (build(c, p, null, root)) {
            result = true;
        }
        
        Log.debug(LocalizedNotion.class, "" + this + ".build():" + result);
        c.pop();
        Log.pop();
        return result;
    }
    
    public boolean complete() {
        if (!hasDataType() && (hasValueMap() || hasBaseValue())) {
            String dt = null;

            if (hasValueMap()) {
                // take the datatype from the map
                dt = getValueMap().getDataType();
                Log.debug(LocalizedNotion.class,
                        "" + this + " use map datatype[" + dt + "]");
            }
            if ((dt == null) && hasBaseValue()) {
                // ask the base value for its datatype
                if (getBaseValue() instanceof FieldVariable) {
                    FieldVariable field = (FieldVariable) getBaseValue();

                    if (getContext() != null) {
                        List list = getContext().getPreprocess();

                        for (int i = list.size(); i > 0; i--) {
                            Preprocess pp = (Preprocess) list.get(i - 1);

                            if (pp.willProcessField(field.getName())
                                    && (pp.getDataType() != null)) {
                                dt = pp.getDataType();
                                Log.debug(LocalizedNotion.class,
                                        "" + this + " use field preprocess[" + i
                                        + "] datatype[" + dt + "]");
                                break;
                            }
                        }
                    }
                }
            }
            if ((dt == null) && getScope().hasDefaultDataType()) {
                // take the default type from the scope
                dt = getScope().getDefaultDataType();
                Log.debug(LocalizedNotion.class,
                        "" + this + " use default scope datatype[" + dt + "]");
            }
            if ((dt == null) && hasValues()) {
                // there are predefined values, so we assume an enumeration is wanted
                dt = "ENUM";
                Log.debug(LocalizedNotion.class,
                        "" + this + " predefined values so use datatype[" + dt
                        + "]");
            }
            if (dt == null) {
                // hardcoded default datatype
                dt = "FREE";
                Log.debug(LocalizedNotion.class,
                        "" + this + " use hardcoded default datatype[" + dt
                        + "]");
            }
            setDataType(dt);
        } else {
            Log.debug(LocalizedNotion.class,
                    "" + this + " use explicit datatype[" + getDataType() + "]");
        }
        if (hasGroups()) {
            for (ListIterator iter = groups.listIterator(); iter.hasNext();) {
                List group = (List) iter.next();

                for (Iterator members = group.iterator(); members.hasNext();) {
                    Object member = members.next();

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
            }
        }
        return true;
    }

    public boolean check() {
        if (getBaseValue() != null) {
            if (getBaseValue().parameterized()) {
                Log.error(LocalizedNotion.class,
                        "" + this
                        + " still contains parameters in the NOTION IS expression: "
                        + getBaseValue());
                return false;
            }
        }
        if (valuemap != null) {
            if (!valuemap.check()) {
                Log.error(LocalizedNotion.class,
                        "" + this + " contains an inconsistent value map: "
                        + valuemap);
                return false;
            }
        }
        if (key != null) {
            if (key.parameterized()) {
                Log.error(LocalizedNotion.class,
                        "" + this
                        + " still contains parameters in the NOTION KEY expression: "
                        + key);
                return false;
            }
        }
        if (precond != null) {
            if (precond.parameterized()) {
                Log.error(LocalizedNotion.class,
                        "" + this
                        + " still contains parameters in the NOTION WHEN expression: "
                        + precond);
                return false;
            }
        }
        if (hasGroups()) {
            for (ListIterator iter = groups.listIterator(); iter.hasNext();) {
                int grp = iter.nextIndex();
                Expression grpcond = (Expression) grpconds.get(grp);

                if (grpcond != null) {
                    if (grpcond.parameterized()) {
                        Log.error(LocalizedNotion.class,
                                "" + this
                                + " still contains parameters in the NOTION GROUPS WHEN expression: "
                                + grpcond);
                        return false;
                    }
                }
                List group = (List) iter.next();

                for (Iterator members = group.iterator(); members.hasNext();) {
                    Object member = members.next();

                    if (member instanceof LocalizedNotion) {
                        if (!((LocalizedNotion) member).check()) {
                            return false;
                        }
                    } else if (member instanceof Foreach) {
                        if (!((Foreach) member).check()) {
                            return false;
                        }
                    } else {
                        Log.fatal(LocalizedNotion.class,
                                "don't know how to check " + member);
                    }
                }
            }
        }
        return true;
    }
    
    public Element toXML() {
        Element mapping = null;

        if (Log.debug()) {
            mapping = new Element("mapping");
            mapping.addAttribute(
                    new Attribute("scope", getLocalScope().getName()));
            if (hasBaseValue() && (getBaseValue() instanceof FieldVariable)) {
                Element field = new Element("base");

                field.addAttribute(
                        new Attribute("source", getLocalScope().getName()));
                field.addAttribute(
                        new Attribute("field",
                        ((Variable) getBaseValue()).getName()));
                mapping.appendChild(field);
            }
            if (hasValueMap()) {
                Element map = getValueMap().toXML();

                if (map != null) {
                    mapping.appendChild(map);
                }
            }
        }
        return mapping;
    }
    
    public void debug(String indent, String incr) {
        super.debug(indent, incr);
        if (key != null) {
            Log.debug(LocalizedNotion.class, indent + "- key: " + key);
        }
        if (getBaseValue() != null) {
            Log.debug(LocalizedNotion.class, indent + "- is: " + getBaseValue());
        }
        if (precond != null) {
            Log.debug(LocalizedNotion.class, indent + "- when: " + precond);
        }
        if (hasGroups()) {
            for (ListIterator iter = groups.listIterator(); iter.hasNext();) {
                int grp = iter.nextIndex();
                Expression grpcond = (Expression) grpconds.get(grp);
                List group = (List) iter.next();

                Log.debug(LocalizedNotion.class,
                        indent + "- group " + grp + ": ");
                if (grpcond != null) {
                    Log.debug(LocalizedNotion.class,
                            indent + incr + "- when: " + grpcond);
                }
                for (Iterator members = group.iterator(); members.hasNext();) {
                    Notion n = (Notion) members.next();

                    n.debug(indent + incr, incr);
                }
            }
        }
    }
}
