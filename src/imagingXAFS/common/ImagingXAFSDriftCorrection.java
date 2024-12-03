package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Measurements;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import mpicbg.stitching.PairWiseStitchingImgLib;
import mpicbg.stitching.PairWiseStitchingResult;
import mpicbg.stitching.StitchingParameters;

public class ImagingXAFSDriftCorrection implements PlugIn {

	public static final String[] calculationMode = { "Highest-energy image", "Each following image" };
	public double[] phaseCorrelation;
	public double[] crossCorrelation;
	public double[] offsetX;
	public double[] offsetY;

	public void run(String arg) {
	}

	public ImagePlus GetCorrectedStack(ImagePlus imp, double sigma, boolean variance, Roi roi, int mode,
			boolean subpixel, boolean crop) {
		double energy[] = ImagingXAFSCommon.getPropEnergies(imp);
		if (energy == null)
			return null;

		int wid = imp.getWidth();
		int hei = imp.getHeight();
		int slc = imp.getNSlices();

		int currentSlice = imp.getSlice();
		String[] labels = imp.getStack().getSliceLabels();
		imp.setRoi(roi);
		imp.hide();
		ImageStack stackSrc = imp.crop("stack").getStack();
		GaussianBlur gb = new GaussianBlur();
		ContrastEnhancer ce = new ContrastEnhancer();
		RankFilters rf = new RankFilters();
		ce.setNormalize(true);
		for (int i = 1; i <= slc; i++) {
			gb.blurGaussian(stackSrc.getProcessor(i), sigma);
			ImageStatistics stats = ImageStatistics.getStatistics(stackSrc.getProcessor(i), Measurements.MIN_MAX, null);
			ce.stretchHistogram(stackSrc.getProcessor(i), 0.1, stats);
			if (variance) {
				gb.blurGaussian(stackSrc.getProcessor(i), sigma);
				rf.rank(stackSrc.getProcessor(i), sigma, RankFilters.VARIANCE);
			}
		}

		StitchingParameters params = new StitchingParameters();
		params.dimensionality = 2;
		params.fusionMethod = 7;
		params.fusedName = "Fused";
		params.checkPeaks = 5;
		params.ignoreZeroValuesFusion = false;
		params.displayFusion = false;
		params.computeOverlap = true;
		params.subpixelAccuracy = subpixel;
		params.xOffset = params.yOffset = params.zOffset = 0.0;
		params.channel1 = params.channel2 = 1;
		params.timeSelect = 0;
		PairWiseStitchingResult result;
		phaseCorrelation = crossCorrelation = offsetX = offsetY = null;
		double[] pc = new double[slc];
		double[] cc = new double[slc];
		double[] ox = new double[slc];
		double[] oy = new double[slc];
		ImagePlus imp1, imp2;

		switch (mode) {
		case 0:
			imp1 = new ImagePlus(labels[slc - 1], stackSrc.getProcessor(slc).duplicate());
			pc[slc - 1] = cc[slc - 1] = 0.0;
			ox[slc - 1] = oy[slc - 1] = 0.0;
			for (int i = 1; i < slc; i++) {
				IJ.showStatus("Caluculating drift " + i + "/" + slc);
				imp2 = new ImagePlus(labels[i - 1], stackSrc.getProcessor(i).duplicate());
				result = PairWiseStitchingImgLib.stitchPairwise(imp1, imp2, null, null, 1, 1, params);
				pc[i - 1] = result.getPhaseCorrelation();
				cc[i - 1] = result.getCrossCorrelation();
				ox[i - 1] = result.getOffset(0);
				oy[i - 1] = result.getOffset(1);
			}
			break;
		case 1:
			pc[slc - 1] = cc[slc - 1] = 0.0;
			ox[slc - 1] = oy[slc - 1] = 0.0;
			for (int i = slc - 1; i > 0; i--) {
				IJ.showStatus("Caluculating drift " + (slc - i) + "/" + slc);
				imp1 = new ImagePlus(labels[i], stackSrc.getProcessor(i + 1).duplicate());
				imp2 = new ImagePlus(labels[i - 1], stackSrc.getProcessor(i).duplicate());
				result = PairWiseStitchingImgLib.stitchPairwise(imp1, imp2, null, null, 1, 1, params);
				pc[i - 1] = result.getPhaseCorrelation();
				cc[i - 1] = result.getCrossCorrelation();
				ox[i - 1] = ox[i] + result.getOffset(0);
				oy[i - 1] = oy[i] + result.getOffset(1);
			}
			break;
		default:
			return null;
		}

		phaseCorrelation = pc;
		crossCorrelation = cc;
		offsetX = ox;
		offsetY = oy;
		ImagePlus impResult = imp.duplicate();
		FileInfo fi = imp.getOriginalFileInfo();
		String title = imp.getTitle().endsWith(".tif") ? imp.getTitle().replace(".tif", "_corr.tif")
				: imp.getTitle() + "_corr.tif";
		fi.fileName = title;
		ImageProcessor ip;
		for (int i = 0; i < slc; i++) {
			IJ.showStatus("Applying drift correction " + i + "/" + slc);
			ip = impResult.getStack().getProcessor(i + 1);
			ip.setInterpolationMethod(ImageProcessor.BILINEAR);
			ip.translate(ox[i], oy[i]);
		}
		imp.show();
		if (crop) {
			double doubleL = 0.0;
			double doubleR = 0.0;
			double doubleT = 0.0;
			double doubleB = 0.0;
			for (int i = 0; i < slc; i++) {
				doubleL = Math.max(doubleL, offsetX[i]);
				doubleR = Math.min(doubleR, offsetX[i]);
				doubleT = Math.max(doubleT, offsetY[i]);
				doubleB = Math.min(doubleB, offsetY[i]);
			}
			int intL = (int) Math.ceil(doubleL);
			int intR = (int) Math.floor(doubleR);
			int intT = (int) Math.ceil(doubleT);
			int intB = (int) Math.floor(doubleB);
			Roi roiToCrop = new Roi(intL, intT, wid - intL + intR, hei - intT + intB);
			roiToCrop.setPosition(0);
			impResult.setRoi(roiToCrop);
			impResult = impResult.crop("stack");
		}
		impResult.setTitle(title);
		impResult.setFileInfo(fi);
		impResult.setSlice(currentSlice);
		return impResult;
	}
}
