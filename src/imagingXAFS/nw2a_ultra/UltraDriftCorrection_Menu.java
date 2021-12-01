package imagingXAFS.nw2a_ultra;

import imagingXAFS.common.ImagingXAFSCommon;

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

		GenericDialog gd = new GenericDialog("Drift correction");
		gd.addChoice("Imagestack", listStackTitle, listStackTitle[0]);
		gd.addMessage("Preprocess:");
		gd.addCheckbox("Use ROI for calculation", false);
		gd.addNumericField("Gaussian blur sigma (radius)", 2.0, 1);
		gd.addMessage("Calculation:");
		gd.addChoice("Calculate drift to", UltraDriftCorrection.calculationMode,
				UltraDriftCorrection.calculationMode[0]);
		gd.addCheckbox("Subpixel accuracy", false);
		gd.addMessage("Postprocess:");
		gd.addCheckbox("Plot results", true);
		gd.addCheckbox("Crop", true);
		gd.addCheckbox("Save automatically", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impSrc = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		double[] energy = ImagingXAFSCommon.getPropEnergies(impSrc);
		int wid = impSrc.getWidth();
		int hei = impSrc.getHeight();
		int slc = impSrc.getNSlices();
		Roi roi = gd.getNextBoolean() ? impSrc.getRoi() : null;
		double sigma = gd.getNextNumber();
		int mode = gd.getNextChoiceIndex();
		boolean subpixel = gd.getNextBoolean();
		boolean plot = gd.getNextBoolean();
		boolean crop = gd.getNextBoolean();
		boolean autoSave = gd.getNextBoolean();
		UltraDriftCorrection udc = new UltraDriftCorrection();

		ImagePlus impResult = udc.GetCorrectedStack(impSrc, sigma, roi, mode, subpixel);
		if (plot) {
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
		if (crop) {
			double doubleL = 0.0;
			double doubleR = 0.0;
			double doubleT = 0.0;
			double doubleB = 0.0;
			for (int i = 0; i < slc; i++) {
				doubleL = Math.max(doubleL, udc.offsetX[i]);
				doubleR = Math.min(doubleR, udc.offsetX[i]);
				doubleT = Math.max(doubleT, udc.offsetY[i]);
				doubleB = Math.min(doubleB, udc.offsetY[i]);
			}
			int intL = (int) Math.ceil(doubleL);
			int intR = (int) Math.floor(doubleR);
			int intT = (int) Math.ceil(doubleT);
			int intB = (int) Math.floor(doubleB);
			Roi roiToCrop = new Roi(intL, intT, wid - intL + intR, hei - intT + intB);
			roiToCrop.setPosition(0);
			impResult.setRoi(roiToCrop);
			ImagePlus impCrop = impResult.crop("stack");
			impCrop.setTitle(impResult.getTitle());
			impCrop.setFileInfo(impResult.getOriginalFileInfo());
			impResult = impCrop;
		}
		impResult.show();
		if (autoSave) {
			IJ.saveAsTiff(impResult, impResult.getOriginalFileInfo().directory + impResult.getTitle());
		}
	}

}
