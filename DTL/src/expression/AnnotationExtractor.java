package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;
import nl.uu.let.languagelink.tds.dtl.annotate.Annotation;


/**
 * The Annotation operations extract a specific annotation from a value.
 **/
public class AnnotationExtractor extends Value {
  
    protected String     ann = "LABEL";
    protected Expression expr = null;

    public AnnotationExtractor(String ann, Expression e) {
        this.ann = ann;
        this.expr = e;
    }

    public Data eval(DataContext c) {
        Data res = null;
        Data e = c.getFocus();

        if (expr != null) {
            e = expr.eval(c);
        } 
        if (e == null) {
            Log.error(AnnotationExtractor.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (e.hasAnnotation()) {
            Annotation a = e.getAnnotation();

            if (ann.equals("LABEL OR VALUE")) {
                Data l = e;
				if (a.label != null)
                	l = a.label.eval(c);

                if (l != null) {
                    res = new Data(l.getValue());
                    res.addSource(e);
                    res.addMarkedSource(l);
                    res.setNote("GET-LABEL-OR-VALUE(" + e + ")");
                } else
					Log.warning(AnnotationExtractor.class,""+this+" didn't find a label or value");
            } else if (ann.equals("LABEL") && (a.label != null)) {
                Data l = a.label.eval(c);

                if (l != null) {
                    res = new Data(l.getValue());
                    res.addSource(e);
                    res.addMarkedSource(l);
                    res.setNote("GET-LABEL(" + e + ")");
                } else
					Log.warning(AnnotationExtractor.class,""+this+" didn't find a label");
            } else if (ann.equals("DESCRIPTION") && (a.description != null)) {
                Data d = a.description.eval(c);

                if (d != null) {
                    res = new Data(d.getValue());
                    res.addSource(e);
                    res.addMarkedSource(d);
                    res.setNote("GET-DESCRIPTION(" + e + ")");
                } else
					Log.warning(AnnotationExtractor.class,""+this+" didn't find a description");
            }
        }
        if (res == null) {
            res = new Data(null);
            res.addMarkedSource(e);
            if (ann.equals("LABEL OR VALUE")) {
                res.setNote("GET-LABEL-OR-VALUE(" + e + ") resulted in a NULL");
            } else if (ann.equals("LABEL")) {
                res.setNote("GET-LABEL(" + e + ") resulted in a NULL");
            } else if (ann.equals("DESCRIPTION")) {
                res.setNote("GET-DESCRIPTION(" + e + ") resulted in a NULL");
            }
        }
        return res;
    }
    
    public boolean parameterized() {
        return (expr != null && expr.parameterized());
    }

    public Expression translate(java.util.Map m) {
        expr = expr.translate(m);
        return this;
    }

    public AnnotationExtractor clone() {
        AnnotationExtractor a = (AnnotationExtractor) super.clone();

        a.expr = this.expr.clone();
        return a;
    }

    public String toString() {
        if (ann.equals("LABEL")) {
            return ("GET-LABEL(" + expr + ")");
        } else if (ann.equals("DESCRIPTION")) {
            return ("GET-DESCRIPTION(" + expr + ")");
        }
        return ("GET-ANNOTATION(" + expr + ")");
    }
}
