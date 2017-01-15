package nl.uu.let.languagelink.tds.dtl.event;


/**
 * The Includer interface is used to ask if a sub DTL spec should be included
 * or skipped.
 **/
public interface Includer {

    public boolean include(String inc);

}

