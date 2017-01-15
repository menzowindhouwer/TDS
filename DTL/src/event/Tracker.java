package nl.uu.let.languagelink.tds.dtl.event;


/**
 * The Tracker interface allows to keep track of context switches.
 **/
public interface Tracker {
    public void push(String c, Object o);
    public void pop();
}
