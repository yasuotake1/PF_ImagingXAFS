package imagingXAFS.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.io.FileInfo;
import ij.measure.CurveFitter;
import ij.plugin.LutLoader;
import ij.plugin.PlugIn;
import ij.process.LUT;

public class Normalization implements PlugIn {

	public static ImagePlus impNorm, impE0, impDmut;

	public void run(String arg) {
	}

	public static void Normalize(ImagePlus impSrc, boolean zeroSlope, float threshold, boolean showSummary,
			boolean statsImages, boolean saveResults, boolean saveStack) {
		double[] energy = ImagingXAFSCommon.getPropEnergies(impSrc);
		int[] indices = ImagingXAFSCommon.searchNormalizationIndices(energy);
		if (indices == null)
			return;

		String baseName = impSrc.getTitle().replace("_corrected", "").replace(".tif", "");

		boolean wasVisible = impSrc.isVisible();
		if (wasVisible) {
			impSrc.hide();
			IJ.log("Calculating pre-edge and post-edge lines...");
		}
		double[] arrPreE = Arrays.copyOfRange(energy, indices[0], indices[1]);
		double[] arrPreA = new double[arrPreE.length];
		double[] arrPostE = Arrays.copyOfRange(energy, indices[2], indices[3]);
		double[] arrPostA = new double[arrPostE.length];
		int wid = impSrc.getWidth();
		int hei = impSrc.getHeight();
		int slc = impSrc.getNSlices();
		float[] floatEnergy = new float[energy.length];
		for (int i = 0; i < floatEnergy.length; i++) {
			floatEnergy[i] = (float) energy[i];
		}
		int len = wid * hei;
		ImagePlus impMeanPre = NewImage.createFloatImage("Mean value at pre-edge", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsMeanPre = (float[]) impMeanPre.getProcessor().getPixels();
		ImagePlus impSlopePre = NewImage.createFloatImage("Slope at pre-edge", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsSlopePre = (float[]) impSlopePre.getProcessor().getPixels();
		ImagePlus impStdDevPre = NewImage.createFloatImage("Standard deviation at pre-edge", wid, hei, 1,
				NewImage.FILL_BLACK);
		float[] pixelsStdDevPre = (float[]) impStdDevPre.getProcessor().getPixels();
		ImagePlus impMeanPost = NewImage.createFloatImage("Mean value at post-edge", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsMeanPost = (float[]) impMeanPost.getProcessor().getPixels();
		ImagePlus impSlopePost = NewImage.createFloatImage("Slope at post-edge", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsSlopePost = (float[]) impSlopePost.getProcessor().getPixels();
		ImagePlus impStdDevPost = NewImage.createFloatImage("Standard deviation at post-edge", wid, hei, 1,
				NewImage.FILL_BLACK);
		float[] pixelsStdDevPost = (float[]) impStdDevPost.getProcessor().getPixels();
		List<ImagePlus> impStats = new ArrayList<ImagePlus>();
		Collections.addAll(impStats, impMeanPre, impSlopePre, impStdDevPre);
		Collections.addAll(impStats, impMeanPost, impSlopePost, impStdDevPost);
		StandardDeviation classStdDev = new StandardDeviation();
		CurveFitter cf;
		float[] voxelsPre = new float[arrPreA.length];
		float[] voxelsPost = new float[arrPostA.length];
		float[] a0 = new float[len];
		float[] b0 = new float[len];
		float[] a1 = new float[len];
		float[] b1 = new float[len];
		for (int i = 0; i < len; i++) {
			if (wasVisible)
				IJ.showProgress(i, len);

			impSrc.getStack().getVoxels(i % wid, i / wid, indices[0], 1, 1, voxelsPre.length, voxelsPre);
			for (int j = 0; j < arrPreA.length; j++) {
				arrPreA[j] = (double) voxelsPre[j];
			}
			pixelsMeanPre[i] = (float) Arrays.stream(arrPreA).average().getAsDouble();
			if (zeroSlope) {
				a0[i] = pixelsMeanPre[i];
				b0[i] = pixelsSlopePre[i] = 0f;
			} else {
				cf = new CurveFitter(arrPreE, arrPreA);
				cf.doFit(CurveFitter.STRAIGHT_LINE);
				a0[i] = (float) cf.getParams()[0];
				b0[i] = pixelsSlopePre[i] = (float) cf.getParams()[1];
			}
			for (int j = 0; j < arrPreA.length; j++) {
				arrPreA[j] = arrPreA[j] - a0[i] - b0[i] * arrPreE[j];
			}
			pixelsStdDevPre[i] = (float) classStdDev.evaluate(arrPreA, 0);

			impSrc.getStack().getVoxels(i % wid, i / wid, indices[2], 1, 1, voxelsPost.length, voxelsPost);
			for (int j = 0; j < arrPostA.length; j++) {
				arrPostA[j] = (double) voxelsPost[j];
			}
			pixelsMeanPost[i] = (float) Arrays.stream(arrPostA).average().getAsDouble();
			cf = new CurveFitter(arrPostE, arrPostA);
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			a1[i] = (float) cf.getParams()[0];
			b1[i] = pixelsSlopePost[i] = (float) cf.getParams()[1];
			for (int j = 0; j < arrPostA.length; j++) {
				arrPostA[j] = arrPostA[j] - a1[i] - b1[i] * arrPostE[j];
			}
			pixelsStdDevPost[i] = (float) classStdDev.evaluate(arrPostA, 0);

		}
		if (wasVisible)
			IJ.log("\\Update:Calculating pre-edge and post-edge lines...done.");
		if (showSummary) {
			for (ImagePlus imp : impStats) {
				imp.resetDisplayRange();
				imp.setLut(new LUT(LutLoader.getLut("fire"), imp.getDisplayRangeMin(), imp.getDisplayRangeMax()));
			}
			ImagingXAFSResultWindow.create("Pre-edge and post-edge statistics of " + impSrc.getTitle(), 3, 2, impStats);
		}
		if (statsImages) {
			impMeanPre.setTitle(baseName + "_PreEdgeMean.tif");
			impSlopePre.setTitle(baseName + "_PreEdgeSlope.tif");
			impStdDevPre.setTitle(baseName + "_PreEdgeStdDev.tif");
			impMeanPost.setTitle(baseName + "_PostEdgeMean.tif");
			impSlopePost.setTitle(baseName + "_PostEdgeSlope.tif");
			impStdDevPost.setTitle(baseName + "_PostEdgeStdDev.tif");
			for (ImagePlus imp : impStats) {
				imp.setLut(new LUT(LutLoader.getLut("grays"), imp.getDisplayRangeMin(), imp.getDisplayRangeMax()));
				imp.show();
			}
		}

		if (wasVisible)
			IJ.log("Normalizing all pixels...");
		impNorm = NewImage.createFloatImage(baseName + "_normalized.tif", wid, hei, slc, NewImage.FILL_BLACK);
		ImagingXAFSCommon.setPropEnergies(impNorm, energy);
		ImagePlus impFilter = NewImage.createByteImage("Filter", wid, hei, 1, NewImage.FILL_BLACK);
		byte[] pixelsFilter = (byte[]) impFilter.getProcessor().getPixels();
		impE0 = NewImage.createFloatImage(baseName + "_E0.tif", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsE0 = (float[]) impE0.getProcessor().getPixels();
		impDmut = NewImage.createFloatImage(baseName + "_Dmut.tif", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsDmut = (float[]) impDmut.getProcessor().getPixels();
		float[] voxels = new float[impSrc.getNSlices()];
		float e0_up, e0_down, e0;
		float e0Jump = ImagingXAFSCommon.e0Jump;
		for (int i = 0; i < len; i++) {
			if (wasVisible)
				IJ.showProgress(i, len);
			e0_up = e0_down = e0 = 0F;
			if (isNotFiltered(a0[i], b0[i], a1[i], b1[i], floatEnergy, pixelsStdDevPre[i], pixelsStdDevPost[i],
					threshold)) {
				impSrc.getStack().getVoxels(i % wid, i / wid, 0, 1, 1, voxels.length, voxels);
				for (int k = 0; k < voxels.length; k++) {
					voxels[k] = (voxels[k] - a0[i] - b0[i] * floatEnergy[k])
							/ (a1[i] - a0[i] + (b1[i] - b0[i]) * floatEnergy[k]);
					if (e0_up == 0 && k > 1 && voxels[k] > e0Jump)
						e0_up = floatEnergy[k - 1] + (floatEnergy[k] - floatEnergy[k - 1]) * (e0Jump - voxels[k - 1])
								/ (voxels[k] - voxels[k - 1]);
				}
				for (int k = voxels.length - 1; k >= 0; k--) {
					if (e0_down == 0 && k < voxels.length - 1 && voxels[k] < e0Jump)
						e0_down = floatEnergy[k] + (floatEnergy[k + 1] - floatEnergy[k]) * (e0Jump - voxels[k])
								/ (voxels[k + 1] - voxels[k]);
				}
				e0 = (e0_up + e0_down) / 2;
				impNorm.getStack().setVoxels(i % wid, i / wid, 0, 1, 1, voxels.length, voxels);
				pixelsFilter[i] = -1;// Byte signed integer -1 corresponds to unsigned integer 255 and HEX 0xFF.
			}
			if (e0 != 0) {
				pixelsDmut[i] = a1[i] - a0[i] + (b1[i] - b0[i]) * e0;
				pixelsE0[i] = e0;
			}
		}
		if (wasVisible)
			IJ.log("\\Update:Normalizing all pixels...done.");
		impE0.setDisplayRange(ImagingXAFSCommon.e0Min, ImagingXAFSCommon.e0Max);
		IJ.run(impE0, "Jet", "");
		impDmut.resetDisplayRange();
		if (showSummary) {
			List<ImagePlus> imps = new ArrayList<ImagePlus>();
			Collections.addAll(imps, impFilter, impE0, impDmut);
			ImagingXAFSResultWindow.create("Normalization summary of " + impSrc.getTitle(), 3, 1, imps);
		}
		FileInfo fi = impSrc.getOriginalFileInfo();
		if (saveResults && fi != null) {
			if (statsImages) {
				for (ImagePlus imp : impStats) {
					IJ.saveAsTiff(imp, fi.directory + imp.getTitle());
				}
			}

			IJ.saveAsTiff(impE0, fi.directory + impE0.getTitle());
			IJ.saveAsTiff(impDmut, fi.directory + impDmut.getTitle());
		}
		if (saveStack && fi != null) {
			IJ.saveAsTiff(impNorm, fi.directory + impNorm.getTitle());
		}
		if (wasVisible) {
			impSrc.show();
			impNorm.show();
			impE0.show();
			impDmut.show();
		}
	}

	static boolean isNotFiltered(float a0, float b0, float a1, float b1, float[] energy, float stdDev0, float stdDev1,
			float threshold) {
		return (a1 - a0 + (b1 - b0) * energy[0] > (stdDev0 + stdDev1) * threshold)
				&& (a1 - a0 + (b1 - b0) * energy[energy.length - 1] > (stdDev0 + stdDev1) * threshold);
	}

}
