package nl.uu.let.languagelink.tds.dtl.data;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.expression.Marker;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;
import java.net.URL;


/**
 * The Data class describes one specific value. The value can be annotated
 * and marked. A trace of the sources of a value is kept.
 **/
public class Data {

    protected static boolean meta = false;
    protected static int IDs = 0;
    
    protected int id = IDs++;

    protected Object    value = null;
    protected String     note = null;
    protected Annotation  ann = null;
    protected List       srcs = new Vector();
    protected Set     markers = new HashSet();
    
    protected Data() {
    }

    public Data(Object value) {
        setValue(value);
    }
  
    public int getId() {
        return id;
    }
  
    public void setValue(Object value) {
        this.value = value;
    }
  
    public Object getValue() {
        return value;
    }

    public void setNote(String note) {
        if (meta) {
            this.note = note;
        }
    }
  
    public boolean hasNote() {
        return (note != null);
    }
  
    public String getNote() {
        return note;
    }
  
    public void setAnnotation(Annotation ann) {
        if ((this.ann != null) && !this.ann.isEmpty()
                && ((ann == null) || (ann.isEmpty()))) {
            Log.warning(Data.class,
                    "overwriting non-empty annotation of " + this
                    + " with an empty one");
        }
        this.ann = ann;
    }
  
    public boolean hasAnnotation() {
        return (ann != null);
    }
  
    public Annotation getAnnotation() {
        return ann;
    }
  
    public void addSource(Data src, boolean mark, boolean annotate) {
        if (src != null) {
            if (meta) {
                srcs.add(src);
            }
            if (mark && src.hasMarkers()) {
                addMarkers(src.getMarkers());
            }
            if (annotate && !hasAnnotation() && src.hasAnnotation()) {
                setAnnotation(src.getAnnotation());
            }
        }
    }
  
    public void addSource(Data src) {
        addSource(src, false, false);
    }
  
    public void addMarkedSource(Data src) {
        addSource(src, true, false);
    }
  
    public void addAnnotatedSource(Data src) {
        addSource(src, false, true);
    }

    public void addAnnotatedMarkedSource(Data src) {
        addSource(src, true, true);
    }
  
    public boolean hasSources() {
        return (srcs.size() > 0);
    }
  
    public Data getSource(int i) {
        Data src = null;

        if (i < srcs.size()) {
            src = (Data) srcs.get(i);
        }
        return src;
    }
  
    public List getSources() {
        return srcs;
    }

    public void addMarker(Marker mark) {
        if (mark != null) {
            markers.add(mark);
        }
    }
  
    public void addMarkers(Set markers) {
        if (markers != null) {
            this.markers.addAll(markers);
        }
    }
  
    public boolean hasMarkers() {
        return (markers.size() > 0);
    }
  
    public boolean hasMarker(Marker mark) {
        return markers.contains(mark);
    }
  
    public boolean hasMarkers(Set markers) {
        return this.markers.containsAll(markers);
    }
  
    public Set getMarkers() {
        return markers;
    }
  
    public boolean isEmpty() {
        return ((value == null) && (markers.size() == 0));
    }
  
    public String toString() {
        String res = "DATA[@" + id + "]";

        if (value != null) {
            res += "[=" + value + "]";
        } else {
            res += "[=<null>]";
        }
        if (markers.size() > 0) {
            res += "[~";
            for (Iterator iter = markers.iterator(); iter.hasNext();) {
                res += "" + (Marker) iter.next();
                if (iter.hasNext()) {
                    res += ",";
                }
            }
            res += "]";
        }
        if (note != null) {
            res += "[?" + note + "]";
        }
        if (ann != null) {
            res += "[??" + ann + "]";
        }
        if (srcs.size() > 0) {
            res += "[<@";
            for (Iterator iter = srcs.iterator(); iter.hasNext();) {
                res += "" + ((Data) iter.next()).getId();
                if (iter.hasNext()) {
                    res += ",";
                }
            }
            res += "]";
        }
        return res;
    }
}
