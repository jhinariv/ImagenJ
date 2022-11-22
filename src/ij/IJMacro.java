package ij;

import ij.macro.Interpreter;
import ij.macro.MacroRunner;
import java.util.*;

public class IJMacro {

  protected static Interpreter macroInterpreter;
  protected static boolean macroRunning;

  private static ImageJ ij;
  private static Thread previousThread;
  private static Hashtable commandTable;

  /** Runs an ImageJ command, with options that are passed to the
		GenericDialog and OpenDialog classes. Does not return until
		the command has finished executing. To generate run() calls,
		start the recorder (Plugins/Macro/Record) and run commands
		from the ImageJ menu bar.
	*/
	public static void run(String command, String options) {
		//IJMessage.log("run1: "+command+" "+Thread.currentThread().hashCode()+" "+options);
		if (ij==null && Menus.getCommands()==null)
		  init();
		Macro.abort = false;
		Macro.setOptions(options);
		Thread thread = Thread.currentThread();
		if (previousThread==null || thread!=previousThread) {
			String name = thread.getName();
			if (!name.startsWith("Run$_"))
				thread.setName("Run$_"+name);
		}
		command = convert(command);
		previousThread = thread;
		macroRunning = true;
		Executer e = new Executer(command);
		e.run();
		macroRunning = false;
		Macro.setOptions(null);
		testAbort();
		macroInterpreter = null;
		//IJMessage.log("run2: "+command+" "+Thread.currentThread().hashCode());
	}

	/** The macro interpreter uses this method to run commands. */
	public static void run(Interpreter interpreter, String command, String options) {
		macroInterpreter = interpreter;
		run(command, options);
		macroInterpreter = null;
	}

  /**Displays a "no images are open" dialog box.*/
	public static void noImage() {
		String msg = "There are no images open";
		if (macroInterpreter!=null) {
			macroInterpreter.abort(msg);
			macroInterpreter = null;
		} else
			IJMessage.error("No Image", msg);
	}

  static void init() {
		Menus m = new Menus(null, null);
		Prefs.load(m, null);
		m.addMenuBar();
	}

  /** Returns true if the run(), open() or newImage() method is executing. */
	public static boolean macroRunning() {
		return macroRunning;
	}

	/** Returns true if a macro is running, or if the run(), open()
		or newImage() method is executing. */
	public static boolean isMacro() {
		return macroRunning || Interpreter.getInstance()!=null;
	}

  /** Converts commands that have been renamed so
		macros using the old names continue to work. */
	private static String convert(String command) {
		if (commandTable==null) {
			commandTable = new Hashtable(30);
			commandTable.put("New...", "Image...");
			commandTable.put("Threshold", "Make Binary");
			commandTable.put("Display...", "Appearance...");
			commandTable.put("Start Animation", "Start Animation [\\]");
			commandTable.put("Convert Images to Stack", "Images to Stack");
			commandTable.put("Convert Stack to Images", "Stack to Images");
			commandTable.put("Convert Stack to RGB", "Stack to RGB");
			commandTable.put("Convert to Composite", "Make Composite");
			commandTable.put("RGB Split", "Split Channels");
			commandTable.put("RGB Merge...", "Merge Channels...");
			commandTable.put("Channels...", "Channels Tool...");
			commandTable.put("New... ", "Table...");
			commandTable.put("Arbitrarily...", "Rotate... ");
			commandTable.put("Measurements...", "Results... ");
			commandTable.put("List Commands...", "Find Commands...");
			commandTable.put("Capture Screen ", "Capture Screen");
			commandTable.put("Add to Manager ", "Add to Manager");
			commandTable.put("In", "In [+]");
			commandTable.put("Out", "Out [-]");
			commandTable.put("Enhance Contrast", "Enhance Contrast...");
			commandTable.put("XY Coodinates... ", "XY Coordinates... ");
			commandTable.put("Statistics...", "Statistics");
			commandTable.put("Channels Tool... ", "Channels Tool...");
			commandTable.put("Profile Plot Options...", "Plots...");
			commandTable.put("AuPbSn 40 (56K)", "AuPbSn 40");
			commandTable.put("Bat Cochlea Volume (19K)", "Bat Cochlea Volume");
			commandTable.put("Bat Cochlea Renderings (449K)", "Bat Cochlea Renderings");
			commandTable.put("Blobs (25K)", "Blobs");
			commandTable.put("Boats (356K)", "Boats");
			commandTable.put("Cardio (768K, RGB DICOM)", "Cardio (RGB DICOM)");
			commandTable.put("Cell Colony (31K)", "Cell Colony");
			commandTable.put("Clown (14K)", "Clown");
			commandTable.put("Confocal Series (2.2MB)", "Confocal Series");
			commandTable.put("CT (420K, 16-bit DICOM)", "CT (16-bit DICOM)");
			commandTable.put("Dot Blot (7K)", "Dot Blot");
			commandTable.put("Embryos (42K)", "Embryos");
			commandTable.put("Fluorescent Cells (400K)", "Fluorescent Cells");
			commandTable.put("Fly Brain (1MB)", "Fly Brain");
			commandTable.put("Gel (105K)", "Gel");
			commandTable.put("HeLa Cells (1.3M, 48-bit RGB)", "HeLa Cells (48-bit RGB)");
			commandTable.put("Leaf (36K)", "Leaf");
			commandTable.put("Line Graph (21K)", "Line Graph");
			commandTable.put("Mitosis (26MB, 5D stack)", "Mitosis (5D stack)");
			commandTable.put("MRI Stack (528K)", "MRI Stack");
			commandTable.put("M51 Galaxy (177K, 16-bits)", "M51 Galaxy (16-bits)");
			commandTable.put("Neuron (1.6M, 5 channels)", "Neuron (5 channels)");
			commandTable.put("Nile Bend (1.9M)", "Nile Bend");
			commandTable.put("Organ of Corti (2.8M, 4D stack)", "Organ of Corti (4D stack)");
			commandTable.put("Particles (75K)", "Particles");
			commandTable.put("T1 Head (2.4M, 16-bits)", "T1 Head (16-bits)");
			commandTable.put("T1 Head Renderings (736K)", "T1 Head Renderings");
			commandTable.put("TEM Filter (112K)", "TEM Filter");
			commandTable.put("Tree Rings (48K)", "Tree Rings");
		}
		String command2 = (String) commandTable.get(command);
		if (command2!=null)
			return command2;
		else
			return command;
	}

  private static void testAbort() {
		if (Macro.abort)
			Macro.abort();
	}


}
