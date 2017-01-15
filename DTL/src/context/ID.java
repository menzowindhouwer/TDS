package nl.uu.let.languagelink.tds.dtl.context;


/**
 * An ID consists of the (within a specific context unique) combination of
 * a scope and a name.
 **/
public class ID {

    public Scope scope = null;
    public String name = null;

    public ID(Scope scope, String name) {
        this.scope = scope;
        this.name = name;
    }

    public ID(String name) {
        this.name = name;
    }

    public boolean hasScope() {
        return (scope != null);
    }

    public String toString() {
        return ("" + (scope != null ? scope.getName() + ":" : "") + name);
    }
}

