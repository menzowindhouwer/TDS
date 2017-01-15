package nl.uu.let.languagelink.tds.dtl;

import nl.uu.let.languagelink.tds.dtl.Log;
import nl.uu.let.languagelink.tds.dtl.event.*;

import java.io.*;
import java.net.*;
import java.util.*;

import nu.xom.*;
import nu.xom.xslt.*;

import com.oreilly.javaxslt.util.*;

import org.apache.commons.cli.*;


/**
 * Convert a CSV file to an XML document
 **/
public class CSV2XML {
    
    /* Main method */
    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp("CSV2XML [options] [CSV file]",
                "Where possible options are:", options,
                "When there is no CSV file specified on the commandline, the specification is read from standard input");
    }

    public static void main(String[] args) {
        // Command line options
        Options options = new Options();

        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(new Option("g", "debug", false, "show debug messages"));

        options.addOption(new Option("d", "delimiter", true, "use this field delimiter"));
        options.addOption(new Option("f", "first", false, "first line contains field names"));

        options.addOption(new Option("t", "table", true, "use this as table name"));
        options.addOption(new Option("r", "resource", true, "use this as resource name"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmdline = null;

        try {
            // parse the command line arguments
            cmdline = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            Log.error(CSV2XML.class, "Couldn't parse command line options", exp);
            usage(options);
            System.exit(1);
        }

        if (cmdline.hasOption("h")) {
            usage(options);
            System.exit(0);
        }
	
	String delim = ",";
        if (cmdline.hasOption('d')) {
		delim = cmdline.getOptionValue('d');
	}
	
	boolean header = cmdline.hasOption("f");
        
	String table = "csv";
        if (cmdline.hasOption('t')) {
		table = cmdline.getOptionValue('t');
	}

	String resource = "tds";
        if (cmdline.hasOption('r')) {
		resource = cmdline.getOptionValue('r');
	}

        String file = null;
        InputStream in = System.in;

        if (cmdline.getArgs().length > 0) {
            if (cmdline.getArgs().length > 1) {
                Log.error(CSV2XML.class,
                        "You should give only one CSV file on the command line");
                usage(options);
                System.exit(1);
            }
            file = cmdline.getArgs()[0];
            try {
                in = new FileInputStream(file);
            } catch (FileNotFoundException exp) {
                Log.error(CSV2XML.class, "Couldn't open the CSV file: " + file);
                System.exit(1);
            }
        }
        
        DefaultListener dl = new DefaultListener();

        Log.addListener(dl);
        Log.addTracker(dl);

	try {
		Document doc = (new Builder(new CSVXMLReader(delim))).build(in);
		
		if (header) {
			nux.xom.pool.XSLTransformFactory factory = new nux.xom.pool.XSLTransformFactory() {
			    protected String[] getPreferredTransformerFactories() {
				return new String[] {
				    "net.sf.saxon.TransformerFactoryImpl"
				};
			    }
			}; 

			URL url = CSV2XML.class.getResource("/CSVconvert.xsl");
			InputStream stream = url.openStream();
			URI uri = new URI(url.toString());
			XSLTransform xslt = factory.createTransform(stream,uri);
	
			xslt.setParameter("header",   "true");
			xslt.setParameter("table",    table);
			xslt.setParameter("resource", resource);
			
			Nodes nodes = xslt.transform(doc);
			if ((nodes.size()==1) && (nodes.get(0) instanceof Element)) {
			    doc = new Document((Element)nodes.get(0));
			}
		}
		System.out.println(doc.toXML());
	} catch(Exception e) {
		Log.error(CSV2XML.class,"Couldn't load the CSV file!",e);
	}
    }
}
