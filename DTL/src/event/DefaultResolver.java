package nl.uu.let.languagelink.tds.dtl.event;


import nl.uu.let.languagelink.tds.dtl.Log;

import java.io.*;
import java.net.*;


/**
 * The default implementation of the Resolver interface. Tries to resolve as
 * (1) a relative URL, (2) an absolute URL, (3) a relative file path and
 * (4) an absolute file path.
 **/
public class DefaultResolver implements Resolver {

    public Reader resolve(String base, String rel) {
        Reader result = null;

        URL url = null;

        if (base != null) {
            if (Log.debug()) {
                Log.debug(DefaultResolver.class,
                        "first try: open url(" + base + "," + rel + ")");
            }
            try {
                url = new URL(new URL(base), rel);
            } catch (MalformedURLException e) {// Log.warning("couldn't create URL:"+e);
            }
            if (url != null) {
                try {
                    result = new BufferedReader(
                            new InputStreamReader(
                                    url.openConnection().getInputStream(),"UTF-8"));
                    if (Log.debug()) {
                        Log.debug(DefaultResolver.class,
                                "success opening url(" + url + ")");
                    }
                } catch (IOException e) {
                    if (Log.debug()) {
                        Log.warning(DefaultResolver.class,
                                "couldn't open URL:" + e);
                    }
                }
            }
        }

        if (result == null) {
            if (Log.debug()) {
                Log.debug(DefaultResolver.class, "second try: url(" + rel + ")");
            }
            url = null;
            try {
                url = new URL(rel);
            } catch (MalformedURLException e) {// Log.warning("couldn't create URL:"+e);
            }
            if (url != null) {
                try {
                    result = new BufferedReader(
                            new InputStreamReader(
                                    url.openConnection().getInputStream(),"UTF-8"));
                    if (Log.debug()) {
                        Log.debug(DefaultResolver.class,
                                "success opening url(" + url + ")");
                    }
                } catch (IOException e) {
                    if (Log.debug()) {
                        Log.warning(DefaultResolver.class,
                                "couldn't open URL:" + e);
                    }
                }
            }
        }

        if ((result == null) && (base != null)) {
            if (Log.debug()) {
                Log.debug(DefaultResolver.class,
                        "third try: file(" + base + "," + rel + ")");
            }
            try {
                File b = new File(base);

                result = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(new File(b.getParent(), rel)),"UTF-8"));
                if (Log.debug()) {
                    Log.debug(DefaultResolver.class,
                            "success opening file(" + rel + ")");
                }
            } catch (IOException e) {
                if (Log.debug()) {
                    Log.warning(DefaultResolver.class,
                            "couldn't open file[base=" + base + ",rel=" + rel
                            + "]:" + e);
                }
            }
        }

        if (result == null) {
            if (Log.debug()) {
                Log.debug(DefaultResolver.class, "fourth try: file(" + rel + ")");
            }
            try {
                result = new BufferedReader(
                        new InputStreamReader(new FileInputStream(rel),"UTF-8"));
                if (Log.debug()) {
                    Log.debug(DefaultResolver.class,
                            "success opening file(" + rel + ")");
                }
            } catch (IOException e) {
                if (Log.debug()) {
                    Log.warning(DefaultResolver.class,
                            "couldn't open file[" + rel + "]:" + e);
                }
            }
        }
        
        return result;
    }

}
