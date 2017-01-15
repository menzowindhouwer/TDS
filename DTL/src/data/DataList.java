package nl.uu.let.languagelink.tds.dtl.data;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.expression.Marker;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.util.*;
import java.net.URL;


/**
 * The DataList class describes a list of values.
 *
 * In the future we want the DTL engine to be able to handle lists of values everywhere,
 * but for now it returns this class returns the data of the first element in the values.
 * However, parts of the egine that can already handle lists can now inspect if there is a list
 * available.
 **/
public class DataList extends Data {
	
	protected Vector values = new Vector();
	
	public DataList() {
	}
	
	public DataList(Object value) {
		addValue(value);
	}
	
	public DataList(Data data) {
		addData(data);
	}

	public Data addData(Data data) {
		values.add(data);
		return data;
	}
	
	public Data addValue(Object value) {
		return addData(new Data(value));
	}
	
	public Data getData() {
		return getHead(true);
	}
	
	public List getList() {
		return values;
	}

	public Data getHead(boolean warn) {
		if (values.size() == 0)
			Log.fatal(DataList.class,"" + this + ":doesn't contain any value");
		if (warn && (values.size()>1))
			Log.warning(DataList.class,"" + this + ":mimics a single value but actually contains multiple values");
		return (Data)values.firstElement();
	}
	
	// mimic the Data class, by wrapping the head of the last
	
	public int getId() {
		return getHead(true).getId();
	}
	
	public void setValue(Object value) {
		getHead(true).setValue(value);
    	}
    	
    	public Object getValue() {
    		return getHead(true).getValue();
    	}
    	
    	public void setNote(String note) {
    		getHead(true).setNote(note);
    	}
    	
    	public boolean hasNote() {
    		return getHead(true).hasNote();
    	}
    	
    	public String getNote() {
    		return getHead(true).getNote();
    	}
    	
    	public void setAnnotation(Annotation ann) {
    		getHead(true).setAnnotation(ann);
    	}
    	
    	public boolean hasAnnotation() {
    		return getHead(true).hasAnnotation();
    	}
    	
    	public Annotation getAnnotation() {
    		return getHead(true).getAnnotation();
    	}
    	
    	public void addSource(Data src, boolean mark, boolean annotate) {
    		getHead(true).addSource(src,mark,annotate);
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
		return getHead(true).hasSources();
	}
	
	public Data getSource(int i) {
		return getHead(true).getSource(i);
	}
	
	public List getSources() {
		return getHead(true).getSources();
	}
	
	public void addMarker(Marker mark) {
		getHead(true).addMarker(mark);
	}
	
	public void addMarkers(Set markers) {
		getHead(true).addMarkers(markers);
	}
	
	public boolean hasMarkers() {
		return getHead(true).hasMarkers();
	}
	
	public boolean hasMarker(Marker mark) {
		return getHead(true).hasMarker(mark);
	}
	
	public boolean hasMarkers(Set markers) {
		return getHead(true).hasMarkers(markers);
	}
	
	public Set getMarkers() {
		return getHead(true).getMarkers();
	}
  
	public boolean isEmpty() {
		return getHead(true).isEmpty();
	}
  
	public String toString() {
		String res = "DATALIST(";
		for (Iterator iter = values.iterator(); iter.hasNext();) {
			res += "" + (Data) iter.next();
			if (iter.hasNext()) {
			    res += ",";
			}
		}
		res += ")";
		return res;
	}
}
