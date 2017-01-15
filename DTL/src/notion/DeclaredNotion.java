package nl.uu.let.languagelink.tds.dtl.notion;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.expression.Value;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;


/**
 * A declared notion.
 **/
public class DeclaredNotion extends Notion {

    public DeclaredNotion(Scope scope, String name) {
        super(scope, name);
        this.ntp = "DECLARED";
    }

    public boolean addMember(DeclaredNotion n) {
        return addMember((Notion) n);
    }

    public DeclaredNotion addMember(ID id) {
        DeclaredNotion n = null;

        if (!hasMember(id)) {
            if (id.hasScope()) {
                n = id.scope.declareNotion(id.name);
                if (!addMember(n)) {
                    Log.error(DeclaredNotion.class,
                            "failed to add " + n
                            + " as a member, while we're sure it isn't a duplicate!");
                    n = null;
                }
            }
        }
        return n;
    }

    public DeclaredNotion getMember(ID id) {
        return (DeclaredNotion) super.getMember(id);
    }
    
    public boolean setAbstract(boolean a) {
        boolean old = abstr;

        abstr = a;
        return old;
    }

    public boolean checkProperties(int tpe, boolean opt, boolean abs) {
        if (checkProperties(tpe, opt)) {
            return (isAbstract() == abs);
        }
        return false;
    }

}
