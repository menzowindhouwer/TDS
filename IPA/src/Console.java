package nl.uu.let.languagelink.tds.ipa;


import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;

import javax.jnlp.*;

import org.pietschy.explicit.*;
import org.pietschy.explicit.style.Margin;
import com.zookitec.layout.Expression;


public class Console extends JPanel implements SingleInstanceListener {

    // main frame
    protected JFrame frame = null;

    // JNLP services
    protected BasicService          bs = null;
    protected SingleInstanceService ss = null;
    protected ClipboardService      cs = null;
    protected PersistenceService    ps = null;
    
    protected boolean    traySupported = true;
    
    // Are we on Mac OS X?
    protected boolean onMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  
    // IPA font
    protected Font system = null;
    
    // default button widths
    protected float defButtonWidth = (float)40.0;
    protected float minButtonWidth = (float)30.0;
    
    // default font sizes
    protected float bigFontSize = (float)24.0;

    // a ButtonGroup containing all IPA character buttons
    protected ButtonGroup chars = new ButtonGroup();
    
    // the character chooser
    protected CharacterChooser charChooser = null;
    
    // the IPA text
    protected JScrollPane textscroll = null;
    protected JTextArea text = null;
    
    // the font in use
    protected JComboBox font = null;
    
    // 'big' preview of a IPA character and its associated tooltip
    protected JLabel preview = null;
    protected JLabel previewtip = null;

    // history of used IPA character buttons
    protected int HISTORY = 20;
    protected java.util.List history   = new LinkedList();
    protected java.util.List histogram = new LinkedList();
    protected Vector mimics = new Vector();
    protected JTabbedPane histoTabs = null;

    // converting legacy IPA fonts to Unicode IPA
    protected JTextArea     convert = null;
    protected JComboBox     convert_maps = null;
    protected Conversion    conversion = null;
    protected JButton       paste = null;
    protected JToggleButton fix = null;
    protected String        unfixed = null;
    protected String        fixed = null;
    protected String convert_font = null;

    // settings
    protected JCheckBox tray = null;
    protected JCheckBox iconifyToTray = null;
    protected JComboBox iconify = null;
    protected JCheckBox formatted = null;
    protected JSpinner  size = null;
    protected JComboBox system_font = null;
    protected JComboBox normalize = null;
    
    // tray icon
    protected TrayIcon  trayIcon = null;
	protected MenuItem  trayMenu = null;
    
    // messages to be shown to the user
    protected String msg = "";
    
    // are settings already loaded?
    protected boolean loaded = false;
    
    protected static DefaultListModel logModel = new DefaultListModel();
    protected static PrintStream      log      = new PrintStream(new LogOutputStream(logModel,System.err),true);
    
    // THE constructor
    public Console(JFrame frame) {
        super();
    
        this.frame = frame;

        // try to redirect stdout and stderr to the log viewer
        try {
            System.setOut(new PrintStream(new LogOutputStream(logModel,System.out),true));
            System.setErr(log);
        } catch (Exception e) {
        }

	// make sure we use the cross platform look&feel
        try {
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
        }
	
	// if we are on Mac OS X change some default values
	if (onMac) {
            log.println("INF: Mac OS X");
	/*
	    defButtonWidth = (float)58.0;
	    minButtonWidth = (float)58.0;
	    bigFontSize = (float)24.0;
	*/
	}
        
        // try to instantiate the JNLP services
        try {
            bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService"); 
            ss = (SingleInstanceService) ServiceManager.lookup(
                    "javax.jnlp.SingleInstanceService");
            ps = (PersistenceService) ServiceManager.lookup(
                    "javax.jnlp.PersistenceService"); 
            cs = (ClipboardService) ServiceManager.lookup(
                    "javax.jnlp.ClipboardService");
        } catch (UnavailableServiceException e) {
			ss = null;
            ps = null;
            cs = null;
        }

		if (ss == null) {
            log.println("INF: can't manage singleton");
		} else {
			ss.addSingleInstanceListener(this);
		}
    
        if (cs == null) {
            log.println("INF: can't get access to the clipboard service");
        }
    
        if (ps == null) {
            log.println(
                    "INF: can't get access to the persistent settings");
        }
        
        traySupported = SystemTray.isSupported();
        if (traySupported) {
            try {
                SystemTray.getSystemTray();
            } catch(Exception e) {
                log.println(
                    "INF: can't get access to the system tray");
                traySupported = false;
            }
        }
	
	if (onMac) {
                log.println(
                    "INF: on a Mac the system tray isn't supported");
		traySupported = false;
	}
        
        JTabbedPane tabs = new JTabbedPane();
    
        // Create the 'Pulmonic consonants and vowels' tab
        TableBuilder tab = new TableBuilder();

        tab.add(createConsonantsTable(), 0, 0).fill();
        tab.add(createVowelsGraph(), 1, 0);
        tab.buildLayout();
    
        JPanel tabPanel = tab.getPanel();

        tabPanel.setBackground(Color.WHITE);
        tabs.add("Pulmonic consonants and vowels", tabPanel);
    
        // Create the 'Suprasegmentals and diacritics' tab
        tab = new TableBuilder();
        tab.add(createSuprasegmentalsList(), 0, 0).fill();
        tab.add(createDiacriticsList(), 1, 0);
        tab.add(new UserCharsPanel(this,"USER DIACRITICS"), 2, 0).fill();
        tab.buildLayout();
    
        tabPanel = tab.getPanel();
        tabPanel.setBackground(Color.WHITE);
        tabs.add("Suprasegmentals and diacritics", tabPanel);
    
        // Create the 'Non-pulmonic consonants and other symbols' tab
        tab = new TableBuilder();
        tab.add(createConsonantsList(), 0, 0).fill();
        tab.add(createOtherSymbolsList(), 1, 0);
        tab.add(new UserCharsPanel(this,"USER SYMBOLS"), 2, 0).fill();
        tab.buildLayout();
    
        tabPanel = tab.getPanel();
        tabPanel.setBackground(Color.WHITE);
        tabs.add("Non-pulmonic consonants and other symbols", tabPanel);

        // Create the 'import' tab
        tabs.add("Import IPA", createConvertPane());
        
        // Create the main layout
        tab = new TableBuilder(this);

        tab.rows().defaultPaddingTop(new Expression(0.0));
        tab.rows().defaultPaddingBottom(new Expression(0.0));
        tab.columns().defaultPaddingLeft(new Expression(0.0));
        tab.columns().defaultPaddingRight(new Expression(0.0));

        tab.add(tabs, 1, 0).fill();
        tab.add(createClipboardPane(), 2, 0).fill();

        // try to set our preferred default font
        system = getSystemFont();
        if (system != null) {
            useSystemFont(system);
            font(system);
        } else
            system = (new JButton()).getFont();
        
        // add the 'settings' tab
        tabs.add("Settings", createSettingsPane());
    
        // Create the 'about' tab
        tabs.add("Help", createAboutPane());
    
        tab.buildLayout();
        
        loadSettings();
		loaded = true;
		loadHistory();
		loadHistogram();
        
        // Init the system tray
        tray();

        // Init the character chooser
        charChooser = new CharacterChooser();
        
        // Check the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if ((screenSize.width<1024) || (screenSize.height<768)) {
            msg += "Your screen resolution is too small, the IPA Console needs at least a resolution of 1024 x 768!";
            log.println("ERR: resolution["+screenSize.width+"x"+screenSize.height+"] is too small");
        } else
            log.println("INF: resolution["+screenSize.width+"x"+screenSize.height+"]");

		// Set the frame icon
        ArrayList icons = new ArrayList();
		icons.add((new ImageIcon(Console.class.getResource("/icon-16.png"))).getImage());
		icons.add((new ImageIcon(Console.class.getResource("/icon-32.png"))).getImage());
		icons.add((new ImageIcon(Console.class.getResource("/icon-64.png"))).getImage());
		frame.setIconImages(icons);

        // Monitor the frame close button
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (tray.isEnabled() && tray.isSelected())
                    visible(false);
                else
                    exit();
            }
        });
    }
    
    // Hide/show the Console
    public void visible(boolean show) {
        if (show) {
            if (!frame.isVisible()) {
                frame.setVisible(true);
            }
            if (frame.getExtendedState()==frame.ICONIFIED)
                frame.setExtendedState(frame.NORMAL);
            if (trayMenu!=null)
                trayMenu.setLabel("Hide IPA Console");
        } else {
            if (frame.isVisible()) {
                frame.setVisible(false);
            }
            if (trayMenu!=null)
                trayMenu.setLabel("Show IPA Console");
        }
    }

	// Exit
	public void exit() {
		if (ss != null)
			ss.removeSingleInstanceListener(this);
		System.exit(0);
	}

	// Make sure only one instance is run
	public void newActivation(String[] params) {
		log.println("INF: a new instance was activated");
		visible(true);
	}
    
    // Fix some display issues, when all sizes are known
    public void fixDisplay() {
        //log.println("DBG: text scrollpane maximum size["+textscroll.getMaximumSize()+"]"); 
        textscroll.setMinimumSize(textscroll.getSize());
        textscroll.setPreferredSize(textscroll.getSize());
        textscroll.setMaximumSize(textscroll.getSize());
        //log.println("DBG: text scrollpane fixed maximum size["+textscroll.getMaximumSize()+"]"); 
    }

    // Utility methods to help constructing the various table layouts
    
    protected Cell addTopHeader(TableBuilder tb, String label, int row, int col, int rowspan, int colspan) {
        JLabel l = new JLabel(label);

        l.setHorizontalAlignment(JLabel.CENTER);
        return tb.add(l, row, col, rowspan, colspan).alignCentre();
    }

    protected Cell addSideHeader(TableBuilder tb, String label, Component comp, int row, int col) {
        JLabel l = new JLabel(label);
        if (comp!=null)
            l.setLabelFor(comp);

        l.setHorizontalAlignment(JLabel.LEFT);
        return tb.add(l, row, col).alignBaseline(true);
    }
  
    protected Cell addSideHeader(TableBuilder tb, String label, int row, int col) {
        return addSideHeader(tb,label,null,row,col);
    }
    
    protected JToggleButton addButton(ButtonGroup group, String label, String tip, float size, boolean resize) {
        JToggleButton button = new JToggleButton(label);

        if (system != null) {
            button.setFont(system.deriveFont((float) button.getFont().getSize()));
        }
        if (size > 0) {
            button.setFont(button.getFont().deriveFont(size));
        }
        
        String cps = "[";
        for (int i=0;i<label.length();) {
            int cp = label.codePointAt(i);
            String hex = Integer.toHexString(cp).toUpperCase();
            while (hex.length()<4)
                hex = "0" + hex;
            cps += "U+"+hex;
            i += Character.charCount(cp);
            if (i<label.length())
                cps += " ";
        }
        cps += "]";
        
        if (tip != null)
            tip = tip + " " + cps;
        else if (label.length()==1) {
            int idx = codepoints.getIndex(label.codePointAt(0));
            if (idx>=0)
                tip = codepoints.getName(idx) + " " + cps;
        }
        
        if (tip == null)
            tip = cps;
        
        button.setToolTipText(tip);
        
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });
        button.addMouseListener(new PreviewListener());

        if (group != null)
            group.add(button);
        
        histogram.add(new FrequencyButton(histogram,button));

        button.setMargin(new Insets(2, 2, 2, 2));

		if (resize) {
			button.setPreferredSize(new Dimension(10,10));
			button.setMinimumSize(new Dimension(10,10));
			button.setMaximumSize(new Dimension(10,10));
		}
        
        return button;
    }

    protected JToggleButton addButton(ButtonGroup group, String label, String tip, float size) {
		return addButton(group,label,tip,size,true);
	}
    
    protected JToggleButton addButton(String label, String tip, float size, boolean resize) {
        return addButton(chars,label,tip,size,resize);
    }
    
    protected JToggleButton addButton(String label, String tip, float size) {
		return addButton(label,tip,size,true);
	}

    protected JToggleButton addButton(String label, String tip, boolean resize) {
        return addButton(label,tip,(float)0.0,resize);
    }

    protected JToggleButton addButton(String label, String tip) {
        return addButton(label,tip,(float)0.0,true);
    }
    
    protected Cell addButton(TableBuilder tb, ButtonGroup group, String label, String tip, int row, int col, float size,boolean resize) {
        JToggleButton button = addButton(group,label,tip,size,resize);

        Cell cell = tb.add(button, row, col);

        cell.alignCentre();
        cell.alignBaseline(true);
        cell.fill();
        return cell;
    }

    protected Cell addButton(TableBuilder tb, ButtonGroup group, String label, String tip, int row, int col, float size) {
    	return addButton(tb,group,label,tip,row,col,size,true);
	}
  
    protected Cell addButton(TableBuilder tb, ButtonGroup group, String label, String tip, int row, int col, boolean resize) {
        return addButton(tb, group, label, tip, row, col, (float) 0.0,resize);
    }

    protected Cell addButton(TableBuilder tb, ButtonGroup group, String label, String tip, int row, int col) {
        return addButton(tb, group, label, tip, row, col, (float) 0.0,true);
    }
  
    protected Cell addShading(TableBuilder tb, int row, int col, int colspan) {
        JPanel p = new JPanel();

        p.setBackground(Color.LIGHT_GRAY);
        return tb.add(p, row, col, 1, colspan).fill();
    }
    
    // Methods to create the IPA tables

    protected JPanel createConsonantsTable() {
        // Create and set up the IPA chars table
        TablePanel charsTable = new TablePanel();
        TableBuilder tb = new TableBuilder(charsTable);

        charsTable.setBuilder(tb, Color.GRAY, Color.LIGHT_GRAY);
        charsTable.setBackground(Color.WHITE);

        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(1.0));
        tb.rows().defaultPaddingBottom(new Expression(1.0));
        tb.columns().defaultPaddingLeft(new Expression(1.0));
        tb.columns().defaultPaddingRight(new Expression(1.0));

        addTopHeader(tb, "Bilabial", 0, 1, 1, 2);
        addTopHeader(tb, "Labiodental", 0, 3, 1, 2);
        addTopHeader(tb, "Dental", 0, 5, 1, 2);
        addTopHeader(tb, "Alveolar", 0, 7, 1, 2);
        addTopHeader(tb, "Postalveolar", 0, 9, 1, 2);
        addTopHeader(tb, "Retroflex", 0, 11, 1, 2);
        addTopHeader(tb, "Palatal", 0, 13, 1, 2);
        addTopHeader(tb, "Velar", 0, 15, 1, 2);
        addTopHeader(tb, "Uvular", 0, 17, 1, 2);
        addTopHeader(tb, "Pharyngeal", 0, 19, 1, 2);
        addTopHeader(tb, "Glottal", 0, 21, 1, 2);
    
        addSideHeader(tb, "Plosive", 1, 0);
        addSideHeader(tb, "Nasal", 2, 0);
        addSideHeader(tb, "Trill", 3, 0);
        addSideHeader(tb, "Tap or Flap", 4, 0);
        addSideHeader(tb, "Fricative", 5, 0);
        addSideHeader(tb, "Lateral fricative", 6, 0);
        addSideHeader(tb, "Approximant", 7, 0);
        addSideHeader(tb, "Lateral approximant", 8, 0);
    
        // apa
        addButton(tb, chars, "p", null, 1, 1);
    
        // aba
        addButton(tb, chars, "b", null, 1, 2);
    
        // ata
        addButton(tb, chars, "t", null, 1, 7);
    
        // ada
        addButton(tb, chars, "d", null, 1, 8);
    
        // atra ʈ
        addButton(tb, chars, "ʈ", null, 1, 11);
    
        // adra ɖ
        addButton(tb, chars, "ɖ", null, 1, 12);

        // atja c
        addButton(tb, chars, "c", null, 1, 13);
    
        // adja ɟ
        addButton(tb, chars, "ɟ", null, 1, 14);
    
        // aka k
        addButton(tb, chars, "k", null, 1, 15);
    
        // aga ɡ
        addButton(tb, chars, "ɡ", null, 1, 16);
    
        // aqa q
        addButton(tb, chars, "q", null, 1, 17);
    
        // ahGa ɢ
        addButton(tb, chars, "ɢ", null, 1, 18);

        // a_a ʔ
        addButton(tb, chars, "ʔ", null, 1, 21);
    
        // ama m
        addButton(tb, chars, "m", null, 2, 2);
    
        // amja ɱ
        addButton(tb, chars, "ɱ", null, 2, 4);
    
        // ana n
        addButton(tb, chars, "n", null, 2, 8);
    
        // anra ɳ
        addButton(tb, chars, "ɳ", null, 2, 12);
    
        // anja ɲ
        addButton(tb, chars, "ɲ", null, 2, 14);
    
        // anga ŋ
        addButton(tb, chars, "ŋ", null, 2, 16);
    
        // ahNa ɴ
        addButton(tb, chars, "ɴ", null, 2, 18);
    
        // ahBa ʙ
        addButton(tb, chars, "ʙ", null, 3, 2);
    
        // ara r
        addButton(tb, chars, "r", null, 3, 8);
    
        // ahRa ʀ
        addButton(tb, chars, "ʀ", null, 3, 18);
        
        // labiodental flap
        addButton(tb, chars, "\u2C71", "labiodental flap", 4, 4);
    
        // flap ɾ
        addButton(tb, chars, "ɾ", null, 4, 8);
    
        // arra ɽ
        addButton(tb, chars, "ɽ", null, 4, 12);
    
        // affa ɸ
        addButton(tb, chars, "ɸ", null, 5, 1);
    
        // avva β
        addButton(tb, chars, "β", null, 5, 2);
    
        // afa f
        addButton(tb, chars, "f", null, 5, 3);
    
        // ava v
        addButton(tb, chars, "v", null, 5, 4);
    
        // atha θ
        addButton(tb, chars, "θ", null, 5, 5);
    
        // adha ð
        addButton(tb, chars, "ð", null, 5, 6);
    
        // asa s
        addButton(tb, chars, "s", null, 5, 7);
    
        // aza z
        addButton(tb, chars, "z", null, 5, 8);
    
        // asja ʃ
        addButton(tb, chars, "ʃ", null, 5, 9);
    
        // azja ʒ
        addButton(tb, chars, "ʒ", null, 5, 10);
    
        // asra ʂ
        addButton(tb, chars, "ʂ", null, 5, 11);
    
        // azra ʐ
        addButton(tb, chars, "ʐ", null, 5, 12);
    
        // acja ç
        addButton(tb, chars, "ç", null, 5, 13);
    
        // ajja ʝ
        addButton(tb, chars, "ʝ", null, 5, 14);
    
        // axa x
        addButton(tb, chars, "x", null, 5, 15);
    
        // acha ɣ
        addButton(tb, chars, "ɣ", null, 5, 16);
    
        // achia χ
        addButton(tb, chars, "χ", null, 5, 17);
    
        // aohra ʁ
        addButton(tb, chars, "ʁ", null, 5, 18);
    
        // ah-a ħ
        addButton(tb, chars, "ħ", null, 5, 19);
    
        // as_a ʕ
        addButton(tb, chars, "ʕ", null, 5, 20);
    
        // aha h
        addButton(tb, chars, "h", null, 5, 21);
    
        // a_ha ɦ
        addButton(tb, chars, "ɦ", null, 5, 22);
    
        // achla ɬ
        addButton(tb, chars, "ɬ", null, 6, 7);
    
        // alzja ɮ
        addButton(tb, chars, "ɮ", null, 6, 8);
    
        // awva ʋ
        addButton(tb, chars, "ʋ", null, 7, 4);
    
        // aora ɹ
        addButton(tb, chars, "ɹ", null, 7, 8);
    
        // aorra ɻ
        addButton(tb, chars, "ɻ", null, 7, 12);
    
        // aja j
        addButton(tb, chars, "j", null, 7, 14);
    
        // amga ɰ
        addButton(tb, chars, "ɰ", null, 7, 16);
    
        // ala l
        addButton(tb, chars, "l", null, 8, 8);
    
        // alra ɭ
        addButton(tb, chars, "ɭ", null, 8, 12);
    
        // alja ʎ
        addButton(tb, chars, "ʎ", null, 8, 14);
    
        // ahLa ʟ
        addButton(tb, chars, "ʟ", null, 8, 16);
    
        // impossible articulations
        addShading(tb, 1, 20, 1);
        addShading(tb, 1, 22, 1);
        addShading(tb, 2, 19, 2);
        addShading(tb, 2, 21, 2);
        addShading(tb, 3, 13, 2);
        addShading(tb, 3, 21, 2);
        addShading(tb, 4, 13, 2);
        addShading(tb, 4, 21, 2);
        addShading(tb, 6, 1, 2);
        addShading(tb, 6, 3, 2);
        addShading(tb, 6, 19, 2);
        addShading(tb, 6, 21, 2);
        addShading(tb, 7, 21, 2);
        addShading(tb, 8, 1, 2);
        addShading(tb, 8, 3, 2);
        addShading(tb, 8, 19, 2);
        addShading(tb, 8, 21, 2);
    
        // glue some columns together
        tb.column(1).paddingRight(new Expression(0));
        tb.column(2).paddingLeft(new Expression(0));
        tb.column(3).paddingRight(new Expression(0));
        tb.column(4).paddingLeft(new Expression(0));
        tb.column(5).paddingRight(new Expression(0));
        tb.column(6).paddingLeft(new Expression(0));
        tb.column(7).paddingRight(new Expression(0));
        tb.column(8).paddingLeft(new Expression(0));
        tb.column(9).paddingRight(new Expression(0));
        tb.column(10).paddingLeft(new Expression(0));
        tb.column(11).paddingRight(new Expression(0));
        tb.column(12).paddingLeft(new Expression(0));
        tb.column(13).paddingRight(new Expression(0));
        tb.column(14).paddingLeft(new Expression(0));
        tb.column(15).paddingRight(new Expression(0));
        tb.column(16).paddingLeft(new Expression(0));
        tb.column(17).paddingRight(new Expression(0));
        tb.column(18).paddingLeft(new Expression(0));
        tb.column(19).paddingRight(new Expression(0));
        tb.column(20).paddingLeft(new Expression(0));
        tb.column(21).paddingRight(new Expression(0));
        tb.column(22).paddingLeft(new Expression(0));
    
        // make most columns the same size
        for (int i = 1; i < 23; i++) {
            tb.column(i).preferredWidth(new Expression(defButtonWidth));
        }
   
        // create the panel
        tb.buildLayout();
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("CONSONANTS (PULMONIC)"), BorderLayout.NORTH);
        charsPane.add(charsTable, BorderLayout.CENTER);
        charsPane.add(
                new JLabel(
                        "Where symbols appear in pairs, the one to the right represents a voiced consonant. Shaded areas denote articulations judged impossible."),
                        BorderLayout.SOUTH);

        return charsPane;
    }
  
    protected JPanel createConsonantsList() {
        TablePanel charsList = new TablePanel();
    
        TableBuilder tb = new TableBuilder(charsList);

        charsList.setBuilder(tb, Color.WHITE, Color.WHITE);
        charsList.setBackground(Color.WHITE);
     
        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));
    
        int col = 0;

        addTopHeader(tb, "Clicks", 0, col, 1, 5).alignLeft();
        addButton(tb, chars, Character.toString('\u0298'), "bilabial click", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u01C0'), "dental click", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u01C3'), "retroflex click", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u01C2'), "alveolar click", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u01C1'), "alveolar lateral click", 1, col++).preferredWidth(
                new Expression(defButtonWidth));

        addTopHeader(tb, "Voiced implosives", 0, col, 1, 5).alignLeft();
        tb.column(col).paddingLeft(new Expression(2.0));
        tb.column(col - 1).paddingRight(new Expression(2.0));
        addButton(tb, chars, Character.toString('\u0253'), "voiced bilabial implosive", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0257'), "voiced alveolar implosive", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0284'), "voiced palatal implosive", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0260'), "voiced velar implosive", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u029B'), "voiced uvular implosive", 1, col++).preferredWidth(
                new Expression(defButtonWidth));

        addTopHeader(tb, "Ejectives", 0, col, 1, 1).alignLeft();
        tb.column(col).paddingLeft(new Expression(2.0));
        tb.column(col - 1).paddingRight(new Expression(2.0));
        addButton(tb, chars, Character.toString('\u02BC'), "ejective", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
    
        // create the panel
        tb.buildLayout();
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("CONSONANTS (NON-PULMONIC)"),
                BorderLayout.NORTH);
        charsPane.add(charsList, BorderLayout.CENTER);

        return charsPane;
    }

    protected JPanel createSuprasegmentalsList() {
        TablePanel charsList = new TablePanel();
    
        TableBuilder tb = new TableBuilder(charsList);

        charsList.setBuilder(tb, Color.WHITE, Color.WHITE);
        charsList.setBackground(Color.WHITE);
     
        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));
    
        int col = 0;

        addButton(tb, chars, Character.toString('\u02C8'), "(primary) stress mark", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02CC'), "secondary stress", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02D0'), "length mark", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02D1'), "half-length", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0306'), "extra-short", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, ".", "syllable break", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u2223'), "minor (foot) group", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u2225'), "major (intonation) group", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u203F'), "linking (absence of a break)", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
    
        // TONES & WORD ACCENTS
        addTopHeader(tb, "TONES & WORD ACCENTS", 0, col, 1, 11).alignLeft();
    
        // LEVEL
        addTopHeader(tb, "LEVEL", 1, col, 1, 7).alignLeft();
        tb.column(col).paddingLeft(new Expression(2.0));
        tb.column(col - 1).paddingRight(new Expression(2.0));
        addButton(tb, chars, Character.toString('\u030B'), "extra high tone", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02E5'), "extra high tone", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0301'), "high tone", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02E6'), "high tone", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0304'), "mid tone", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02E7'), "mid tone", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0300'), "low tone", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02E8'), "low tone", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u030F'), "extra low tone", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02E9'), "extra low tone", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u2193'), "downstep", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u2191'), "upstep", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
    
        // CONTOUR
        addTopHeader(tb, "CONTOUR", 1, col, 1, 4).alignLeft();
        tb.column(col).paddingLeft(new Expression(2.0));
        tb.column(col - 1).paddingRight(new Expression(2.0));
        
        addButton(tb, chars, Character.toString('\u030C'), "rising", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, "\u02E9\u02E5", "rising", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
                
        addButton(tb, chars, Character.toString('\u0302'), "falling", 2, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, "\u02E5\u02E9", "falling", 3, col++).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb,chars,Character.toString('\u1DC4'),"high rising",2,col).preferredWidth(new Expression(defButtonWidth));
        addButton(tb,chars,"\u02E6\u02E5","high rising",3,col++).preferredWidth(new Expression(defButtonWidth));
        
        addButton(tb,chars,Character.toString('\u1DC5'),"low rising",2,col).preferredWidth(new Expression(defButtonWidth));
        addButton(tb,chars,"\u02E9\u02E8","low rising",3,col++).preferredWidth(new Expression(defButtonWidth));
        
        addButton(tb,chars,Character.toString('\u1DC8'),"rising falling etc.",2,col).preferredWidth(new Expression(defButtonWidth));
        addButton(tb,chars,"\u02E6\u02E5\u02E6","rising falling etc.",3,col++).preferredWidth(new Expression(defButtonWidth));
        
        
        addButton(tb, chars, Character.toString('\u2197'), "global rise", 2, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u2198'), "global fall", 2, col++).preferredWidth(
                new Expression(defButtonWidth));

        // create the panel
        tb.buildLayout();
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("SUPRASEGMENTALS"), BorderLayout.NORTH);
        charsPane.add(charsList, BorderLayout.CENTER);

        return charsPane;
    }
  
    protected JPanel createVowelsGraph() {
        VowelsPanel bg = new VowelsPanel(Color.DARK_GRAY);

        bg.setBackground(Color.WHITE);
    
        TableBuilder tb = new TableBuilder(bg);

        bg.setBuilder(tb, Color.GRAY, Color.LIGHT_GRAY);
     
        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));

        JComponent top = null;
        JComponent left = null;
    
        addTopHeader(tb, "Front", 0, 1, 1, 3);
        addTopHeader(tb, "Central", 0, 9, 1, 3);
        addTopHeader(tb, "Back", 0, 15, 1, 3);

        addSideHeader(tb, "Close", 1, 0);
        addSideHeader(tb, "Close-mid", 3, 0);
        addSideHeader(tb, "Open-mid", 5, 0);
        addSideHeader(tb, "Open", 7, 0);
    
        // tb.row(0).paddingBottom(new Expression(1.0));
        // tb.column(0).paddingRight(new Expression(1.0));
    
        top = (JComponent) tb.cell(0, 1).getComponent();
        left = (JComponent) tb.cell(3, 0).getComponent();
    
        addButton(tb, chars, Character.toString('\u0069'), null, 1, 1).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 1, 2).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0079'), null, 1, 3).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0268'), null, 1, 9).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 1, 10).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0289'), null, 1, 11).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u026F'), null, 1, 15).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 1, 16).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0075'), null, 1, 17).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u026A'), null, 2, 5).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u028F'), null, 2, 6).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u028A'), null, 2, 14).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0065'), null, 3, 3).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 3, 4).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u00F8'), null, 3, 5).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0258'), null, 3, 10).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 3, 11).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0275'), null, 3, 12).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb, chars, Character.toString('\u0258'), null, 3, 10).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 3, 11).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0275'), null, 3, 12).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0264'), null, 3, 15).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 3, 16).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u006F'), null, 3, 17).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0259'), null, 4, 12).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u025B'), null, 5, 5).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 5, 6).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0153'), null, 5, 7).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb, chars, Character.toString('\u025C'), null, 5, 11).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 5, 12).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u025E'), null, 5, 13).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb, chars, Character.toString('\u028C'), null, 5, 15).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 5, 16).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0254'), null, 5, 17).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb, chars, Character.toString('\u00E6'), null, 6, 6).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0250'), null, 6, 12).preferredWidth(
                new Expression(defButtonWidth));
    
        addButton(tb, chars, Character.toString('\u0061'), null, 7, 7).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 7, 8).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0276'), null, 7, 9).preferredWidth(
                new Expression(defButtonWidth));

        addButton(tb, chars, Character.toString('\u0251'), null, 7, 15).preferredWidth(
                new Expression(defButtonWidth));
        tb.add(new JLabel("•", JLabel.CENTER), 7, 16).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0252'), null, 7, 17).preferredWidth(
                new Expression(defButtonWidth));

        // create the panel
        tb.buildLayout();
        bg.setOffsets(top, left);
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("VOWELS"), BorderLayout.NORTH);
        charsPane.add(bg, BorderLayout.CENTER);
        charsPane.add(
                new JLabel(
                        "Where symbols appear in pairs, the one to the right represents a rounded vowel."),
                        BorderLayout.SOUTH);

        return charsPane;
    }

    protected JPanel createDiacriticsList() {
        TablePanel charsList = new TablePanel();
    
        TableBuilder tb = new TableBuilder(charsList);

        charsList.setBuilder(tb, Color.WHITE, Color.WHITE);
        charsList.setBackground(Color.WHITE);
     
        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));
    
        int col = 0;
	int row = 0;

        addButton(tb, chars, Character.toString('\u0325'), "voiceless", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u030A'), "voiceless (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u032C'), "voiced", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        // \u030C used for rising
        addButton(tb, chars, Character.toString('\u02B0'), "aspirated", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0339'), "more rounded", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0351'), "more rounded (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u031C'), "less rounded", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0357'), "less rounded (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u031F'), "advanced", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0320'), "retracted", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0308'), "centralized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        // \u0324 used for breathy voiced
        addButton(tb, chars, Character.toString('\u033D'), "mid-centralized", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0353'), "mid-centralized (use if character has ascender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0329'), "syllabic", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u030D'), "syllabic (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u032F'), "non-syllabic", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0311'), "non-syllabic (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u02DE'), "rhoticity", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u025A'), null, row+2, col).preferredWidth(new Expression(minButtonWidth)).fill();
        addButton(tb, chars, Character.toString('\u025D'), null, row+3, col++).preferredWidth(new Expression(minButtonWidth)).fill();
        addButton(tb, chars, Character.toString('\u0324'), "breathy voiced", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        // \u0308 used for centralized
        addButton(tb, chars, Character.toString('\u0330'), "creaky voiced", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        // \u0303 used for nasalized
        addButton(tb, chars, Character.toString('\u033C'), "linguolabial", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
	
	/*
	if (onMac) {
		row = 5;
		col = 0;
	
		tb.row(row).paddingTop(new Expression(2.0));
		tb.row(row - 1).paddingBottom(new Expression(2.0));
	}
	*/
	
        addButton(tb, chars, Character.toString('\u02B7'), "labialized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u02B2'), "palatalized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u02E0'), "velarized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u02E4'), "pharyngealized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0334'), "velarized or pharyngealized", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u026B'), "dark l", row+2, col++).preferredWidth(new Expression(minButtonWidth)).fill();
        addButton(tb, chars, Character.toString('\u031D'), "raised", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u031E'), "lowered", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0318'), "advanced tongue root", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0319'), "retracted tongue root", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u032A'), "dental", row, col, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0346'), "dental (use if character has descender)", row+1, col++).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u033A'), "apical", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u033B'), "laminal", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u0303'), "nasalized", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u207F'), "nasal release", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth)); 
        addButton(tb, chars, Character.toString('\u02E1'), "lateral release", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
        addButton(tb, chars, Character.toString('\u031A'), "no audible release", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));
    
        tb.column(col).paddingLeft(new Expression(2.0));
        tb.column(col - 1).paddingRight(new Expression(2.0));

        addButton(tb, chars, Character.toString('\u0323'), "closer variety of vowel", row, col++, bigFontSize).preferredWidth(
                new Expression(minButtonWidth));

        // create the panel
        tb.buildLayout();
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("DIACRITICS"), BorderLayout.NORTH);
        charsPane.add(charsList, BorderLayout.CENTER);

        return charsPane;
    }
  
    protected JPanel createOtherSymbolsList() {
        TablePanel charsList = new TablePanel();
    
        TableBuilder tb = new TableBuilder(charsList);

        charsList.setBuilder(tb, Color.WHITE, Color.WHITE);
        charsList.setBackground(Color.WHITE);
     
        tb.setIgnorePaddingOnEdges(false);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));
    
        int col = 0;

        addButton(tb, chars, Character.toString('\u028D'), "voiceless labial-velar fricative", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0077'), "voiced labial-velar approximant", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0265'), "voiced labial-palatal approximant", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u029C'), "voiceless epiglottal fricative", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02A2'), "voiced epiglottal fricative", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02A1'), "voiced epiglottal plosive", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0255'), "alveolo-palatal fricative", 0, col).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0291'), "alveolo-palatal fricative", 1, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u027A'), "alveolar lateral flap", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0267'), "voiceless multiple-place fricative", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0361'), "tie bar", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u035C'), "tie bar", 0, col++).preferredWidth(
                new Expression(defButtonWidth));
    
        col = 0;
        addButton(tb, chars, Character.toString('\u00D8'), "zero realization", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u02B1'), "breathy release", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\uF25F'), "labiodental flap (SIL codepoint)", 3, col++).preferredWidth(
                new Expression(defButtonWidth));
        addButton(tb, chars, Character.toString('\u0475'), "labiodental flap (look-alike)", 3, col++).preferredWidth(
                new Expression(defButtonWidth));

        // create the panel
        tb.buildLayout();
        JPanel charsPane = new JPanel(new BorderLayout());

        charsPane.setBackground(Color.WHITE);
        charsPane.add(new JLabel("OTHER SYMBOLS"), BorderLayout.NORTH);
        charsPane.add(charsList, BorderLayout.CENTER);

        return charsPane;
    }
    
    // Create the convert pane, which allows to transform (IPA93) encoded legacy text to Unicode IPA
    protected JPanel createConvertPane() {
        JEditorPane help = new JEditorPane();
        help.setContentType("text/html");
        try {
            help.setPage(Console.class.getResource("/help-import.html"));
        } catch (IOException e) {
            log.println("ERR: couldn't load import help page: "+e);
            e.printStackTrace(System.err);
        }
        help.addHyperlinkListener(new Hyperactive());
        help.setEditable(false);
        
        JScrollPane helpScrollPane = new JScrollPane(help);
        helpScrollPane.setPreferredSize(new Dimension(250, 190));
        helpScrollPane.setMinimumSize(new Dimension(10, 160));

        JPanel m = new JPanel(new BorderLayout());
        m.add(helpScrollPane,BorderLayout.NORTH);
        
        JPanel p = new JPanel(new BorderLayout());
        m.add(p,BorderLayout.CENTER);

        conversion = new Conversion();
        convert_maps = new JComboBox(conversion.getMapNames().toArray());
        convert_maps.setBackground(Color.WHITE);
        convert_maps.setSelectedItem("SIL IPA93 Font encoding");
        
        convert_maps.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                convertfont();
                saveSettings();
            }
        });
    
        p.add(convert_maps,BorderLayout.NORTH);
    
        convert = new JTextArea("");
        convertfont();
        
        p.add(new JScrollPane(convert), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1, 4));

        paste = new JButton("paste");
        paste.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                paste_convert();
            }
        });
/*
        if (cs == null) {
            paste.setEnabled(false);
            log.println("INF: disabled paste button");
        }
*/
        buttons.add(paste);
    
        fix = new JToggleButton("try alternate mode");
        fix.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fix();
            }
        });
        buttons.add(fix);
        
        JButton convert = new JButton("convert");

        convert.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        });
        buttons.add(convert);
    
        JButton clear = new JButton("clear");

        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clear_convert();
            }
        });
        buttons.add(clear);
    
        p.add(buttons, BorderLayout.SOUTH);
    
        return m;
    }

	protected final SpinnerModel fontSizes  = new SpinnerNumberModel(12,1,255,1);

	protected final int NO     = 0;
	protected final int DECOMP = 1;
	protected final int COMP   = 2;

	protected String [] normalize_modes = new String[3];
    protected String [] normalize_mode_setting = new String[3];

	protected final int NONE = 0;
	protected final int MINI = 1;

	protected String [] iconify_modes = new String[2];
	protected String [] iconify_mode_setting = new String[2];

    // Create the settings pane
    protected JPanel createSettingsPane() {
        final JPanel p = new JPanel(new BorderLayout());
        
        TableBuilder tb = new TableBuilder();

		int row = 0;
    
        addSideHeader(tb,"Put font information on the clipboard:",row,0);

        formatted = new JCheckBox();
        formatted.setBackground(Color.WHITE);
        formatted.setSelected(true);
        formatted.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
				if (formatted.isSelected())
					size.setEnabled(true);
				else
					size.setEnabled(false);
                saveSettings();
            }
        });
        tb.add(formatted, row, 1, 1, 2);
        
		row++;
    
        addSideHeader(tb, "Font size (if font information is put on the clipboard):",
                row, 0);
        size = new JSpinner(fontSizes);
        size.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                saveSettings();
            }
        });
        tb.add(size, row, 1, 1, 2);

		row++;
    
        Vector fonts = findSystemFonts();
        if (fonts.size()<=1) {
            fonts = new Vector();
            Font[] f = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
            for (int i=0;i<f.length;i++)
              fonts.add(f[i]);
         }

        system_font = new JComboBox();
        system_font.setBackground(Color.WHITE);
        for (int i = 0; i < fonts.size(); i++) {
            system_font.addItem(((Font) fonts.get(i)).getFontName());
        }
        if (system != null) {
            system_font.setSelectedItem(system.getFontName());
        }
        system_font.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                systemfont();
                saveSettings();
            }
        });
    
        addSideHeader(tb, "Font used on IPA Console buttons:", row, 0);
        tb.add(system_font, row, 1, 1, 2);
        
        row++;
        
		normalize_modes[NO]            = "don't normalize";
		normalize_mode_setting[NO]     = "NONE";
		normalize_modes[DECOMP]        = "use canonical decomposition";
		normalize_mode_setting[DECOMP] = "CANON DECOMP";
		normalize_modes[COMP]          = "use canonical composition";
		normalize_mode_setting[COMP]   = "CANON COMP";

        normalize = new JComboBox(normalize_modes);
        normalize.setBackground(Color.WHITE);
        normalize.setSelectedIndex(COMP);
        normalize.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        tb.add(normalize, row, 1, 1, 2);
        addSideHeader(tb, "Normalize Unicode text put on the clipboard:", row, 0);

		row++;
        
        tray = new JCheckBox();
        tray.setBackground(Color.WHITE);
        //tray.setSelected(true);
        tray.setEnabled(traySupported);
        tray.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tray();
                saveSettings();
            }
        });
        if (traySupported) {
            addSideHeader(tb, "Keep the IPA Console in the system tray:", row, 0);
            tb.add(tray, row, 1, 1, 2);
        
            row++;
        }

        iconifyToTray = new JCheckBox();
        iconifyToTray.setBackground(Color.WHITE);
        iconifyToTray.setEnabled(traySupported);
        iconifyToTray.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (iconifyToTray.isEnabled() && iconifyToTray.isSelected()) {
                    if (tray.isEnabled() && !tray.isSelected()) {
                        tray.setSelected(true);
                        tray();
                    }
                }
                saveSettings();
            }
        });
        if (traySupported) {
            addSideHeader(tb, "Minimize to the system tray:", iconifyToTray, row, 0);
            tb.add(iconifyToTray, row, 1, 1, 2);
        
            row++;
        }
        
        addSideHeader(tb, "Action to take after Cut or Copy to clipboard:", row, 0);

		iconify_modes[NONE]        = "no action";
		iconify_mode_setting[NONE] = "NONE";
		iconify_modes[MINI]        = "minimize";
		iconify_mode_setting[MINI] = "MINIMIZE";

        iconify = new JComboBox(iconify_modes);
        iconify.setBackground(Color.WHITE);
        iconify.setSelectedIndex(NONE);
        iconify.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveSettings();
            }
        });
        tb.add(iconify, row, 1, 1, 2);

        row++;
        
        addSideHeader(tb, "Additional actions:", row, 0);
        
        JButton clear = new JButton("Clear history");
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                history.clear();
                saveHistory();
                for (Iterator iter = histogram.iterator();iter.hasNext();)
                    ((FrequencyButton)iter.next()).frequency(0);
                histogram();
            }
        });
        tb.add(clear, row, 1);
        
        JList logList = new JList(logModel);
        final JScrollPane logScrollPane = new JScrollPane(logList);
        logScrollPane.setPreferredSize(new Dimension(250, 150));
        logScrollPane.setMinimumSize(new Dimension(10, 10));
        logScrollPane.setVisible(false);

        final JToggleButton logToggle = new JToggleButton("Show event log");
        logToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logScrollPane.setVisible(logToggle.isSelected());
                if (logToggle.isSelected())
                    logToggle.setText("Hide event log");
                else
                    logToggle.setText("Show event log");
                p.revalidate();
            }
        });
        tb.add(logToggle, row, 2);
        
        p.add(logScrollPane,BorderLayout.SOUTH);
    
        tb.buildLayout();
        tb.getPanel().setBackground(Color.WHITE);
        
        p.add(tb.getPanel(), BorderLayout.CENTER);
        
        return p;
    }
  
    protected JPanel createAboutPane() {
        JPanel about = new JPanel(new BorderLayout());

        JEditorPane help = new JEditorPane();
        help.setContentType("text/html");
        try {
            help.setPage(Console.class.getResource("/help.html"));
        } catch (IOException e) {
            log.println("ERR: couldn't load help page: "+e);
            e.printStackTrace(System.err);
        }
        help.addHyperlinkListener(new Hyperactive());
        help.setEditable(false);

        JScrollPane helpScrollPane = new JScrollPane(help);
        helpScrollPane.setPreferredSize(new Dimension(250, 150));
        helpScrollPane.setMinimumSize(new Dimension(10, 10));
        
        about.add(helpScrollPane,BorderLayout.CENTER);
        
        return about;
    }
    
    // Create the Clipboard pane, which is the static main area on the lower part of the console
    protected JPanel createClipboardPane() {
        // Create and set up the text panel.
        JPanel textpanel = new JPanel(new BorderLayout());
        
        JPanel historypanel = new JPanel();

        historypanel.setBackground(Color.WHITE);
    
        TableBuilder tb = new TableBuilder(historypanel);
        
        for (int i = 0; i < HISTORY; i++) {
            HistoryButton hb = new HistoryButton(history, i);
            histogram.add(new FrequencyButton(histogram,hb));

			hb.setPreferredSize(new Dimension(10,10));
			hb.setMinimumSize(new Dimension(10,10));
			hb.setMaximumSize(new Dimension(10,10));

            hb.setMargin(new Insets(2, 2, 2, 2));
            hb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    add();
                }
            });
            hb.addMouseListener(new PreviewListener());
            chars.add(hb);
            mimics.add(hb);
            tb.add(hb, 0, i).preferredWidth(new Expression(defButtonWidth)).alignBaseline(true).fill();
        }
        tb.buildLayout();
        
        JPanel frequentpanel = new JPanel();

        frequentpanel.setBackground(Color.WHITE);
    
        tb = new TableBuilder(frequentpanel);
        
        for (int i = 0; i < HISTORY; i++) {
            HistoryButton hb = new FrequentButton(histogram, i);
            histogram.add(new FrequencyButton(histogram,hb));

			hb.setPreferredSize(new Dimension(10,10));
			hb.setMinimumSize(new Dimension(10,10));
			hb.setMaximumSize(new Dimension(10,10));

            hb.setMargin(new Insets(2, 2, 2, 2));
            hb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    add();
                }
            });
            hb.addMouseListener(new PreviewListener());
            chars.add(hb);
            mimics.add(hb);
            tb.add(hb, 0, i).preferredWidth(new Expression(defButtonWidth)).alignBaseline(true).fill();;
        }
        tb.buildLayout();
        
        histoTabs = new JTabbedPane();
        histoTabs.add("Recently used symbols", historypanel);
        histoTabs.add("Frequently used symbols", frequentpanel);
        histoTabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                saveSettings();
            }
        });

        JPanel previewpanel = new JPanel(new BorderLayout());

        //previewpanel.add(new JLabel("Preview", JLabel.CENTER),
        //        BorderLayout.NORTH);
        previewpanel.setBackground(Color.WHITE);
        previewpanel.setBorder(BorderFactory.createEtchedBorder());
        preview = new JLabel("     ",JLabel.CENTER);
        preview.setFont(preview.getFont().deriveFont((float) 32));
        previewpanel.add(preview, BorderLayout.CENTER);
        previewtip = new JLabel("               ", JLabel.CENTER);
        previewtip.setFont(previewtip.getFont().deriveFont((float) 8));
        previewpanel.add(previewtip,BorderLayout.SOUTH);
        
        //previewpanel.setMinimumSize(previewpanel.getPreferredSize());
        //previewpanel.setMaximumSize(previewpanel.getPreferredSize());

        JPanel tools = new JPanel();
        tools.setBackground(Color.WHITE);
        tb = new TableBuilder(tools);
        tb.rows().defaultPaddingTop(new Expression(0.0));
        tb.rows().defaultPaddingBottom(new Expression(0.0));
        tb.columns().defaultPaddingLeft(new Expression(0.0));
        tb.columns().defaultPaddingRight(new Expression(0.0));
        tb.add(histoTabs,0,0).fill();
        tb.add(previewpanel,0,1).preferredWidth(new Expression(80.0)).minimumWidth(new Expression(80.0));
        tb.buildLayout();
    
        textpanel.add(tools, BorderLayout.NORTH);

        text = new JTextArea();
        text.setLineWrap(true);
        text.setFont(text.getFont().deriveFont((float) 32));
        KeyStroke[] keys = text.getInputMap().allKeys();
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK);
        if (keys!=null) {
            for (int i = 0;i<keys.length;i++) {
                if (text.getInputMap().get(keys[i]).equals(DefaultEditorKit.copyAction)) {
                    key = keys[i];
                    log.println("DBG: default copy accelerator key ["+key+"]");
                    break;
                }
            }
        }
        log.println("INF: copy accelerator key ["+key+"]");
        text.getActionMap().put(text.getInputMap().get(key),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    copy();
                    iconify();
                }
            }
        );
        key = KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK);
        if (keys!=null) {
            for (int i = 0;i<keys.length;i++) {
                if (text.getInputMap().get(keys[i]).equals(DefaultEditorKit.cutAction)) {
                    key = keys[i];
                    log.println("DBG: default cut accelerator key ["+key+"]");
                    break;
                }
            }
        }
        log.println("INF: cut accelerator key ["+key+"]");
        text.getActionMap().put(text.getInputMap().get(key),
            new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    cut();
                    iconify();
                }
            }
        );
        
        textscroll = new JScrollPane(text);
        textpanel.add(textscroll, BorderLayout.CENTER);

        font = new JComboBox();
        font.setBackground(Color.WHITE);
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        for (int i = 0; i < fonts.length; i++) {
            font.addItem(fonts[i].getFontName());
        }
        font.setSelectedItem(text.getFont().getFontName());
        font.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                font();
                saveSettings();
            }
        });
    
        textpanel.add(font, BorderLayout.EAST);
    
        JPanel buttons = new JPanel(new GridLayout(1, 4));

        JButton cut = new JButton("cut");

        cut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cut();
                iconify();
            }
        });
/*
        if (cs == null) {
            cut.setEnabled(false);
            log.println("INF: disabled cut button");
        }
*/
        buttons.add(cut);
    
        JButton copy = new JButton("copy");

        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                copy();
                iconify();
            }
        });
/*
        if (cs == null) {
            copy.setEnabled(false);
            log.println("INF: disabled copy button");
        }
*/
        buttons.add(copy);
    
        JButton delete = new JButton("remove");

        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
        buttons.add(delete);

        JButton clear = new JButton("clear");

        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        buttons.add(clear);
    
        textpanel.add(buttons, BorderLayout.SOUTH);
    
        return textpanel;
    }
  
    // Public action methods
    
    // Load the settings
    public boolean loadSettings() {
		boolean res = false;
        if (ps!=null) {
            try {
                FileContents fc = ps.get(new URL(bs.getCodeBase(), "settings"));
                if (fc.canRead()) {
                    Properties prop = new Properties();
                    prop.loadFromXML(fc.getInputStream());
                    
                    log.println("INF: loading settings["+prop+"]");

                    if (prop.getProperty("version","0").equals("1.0")) {
                        
                        formatted.setSelected(prop.getProperty("formatted","true").equals("true"));
                        if (formatted.isSelected()) {
                            size.setEnabled(true);
                        } else {
                            size.setEnabled(false);
                        }
                        
						int i = Integer.parseInt(prop.getProperty("size","0"));
						if (i>0)
                            fontSizes.setValue(i);
                        
                        String fnt = prop.getProperty("system_font","");
                        if (!fnt.equals("")) {
                            DefaultComboBoxModel model = (DefaultComboBoxModel)system_font.getModel();
                            if (model.getIndexOf(fnt)>=0) {
                                model.setSelectedItem(fnt);
                                systemfont();
                            } else
                                log.println("ERR: couldn't select system font["+fnt+"], has it been removed from the system?");
                        }
                        
                        fnt = prop.getProperty("clipboard_font","");
                        if (!fnt.equals("")) {
                            DefaultComboBoxModel model = (DefaultComboBoxModel)font.getModel();
                            if (model.getIndexOf(fnt)>=0) {
                                model.setSelectedItem(fnt);
                                font();
                            } else
                                log.println("ERR: couldn't select clipboard font["+fnt+"], has it been removed from the system?");
                        }
                        
                        String map = prop.getProperty("convert_map","");
                        if (!map.equals("")) {
                            DefaultComboBoxModel model = (DefaultComboBoxModel)convert_maps.getModel();
                            if (model.getIndexOf(map)>=0) {
                                model.setSelectedItem(map);
                                convertfont();
                            }
                        }
                        
                        String tab = prop.getProperty("histo_tab","");
                        if (!tab.equals("")) {
							for (int t=0;t<histoTabs.getTabCount();t++) {
								if (histoTabs.getTitleAt(t).equals(tab)) {
									histoTabs.setSelectedIndex(t);
									break;
								}
							}
                        }
                        
                        String mode = prop.getProperty("normalize","");
                        if (!mode.equals("")) {
							for (int s = 0;s<normalize_mode_setting.length;s++) {
								if (normalize_mode_setting[s].equals(mode)) {
									normalize.setSelectedIndex(s);
									break;
								}
							}
                        }
                        
                        mode = prop.getProperty("iconify","");
                        if (!mode.equals("")) {
							for (int s = 0;s<iconify_mode_setting.length;s++) {
								if (iconify_mode_setting[s].equals(mode)) {
									iconify.setSelectedIndex(s);
									break;
								}
							}
                        }
                        
                        tray.setSelected(prop.getProperty("tray","true").equals("true") && traySupported);
                        tray();
                        
                        iconifyToTray.setSelected(prop.getProperty("iconify_to_tray","true").equals("true") && traySupported && iconifyToTray.isEnabled());
                        
                        log.println("INF: loaded settings");
                        res = true;
                    } else
                        log.println("ERR: couldn't load settings: unknown version["+prop.getProperty("version","0")+"]");
                } else
                    log.println("ERR: couldn't load settings: not allowed to read");
            } catch (java.io.FileNotFoundException e) {
            } catch (Exception e) {
                log.println("ERR: couldn't load the settings:" + e);
            	e.printStackTrace(System.err);
            }
        }
        return res;
    }
  
    // Save the settings
    public boolean saveSettings() {
        if (loaded) {
            if (ps != null) {
                try {
                    long size = ps.create(new URL(bs.getCodeBase(), "settings"),
                            65536);
                } catch (Exception e) {}
                try {
                    Properties prop = new Properties();
                    
                    prop.setProperty("version","1.0");
    
                    prop.setProperty("formatted",(formatted.isSelected()?"true":"false"));
                    prop.setProperty("size",size.getValue().toString());
                    prop.setProperty("system_font",system_font.getSelectedItem().toString());
                    prop.setProperty("clipboard_font",font.getSelectedItem().toString());
                    prop.setProperty("convert_map",convert_maps.getSelectedItem().toString());
                    prop.setProperty("histo_tab",histoTabs.getTitleAt(histoTabs.getSelectedIndex()));
                    prop.setProperty("normalize",normalize_mode_setting[normalize.getSelectedIndex()]);
                    prop.setProperty("tray",(tray.isSelected()?"true":"false"));
                    prop.setProperty("iconify_to_tray",(iconifyToTray.isSelected()?"true":"false"));
                    prop.setProperty("iconify",iconify_mode_setting[iconify.getSelectedIndex()]);
                    
                    FileContents fc = ps.get(new URL(bs.getCodeBase(), "settings"));
                    if (fc.canWrite()) {
                        prop.storeToXML(fc.getOutputStream(true), null, "UTF-8");
                        log.println("INF: saved settings["+prop+"]");
                        return true;
                    } else
                        log.println("ERR: couldn't save the settings: not allowed to write");
                } catch (Exception e) {
                    log.println("ERR: couldn't save the settings:" + e);
                    e.printStackTrace(System.err);
                }
            }
        }
        return false;
    }

	// load the history
	public boolean loadHistory() {
		boolean res = false;
       	if (ps!=null) {
       		try {
              	FileContents fc = ps.get(new URL(bs.getCodeBase(), "history"));
               	if (fc.canRead()) {
               		DataInputStream is = new DataInputStream(fc.getInputStream());
                   	String histo = is.readUTF();
                   	//log.println("DBG: load history ["+histo+"]");
					history.clear();
                   	String[] entries = histo.split("\n");
                   	for (int i=0;i<entries.length;i++) {
                   		if (!entries[i].trim().equals("")) {
                           	String label = entries[i].trim();
       						for (Enumeration e = chars.getElements(); e.hasMoreElements();) {
           						JToggleButton button = (JToggleButton) e.nextElement();
								if (!(button instanceof HistoryButton)) {
									if (button.getText().equals(label)) {
										history.add(button);
										break;
									}
								}
                           	}
                       	}
                   	}
                   	is.close();
                   	log.println("INF: loaded history");
					res = true;
               	} else
                   	log.println("ERR: couldn't load history: not allowed to read");
       		} catch (java.io.FileNotFoundException e) {
       		} catch (Exception e) {
           		log.println("ERR: couldn't load the histogram:" + e);
       		}
        }
		return res;
	}

	// save the history
	public boolean saveHistory() {
		boolean res = false;
        if (ps != null) {
            try {
                long size = ps.create(new URL(bs.getCodeBase(), "history"),
                        65536);
            } catch (Exception e) {}
        	try {
           	    FileContents fc = ps.get(new URL(bs.getCodeBase(), "history"));
           	    if (fc.canWrite()) {
           	        DataOutputStream os = new DataOutputStream(fc.getOutputStream(true));
                    String entries = "";
                    for (Iterator iter = history.iterator();iter.hasNext();) {
                        AbstractButton entry = (AbstractButton)iter.next();
                       	entries += ""+entry.getText()+"\n";
                    }
                    //log.println("DBG: save history ["+entries+"]");
                    os.writeUTF(entries);
                    os.flush();
                    os.close();
                    log.println("INF: saved history");
    				res = true;
                } else
                    log.println("ERR: couldn't save the history: not allowed to write");
            } catch (Exception e) {
                log.println("ERR: couldn't save the history:" + e);
                e.printStackTrace(System.err);
            }
        }
		return res;
	}
    
	// load the histogram
	public boolean loadHistogram() {
		boolean res = false;
        if (ps!=null) {
        	try {
                FileContents fc = ps.get(new URL(bs.getCodeBase(), "histogram"));
                if (fc.canRead()) {
                    DataInputStream is = new DataInputStream(fc.getInputStream());
                    String histo = is.readUTF();
                    //log.println("DBG: load histogram ["+histo+"]");
                    String label = null;
                    String freq  = null;
                    String[] entries = histo.split("\n");
                    for (int e=0;e<entries.length;e++) {
                        if (!entries[e].trim().equals("")) {
                            String[] split = entries[e].split(":=",2);
                            if (split.length==2) {
                                label = split[0];
                                freq  = split[1];
                    			for (Iterator iter = histogram.iterator();iter.hasNext();) {
                        			FrequencyButton button = (FrequencyButton)iter.next();
									if (button.button().getText().equals(label)) {
										button.frequency(Integer.parseInt(freq));
										break;
									}
								}
                            } else
                                log.println("ERR: can't load histogram entry: "+entries[e]);
                        }
                    }
                    is.close();
                    log.println("INF: loaded histogram");
					res = true;
                } else
                    log.println("ERR: couldn't load histogram: not allowed to read");
        	} catch (java.io.FileNotFoundException e) {
        	} catch (Exception e) {
           		log.println("ERR: couldn't load the histogram:" + e);
        	}
        }
		if (res)
			histogram();
		return res;
	}

	// save the histogram
	public boolean saveHistogram() {
		boolean res = false;
        if (ps != null) {
            try {
                long size = ps.create(new URL(bs.getCodeBase(),"histogram"),
                    65536);
            } catch (Exception e) {}
            try {
                FileContents fc = ps.get(new URL(bs.getCodeBase(), "histogram"));
                if (fc.canWrite()) {
                    DataOutputStream os = new DataOutputStream(fc.getOutputStream(true));
                    String entries = "";
                    for (Iterator iter = histogram.iterator();iter.hasNext();) {
                        FrequencyButton entry = (FrequencyButton)iter.next();
						if (entry.frequency()>0) {
                       	    entries += ""+entry.button().getText()+":=";
                            entries += ""+entry.frequency()+"\n";
						}
                    }
                    //log.println("DBG: save histogram ["+entries+"]");
                    os.writeUTF(entries);
                    os.flush();
                    os.close();
                    log.println("INF: saved histogram");
					res = true;
                } else
                    log.println("ERR: couldn't save the histogram: not allowed to write");
            } catch (Exception e) {
                log.println("ERR: couldn't save the histogram:" + e);
                e.printStackTrace(System.err);
            }
        }
		return res;
	}
    
    // Convert a string
    public void convert(String convert) {
        String converted = conversion.convert(
                convert_maps.getSelectedItem().toString(), convert);

        if (converted != null) {
            text.setText(converted);
        } else {
            log.println("ERR: conversion failed");
        }
    }
  
    // Convert the string pasted in the Import pane
    protected void convert() {
        convert(convert.getText());
    }
  
    // Fix or unfix the string pasted in the Import pane
    protected void fix() {
        if (fix.isSelected()) {
            //fix the current convert text
            unfixed = convert.getText();
            fixed = conversion.lift(
                convert_maps.getSelectedItem().toString(), unfixed);
            if (fixed != null) {
                // show fixed text in readonly
                convert.setText(fixed);
                convert.setEnabled(false);
                paste.setEnabled(false);
                fix.setText("back to general mode");
                log.println("DBG: fix -> unfixed["+unfixed+"] fixed["+fixed+"]");
            } else {
                log.println("ERR: fix failed");
                fix.setSelected(false);
            }
        } else {
            // restore the unfixed text
            log.println("DBG: unfix -> fixed["+fixed+"] unfixed["+unfixed+"]");
            convert.setText(unfixed);
            convert.setEnabled(true);
            paste.setEnabled(true);
            fix.setText("try alternate mode");
        }
    }

    // Add a button to the history
    protected void history(JToggleButton button) {
        if (button instanceof FrequentButton) {
            button = ((FrequentButton)button).button();
        }
        if (!(button instanceof HistoryButton)) {
            if (!history.contains(button)) {
                history.add(button);
                if (history.size() > HISTORY) {
                    history.remove(0);
                }
            }
			saveHistory();
        }
        mimic();
        chars.clearSelection();
    }
    
    protected void histogram() {
        Collections.sort(histogram);
        mimic();
		saveHistogram();
    }
    
    protected void mimic() {
        for (Iterator iter = mimics.iterator(); iter.hasNext();) {
            ((HistoryButton) iter.next()).mimic();
        }
    }
    
    // Add the selected button
    public void add() {
        try {
            if (chars.getSelection() != null) {
                for (Enumeration e = chars.getElements(); e.hasMoreElements();) {
                    JToggleButton button = (JToggleButton) e.nextElement();

                    if (button.isSelected()) {
                        if ((text.getSelectionEnd() - text.getSelectionStart())
                                != 0) {
                            text.getDocument().remove(text.getSelectionStart(),
                                    text.getSelectionEnd()
                                    - text.getSelectionStart());
                        }
                        text.getDocument().insertString(text.getCaretPosition(),
                                button.getText(), null);
                        history(button);
                    }
                }
            }
        } catch (javax.swing.text.BadLocationException e) {
            log.println("ERR: couldn't add the selected character:" + e);
        }
        text.requestFocusInWindow();
    }

    // Copy the selected Unicode IPA text to the clipboard
    public void copy() {
        boolean reset = false;

        if ((text.getSelectionEnd() - text.getSelectionStart()) == 0) {
            text.setSelectionStart(0);
            text.setSelectionEnd(text.getText().length());
            reset = true;
        }
	
	String clip = text.getSelectedText();

        if (cs != null) {
		Transferable t = null;
		
		switch (normalize.getSelectedIndex()) {
			case DECOMP: {
			    String org = clip;
			    clip = java.text.Normalizer.normalize(clip,java.text.Normalizer.Form.NFD);
			    log.println("INF: canonical decomposition: original["+org+"]["+org.length()+"] normalized["+clip+"]["+clip.length()+"]");
			}; break;
			case COMP: {
			    String org = clip;
			    clip = java.text.Normalizer.normalize(clip,java.text.Normalizer.Form.NFC);
			    log.println("INF: canonical composition: original["+org+"]["+org.length()+"] normalized["+clip+"]["+clip.length()+"]");
			}; break;
		}
		
		if (formatted.isSelected()) {
			t = new Selection(text.getFont(),Integer.parseInt(size.getValue().toString()),clip);
			log.println("DBG: formatted(text["+clip+"]) on clipboard service");
		} else {
			t = new StringSelection(clip);
			log.println("DBG: plain(text["+clip+"]) on clipboard service");
		}
      
		cs.setContents(t);
        } else {
		try { 
			StringSelection t = new StringSelection(clip);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t,t);
			log.println("DBG: plain(text["+clip+"]) on system clipboard");
		} catch(Exception e) {
			log.println("ERR: can't get access to the clipboard");
		}
        }
        if (reset) {
            text.setSelectionStart(0);
            text.setSelectionEnd(0);
        }
        text.requestFocusInWindow();
    }

    // Cut the selected Unicode IPA text to the clipboard
    public void cut() {
        if ((text.getSelectionEnd() - text.getSelectionStart()) == 0) {
            text.setSelectionStart(0);
            text.setSelectionEnd(text.getText().length());
        }
        copy();
        if ((text.getSelectionEnd() - text.getSelectionStart()) != 0) {
            try {
                text.getDocument().remove(text.getSelectionStart(),
                        text.getSelectionEnd() - text.getSelectionStart());
            } catch (javax.swing.text.BadLocationException e) {
                log.println(
                        "ERR: couldn't delete the selected character:" + e);
            }
        } else {
            clear();
        }
        text.requestFocusInWindow();
    }

    // Delete the selected text or the last character
    public void delete() {
        try {
            if ((text.getSelectionEnd() - text.getSelectionStart()) != 0) {
                text.getDocument().remove(text.getSelectionStart(),
                    text.getSelectionEnd() - text.getSelectionStart());
            } else if (!text.getText().equals("")) {
                if (text.getCaretPosition()>0) {
                    int cp  = text.getText().codePointBefore(text.getCaretPosition());
                    int len = Character.charCount(cp);
                    text.getDocument().remove(text.getCaretPosition() - len, len);
                }
            }
        } catch (javax.swing.text.BadLocationException e) {
            log.println("ERR: couldn't remove the last character:" + e);
        }
        text.requestFocusInWindow();
    }

    // Clear the Unicode IPA text
    public void clear() {
        text.setText("");
        text.requestFocusInWindow();
    }
  
    // Find a specific font
    protected Font findFont(String name) {
        Font f = null;
        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();

        for (int i = 0; i < fonts.length; i++) {
            if (fonts[i].getFontName().equals(name)) {
                f = fonts[i];
                break;
            }
        }
        return f;
    }
  
    // Find all the fonts that can display all Unicode IPA codepoints
    protected static Vector f = null;
    protected Vector findSystemFonts() {
        if (f==null) {
            f = new Vector();
            Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    
            for (int i = 0; i < fonts.length; i++) {
                if (fonts[i].isPlain()) {
                    boolean all = true;
    
                    for (Enumeration e = chars.getElements(); all
                            && e.hasMoreElements();) {
                        JToggleButton button = (JToggleButton) e.nextElement();
                        String label = button.getText();
                        int len = label.codePointCount(0, label.length());
    
                        for (int c = 0; all && c < len; c++) {
                            if (!fonts[i].canDisplay(label.codePointAt(c))) {
                                all = false;
                            }
                        }
                    }
                    if (all) {
                        f.add(fonts[i]);
                    }
                }
            }
        }
        
        return f;
    }
  
    // Get the system font, i.e. the font used to render the Unicode IPA symbols
    public Font getSystemFont() {
        Vector pre = new Vector();
        Vector post = new Vector();

        post.add("DejaVu LGC Sans"); // prefered fonts
	if (!System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
		post.add("Arial Unicode MS");
	else
		post.add("ArialUnicodeMS");
        Font f = null;

        // can we find any of the pre fonts?
        for (Iterator iter = pre.iterator(); iter.hasNext();) {
            f = findFont((String) iter.next());
            if (f != null) {
                break;
            }
        }
        // can we find a fully compatible font?
        if (f == null) {
            Vector fonts = findSystemFonts();

            if (fonts.size() > 0) {
                f = (Font) fonts.get(0); // take the first font
            }
        }
        // can we find any of the post fonts
        if (f == null) {
            for (Iterator iter = post.iterator(); iter.hasNext();) {
                f = findFont((String) iter.next());
                if (f != null) {
                    String m = "MSG: Couldn't find a full compatible Unicode font on your machine.\n";

                    m += "MSG: Now using the '" + f.getFontName()
                            + "' font which has most IPA symbols,\n";
                    m += "MSG: however some alternative characters may not render properly.";
                    log.println(m);
                    break;
                }
            }
        }
        // still no compatible font, show the user a message
        if (f == null) {
            msg += "Couldn't find a (full) IPA compatible Unicode font on your machine. Some characters may not render properly.\n";
            if (pre.size() > 0) {
                msg += "You may try to install one of the following complete fonts:\n";
                for (Iterator iter = pre.iterator(); iter.hasNext();) {
                    msg += "- " + iter.next() + "\n";
                }
            }
            if (post.size() > 0) {
                if (pre.size() > 0) {
                    msg += "Or one of the following almost complete fonts:\n";
                } else {
                    msg += "You may try to install one of the following almost complete Unicode fonts:\n";
                }
                for (Iterator iter = post.iterator(); iter.hasNext();) {
                    msg += "- " + iter.next() + "\n";
                }
            }
            log.println("INF: using default font to show IPA symbols");
        } else {
            log.println(
                    "INF: using Unicode font [" + f.getFontName()
                    + "] to show IPA symbols");
        }
        return f;
    }

    // Use a specific font as system font
    protected void useSystemFont(Font f) {
        for (Enumeration e = chars.getElements(); e.hasMoreElements();) {
            JToggleButton button = (JToggleButton) e.nextElement();

            if (f != null) {
                button.setFont(f.deriveFont((float) button.getFont().getSize()));
            }
        }
        preview.setFont(f.deriveFont((float) preview.getFont().getSize()));
        system = f;
        log.println(
                "INF: using font [" + system.getFontName()
                + "] to show IPA symbols");
    }
  
    // Use a specific font as system font
    protected boolean useSystemFont(String name) {
        if (system.getFontName().equals(name)) {
            return true;
        }
        Font f = findFont(name);

        if (f != null) {
            useSystemFont(f);
            return true;
        }
        return false;
    }
  
    // Use a specific font as convert font
    protected void useConvertFont(Font f) {
        convert.setFont(f.deriveFont((float) convert.getFont().getSize()));
    }
  
    // Use a specific font as convert font
    protected boolean useConvertFont(String name) {
        if (convert.getFont().getFontName().equals(name)) {
            return true;
        }
        Font f = findFont(name);

        if (f != null) {
            useConvertFont(f);
            return true;
        }
        return false;
    }

    // Use a specific font as text font
    public void font(Font f) {
        text.setFont(f.deriveFont((float) text.getFont().getSize()));
        font.setSelectedItem(text.getFont().getFontName());
        log.println(
                "INF: using font [" + f.getFontName() + "] to show the text");
    }

    // Use a specific font as text font
    public boolean font(String name) {
        if (text.getFont().getFontName().equals(name)) {
            return true;
        }
        Font f = findFont(name);

        if (f != null) {
            font(f);
            return true;
        }
        return false;
    }
  
    // Use the selected font as text font
    protected void font() {
        if (!font(font.getSelectedItem().toString())) {
            log.println(
                    "ERR: couldn't change to font ["
                    + font.getSelectedItem().toString()+"]");
        }
        text.requestFocusInWindow();
        font.setSelectedItem(text.getFont().getFontName());
    }
  
    // Use the selected font as system font
    protected void systemfont() {
        if (!useSystemFont(system_font.getSelectedItem().toString())) {
            log.println(
                    "ERR: couldn't change to system font ["
                            + system_font.getSelectedItem().toString() + "]");
        }
        text.requestFocusInWindow();
        system_font.setSelectedItem(system.getFontName());
    }

    // Use the selected map's byte font as convert font
    protected void convertfont() {
        String convert_font = conversion.font(convert_maps.getSelectedItem().toString());
        if ((convert_font!=null) && !useConvertFont(convert_font))
            log.println("ERR: couldn't find IPA font [" + convert_font + "] belonging to map [" + convert_maps.getSelectedItem()+"]");
        else
            log.println("DBG: found IPA font [" + convert_font + "] belonging to map [" + convert_maps.getSelectedItem()+"]");
    }
    
    // Paste text from the clipboard into the convert text area
    protected void paste_convert() {
        if (cs!=null) {
            try {
                if (convert.isEnabled()) {
                    convert.getDocument().insertString(convert.getCaretPosition(),cs.getContents().getTransferData(DataFlavor.stringFlavor).toString(),null);
                }
            } catch(Exception e) {
                log.println("ERR: couldn't paste from clipboard: "+e);
            }
        }
    }
  
    // Clear the convert text area
    protected void clear_convert() {
        unfixed = null;
        fixed = null;
        convert.setText("");
        convert.setEnabled(true);
        paste.setEnabled(true);
        fix.setSelected(false);
        fix.setText("try alternate mode");
    }

    // Show the user mesages
    protected void showMessages() {
        if ((frame != null) && (msg != null) && (!msg.trim().equals(""))) {
            JOptionPane.showMessageDialog(frame, msg);
        }
        msg = null;
    }
    
    // Put the application in the system tray
    protected boolean tray() {
        try {
            if (tray.isEnabled()) {
                if (tray.isSelected()) {
                    // put it in the tray
                    if (trayIcon==null) {
                        ImageIcon icon = null;
                        
                        double h = SystemTray.getSystemTray().getTrayIconSize().getHeight();
                        log.println("INF: tray: height["+h+"]");
                        if (64<=h) {
                            icon = new ImageIcon(Console.class.getResource("/icon-64.png"));
                            log.println("INF: tray: use 64 pixel version of the icon");
                        } else if (16>=h) {
                            icon = new ImageIcon(Console.class.getResource("/icon-16.png"));
                            log.println("INF: tray: use 16 pixel version of the icon");
                        } else {
                            icon = new ImageIcon(Console.class.getResource("/icon-32.png"));
                            log.println("INF: tray: use 32 pixel version of the icon");
                        }
                        
                        PopupMenu popup = new PopupMenu();
                        trayMenu = new MenuItem("Hide IPA Console");
                        trayMenu.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (trayMenu.getLabel().equals("Show IPA Console")) {
                                    visible(true);
                                } else {
                                    visible(false);
                                }
                            }
                        });
                        frame.addWindowStateListener(new WindowStateListener() {
                            public void windowStateChanged(WindowEvent e) {
                                if (e.getNewState()==frame.ICONIFIED) {
                                    if (iconifyToTray.isEnabled() && iconifyToTray.isSelected())
                                        if (tray.isEnabled() && tray.isSelected())
                                            visible(false);
                                }
                            }
                        });
                        popup.add(trayMenu);
                        popup.addSeparator();
                        MenuItem exitItem = new MenuItem("Exit");
                        exitItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                exit();
                            }
                        });
                        popup.add(exitItem);
                        trayIcon = new TrayIcon(icon.getImage(),"IPA Console",popup);
                        trayIcon.setImageAutoSize(true);
                        trayIcon.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                if (trayMenu.getLabel().equals("Show IPA Console")) {
                                    visible(true);
                                } else {
                                    visible(false);
                                }
                            }
                        });
                    }
                    if (SystemTray.getSystemTray().getTrayIcons().length==0)
                        SystemTray.getSystemTray().add(trayIcon);
                } else {
                    // get it out of the tray
                    if (trayIcon!=null)
                        SystemTray.getSystemTray().remove(trayIcon);
                    iconifyToTray.setSelected(false);
                }
            }
        } catch(Exception e) {
            log.println("ERR: couldn't access the system tray: "+e);
            tray.setSelected(false);
            tray.setEnabled(false);
            iconifyToTray.setSelected(false);
            iconifyToTray.setEnabled(false);
            return false;
        }
        return true;
    }
  
    // Iconify the application
    protected boolean iconify() {
        boolean res = false;
        
        if(iconify.getSelectedIndex()==MINI) {
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(frame.ICONIFIED)) {
                if (frame != null) {
                    frame.setExtendedState(frame.ICONIFIED);
                    res = true;
                }
            }
        }
        return res;
    }
    
    // Class to handle history buttons, which mimic other existing buttons
    class HistoryButton extends JToggleButton {
    
        java.util.List history = null;
        int pos = 0;
    
        public HistoryButton(java.util.List history, int pos) {
            super(" ");
            this.history = history;
            this.pos = pos;
            mimic();
        }
    
        public boolean mimic() {
            boolean res = false;

            JToggleButton button = button();
            if (button!=null) {
                setText(button.getText());
                setToolTipText(button.getToolTipText());
                res = true;
                //log.println("DBG: HistoryButton["+pos+"] "+"["+button.getText()+"]");
            }
            setVisible(res);
            setEnabled(res);
            revalidate();
            return res;
        }
        
        public JToggleButton button() {
            if ((history != null) && (pos < history.size())) {
                Object o = history.get(pos);
                if (o instanceof JToggleButton)
                    return (JToggleButton)o;
                if (o instanceof FrequencyButton) {
                    FrequencyButton fb = (FrequencyButton)o;
                    if (fb.frequency()>0) {
                        //log.println("DBG: FrequencyButton["+pos+"] ["+fb.frequency()+"] ["+fb.button().getText()+"]");
                        return (JToggleButton)fb.button();
                    }
                }
            }
            return null;
        }
    
    }
  
    // Class the handle the special rendering needs of the Vowels table
    class VowelsPanel extends TablePanel {

        Color color = null;
        JComponent top = null;
        JComponent left = null;

        public VowelsPanel(Color c) {
            super();
            color = c;
        }
    
        public void setOffsets(JComponent t, JComponent l) {
            top = t;
            left = l;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(color);
      
            int l = 0;
            int t = 0;
      
            if (left != null) {
                l = left.getWidth();
            }
      
            if (top != null) {
                t = top.getHeight();
            }
      
            int w = (this.getWidth() - l) / 17;
            int h = (this.getHeight() - t) / 7;
      
            int w_o = l + (w / 2);
            int h_o = t + (h / 2);
      
            // (front,close) to (back,close)
            g.drawLine(w_o + (1 * w), h_o + (0 * h), w_o + (15 * w),
                    h_o + (0 * h));
            // (~front,close-mid) to (back,close-mid)
            g.drawLine(w_o + (3 * w), h_o + (2 * h), w_o + (15 * w),
                    h_o + (2 * h));
            // (~front,open-mid) to (back,open-mid)
            g.drawLine(w_o + (5 * w), h_o + (4 * h), w_o + (15 * w),
                    h_o + (4 * h));
            // (~central,open) to (back,open)
            g.drawLine(w_o + (7 * w), h_o + (6 * h), w_o + (15 * w),
                    h_o + (6 * h));
            // (front,close) to (~central,open)
            g.drawLine(w_o + (1 * w), h_o + (0 * h), w_o + (7 * w),
                    h_o + (6 * h));
            // (central,close) to (~central,open)
            g.drawLine(w_o + (9 * w), h_o + (0 * h), w_o + (12 * w),
                    h_o + (6 * h));
            // (back,close) to (back,open)
            g.drawLine(w_o + (15 * w), h_o + (0 * h), w_o + (15 * w),
                    h_o + (6 * h));
        }
    }
  
    // Class to handle the preview
    class PreviewListener extends javax.swing.event.MouseInputAdapter {
    
        public void mouseEntered(MouseEvent e) {
            JToggleButton button = (JToggleButton) e.getComponent();

            if (button.isEnabled()) {
                if (preview != null) {
                    preview.setText("  " + button.getText() + "  ");
                }
                if (previewtip != null) {
                    String tip = button.getToolTipText();
                    if (tip!=null) {
                        if (tip.length()>15)
                            tip = tip.substring(0,13)+'\u2026';
                        previewtip.setText(tip);
                    } else
                        previewtip.setText(" ");
                }
            }
        }
    
        public void mouseExited(MouseEvent e) {
            if (preview != null) {
                preview.setText("   ");
            }
            if (previewtip != null) {
                previewtip.setText(" ");
            }
        }
    }
  
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        // Make sure we have nice window decorations.
        // JFrame.setDefaultLookAndFeelDecorated(true);
        
        // Create and set up the window.
        JFrame frame = new JFrame(
                "THE INTERNATIONAL PHONETIC ALPHABET (revised to 2005)");

        //frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);

        Console console = new Console(frame);

        console.setBackground(Color.WHITE);
        frame.getContentPane().add(console);

        // Display the window.
        frame.pack();
        frame.setVisible(true);
        
        // Fix some display issues
        console.fixDisplay();
    
        // Display any pending messages
        console.showMessages();
    }

    public static void main(String[] args) {
		System.err.println("INF: Welcome to the IPA Console!");
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
  
    private static class Selection implements Transferable {
        private static ArrayList Flavors = new ArrayList();
    
        static {
            try {
                Flavors.add(new DataFlavor("text/plain;class=java.lang.String"));
                Flavors.add(new DataFlavor("text/plain;class=java.io.Reader"));
                Flavors.add(new DataFlavor("text/plain;charset=Unicode;class=java.io.InputStream"));
                Flavors.add(new DataFlavor("text/rtf;class=java.lang.String"));
                Flavors.add(new DataFlavor("text/rtf;class=java.io.Reader"));
                Flavors.add(new DataFlavor("text/rtf;charset=Unicode;class=java.io.InputStream"));
                Flavors.add(new DataFlavor("text/html;class=java.lang.String"));
                Flavors.add(new DataFlavor("text/html;class=java.io.Reader"));
                Flavors.add(new DataFlavor("text/html;charset=Unicode;class=java.io.InputStream"));
            } catch (ClassNotFoundException ex) {
                log.println("ERR: couldn't initialize clipboard format: "+ex);
                ex.printStackTrace(System.err);
            }
        }
    
        private String text = "";
        private String rtf = "";
        private String html = "";
    
        public Selection(Font f,int s,String t) {
            log.println("DBG: CLIP(font["+(f!=null?f.getName():"<null>")+"],size["+s+"],text["+(t!=null?t:"<null>")+"])");
            // plain text
			this.text = t;
            // RTF
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				com.lowagie.text.Document doc = new com.lowagie.text.Document();
				com.lowagie.text.rtf.RtfWriter.getInstance(doc,baos);
				doc.open();
				com.lowagie.text.Font fnt = new com.lowagie.text.rtf.style.RtfFont(f.getName(),(new Integer(s)).floatValue());
                if (fnt!=null)
                    doc.add(new com.lowagie.text.Chunk(t,fnt));
                else {
                    log.println("ERR: itext library couldn't find font["+f.getName()+"]");
                    doc.add(new com.lowagie.text.Chunk(t));
                }
				doc.close();
            	this.rtf = baos.toString("US-ASCII");
			} catch (Exception e) {
				log.println("ERR: failed to put the IPA text as RTF on the clipboard: "+e);
				e.printStackTrace();
			}
            // HTML
            this.html = "<tt style='"
                      + "font-family:\"" + f.getFontName() + "\";"
                      + (s>0?" font-size:"+s+"pt;":"")
                      + "'>"
                      + t.replace("<","&lt;")
                      + "</tt>";
        }
    
        public DataFlavor[] getTransferDataFlavors() {
            return (DataFlavor[]) Flavors.toArray(
                    new DataFlavor[Flavors.size()]);
        }
    
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Flavors.contains(flavor);
        }
    
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		log.println("DBG: clipboard request:mime["+flavor.getMimeType()+"]");
            String out = this.text;
            if (flavor.getMimeType().startsWith("text/rtf")) {
                out = this.rtf;
            } else {
                if (flavor.getMimeType().startsWith("text/html")) {
                    out = this.html;
                }
            }
            log.println("DBG: clip(["+out+"])");
            if (String.class.equals(flavor.getRepresentationClass())) {
                return out;
            }
            if (Reader.class.equals(flavor.getRepresentationClass())) {
                return new StringReader(out);
            }
            if (InputStream.class.equals(flavor.getRepresentationClass())) {
                return new StringBufferInputStream(out);
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    /**
     * HtmlSelection class thanks to Elliott Hughes
     * see http://elliotth.blogspot.com/2005/01/copying-html-to-clipboard-from-java.html
     **/
    private static class HtmlSelection implements Transferable {
        private static ArrayList htmlFlavors = new ArrayList();
    
        static {
            try {
                htmlFlavors.add(new DataFlavor("text/plain;class=java.lang.String"));
                htmlFlavors.add(new DataFlavor("text/html;class=java.lang.String"));
                htmlFlavors.add(new DataFlavor("text/html;class=java.io.Reader"));
                htmlFlavors.add(new DataFlavor("text/html;charset=Unicode;class=java.io.InputStream"));
            } catch (ClassNotFoundException ex) {
                log.println("ERR: couldn't initialize HTML clipboard format: "+ex);
                ex.printStackTrace(System.err);
            }
        }
    
        private String text = "";
        private String html = "";
    
        public HtmlSelection(Font f,int s,String t,String tag) {
            log.println("DBG: HTML(font["+(f!=null?f.getName():"<null>")+"],size["+s+"],text["+(t!=null?t:"<null>")+"],tag["+tag+"])");
			this.text = t;
            this.html = "<"+tag+" style='"
                      + "font-family:\"" + f.getFontName() + "\";"
                      + (s>0?" font-size:"+s+"pt;":"")
                      + "'>"
                      + t.replace("<","&lt;")
                      + "</"+tag+">";
            log.println("DBG: HTML["+this.html+"]");
        }
    
        public DataFlavor[] getTransferDataFlavors() {
            return (DataFlavor[]) htmlFlavors.toArray(
                    new DataFlavor[htmlFlavors.size()]);
        }
    
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return htmlFlavors.contains(flavor);
        }
    
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (String.class.equals(flavor.getRepresentationClass())) {
				if (flavor.getMimeType().startsWith("text/plain"))
					return text;
                return html;
            } else if (Reader.class.equals(flavor.getRepresentationClass())) {
                return new StringReader(html);
            } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
                return new StringBufferInputStream(html);
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    private static class RtfSelection implements Transferable {
        private static ArrayList rtfFlavors = new ArrayList();
    
        static {
            try {
                rtfFlavors.add(
                        new DataFlavor("text/plain;class=java.lang.String"));
                rtfFlavors.add(
                        new DataFlavor("text/rtf;class=java.lang.String"));
                rtfFlavors.add(new DataFlavor("text/rtf;class=java.io.Reader"));
                rtfFlavors.add(
                        new DataFlavor(
                                "text/rtf;charset=Unicode;class=java.io.InputStream"));
            } catch (ClassNotFoundException ex) {
                log.println("ERR: couldn't initialize RTF clipboard format: "+ex);
                ex.printStackTrace(System.err);
            }
        }
    
        private String text = "";
        private String rtf = "";
    
        public RtfSelection(Font f,int s,String t) {
            log.println("DBG: RTF(font["+(f!=null?f.getName():"<null>")+"],size["+s+"],text["+(t!=null?t:"<null>")+"])");
			this.text = t;
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				com.lowagie.text.Document doc = new com.lowagie.text.Document();
				com.lowagie.text.rtf.RtfWriter.getInstance(doc,baos);
				doc.open();
				com.lowagie.text.Font fnt = new com.lowagie.text.rtf.style.RtfFont(f.getName(),(new Integer(s)).floatValue());
                if (fnt!=null)
                    doc.add(new com.lowagie.text.Chunk(t,fnt));
                else {
                    log.println("ERR: itext library couldn't find font["+f.getName()+"]");
                    doc.add(new com.lowagie.text.Chunk(t));
                }
				doc.close();
            	this.rtf = baos.toString("US-ASCII");
				//log.println("DBG: RTF["+this.rtf+"]");
/*
                this.rtf  = "{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl{\\f0\\fcharset0 "+f.getFontName()+";}}";
                this.rtf += "\\uc1\\pard\\f0\\fs"+(s*2)+" ";
                for (int i=0;i<t.length();i++) {
                    this.rtf += "\\u"+((int)t.charAt(i))+"?";
                }
                this.rtf += "}";
                log.println("DBG: alternative RTF["+this.rtf+"]");
*/
			} catch (Exception e) {
				log.println("ERR: failed to put the IPA text as RTF on the clipboard: "+e);
				e.printStackTrace();
			}
        }
    
        public DataFlavor[] getTransferDataFlavors() {
            return (DataFlavor[]) rtfFlavors.toArray(
                    new DataFlavor[rtfFlavors.size()]);
        }
    
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return rtfFlavors.contains(flavor);
        }
    
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (String.class.equals(flavor.getRepresentationClass())) {
				if (flavor.getMimeType().startsWith("text/plain"))
					return text;
                return rtf;
            } else if (Reader.class.equals(flavor.getRepresentationClass())) {
                return new StringReader(rtf);
            } else if (InputStream.class.equals(flavor.getRepresentationClass())) {
                return new StringBufferInputStream(rtf);
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
    
    // Hyperactive tries to load followed links from a JEditorPane in the default browser
    class Hyperactive implements HyperlinkListener {
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getURL().getProtocol().equals("jar")) {
                    log.println("DBG: please jump to URL["+e.getURL()+"]");
                     try {
                         ((JEditorPane) e.getSource()).setPage(e.getURL());
                     } catch (Exception ex) {
                        log.println("ERR: jump to URL["+e.getURL()+"] failed: "+ex);
                     }
                } else {
                    log.println("DBG: please load URL["+e.getURL()+"] in the default browser");
                    if (bs!=null) {
                        if (bs.isWebBrowserSupported()) {
                            if (!bs.showDocument(e.getURL()))
                                log.println("ERR: opening URL["+e.getURL()+"] in the default browser failed");
                        } else
                            log.println("ERR: opening URL["+e.getURL()+"] in the default browser isn't supported on this platform");
                    } else {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                            try {
                                Desktop.getDesktop().browse(e.getURL().toURI());
                            } catch(Exception ex) {
                                log.println("ERR: opening URL["+e.getURL()+"] in the default browser failed: "+ex);
                            }
                        } else
                            log.println("ERR: opening URL["+e.getURL()+"] in the default browser isn't supported on this platform");
                    }
                }
            }
        }
    }
    
    protected static UnicodeTableModel codepoints = new UnicodeTableModel("^<.+>$");
    class CharacterChooser extends JDialog implements ListSelectionListener {
        
        protected JTable table = null;
        protected Font system_font = null;
        protected TableRowSorter sorter = null;
        protected JTextField filter = null;
        protected JButton select = null;
        protected String selection = null;
        protected boolean selected = false;
        protected JLabel preview = null;
        protected JTextField descr = null;
        protected JLabel hex = null;
      
        public CharacterChooser() {
            super(frame,"Character chooser",true);
            
            JEditorPane help = new JEditorPane();
            help.setContentType("text/html");
            try {
                help.setPage(Console.class.getResource("/help-addchar.html"));
            } catch (IOException e) {
                log.println("ERR: couldn't load addchar help page: "+e);
                e.printStackTrace(System.err);
            }
            help.addHyperlinkListener(new Hyperactive());
            help.setEditable(false);
        
            JScrollPane helpScrollPane = new JScrollPane(help);
            helpScrollPane.setPreferredSize(new Dimension(250, 67));
            helpScrollPane.setMinimumSize(new Dimension(10, 67));

            JPanel helpmain = new JPanel(new BorderLayout());
            helpmain.setBackground(Color.WHITE);
            helpmain.add(helpScrollPane,BorderLayout.NORTH);
            
            JPanel main = new JPanel(new BorderLayout());
            main.setBackground(Color.WHITE);
            
            JPanel list = new JPanel(new BorderLayout());
            list.setBackground(Color.WHITE);
            
            table = new JTable(codepoints);
            table.setBackground(Color.WHITE);
            sorter = new TableRowSorter(codepoints);
            table.setRowSorter(sorter);
            table.getTableHeader().getColumnModel().getColumn(1).setCellRenderer(new CharacterCellRenderer());
            table.getColumnModel().getColumn(0).setPreferredWidth(table.getColumnModel().getColumn(0).getPreferredWidth()/8);
            table.getColumnModel().getColumn(1).setPreferredWidth(table.getColumnModel().getColumn(1).getPreferredWidth()/4);
            table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getSelectionModel().addListSelectionListener(this);
            
            JScrollPane tableScrollPane = new JScrollPane(table);
            tableScrollPane.setBackground(Color.WHITE);
            
            list.add(tableScrollPane,BorderLayout.CENTER);
            
            JPanel panel = new JPanel(new GridLayout(1, 2));
            panel.setBackground(Color.WHITE);

            hex = new JLabel("");
            panel.add(hex);
            
            preview = new JLabel(" ",SwingConstants.CENTER);
            panel.add(preview);
            
            JPanel panel2 = new JPanel(new GridLayout(1, 2));
            panel2.setBackground(Color.WHITE);
            panel2.add(panel);
            
            descr = new JTextField();
            panel2.add(descr);

            hex.setFont(descr.getFont());
            
            list.add(panel2,BorderLayout.SOUTH);
            
            main.add(list,BorderLayout.CENTER);
            
            JPanel buttons = new JPanel(new GridLayout(1, 2));
            buttons.setBackground(Color.WHITE);

            filter = new JTextField();
            filter.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    filter();
                }
            });
            
            buttons.add(filter);
            
            JButton button = new JButton("search");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    filter();
                }
            });
            buttons.add(button);

            main.add(buttons,BorderLayout.NORTH);
            
            buttons = new JPanel(new GridLayout(1, 2));
            
            button = new JButton("cancel");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    cancel();
                }
            });
            buttons.add(button);

            select = new JButton("add");
            select.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    select();
                }
            });
            select.setEnabled(false);
            buttons.add(select);

            main.add(buttons,BorderLayout.SOUTH);
            
            helpmain.add(main,BorderLayout.CENTER);
            
            getContentPane().add(helpmain);
            pack();

            setLocationRelativeTo(frame);
        }
        
        protected void reset() {
            selected=false;
            selection=null;

            filter.setText("");
            filter();

            table.clearSelection();

            hex.setText("");
            preview.setText(" ");
            descr.setText("");
        }
        
        public String getCharacter(Font system) {
            system_font = system;
            preview.setFont(system_font.deriveFont((float) 32));
            reset();
            setVisible(true);
            if (selected)
                return selection;
            return null;
        }
        
        public String getDescription() {
            if (selection!=null)
                return descr.getText();
            return null;
        }
        
        protected void filter() {
            String f = filter.getText();
            if (!f.trim().equals("")) {
				String re = "^(?iu:.*"+f+".*)$";
                sorter.setRowFilter(RowFilter.regexFilter(re));
				log.println("DBG: row filter["+re+"]: "+table.getRowCount());
                if (table.getRowCount()==0) {
                    String text = filter.getText().trim().toUpperCase();
                    if (text.matches("^(?iu:(U\\+)?\\p{XDigit}+)$")) {
                        String h = text.replaceAll("^[Uu]\\+","");
                        while(h.length()<4)
                            h = "0" + h;
                        h = "U+" + h;
                        hex.setText(h);
                        codepoint();
                    }
                }
            } else {
                sorter.setRowFilter(null);
                log.println("DBG: filter is resetted");
            }
        }
        
        protected void select() {
            selected = true;
            setVisible(false);
        }
        
        protected void cancel() {
            selected = false;
            setVisible(false);
        }
        
        protected void codepoint() {
            String text = hex.getText();
            try {
                int cp = Integer.parseInt(text.replaceAll("^U\\+",""),16);
                int idx = codepoints.getIndex(cp);
                if (idx>=0)
                    table.setRowSelectionInterval(table.convertRowIndexToView(idx),table.convertRowIndexToView(idx));
                else {
                    preview.setText(new String(Character.toChars(cp)));
                    selection = preview.getText();
                    descr.setText("");
                    select.setEnabled(true);
                }
            } catch(Exception e) {
                log.println("ERR: not a codepoint["+text+"]: "+e);
                hex.setText("");
                preview.setText(" ");
                descr.setText("");
                select.setEnabled(false);
            }
        }
        
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                if (table.getSelectedRow()>=0) {
                    hex.setText(codepoints.getCodepoint(table.convertRowIndexToModel(table.getSelectedRow())));
                    selection = codepoints.getCharacter(table.convertRowIndexToModel(table.getSelectedRow()));
                    preview.setText(selection);
                    selection = preview.getText();
                    descr.setText(codepoints.getName(table.convertRowIndexToModel(table.getSelectedRow())));
                    select.setEnabled(true);
                    log.println("INF: selected ["+selection+"]");
                } else {
                    hex.setText("");
                    preview.setText(" ");
                    descr.setText("");
                    select.setEnabled(false);
                    log.println("INF: selected nothing");
                }
            }/* else
                log.println("DBG: adjusting select");*/
        }
        
        class CharacterCellRenderer extends DefaultTableCellRenderer {
            public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column) {
                Component comp = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);
                if (system_font!=null) {
                    comp.setFont(system_font.deriveFont((float) comp.getFont().getSize()));
                }
                return comp;
            }
        }
    }
    
    class UserCharsPanel extends JPanel {
        
        protected Console console = null;
        protected String title = null;
        protected JPanel chars = null;
        protected Vector buttons = new Vector();
        protected JButton del = null;
        
        public UserCharsPanel(Console console,String title) {
            super(new BorderLayout());
            setBackground(Color.WHITE);
            
            this.console = console;
            
            this.title = title;
            add(new JLabel(title),BorderLayout.NORTH);
            
            chars = new JPanel(new FlowLayout(FlowLayout.LEFT));
            chars.setBackground(Color.WHITE);
            add(chars,BorderLayout.CENTER);
            
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
            actions.setBackground(Color.WHITE);
            
            JButton add = new JButton("add");
            add.setToolTipText("add a new character");
            
            add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    add();
                }
            });
        
            actions.add(add);
            
            del = new JButton("delete");
            del.setToolTipText("delete a character");
            
            del.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    del();
                }
            });

            del.setEnabled(false);
            
            actions.add(del);
            
            add(actions,BorderLayout.SOUTH);
            
            load();
        }
        
        // Add a new user defined char
        protected void add() {
            String chr = charChooser.getCharacter(system);
            if (chr!=null) {
                log.println("INF: selected char["+chr+"] description["+charChooser.getDescription()+"]");
                JToggleButton button = addButton(chr,charChooser.getDescription(),false);
                buttons.add(button);
                chars.add(button);
                chars.revalidate();
                del.setEnabled(true);
                save();
            } else
                log.println("INF: cancelled char selection");
        }
        
        // Delete a user defined char
        protected void del() {
            if (buttons.size()>0) {
                Vector vals = new Vector();
                for (Iterator iter=buttons.iterator();iter.hasNext();) {
                    JToggleButton button = (JToggleButton)iter.next();
                    vals.add(""+button.getText()+" - "+button.getToolTipText());
                }
                String sel = (String)JOptionPane.showInputDialog(
                    frame,
                    "Select the character to delete:",
                    "Delete a character",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    vals.toArray(),
                    null);
                if (sel!=null) {
                    JToggleButton button = null;
                    for (Iterator iter=buttons.iterator();iter.hasNext();) {
                        button = (JToggleButton)iter.next();
                        if (sel.equals(""+button.getText()+" - "+button.getToolTipText()))
                            break;
                        button = null;
                    }
                    if (button!=null) {
                        buttons.remove(button);
                        chars.remove(button);
                        chars.revalidate();
                        del.setEnabled(buttons.size()>0);
                        save();
                    }
                }
            }
        }

        protected boolean save() {
            if (ps != null) {
                try {
                    long size = ps.create(new URL(bs.getCodeBase(),title),
                            65536);
                } catch (Exception e) {}
                try {
                    FileContents fc = ps.get(new URL(bs.getCodeBase(), title));
                    if (fc.canWrite()) {
                        DataOutputStream os = new DataOutputStream(fc.getOutputStream(true));
                        String entries = "";
                        for (Iterator iter = buttons.iterator();iter.hasNext();) {
                            JToggleButton button = (JToggleButton)iter.next();
                            entries += ""+button.getText()+":=";
                            entries += ""+button.getToolTipText().replaceAll(" \\[(U\\+\\p{XDigit}+\\ ?)+]","")+"\n";
                        }
                        //log.println("DBG: save "+title+" buttons ["+entries+"]");
                        os.writeUTF(entries);
                        os.flush();
                        os.close();
                        log.println("INF: saved "+title+" buttons");
                    } else
                        log.println("ERR: couldn't save the "+title+" buttons: not allowed to write");
                } catch (Exception e) {
                    log.println("ERR: couldn't save the "+title+" buttons:" + e);
                    e.printStackTrace(System.err);
                }
            }
            return false;
        }
        
        protected boolean load() {
            if (ps!=null) {
                try {
                    FileContents fc = ps.get(new URL(bs.getCodeBase(), title));
                    if (fc.canRead()) {
                        DataInputStream is = new DataInputStream(fc.getInputStream());
                        String btns = is.readUTF();
                        //log.println("DBG: load "+title+" buttons ["+btns+"]");
                        String label = null;
                        String tip   = null;
                        String[] entries = btns.split("\n");
                        for (int e=0;e<entries.length;e++) {
                            if (!entries[e].trim().equals("")) {
                                String[] split = entries[e].split(":=",2);
                                if (split.length==2) {
                                    label = split[0];
                                    tip   = split[1];
                                    tip   = tip.replaceAll(" \\[(U\\+\\p{XDigit}+\\ ?)+]","");
                                    JToggleButton button = addButton(label,tip);
                                    buttons.add(button);
                                    chars.add(button);
                                    del.setEnabled(true);
                                } else
                                    log.println("ERR: can't load "+title+" button entry: "+entries[e]);
                            }
                        }
                        is.close();
                        chars.revalidate();
                        log.println("INF: loaded "+title+" buttons");
						return true;
                    } else
                        log.println("ERR: couldn't load "+title+" buttons: not allowed to read");
        	   } catch (java.io.FileNotFoundException e) {
               } catch (Exception e) {
                   log.println("ERR: couldn't load the "+title+" buttons:" + e);
               }
            }
            return false;
        }
    }
    
    class FrequentButton extends HistoryButton {
        
        public FrequentButton(java.util.List histogram, int pos) {
            super(histogram,pos);
        }
        
        public boolean mimic() {
            if (!(button() instanceof HistoryButton))
                return super.mimic();
            return false;
        }
    }
    
    class FrequencyButton implements Comparable<FrequencyButton> {
        
        protected java.util.List histogram = null;
        protected AbstractButton button = null;
        protected int frequency = 0;
        
        public FrequencyButton(java.util.List histogram,AbstractButton button,int frequency) {
            this.histogram = histogram;
            this.button = button;
            this.frequency = frequency;
            
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    hit();
                }
            });
        }
        
        public FrequencyButton(java.util.List histogram,AbstractButton button) {
            this(histogram,button,0);
        }
        
        public AbstractButton button() {
            return button;
        }
        
        public int frequency() {
            return frequency;
        }
        
        public void frequency(int frequency) {
			if (frequency>=0)
            	this.frequency = frequency;
        }
        
        protected FrequencyButton find(AbstractButton button) {
            for (Iterator iter = histogram.iterator();iter.hasNext();) {
                FrequencyButton fb = (FrequencyButton)iter.next();
                if (fb.button()==button)
                    return fb;
            }
            return null;
        }
        
        public void hit() {
            AbstractButton b = button;
            if (b instanceof HistoryButton) {
                while (b instanceof HistoryButton)
                    b = ((HistoryButton)b).button();
                FrequencyButton fb = find(b);
                if (fb!=null)
                    fb.hit();
            } else if (frequency<Integer.MAX_VALUE) {
                frequency++;
                //log.println("DBG: hit button["+b.getText()+"] "+frequency);
            }
            histogram();
        }
        
        public int compareTo(FrequencyButton fb) {
            return -(new Integer(frequency)).compareTo(new Integer(fb.frequency()));
        }
    }

}
