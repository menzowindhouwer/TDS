package nl.uu.let.languagelink.tds.ipa;


import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.OutputURIResolver;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.sxpath.*;


public class Conversion {
  
    protected static HashMap conversionMaps = null;
    protected boolean verbose = true;

    public Conversion(boolean verbose) {
        verbose(verbose);
        if (conversionMaps == null) {
            conversionMaps = new HashMap();
            try {
                // Initialization of the maps ...
                XPathEvaluator eval = new XPathEvaluator();
                NodeInfo doc = eval.getConfiguration().buildDocument(
                        new StreamSource(
                                Console.class.getResource("/maps.xml").openStream()));

                ((IndependentContext)eval.getStaticContext()).setNamespaces(
                        (NodeInfo) doc.iterateAxis(Axis.CHILD).next());
                // loop over the maps
                java.util.List maps = eval.createExpression("/maps/characterMapping").evaluate(doc);

                if (maps != null) {
                    for (Iterator iter = maps.iterator(); iter.hasNext();) {
                        NodeInfo map = (NodeInfo) iter.next();
                        String  name = eval.createExpression("string(@description)").evaluateSingle(map).toString();
                        if ((name==null) || name.trim().equals(""))
                            name = eval.createExpression("string(@id)").evaluateSingle(map).toString();

                        conversionMaps.put(name, new Map(name, eval, map));
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "!ERROR:couldn't load the encoding conversion maps:" + e);
                e.printStackTrace(System.err);
            }
        }
    }
    
    public Conversion() {
        this(true);
    }
    
    public boolean verbose() {
        return verbose;
    }
    
    public void verbose(boolean verbose) {
        this.verbose = verbose;
    }

    public java.util.List getMapNames() {
        return new Vector(conversionMaps.keySet());
    }
  
    public Map getMap(String name) {
        if (conversionMaps.containsKey(name)) {
            return (Map) conversionMaps.get(name);
        }
        return null;
    }
    
    public String convert(String name, String convert, boolean drop) {
        if (conversionMaps.containsKey(name)) {
            return ((Map) conversionMaps.get(name)).convert(convert, drop);
        }
        return null;
    }
    
    public String convert(String name, String convert) {
        return convert(name, convert, true);
    }

    public String lift(String name, String convert) {
        if (conversionMaps.containsKey(name)) {
            return ((Map) conversionMaps.get(name)).lift(convert);
        }
        return null;
    }
    
    public String font(String name) {
        if (conversionMaps.containsKey(name)) {
            return ((Map) conversionMaps.get(name)).font();
        }
        System.err.println("ERR: unknown map "+name);
        return null;
    }

    public class Map {
      
        protected String  name = "";
        protected String  font = null;
        protected HashMap conversionMap = null;
        protected int     base = 61440; // F000 ... ist this base the same for all maps?

        public Map(String name, XPathEvaluator eval, NodeInfo map) {
            this.name = name;
            conversionMap = new HashMap();
            try {
                font = eval.createExpression("string(@byte-font)").evaluateSingle(map).toString();
                //System.err.println("DBG: fount byte font "+font+" for map "+name);
                if ((font!=null) && font.trim().equals(""))
                    font = null;
                
                // loop over the assignments
                java.util.List assignments = eval.createExpression("./assignments/a[empty(@bactxt)]").evaluate(map);

                if (assignments != null) {
                    for (Iterator iter = assignments.iterator(); iter.hasNext();) {
                        NodeInfo a = (NodeInfo) iter.next();
                        String   b = eval.createExpression("string(@b)").evaluateSingle(a).toString();
                        String   u = eval.createExpression("string(@u)").evaluateSingle(a).toString();

                        if (((b != null) && !b.equals(""))
                                && ((u != null) && !u.equals(""))) {
                            if (!b.contains(" ")) {
                                conversionMap.put(b.toUpperCase(), u.toUpperCase());
                            } else {
                                System.err.println(
                                        "!ERROR:can't handle character sequences yet:b="
                                                + b + " u=" + u);
                            }
                        } else {
                            System.err.println(
                                    "!ERROR:skipping incomplete assignment:b="
                                            + b + " u=" + u);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(
                        "!ERROR:couldn't load the encoding conversion map["
                                + name + "]:" + e);
                e.printStackTrace(System.err);
            }
        }
        
        public String convert(String convert, boolean drop) {
            int base = this.base;

            if (!drop) {
                base = 0;
            }
            String converted = null;

            if (convert != null) {
                try {
                    if (verbose) {
                        System.err.println(
                                "INF: map["+name+"] convert[" + convert + "]"
                                + (drop ? " drop" : ""));
                    }
                    converted = "";
                    for (int i = 0; i < convert.length(); i++) {
                        int    o = convert.codePointAt(i);
                        String b = Integer.toHexString(o - base).toUpperCase();
                        String u = (String) conversionMap.get(b);

                        if (u == null) {
                            u = Integer.toHexString(o).toUpperCase();
                            if (verbose) {
                                System.err.println(
                                        "INF: kept codepoint [" + u + "/" + b
                                        + "]");
                            }
                        } else {
                            if (verbose) {
                                System.err.println(
                                        "INF: converted legacy codepoint [" + b
                                        + "] to Unicode codepoint [" + u + "]");
                            }
                        }
                        String[] cps = u.split(" ");

                        for (int j = 0; j < cps.length; j++) {
                            converted += new String(
                                    Character.toChars(
                                            Integer.parseInt(cps[j], 16)));
                        }
                    }
                    if (verbose) {
                        System.err.println("INF: converted[" + converted + "]");
                    }
                } catch (Exception e) {
                    System.err.println("!ERROR:conversion failed:" + e);
                    e.printStackTrace(System.err);
                }
            }
            return converted;
        }
        
        public String lift(String convert) {
            String converted = null;

            if (convert != null) {
                try {
                    if (verbose) {
                        System.err.println("INF: lift[" + convert + "]");
                    }
                    converted = "";
                    for (int i = 0; i < convert.length(); i++) {
                        int    o = convert.codePointAt(i);
                        String b = Integer.toHexString(o).toUpperCase();
                        String u = (String) conversionMap.get(b);

                        if ((u == null)/* || (Integer.parseInt(b,16)==Integer.parseInt(u,16))*/) {
                            if (verbose) {
                                System.err.println(
                                        "INF: kept codepoint [" + b + "]");
                            }
                        } else {
                            b = Integer.toHexString(o + base).toUpperCase();
                            if (verbose) {
                                System.err.println(
                                        "INF: lifted legacy codepoint ["
                                                + Integer.toHexString(o).toUpperCase()
                                                + "] to codepoint [" + b + "]");
                            }
                        }
                        converted += new String(
                                Character.toChars(Integer.parseInt(b, 16)));
                    }
                    if (verbose) {
                        System.err.println("INF: lifted[" + converted + "]");
                    }
                } catch (Exception e) {
                    System.err.println("!ERROR:lift failed:" + e);
                    e.printStackTrace(System.err);
                }
            }
            return converted;
        }
        
        public String font() {
            return font;
        }
        
    }
    
}
