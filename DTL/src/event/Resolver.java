package nl.uu.let.languagelink.tds.dtl.event;


import java.io.Reader;


/**
 * The Resolver interface helps opening (relative) resource references.
 **/
public interface Resolver {

    public Reader resolve(String base, String rel);

}

