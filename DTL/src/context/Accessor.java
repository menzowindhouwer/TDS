package nl.uu.let.languagelink.tds.dtl.context;


import nu.xom.Document;

/**
 * Interface to access a data source
 **/
public interface Accessor {
    public Accessor newInstance(Scope scope,Document access);
    public Document execQuery(String q);
    public boolean  hasDefaultQuery();
    public Document execDefaultQuery();
}
