package nl.uu.let.languagelink.tds.dtl;


import nl.uu.let.languagelink.tds.dtl.event.*;

import java.util.*;


/**
 * General static log class, passes all messages on to registered listeners.
 **/
public class Log {
    
    static protected boolean debug = false;
    static protected boolean message = true;
    static protected boolean warning = true;
    
    static protected int     errmax = 0;
    static protected int     errcnt = 0;
    
    static protected java.util.Set listeners = new HashSet();
    
    static public void addListener(Listener l) {
        listeners.add(l);
    }

    static public void delListener(Listener l) {
        listeners.remove(l);
    }
    
    static public int errorMaximum() {
        return errmax;
    }
    
    static public void errorMaximum(int max) {
        if (max > 0) {
            errmax = max;
        } else {
            errmax = 0;
        }
    }
    
    static public void reset() {
        errcnt = 0;
    }
    
    static protected void addError() {
        errcnt++;
        if ((errmax > 0) && (errcnt >= errmax)) {
            fatal(Log.class, "too many errors", null);
        }
    }
    
    static public boolean debug() {
        return debug;
    }
    
    static public void debug(boolean d) {
        debug = d;
    }
    
    static public void debug(Class c, String m) {
        debug(c, m, null);
    }
    
    static public void debug(Class c, Exception e) {
        debug(c, null, e);
    }
    
    static public void debug(Class c, String m, Exception e) {
        if (debug()) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ((Listener) iter.next()).debug(c, m, e);
            }
        }
    }
    
    static public boolean message() {
        return message;
    }
    
    static public void message(boolean m) {
        message = m;
    }
    
    static public void message(Class c, String m) {
        message(c, m, null);
    }
    
    static public void message(Class c, Exception e) {
        message(c, null, e);
    }
    
    static public void message(Class c, String m, Exception e) {
        if (message()) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ((Listener) iter.next()).message(c, m, e);
            }
        }
    }
    
    static public boolean warning() {
        return warning;
    }
    
    static public void warning(boolean w) {
        warning = w;
    }
    
    static public void warning(Class c, String m) {
        warning(c, m, null);
    }
    
    static public void warning(Class c, Exception e) {
        warning(c, null, e);
    }
    
    static public void warning(Class c, String m, Exception e) {
        if (warning()) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ((Listener) iter.next()).warning(c, m, e);
            }
        }
    }
    
    static public void error(Class c, String m) {
        error(c, m, null);
    }
    
    static public void error(Class c, Exception e) {
        error(c, null, e);
    }
    
    static public void error(Class c, String m, Exception e) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ((Listener) iter.next()).error(c, m, e);
        }
        addError();
    }
    
    static public void fatal(Class c, String m) {
        fatal(c, m, null);
    }
    
    static public void fatal(Class c, Exception e) {
        fatal(c, null, e);
    }
    
    static public void fatal(Class c, String m, Exception e) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ((Listener) iter.next()).fatal(c, m, e);
        }
        throw new FatalException(m, e);
    }
    
    static protected java.util.Set trackers = new HashSet();

    static public void addTracker(Tracker t) {
        trackers.add(t);
    }

    static public void delTracker(Tracker t) {
        trackers.remove(t);
    }
    
    static public void push(String c, Object o) {
        for (Iterator iter = trackers.iterator(); iter.hasNext();) {
            ((Tracker) iter.next()).push(c, o);
        }
    }

    static public void pop() {
        for (Iterator iter = trackers.iterator(); iter.hasNext();) {
            ((Tracker) iter.next()).pop();
        }
    }
}
