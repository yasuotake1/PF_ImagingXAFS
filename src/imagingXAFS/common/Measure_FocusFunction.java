package imagingXAFS.common;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;

public class Measure_FocusFunction implements PlugIn {

	static final String[] listMethod = { "StdDev", "Mean of Variance", "StdDev of Variance" };
	static final double radius = 2.0;
	static final String styleData = "connected circle";
	static Color[] colors = ImagingXAFSCommon.listPlotColors;

	public void run(String arg) {
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		if (listStackId.length < 1) {
			IJ.error("Could not find data image(s).");
			return;
		}

		GenericDialog gd = new GenericDialog("Sharpness measurement");
		gd.addChoice("Imagestack", listStackTitle, listStackTitle[0]);
		gd.addChoice("Method", listMethod, listMethod[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impSrc = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		double[] arrX = new double[impSrc.getNSlices()];
		for (int i = 0; i < arrX.length; i++) {
			arrX[i] = i + 1;
		}
		ImagePlus imp;
		int method = gd.getNextChoiceIndex();
		Plot plot = new Plot("Focus function", "Slice number", listMethod[method]);

		RoiManager roiManager = RoiManager.getInstance();
		Roi[] rois;
		if (roiManager != null && roiManager.getCount() > 0) {
			rois = roiManager.getRoisAsArray();
		} else {
			rois = new Roi[] { impSrc.getRoi() };
		}
		String legend = "";
		for (int i = 0; i < rois.length; i++) {
			impSrc.setRoi(rois[i]);
			imp = impSrc.crop("stack");
			if (imp.getType() != ImagePlus.GRAY32) {
				ImageConverter ic = new ImageConverter(imp);
				ic.convertToGray32();
			}
			plot.setColor(colors[i]);
			plot.add(styleData, arrX, calcFocusFunction(imp.getStack(), method));
			legend += i > 0 ? "\t" : "";
			legend += "ROI " + String.valueOf(i + 1);
		}
		if (rois.length > 1) {
			plot.setColor(Color.BLACK);
			plot.addLegend(legend);
		}
		plot.show();
		plot.setLimitsToFit(true);
	}

	private double[] calcFocusFunction(ImageStack stack, int method) {
		GaussianBlur gb = new GaussianBlur();
		RankFilters rf = new RankFilters();
		ImageStatistics is;
		int slc = stack.size();
		if (method > 0) {
			for (int i = 0; i < slc; i++) {
				gb.blurGaussian(stack.getProcessor(i + 1), radius);
				rf.rank(stack.getProcessor(i + 1), radius, RankFilters.VARIANCE);
			}
		}
		double[] arrY = new double[slc];
		for (int i = 0; i < slc; i++) {
			is = ImageStatistics.getStatistics(stack.getProcessor(i + 1));
			arrY[i] = method > 1 ? is.mean : is.stdDev;
		}
		return arrY;
	}
}
