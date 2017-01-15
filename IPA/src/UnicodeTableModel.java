package nl.uu.let.languagelink.tds.ipa;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.table.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.OutputURIResolver;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.sxpath.*;

public class UnicodeTableModel extends AbstractTableModel
{
    private final int COLS = 4;
    private final int CP   = 0;
    private final int CHAR = 1;
    private final int NAME = 2;
    
    private Map    index = null;
    private Vector cols  = null;
    private Vector grps  = null;
	private Vector data  = null;

	private int rows = 0;

	public UnicodeTableModel(String skip)
	{
		System.err.println("INF: loading Unicode codepoints");
        if (cols==null) {
            cols = new Vector();
            //cols.add("nr");
            cols.add("code");
            cols.add("codepoint");
            cols.add("name");
            cols.add("group");
        }
        if ((grps==null) || (data==null)) {
            index = new HashMap();
            grps = new Vector();
            data = new Vector();
            try {
                XPathEvaluator eval = new XPathEvaluator();
                NodeInfo doc = eval.getConfiguration().buildDocument(
                        new StreamSource(
                                Console.class.getResource("/codepoints.xml").openStream()));
				//System.err.println("DBG: loaded XML document describing Unicode codepoints");

                ((IndependentContext)eval.getStaticContext()).setNamespaces(
                        (NodeInfo) doc.iterateAxis(Axis.CHILD).next());
                java.util.List groups = eval.createExpression("/u/g").evaluate(doc);
                if (groups != null) {
					//System.err.println("DBG: loaded Unicode codepoint groups");
                    for (Iterator iter=groups.iterator();iter.hasNext();) {
                        NodeInfo group = (NodeInfo) iter.next();
                        String   name = eval.createExpression("string(@n)").evaluateSingle(group).toString();
                        grps.add(name);
                    }
                }
                java.util.List codepoints = eval.createExpression("/u/g/cp").evaluate(doc);
                if (codepoints != null) {
                    int r = 0;
                    for (Iterator iter=codepoints.iterator();iter.hasNext();) {
                        NodeInfo codepoint = (NodeInfo) iter.next();
                        String   code = eval.createExpression("string(@c)").evaluateSingle(codepoint).toString();
                        Hex      hex  = new Hex(code);
                        String   cdpt = new String(Character.toChars(Integer.parseInt(code,16)));
                        String   name = eval.createExpression("string(n)").evaluateSingle(codepoint).toString();
                        String   grp  = (String)grps.elementAt(Integer.parseInt(eval.createExpression("string(parent::g/count(preceding::g))").evaluateSingle(codepoint).toString()));
                        Vector   row  = new Vector();
                        if ((skip==null) || (!name.matches(skip))) {
                            //row.add(new Integer(r));
                            row.add(hex);
                            row.add(cdpt);
                            row.add(name);
                            row.add(grp);
                            data.add(row);
                            index.put(hex.toInteger(),new Integer(r));
                            r++;
							//if (r<50)
                            // 	System.err.println("DBG: [ "+code+", "+cdpt+", "+name+", "+grp+" ]");
                        }
                    }
                    rows = r;
                    System.err.println("INF: loaded "+rows+" Unicode codepoints");
                }                
            } catch(Exception e) {
                System.err.println("ERR: failed to load Unicode codepoints: "+e);
            }
		}
	}
    
    public UnicodeTableModel() {
        this(null);
    }

	public int getRowCount()
	{
		return rows;
	}

	public int getColumnCount()
	{
		return COLS;
	}

	public Object getValueAt(int row, int col) 
	{
		if (row < rows && col < COLS)
		{
			List l = (List)data.get(row);
			if (col < l.size())
				return l.get(col);
		}
		return null;
	}

	public String getColumnName(int col) {
		if (col < COLS)
			return (String)cols.get(col);
		return null;
	}
    
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
            //case 0: return Integer.class;
            case 0: return Hex.class;
            default: return String.class;
        }
    }
    
    public int getIndex(int cp) {
        Integer idx = (Integer)index.get(new Integer(cp));
        if (idx!=null)
            return idx.intValue();
        return -1;
    }
    
    public String getCharacter(int idx) {
        if (idx < rows) {
			List l = (List)data.get(idx);
            return (String)l.get(CHAR);
        }
        return null;
    }
    
    public String getName(int idx) {
        if (idx < rows) {
			List l = (List)data.get(idx);
            return (String)l.get(NAME);
        }
        return null;
    }
    
    public String getCodepoint(int idx) {
        if (idx < rows) {
			List l = (List)data.get(idx);
            return l.get(CP).toString();
        }
        return null;
    }
    
    class Hex implements Comparable<Hex> {
        protected int    i = 0;
        protected String s = "";
        
        public Hex(String h) {
            i = Integer.parseInt(h,16);
            s = Integer.toHexString(i).toUpperCase();
            while (s.length()<4)
                s = "0" + s;
            s = "U+"+s;
        }
        
        public int compareTo(Hex h) {
            return this.toInteger().compareTo(h.toInteger());
        }
        
        public Integer toInteger() {
            return new Integer(i);
        }
        
        public int toInt() {
            return i;
        }
        
        public String toString() {
            return s;
        }
    }

}
