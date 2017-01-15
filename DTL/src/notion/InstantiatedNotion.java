package nl.uu.let.languagelink.tds.dtl.notion;


import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;
import nl.uu.let.languagelink.tds.dtl.context.*;
import nl.uu.let.languagelink.tds.dtl.expression.Expression;
import nl.uu.let.languagelink.tds.dtl.expression.Value;
import nl.uu.let.languagelink.tds.dtl.Log;

import java.util.*;

import nu.xom.*;


/**
 * The instantiated notion class forms the backbone of the meta data. It collects
 * information from all localized notions which can be scattered around the DTL spec
 * and forms thus the complete (semantic) notion.
 **/
public class InstantiatedNotion extends Notion {

    protected List localizations = new Vector();
    protected boolean data = false;
    protected int instantiations = 0;

    public InstantiatedNotion(Scope scope, String name) {
        super(scope, name);
        this.ntp = "INSTANTIATED";
    }
    
    public void addLocalization(LocalizedNotion n) {
        localizations.add(n);
    }

    public boolean addMember(InstantiatedNotion n) {
        Log.debug(InstantiatedNotion.class,
                "InstantiatedNotion(" + this + ").addMember(InstantiatedNotion="
                + n + ")");
        return addMember((Notion) n);
    }

    public DeclaredNotion getDeclarationContext() {
        Notion group = getGroup();

        while (group != null) {
            if (group instanceof DeclaredInstantiatedNotion) {
                return ((DeclaredInstantiatedNotion) group).getDeclarationContext();
            }
            group = group.getGroup();
        }
        return null;
    }

    public InstantiatedNotion addMember(DeclaredNotion d) {
        Log.debug(InstantiatedNotion.class,
                "InstantiatedNotion.addMember(DeclaredNotion=" + d + ")");
        InstantiatedNotion n = null;

        if (d != null) {
            n = new DeclaredInstantiatedNotion(d);
        }
        if (!addMember(n)) {
            n = null;
        }
        return n;
    }

    public InstantiatedNotion addMember(ID id) {
        InstantiatedNotion n = null;
        DeclaredNotion d = getDeclarationContext();

        if (d != null) {
            d = d.getMember(id);
        } else {
            n = id.scope.getInstantiationRoot(id.name);
        }
        if (n != null) {
            d = id.scope.getDeclarationRoot(id.name);
        }
        if (d != null) {
            return addMember(d);
        }
        if (n == null) {
            n = new InstantiatedNotion((id.hasScope() ? id.scope : getScope()),
                    id.name);
        }
        if (!addMember(n)) {
            n = null;
        }
        return n;
    }

    public InstantiatedNotion getMember(ID id) {
        return (InstantiatedNotion) super.getMember(id);
    }
    
    public void hasData(boolean data) {
        if (data) {
            this.data = true;
        }
    }

   public int addInstantiation() {
	return ++instantiations;
   }

   public int delInstantiation() {
	return --instantiations;
   }

    public Element toXML(boolean main) {
        Element notion = super.toXML(main);

        notion.addAttribute(new Attribute("data", (data ? "true" : "false")));
        notion.addAttribute(new Attribute("instantiations", ""+instantiations));
        for (Iterator iter = localizations.iterator(); iter.hasNext();) {
            LocalizedNotion ln = (LocalizedNotion) iter.next();

            /*
             if (ln.hasBaseValue() && (ln.getBaseValue() instanceof FieldVariable)) {
             Element field = doc.createElement("field");
             field.appendChild(doc.createTextNode(((Variable)ln.getBaseValue()).getName()));
             field.setAttribute("source",ln.getLocalScope().getName());
             notion.appendChild(field);
             }
             */
            Element mapping = ln.toXML();

            if (mapping != null) {
                notion.appendChild(mapping);
            }
        }
        return notion;
    }
}
