package nl.uu.let.languagelink.tds.dtl.expression;


import nl.uu.let.languagelink.tds.dtl.context.DataContext;
import nl.uu.let.languagelink.tds.dtl.data.Data;
import nl.uu.let.languagelink.tds.dtl.Log;

import nu.xom.*;


/**
 * Show a message.
 **/
public class Message extends Expression {
  
    protected String prefix = "?MESSAGE";
    protected Expression msg = null;

    public Message(Value v) {
        msg = v;
    }
    
    protected boolean hasMessage() {
        return (msg != null);
    }
    
    protected Data getMessage(DataContext c) {
        Data res = msg.eval(c); 

        if (res == null) {
            Log.error(Message.class,
                    "" + this + " expression couldn't be evaluated");
            return null;
        }
        if (res == null) {
            res = new Data(null);
            res.setNote("evaluating message (" + msg + ") resulted in a NULL");
        }
        return res;

    }
    
    public void show(DataContext c) {
        if (hasMessage()) {
            Data m = getMessage(c);

            if (!m.isEmpty()) {
                Log.message(Message.class,
                        c.trace() + ":" + m.getValue().toString());
            } else {
                Log.message(Message.class, c.trace() + ":" + "empty message");
            }
        }
    }
    
    public Data eval(DataContext c) {
        show(c);
        return new Data(null);
    }

    public boolean parameterized() {
        return msg.parameterized();
    }

    public Expression translate(java.util.Map m) {
        msg = msg.translate(m);
        return this;
    }

    public Element toXML() {
        return null;
    }
    
    public Message clone() {
        Message m = (Message) super.clone();

        m.prefix = prefix;
        m.msg = msg.clone();
        return m;
    }

    public String toString() {
        if (msg != null) {
            return (prefix + ":" + msg);
        }
        return null;
    }
}
