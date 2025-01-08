package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Normalization_Menu implements PlugIn {

	private static boolean zeroSlope = false;
	private static double threshold = 2.0;
	private static boolean showSummary = true;
	private static boolean statsImages = false;
	private static boolean autoSave = true;

	public void run(String arg) {
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		if (listStackId.length < 1) {
			IJ.error("Could not find data image(s).");
			return;
		}

		GenericDialog gd = new GenericDialog("Normalization");
		gd.addChoice("Imagestack", listStackTitle, listStackTitle[0]);
		gd.addNumericField("Pre-edge from", ImagingXAFSCommon.normalizationParam[0], 2, 7, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[1], 2, 7, "eV");
		gd.addNumericField("Post-edge from", ImagingXAFSCommon.normalizationParam[2], 2, 7, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[3], 2, 7, "eV");
		gd.addMessage(
				"Note for Filter threshold (Ft): Pixels at which\n(pre- and post-edge lines separation) < (StdDev@pre + StdDev@post) * Ft\nare filtered.");
		gd.addCheckbox("Zero-slope pre-edge", zeroSlope);
		gd.addNumericField("Filter threshold", threshold, 1);
		gd.addNumericField("Normalized absorbance at E0", ImagingXAFSCommon.e0Jump, 2);
		gd.addMessage("E0 plot range (can be modified afterwards):");
		gd.addNumericField("minimum", ImagingXAFSCommon.e0Min, 2, 8, "eV");
		gd.addNumericField("maximum", ImagingXAFSCommon.e0Max, 2, 8, "eV");
		gd.addCheckbox("Show statistics and summary", showSummary);
		gd.addCheckbox("Create statistics images", statsImages);
		gd.addCheckbox("Save automatically", autoSave);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impSrc = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		double preStart = gd.getNextNumber();
		double preEnd = gd.getNextNumber();
		double postStart = gd.getNextNumber();
		double postEnd = gd.getNextNumber();
		ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
		zeroSlope = gd.getNextBoolean();
		threshold = gd.getNextNumber();
		double e0Jump = gd.getNextNumber();
		if (e0Jump < 0 || e0Jump > 1) {
			IJ.error("Normalized absorbance at E0 must be within 0 and 1.");
			return;
		}
		double e0Min = gd.getNextNumber();
		double e0Max = gd.getNextNumber();
		if (Double.isNaN(e0Min) || Double.isNaN(e0Max) || e0Min > e0Max) {
			IJ.error("Invalid E0 minimum and/or maximum.");
			return;
		}
		ImagingXAFSCommon.e0Jump = (float) e0Jump;
		ImagingXAFSCommon.e0Min = e0Min;
		ImagingXAFSCommon.e0Max = e0Max;
		showSummary = gd.getNextBoolean();
		statsImages = gd.getNextBoolean();
		autoSave = gd.getNextBoolean();

		Normalization.Normalize(impSrc, zeroSlope, (float) threshold, showSummary, statsImages, autoSave, autoSave);
	}

}
