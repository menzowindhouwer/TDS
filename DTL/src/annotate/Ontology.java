package nl.uu.let.languagelink.tds.dtl.annotate;


import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;
import java.util.*;

import nu.xom.*;


/**
 * The Ontology class provides access to the set of concepts in an OWL ontology.
 **/
public class Ontology {

    protected Reader ontology = null;
    protected String name = null;

    protected Set concepts = null;

    public Ontology(Reader onto, String name) {
        this.ontology = onto;
        this.name = name;
        load();
    }

    private void load() {
        try {
            
            Document onto = new Builder().build(ontology);
            Nodes l = nux.xom.xquery.XQueryUtil.xquery(onto.getRootElement(),
                    "declare namespace owl=\"http://www.w3.org/2002/07/owl#\";"
                    + "declare namespace rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\";"
                    + "  distinct-values(" + "    for    $id"
                    + "    in     //owl:Class/(@rdf:ID union @rdf:about)"
                    + "    return substring-after("
                    + "             resolve-uri("
                    + "               if   (contains($id,':') or starts-with($id,'#'))"
                    + "               then ($id)"
                    + "               else concat('#',$id)" + "              ,"
                    + "               /rdf:RDF/@xml:base" + "             )"
                    + "            ," + "             '#'" + "           )"
                    + "  )[normalize-space(.)!='']");

            if (l.size() > 0) {
                concepts = new HashSet();
                for (int i = 0; i < l.size(); i++) {
                    concepts.add(l.get(i).getValue());
                }
            } else {
                Log.error(Ontology.class,
                        "found no concepts in ontology[" + name + "]");
            }
        } catch (Exception e) {
            Log.error(Ontology.class,
                    "couldn't load concepts from ontology[" + name + "]", e);
        }
    }

    public boolean isConcept(String concept) {
        if (concepts != null) {
            return concepts.contains(concept);
        }
        return false;
    }
}
