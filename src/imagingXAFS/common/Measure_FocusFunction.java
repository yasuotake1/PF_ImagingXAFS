package imagingXAFS.common;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.ImageConverter;
import ij.process.ImageStatistics;

public class Measure_FocusFunction implements PlugIn {

	static final String[] listMethod = { "StdDev", "StdDev of Variance", "Mean of Variance" };
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

		ImagePlus imp = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]).crop("stack");
		if (imp.getType() != ImagePlus.GRAY32) {
			ImageConverter ic = new ImageConverter(imp);
			ic.convertToGray32();
		}
		int method = gd.getNextChoiceIndex();

		ImageStack stack = imp.getStack();
		int slc = imp.getNSlices();
		double[] arrX = new double[slc];
		double[] arrY = new double[slc];
		ImageStatistics is;
		if (method > 0) {// Apply Gaussian Blur and Variance filters.
			GaussianBlur gb = new GaussianBlur();
			RankFilters rf = new RankFilters();
			for (int i = 0; i < slc; i++) {
				gb.blurGaussian(stack.getProcessor(i + 1), radius);
				rf.rank(stack.getProcessor(i + 1), radius, RankFilters.VARIANCE);
			}
		}
		for (int i = 0; i < slc; i++) {
			arrX[i] = i + 1;
			is = ImageStatistics.getStatistics(stack.getProcessor(i + 1));
			arrY[i] = method == 2 ? is.mean : is.stdDev;
		}

		Plot plot = new Plot("Focus function", "Slice number", listMethod[method]);
		plot.setColor(colors[0], colors[0]);
		plot.add(styleData, arrX, arrY);
		plot.show();
//		plot.setColor(Color.black);
//		plot.addLegend("Standard deviation of raw values\tStandard deviation of variance\tMean of variance");
		plot.setLimitsToFit(true);
	}
}
