package nl.uu.let.languagelink.tds.dtl.context;


import nl.uu.let.languagelink.tds.dtl.Log;


/**
 * The FieldInfo class maintains info about the access to a database fields. This allows us to,
 * for example, indicate fields that are never touched by the DTL spec so are either new and should
 * be included or should be part of the skip list.
 **/
public class FieldInfo {

    protected boolean preprocessed = false;
    protected boolean derived = false;
    protected boolean touched = false;
    
    public FieldInfo() {}
    
    public void preprocessed() {
        preprocessed = true;
    }
    
    public boolean isPreprocessed() {
        return preprocessed;
    }
    
    public void derived() {
        derived = true;
    }
    
    public boolean isDerived() {
        return derived;
    }
    
    public void touch() {
        touched = true;
    }
    
    public void untouch() {
        touched = false;
    }
    
    public boolean isTouched() {
        return touched;
    }
    
    public void reset() {
        preprocessed = false;
    }
    
    public boolean merge(FieldInfo info) {
        if (info.isDerived() != isDerived()) {
            return false;
        }
        if (info.isTouched()) {
            touch();
        }
        return true;
    }
    
    public FieldInfo copy() {
        FieldInfo info = new FieldInfo();

        if (isDerived()) {
            info.derived();
        }
        if (isTouched()) {
            info.touch();
        }
        return info;
    }
}
