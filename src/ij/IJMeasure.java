package ij;

import ij.gui.Overlay;
import ij.measure.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.*;
import ij.text.*;

public class IJMeasure {

  protected static void setStackThreshold(ImagePlus imp, ImageProcessor ip, String method) {
		boolean darkBackground = method.indexOf("dark")!=-1;
		int measurements = Analyzer.getMeasurements();
		Analyzer.setMeasurements(Measurements.AREA+Measurements.MIN_MAX);
		ImageStatistics stats = new StackStatistics(imp);
		Analyzer.setMeasurements(measurements);
		AutoThresholder thresholder = new AutoThresholder();
		double min=0.0, max=255.0;
		if (imp.getBitDepth()!=8) {
			min = stats.min;
			max = stats.max;
		}
		int threshold = thresholder.getThreshold(method, stats.histogram);
		double lower, upper;
		if (darkBackground) {
			if (ip.isInvertedLut())
				{lower=0.0; upper=threshold;}
			else
				{lower=threshold+1; upper=255.0;}
		} else {
			if (ip.isInvertedLut())
				{lower=threshold+1; upper=255.0;}
			else
				{lower=0.0; upper=threshold;}
		}
		if (lower>255) lower = 255;
		if (max>min) {
			lower = min + (lower/255.0)*(max-min);
			upper = min + (upper/255.0)*(max-min);
		} else
			lower = upper = min;
		ip.setMinAndMax(min, max);
		ip.setThreshold(lower, upper, ImageProcessor.RED_LUT);
		imp.updateAndDraw();
	}



  /** Deletes 'row1' through 'row2' of the "Results" window, where
		'row1' and 'row2' must be in the range 0-Analyzer.getCounter()-1. */
	public static void deleteRows(int row1, int row2) {
		ResultsTable rt = Analyzer.getResultsTable();
		int tableSize = rt.size();
		rt.deleteRows(row1, row2);
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null)
			Overlay.updateTableOverlay(imp, row1, row2, tableSize);
		rt.show("Results");
	}

  /** Returns a measurement result, where 'measurement' is "Area",
	 * "Mean", "StdDev", "Mode", "Min", "Max", "X", "Y", "XM", "YM",
	 * "Perim.", "BX", "BY", "Width", "Height", "Major", "Minor", "Angle",
	 * "Circ.", "Feret", "IntDen", "Median", "Skew", "Kurt", "%Area",
	 * "RawIntDen", "Ch", "Slice", "Frame", "FeretX", "FeretY",
	 * "FeretAngle", "MinFeret", "AR", "Round", "Solidity", "MinThr"
	 * or "MaxThr". Add " raw" to the argument to disable calibration,
	 * for example IJ.getValue("Mean raw"). Add " limit" to enable
	 * the "limit to threshold" option.
	*/
	public static double getValue(ImagePlus imp, String measurement) {
		String options = "";
		int index = measurement.indexOf(" ");
		if (index>0) {
			if (index<measurement.length()-1)
				options = measurement.substring(index+1, measurement.length());
			measurement = measurement.substring(0, index);
		}
		int measurements = Measurements.ALL_STATS + Measurements.SLICE;
		if (options.contains("limit"))
			measurements += Measurements.LIMIT;
		Calibration cal = null;
		if (options.contains("raw")) {
			cal = imp.getCalibration();
			imp.setCalibration(null);
		}
		ResultsTable rt = new ResultsTable();
		Analyzer analyzer = new Analyzer(imp, measurements, rt);
		analyzer.measure();
		double value = Double.NaN;
		try {
			value = rt.getValue(measurement, 0);
		} catch (Exception e) {};
		if (cal!=null)
			imp.setCalibration(cal);
		return value;
	}

  protected static void showResults() {
		TextWindow resultsWindow = new TextWindow("Results", "", 400, 250);
		TextPanel = resultsWindow.getTextPanel();
		TextPanel.setResultsTable(Analyzer.getResultsTable());
	}
}
