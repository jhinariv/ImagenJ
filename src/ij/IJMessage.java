package ij;

import java.awt.Font;

import ij.gui.*;
import ij.io.LogStream;
import ij.macro.Interpreter;
import ij.text.*;
import ij.util.Tools;

public class IJMessage {

  protected static boolean redirectErrorMessages;
  protected static String lastErrorMessage;

  private static TextPanel logPanel;
  private static Thread statusBarThread;
  private static boolean protectStatusBar;

  /** Displays a message in a dialog box with the specified title.
		Displays HTML formatted text if 'msg' starts with "<html>".
		There are examples at
		"http://imagej.nih.gov/ij/macros/HtmlDialogDemo.txt".
		Writes to the Java console if ImageJ is not present. */
	public static void showMessage(String title, String msg) {
		if (IJ.ij!=null) {
			if (msg!=null && (msg.startsWith("<html>")||msg.startsWith("<HTML>"))) {
				HTMLDialog hd = new HTMLDialog(title, msg);
				if (IJMacro.isMacro() && hd.escapePressed())
					throw new RuntimeException(Macro.MACRO_CANCELED);
			} else {
				MessageDialog md = new MessageDialog(IJ.ij, title, msg);
				if (IJMacro.isMacro() && md.escapePressed())
					throw new RuntimeException(Macro.MACRO_CANCELED);
			}
		} else
			System.out.println(msg);
	}

  /** Displays a message in a dialog box titled "Message".
		Writes the Java console if ImageJ is not present. */
	public static void showMessage(String msg) {
		showMessage("Message", msg);
	}

	/** Displays a message in a dialog box titled "ImageJ". If a
		macro or JavaScript is running, it is aborted. Writes to the
		Java console if the ImageJ window is not present.*/
	public static void error(String msg) {
		error(null, msg);
		if (Thread.currentThread().getName().endsWith("JavaScript"))
			throw new RuntimeException(Macro.MACRO_CANCELED);
		else
			Macro.abort();
	}

	/** Displays a message in a dialog box with the specified title. If a
		macro or JavaScript is running, it is aborted. Writes to the
		Java console if the ImageJ window is not present. */
	public static void error(String title, String msg) {
		if (IJMacro.macroInterpreter !=null) {
			IJMacro.macroInterpreter .abort(msg);
			IJMacro.macroInterpreter  = null;
			return;
		}
		if (msg!=null && msg.endsWith(Macro.MACRO_CANCELED))
			return;
		String title2 = title!=null?title:"ImageJ";
		boolean abortMacro = title!=null;
		lastErrorMessage = msg;
		if (redirectErrorMessages) {
			log(title2 + ": " + msg);
			if (abortMacro && (title.contains("Open")||title.contains("Reader")))
				abortMacro = false;
		} else
			showMessage(title2, msg);
		redirectErrorMessages = false;
		if (abortMacro)
			Macro.abort();
	}

  public static synchronized void log(String s) {
		if (s==null) return;
		if (logPanel==null && IJ.ij!=null) {
			TextWindow logWindow = new TextWindow("Log", "", 400, 250);
			logPanel = logWindow.getTextPanel();
			logPanel.setFont(new Font("SansSerif", Font.PLAIN, 16));
		}
		if (logPanel!=null) {
			if (s.startsWith("\\"))
				handleLogCommand(s);
			else {
				if (s.endsWith("\n")) {
					if (s.equals("\n\n"))
						s= "\n \n ";
					else if (s.endsWith("\n\n"))
						s = s.substring(0, s.length()-2)+"\n \n ";
					else
						s = s+" ";
				}
				logPanel.append(s);
			}
		} else {
		 	LogStream.redirectSystem(false);
			System.out.println(s);
		}
	}

  static void handleLogCommand(String s) {
		if (s.equals("\\Closed"))
			logPanel = null;
		else if (s.startsWith("\\Update:")) {
			int n = logPanel.getLineCount();
			String s2 = s.substring(8, s.length());
			if (n==0)
				logPanel.append(s2);
			else
				logPanel.setLine(n-1, s2);
		} else if (s.startsWith("\\Update")) {
			int cindex = s.indexOf(":");
			if (cindex==-1)
				{logPanel.append(s); return;}
			String nstr = s.substring(7, cindex);
			int line = (int) Tools.parseDouble(nstr, -1);
			if (line<0 || line>25)
				{logPanel.append(s); return;}
			int count = logPanel.getLineCount();
			while (line>=count) {
				log("");
				count++;
			}
			String s2 = s.substring(cindex+1, s.length());
			logPanel.setLine(line, s2);
		} else if (s.equals("\\Clear")) {
			logPanel.clear();
		} else if (s.startsWith("\\Heading:")) {
			logPanel.updateColumnHeadings(s.substring(10));
		} else if (s.equals("\\Close")) {
			Frame f = WindowManager.getFrame("Log");
			if (f!=null && (f instanceof TextWindow))
				((TextWindow)f).close();
		} else
			logPanel.append(s);
	}

  /** Displays a message in a dialog box with the specified title.
		Returns false if the user pressed "Cancel". */
	public static boolean showMessageWithCancel(String title, String msg) {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		gd.showDialog();
		return !gd.wasCanceled();
	}

  /**
	 * Returns the last error message written by IJMessage.error() or null if there
	 * was no error since the last time this method was called.
	 * @see #error(String)
	 */
	public static String getErrorMessage() {
		String msg = lastErrorMessage;
		lastErrorMessage = null;
		return msg;
	}

  /**Displays a message in the ImageJ status bar. If 's' starts
		with '!', subsequent showStatus() calls in the current
		thread (without "!" in the message) are suppressed. */
	public static void showStatus(String s) {
		if ((Interpreter.getInstance()==null&&statusBarThread==null)
		|| (statusBarThread!=null&&Thread.currentThread()!=statusBarThread))
			protectStatusBar(false);
		boolean doProtect = s.startsWith("!"); // suppress subsequent showStatus() calls
		if (doProtect) {
			protectStatusBar(true);
			statusBarThread = Thread.currentThread();
			s = s.substring(1);
		}
		if (doProtect || !protectStatusBar) {
			showStatus(s);
			ImagePlus imp = WindowManager.getCurrentImage();
			ImageCanvas ic = imp!=null?imp.getCanvas():null;
			if (ic!=null)
				ic.setShowCursorStatus(s.length()==0?true:false);
		}
	}

	/**Displays a message in the status bar and flashes
	 * either the status bar or the active image.<br>
	 * See: http://wsr.imagej.net/macros/FlashingStatusMessages.txt
	*/
	public static void showStatus(String message, String options) {
		showStatus(message);
		if (options==null)
			return;
		options = options.replace("flash", "");
		options = options.replace("ms", "");
		Color optionalColor = null;
		int index1 = options.indexOf("#");
		if (index1>=0) {  // hex color?
			int index2 = options.indexOf(" ", index1);
			if (index2==-1) index2 = options.length();
			String hexColor = options.substring(index1, index2);
			optionalColor = Colors.decode(hexColor, null);
			options = options.replace(hexColor, "");
		}
		if (optionalColor==null) {  // "red", "green", etc.
			for (String c : Colors.colors) {
				if (options.contains(c)) {
					optionalColor = Colors.getColor(c, ImageJ.backgroundColor);
					options = options.replace(c, "");
					break;
				}
			}
		}
		boolean flashImage = options.contains("image");
		Color defaultColor = new Color(255,255,245);
		int defaultDelay = 500;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (flashImage) {
			options = options.replace("image", "");
			if (imp!=null && imp.getWindow()!=null) {
				defaultColor = Color.black;
				defaultDelay = 100;
			}
			else
				flashImage = false;
		}
		Color color = optionalColor!=null?optionalColor:defaultColor;
		int delay = (int)Tools.parseDouble(options, defaultDelay);
		if (delay>8000)
			delay = 8000;
		String colorString = null;
		ImageJ ij = IJ.getInstance();
		if (flashImage) {
			Color previousColor = imp.getWindow().getBackground();
			imp.getWindow().setBackground(color);
			if (delay>0) {
				wait(delay);
				imp.getWindow().setBackground(previousColor);
			}
		} else if (ij!=null) {
			ij.getStatusBar().setBackground(color);
			wait(delay);
			ij.getStatusBar().setBackground(ij.backgroundColor);
		}
	}

  public static boolean statusBarProtected() {
		return protectStatusBar;
	}

	public static void protectStatusBar(boolean protect) {
		protectStatusBar = protect;
		if (!protectStatusBar)
			statusBarThread = null;
	}

  /**Delays 'msecs' milliseconds.*/
	public static void wait(int msecs) {
		try {Thread.sleep(msecs);}
		catch (InterruptedException e) { }
	}

  /** Returns the state of the  'redirectErrorMessages' flag, which is set by File/Import/Image Sequence. */
	public static boolean redirectingErrorMessages() {
		return redirectErrorMessages;
	}

}
