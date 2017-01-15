package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * The Memo (lookup) operation maintains a set of named lookup tables.
 * A call with the same list of parameters will always return the same
 * value.
 **/
public class Memo extends Value {
  
    static protected HashMap maps = new HashMap();
    protected String         name = null;
    protected List           keys = null;
    protected Expression     base = null;
    protected boolean      lookup = false;
    protected boolean    tolerant = false;
    
    public Memo(String name, List keys, boolean lookup,boolean tolerant) {
        this.name = name;
        this.lookup = lookup;
        this.tolerant = tolerant;
        MemoMap m = map(name);

        if (keys.size() < 1) {
            Log.fatal(Memo.class,
                    "" + this
                    + " memo (lookup) needs at least one parameter (next to the memo map name)");
        }
        if (!lookup && (m.nrOfKeys() == 0)) {
            // nr of keys is still unspecified, so its a new map
            int nrOfKeys = keys.size();

            if (nrOfKeys > 1) {
                if (!lookup) {
                    nrOfKeys--;
                    m.nrOfKeys(nrOfKeys);
                }
            } else {
                Log.warning(Memo.class,
                        "" + this
                        + " assuming an implicit memo lookup as we got only one parameter");
                this.lookup = true;
            }
        }
        if (lookup) {
            this.keys = keys;
        } else {
            this.base = (Expression) keys.get(keys.size() - 1);
            this.keys = keys.subList(0, keys.size() - 1);
        }
        if (this.keys.size() != m.nrOfKeys()) {
	    if (m.nrOfKeys() > 0) {
          	  Log.error(Memo.class,
                    "" + this
                    + " recieved wrong number of parameters for memo (lookup) (expected "
                    + m.nrOfKeys() + " but got " + this.keys.size() + ")");
            } else if (lookup) {
		// probably the lookup is before the first memo call, so assume the nr of keys will be set lately
	    } else {
          	  Log.error(Memo.class,
                    "" + this
                    + " requested a memo map with zero keys!");
            }
        }
    }
    
    public MemoMap map(String name) {
        if (!maps.containsKey(name)) {
            maps.put(name, new MemoMap(name));
        }
        return (MemoMap) maps.get(name);
    }

    public Data eval(DataContext c) {
        java.util.List keys = new Vector();

        for (Iterator iter = this.keys.iterator(); iter.hasNext();) {
            Expression expr = (Expression) iter.next();
            Data key = expr.eval(c);

            if (key != null) {
				if (key.isEmpty()) {
                	Log.warning(Memo.class,
                        "" + this
                        + " "+expr+" of the memo (lookup) key parameters resulted in an empty value");
				}
                keys.add(key);
            } else {
                Log.error(Memo.class,
                        "" + this + " " + expr
                        + " of the memo (lookup) key parameters couldn't be evaluated");
            }
        }

        Data res = null;
        Data val = map(name).lookup(keys,this.base,c);

        if (val != null) {
            res = new Data(val.getValue());
            res.addAnnotatedMarkedSource(val);
            res.setNote(
                    "MEMO" + (lookup ? "-LOOKUP" : "") + "(" + name
                    + ") succeeded");
        }

        if (res == null) {
            if (lookup) {
		if (tolerant) {
                	Log.warning(Memo.class,
                        "MEMO-LOOKUP(" + name + ") failed, key("
                        + Arrays.toString(keys.toArray())
                        + ") doesn't exist (yet)");
		} else {
                	Log.error(Memo.class,
                        "MEMO-LOOKUP(" + name + ") failed, key("
                        + Arrays.toString(keys.toArray())
                        + ") doesn't exist (yet)");
		}
            } else {
                Log.error(Memo.class,
                        "MEMO(" + name
                        + ") failed, did you provide a default value?");
            }
            res = new Data(null);
            res.setNote(
                    "MEMO" + (lookup ? "-LOOKUP" : "") + "(" + name + ") failed");
        }
        return res;
    }
    
    public boolean parameterized() {
        if (keys != null) {
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                if (expr.parameterized()) {
                    return true;
                }
            }
        }
        if (base != null) {
            if (base.parameterized()) {
                return true;
            }
        }
        return false;
    }

    public Expression translate(java.util.Map m) {
        List k = new Vector();

        if (keys != null) {
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                k.add(expr.translate(m));
            }
            keys = k;
        }
        if (base != null) {
            base = base.translate(m);
        }
        return this;
    }

    public Memo clone() {
        Memo m = (Memo) super.clone();

        if (keys != null) {
            m.keys = new Vector();
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                Expression expr = (Expression) iter.next();

                m.keys.add(expr.clone());
            }
        }
        if (base != null) {
            m.base = base.clone();
        }
        return m;
    }

    public String toString() {
        return ("MEMO" + (lookup ? "-LOOKUP" : "") + "(" + name + ",...)");
    }

    class MemoMap {
        
        String  name = null;
        HashMap  map = new HashMap();
        int nrOfKeys = 0;
        
        public MemoMap(String name) {
            this.name = name;
        }
        
        int nrOfKeys() {
            return nrOfKeys;
        }
        
        void nrOfKeys(int nrOfKeys) {
            this.nrOfKeys = nrOfKeys;
        }
        
        Data lookup(List keys, Expression base, DataContext c) {
            Data res = null;

            if (keys.size() > nrOfKeys) {
                Log.warning(MemoMap.class,
                        "MEMOMAP[" + name + "] ignoring superflous keys");
                keys = keys.subList(0, nrOfKeys - 1);
            } else if (keys.size() < nrOfKeys) {
                Log.warning(MemoMap.class,
                        "MEMOMAP[" + name + "] padding lookup with NULL keys");
                for (int i = keys.size(); i < nrOfKeys; i++) {
                    keys.add(new Data(null));
                }
            }
            String key = name;

            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                Data data = (Data) iter.next();

                key += "+" + data.getValue();
            }
            if (map.containsKey(key)) {
                res = (Data) map.get(key);
                Log.debug(MemoMap.class,
                        "MEMOMAP[" + name + "](" + key + "," + base + "):old:"
                        + res);
            } else if ((base != null) && (c != null)) {
        	Data b = base.eval(c);
            	if (b == null) {
               	    Log.error(Memo.class,
                       	 "" + this + " the default value " + base
                        + " of the memo (lookup) couldn't be evaluated");
                } else {
                    map.put(key, b);
                    res = b;
                    Log.debug(MemoMap.class,
                        "MEMOMAP[" + name + "](" + key + "," + b + "):new:"
                        + res);
		}
            }
            return res;
        }
    }
}
