package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * A note, which is part of an annotation. Notes can have a specific author, and
 * be of a specific type.
 **/
public class Note {

    protected Description descr = null;
    protected String type = null;
    protected String author = null;

    public Note(Description d) {
        descr = d;
    }

    public boolean setType(String t) {
        if (type == null) {
            type = t;
            return true;
        }
        return false;
    }

    public boolean setAuthor(String a) {
        if (author == null) {
            author = a;
            return true;
        }
        return false;
    }

    public Element toXML() {
        Element note = null;

        if (descr != null) {
            note = descr.toXML("note");
            if (note != null) {
                if (type != null) {
                    note.addAttribute(new Attribute("type", type));
                }
                if (author != null) {
                    note.addAttribute(new Attribute("author", author));
                }
            }
        }
        return note;
    }

    public void debug(String indent, String incr) {
        Log.debug(Note.class, indent + "- NOTE");
        if (type != null) {
            Log.debug(Note.class, indent + incr + "- type: " + type);
        }
        if (author != null) {
            Log.debug(Note.class, indent + incr + "- author: " + author);
        }
        if (descr != null) {
            descr.debug(indent + incr);
        }
    }
	
    public void debug() {
        debug("", "  ");
    }
}
