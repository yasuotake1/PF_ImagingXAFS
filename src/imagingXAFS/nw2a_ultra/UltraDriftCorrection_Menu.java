package imagingXAFS.nw2a_ultra;

import imagingXAFS.common.ImagingXAFSCommon;
import imagingXAFS.common.ImagingXAFSDriftCorrection;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.plugin.PlugIn;

public class UltraDriftCorrection_Menu implements PlugIn {

	static final String styleData = "connected circle";
	static final Color colorPhaseCorr = new Color(0x8b0000);
	static final Color colorCrossCorr = new Color(0x00008b);
	static final Color colorOffsetX = new Color(0x008b00);
	static final Color colorOffsetY = new Color(0x000000);

	public void run(String arg) {
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		if (listStackId.length < 1) {
			IJ.error("Could not find data image(s).");
			return;
		}

		GenericDialog gd = new GenericDialog("Drift correction");
		gd.addChoice("Imagestack", listStackTitle, listStackTitle[0]);
		gd.addMessage("Preprocess:");
		gd.addCheckbox("Use ROI for calculation", false);
		gd.addNumericField("Gaussian blur sigma (radius)", 2.0, 1);
		gd.addCheckbox("Edge detection", false);
		gd.addMessage("Calculation:");
		gd.addChoice("Optimization", ImagingXAFSDriftCorrection.OPTIMIZATION,
				ImagingXAFSDriftCorrection.OPTIMIZATION[0]);
		gd.addChoice("Calculate drift to", ImagingXAFSDriftCorrection.CALC_MODE,
				ImagingXAFSDriftCorrection.CALC_MODE[0]);
		gd.addMessage("Postprocess:");
		gd.addCheckbox("Plot results", true);
		gd.addCheckbox("Crop", true);
		gd.addCheckbox("Save automatically", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impSrc = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		double[] energy = ImagingXAFSCommon.getPropEnergies(impSrc);
		Roi roi = gd.getNextBoolean() ? impSrc.getRoi() : null;
		double sigma = gd.getNextNumber();
		boolean edge = gd.getNextBoolean();
		int optimization = gd.getNextChoiceIndex();
		int mode = gd.getNextChoiceIndex();
		boolean plot = gd.getNextBoolean();
		boolean crop = gd.getNextBoolean();
		boolean autoSave = gd.getNextBoolean();
		ImagingXAFSDriftCorrection udc = new ImagingXAFSDriftCorrection();

		ImagePlus impResult = udc.GetCorrectedStack(impSrc, optimization, mode, sigma, edge, roi, crop);
		if (plot) {
			if (udc.phaseCorrelation != null && udc.crossCorrelation != null) {
				Plot plotCorrel = new Plot("Drift correction results of " + impSrc.getTitle(), "Photon energy (eV)",
						"Correlation");
				plotCorrel.setColor(colorPhaseCorr, colorPhaseCorr);
				plotCorrel.add(styleData, energy, udc.phaseCorrelation);
				plotCorrel.setColor(colorCrossCorr, colorCrossCorr);
				plotCorrel.add(styleData, energy, udc.crossCorrelation);
				plotCorrel.show();
				plotCorrel.setColor(Color.black);
				plotCorrel.addLegend("Phase correlation\tCross correlation");
				plotCorrel.setLimitsToFit(true);
			}
			Plot plotOffset = new Plot("Drift correction results of " + impSrc.getTitle(), "Photon energy (eV)",
					"Pixels");
			plotOffset.setColor(colorOffsetX, colorOffsetX);
			plotOffset.add(styleData, energy, udc.offsetX);
			plotOffset.setColor(colorOffsetY, colorOffsetY);
			plotOffset.add(styleData, energy, udc.offsetY);
			plotOffset.show();
			plotOffset.setColor(Color.black);
			plotOffset.addLegend("Offset X\tOffset Y");
			plotOffset.setLimitsToFit(true);
		}
		impResult.show();
		if (autoSave) {
			IJ.saveAsTiff(impResult, impResult.getOriginalFileInfo().directory + impResult.getTitle());
		}
	}

}
