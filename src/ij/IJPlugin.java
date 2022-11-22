package ij;

import ij.io.PluginClassLoader;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.ThresholdAdjuster;
import java.applet.Applet;
import java.io.File;

public class IJPlugin {

  protected static java.applet.Applet applet;
  protected static ClassLoader classLoader;
  private static boolean suppressPluginNotFoundError;

  /** Runs the macro contained in the string <code>macro</code>
		on the current thread. The optional string argument can be
		retrieved in the called macro using the getArgument() macro
		function. Returns any string value returned by the macro, null
		if the macro does not return a value, or "[aborted]" if the
		macro was aborted due to an error.  */
	public static String runMacro(String macro, String arg) {
	  Macro_Runner mr = new Macro_Runner();
		return mr.runMacro(macro, arg);
	}

  /** Runs the specified macro or script file in the current thread.
		The file is assumed to be in the macros folder
 		unless <code>name</code> is a full path.
		The optional string argument (<code>arg</code>) can be retrieved in the called
		macro or script using the getArgument() function.
		Returns any string value returned by the macro, or null. Scripts always return null.
		The equivalent macro function is runMacro(). */
	public static String runMacroFile(String name, String arg) {
		Macro_Runner mr = new Macro_Runner();
		return mr.runMacroFile(name, arg);
	}

  /** Runs the macro contained in the string <code>macro</code>
		on the current thread. Returns any string value returned by
		the macro, null if the macro does not return a value, or
		"[aborted]" if the macro was aborted due to an error. The
		equivalent macro function is eval(). */
	public static String runMacro(String macro) {
		return runMacro(macro, "");
	}

  static Object runUserPlugIn(String commandName, String className, String arg, boolean createNewLoader) {
		if (IJDebugMode.debugMode)
			IJMessage.log("runUserPlugIn: "+className+", arg="+argument(arg));
		if (applet!=null) return null;
		if (createNewLoader)
			classLoader = null;
		ClassLoader loader = getClassLoader();
		Object thePlugIn = null;
		try {
			thePlugIn = (loader.loadClass(className)).newInstance();
			if (thePlugIn instanceof PlugIn)
 				((PlugIn)thePlugIn).run(arg);
 			else if (thePlugIn instanceof PlugInFilter)
				new PlugInFilterRunner(thePlugIn, commandName, arg);
		}
		catch (ClassNotFoundException e) {
			if (className.startsWith("macro:"))
				runMacro(className.substring(6));
			else if (className.contains("_")  && !suppressPluginNotFoundError)
				IJMessage.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
		}
		catch (NoClassDefFoundError e) {
			int dotIndex = className.indexOf('.');
			if (dotIndex>=0 && className.contains("_")) {
				// rerun plugin after removing folder name
				if (IJDebugMode.debugMode)
          IJMessage.log("runUserPlugIn: rerunning "+className);
				return runUserPlugIn(commandName, className.substring(dotIndex+1), arg, createNewLoader);
			}
			if (className.contains("_") && !suppressPluginNotFoundError)
				IJMessage.error("Run User Plugin", "Class not found while attempting to run \"" + className + "\"\n \n   " + e);
		}
		catch (InstantiationException e) {IJMessage.error("Unable to load plugin (ins)");}
		catch (IllegalAccessException e) {IJMessage.error("Unable to load plugin, possibly \nbecause it is not public.");}
		if (thePlugIn!=null && !"HandleExtraFileTypes".equals(className))
 			IJMessage.redirectErrorMessages = false;
		suppressPluginNotFoundError = false;
		return thePlugIn;
	}

  static void wrongType(int capabilities, String cmd) {
		String s = "\""+cmd+"\" requires an image of type:\n \n";
		if ((capabilities&PlugInFilter.DOES_8G)!=0) s +=  "    8-bit grayscale\n";
		if ((capabilities&PlugInFilter.DOES_8C)!=0) s +=  "    8-bit color\n";
		if ((capabilities&PlugInFilter.DOES_16)!=0) s +=  "    16-bit grayscale\n";
		if ((capabilities&PlugInFilter.DOES_32)!=0) s +=  "    32-bit (float) grayscale\n";
		if ((capabilities&PlugInFilter.DOES_RGB)!=0) s += "    RGB color\n";
		IJMessage.error(s);
	}

  private static String argument(String arg) {
		return arg!=null && !arg.equals("") && !arg.contains("\n")?"(\""+arg+"\")":"";
	}

  /** Runs the specified plugin and returns a reference to it. */
	public static Object runPlugIn(String commandName, String className, String arg) {
		if (arg==null) arg = "";
		if (IJDebugMode.debugMode)
			IJMessage.log("runPlugIn: "+className+argument(arg));
		// Load using custom classloader if this is a user
		// plugin and we are not running as an applet
		if (!className.startsWith("ij.") && applet==null)
			return runUserPlugIn(commandName, className, arg, false);
		Object thePlugIn=null;
		try {
			Class c = Class.forName(className);
 			thePlugIn = c.newInstance();
 			if (thePlugIn instanceof PlugIn)
				((PlugIn)thePlugIn).run(arg);
 			else
				new PlugInFilterRunner(thePlugIn, commandName, arg);
		} catch (ClassNotFoundException e) {
			if (!(className!=null && className.startsWith("ij.plugin.MacAdapter"))) {
				IJMessage.log("Plugin or class not found: \"" + className + "\"\n(" + e+")");
				String path = Prefs.getCustomPropsPath();
				if (path!=null);
					IJMessage.log("Error may be due to custom properties at " + path);
			}
		}
		catch (InstantiationException e) {IJMessage.log("Unable to load plugin (ins)");}
		catch (IllegalAccessException e) {IJMessage.log("Unable to load plugin, possibly \nbecause it is not public.");}
		IJMessage.redirectErrorMessages = false;
		return thePlugIn;
	}

  /** Runs the specified macro file. */
	public static String runMacroFile(String name) {
		return runMacroFile(name, null);
	}

	/** Runs the specified plugin and returns a reference to it. */
	public static Object runPlugIn(String className, String arg) {
		return runPlugIn("", className, arg);
	}

  /** Runs the specified plugin using the specified image. */
	public static Object runPlugIn(ImagePlus imp, String className, String arg) {
		if (imp!=null) {
			ImagePlus temp = WindowManager.getTempCurrentImage();
			WindowManager.setTempCurrentImage(imp);
			Object o = runPlugIn("", className, arg);
			WindowManager.setTempCurrentImage(temp);
			return o;
		} else
			return runPlugIn(className, arg);
	}

  /** Returns the class loader ImageJ uses to run plugins or the
		system class loader if Menus.getPlugInsPath() returns null. */
	public static ClassLoader getClassLoader() {
		if (classLoader==null) {
			String pluginsDir = Menus.getPlugInsPath();
			if (pluginsDir==null) {
				String home = System.getProperty("plugins.dir");
				if (home!=null) {
					if (!home.endsWith(Prefs.separator)) home+=Prefs.separator;
					pluginsDir = home+"plugins"+Prefs.separator;
					if (!(new File(pluginsDir)).isDirectory())
						pluginsDir = home;
				}
			}
			if (pluginsDir==null)
				return getClassLoader();
			else {
				if (Menus.jnlp)
					classLoader = new PluginClassLoader(pluginsDir, true);
				else
					classLoader = new PluginClassLoader(pluginsDir);
			}
		}
		return classLoader;
	}

  /** Temporarily suppress "plugin not found" errors. */
	public static void suppressPluginNotFoundError() {
		suppressPluginNotFoundError = true;
	}

  /** Returns the maximum amount of memory available to ImageJ or
		zero if ImageJ is unable to determine this limit. */
	public static long maxMemory() {
		if (maxMemory==0L) {
		  Memory mem = new Memory();
			maxMemory = mem.getMemorySetting();
			if (maxMemory==0L) maxMemory = mem.maxMemory();
		}
		return maxMemory;
	}
}
