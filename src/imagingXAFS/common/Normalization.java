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

	public static void Normalize(ImagePlus impSrc, float threshold, boolean show, boolean autoSave) {
		double[] energy = ImagingXAFSCommon.getPropEnergies(impSrc);
		int[] indices = ImagingXAFSCommon.searchNormalizationIndices(energy);
		if (indices == null)
			return;

		IJ.log("Calculating pre-edge and post-edge lines...");
		impSrc.hide();
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
		double mean, stdDev;
		StandardDeviation classStdDev = new StandardDeviation();
		CurveFitter cf;
		float[] voxelsPre = new float[arrPreA.length];
		float[] voxelsPost = new float[arrPostA.length];
		float[] a0 = new float[len];
		float[] b0 = new float[len];
		float[] a1 = new float[len];
		float[] b1 = new float[len];
		for (int i = 0; i < len; i++) {
			IJ.showProgress(i, len);

			impSrc.getStack().getVoxels(i % wid, i / wid, indices[0], 1, 1, voxelsPre.length, voxelsPre);
			for (int j = 0; j < arrPreA.length; j++) {
				arrPreA[j] = (double) voxelsPre[j];
			}
			mean = Arrays.stream(arrPreA).average().getAsDouble();
			pixelsMeanPre[i] = (float) mean;
			cf = new CurveFitter(arrPreE, arrPreA);
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			a0[i] = (float) cf.getParams()[0];
			b0[i] = pixelsSlopePre[i] = (float) cf.getParams()[1];
			for (int j = 0; j < arrPreA.length; j++) {
				arrPreA[j] = arrPreA[j] - cf.getParams()[0] - cf.getParams()[1] * arrPreE[j];
			}
			stdDev = classStdDev.evaluate(arrPreA, 0);
			pixelsStdDevPre[i] = (float) stdDev;

			impSrc.getStack().getVoxels(i % wid, i / wid, indices[2], 1, 1, voxelsPost.length, voxelsPost);
			for (int j = 0; j < arrPostA.length; j++) {
				arrPostA[j] = (double) voxelsPost[j];
			}
			mean = Arrays.stream(arrPostA).average().getAsDouble();
			pixelsMeanPost[i] = (float) mean;
			cf = new CurveFitter(arrPostE, arrPostA);
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			a1[i] = (float) cf.getParams()[0];
			b1[i] = pixelsSlopePost[i] = (float) cf.getParams()[1];
			for (int j = 0; j < arrPostA.length; j++) {
				arrPostA[j] = arrPostA[j] - cf.getParams()[0] - cf.getParams()[1] * arrPostE[j];
			}
			stdDev = classStdDev.evaluate(arrPostA, 0);
			pixelsStdDevPost[i] = (float) stdDev;

		}
		IJ.log("\\Update:Calculating pre-edge and post-edge lines...done.");
		if (show) {
			List<ImagePlus> imps = new ArrayList<ImagePlus>();
			Collections.addAll(imps, impMeanPre, impSlopePre, impStdDevPre);
			Collections.addAll(imps, impMeanPost, impSlopePost, impStdDevPost);
			for (ImagePlus imp : imps) {
				imp.resetDisplayRange();
				imp.setLut(new LUT(LutLoader.getLut("fire"), imp.getDisplayRangeMin(), imp.getDisplayRangeMax()));
			}
			ImagingXAFSResultWindow.create("Pre-edge and post-edge statistics of " + impSrc.getTitle(), 3, 2, imps);
		}

		IJ.log("Normalizing all pixels...");
		impNorm = NewImage.createFloatImage(
				impSrc.getTitle().replace("_corrected", "").replace(".tif", "") + "_normalized.tif", wid, hei, slc,
				NewImage.FILL_BLACK);
		ImagingXAFSCommon.setPropEnergies(impNorm, energy);
		ImagePlus impFilter = NewImage.createByteImage("Filter", wid, hei, 1, NewImage.FILL_BLACK);
		byte[] pixelsFilter = (byte[]) impFilter.getProcessor().getPixels();
		impE0 = NewImage.createFloatImage("E0", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsE0 = (float[]) impE0.getProcessor().getPixels();
		impDmut = NewImage.createFloatImage("Dmut", wid, hei, 1, NewImage.FILL_BLACK);
		float[] pixelsDmut = (float[]) impDmut.getProcessor().getPixels();
		float[] voxels = new float[impSrc.getNSlices()];
		float e0;
		for (int i = 0; i < len; i++) {
			IJ.showProgress(i, len);
			e0 = 0F;
			if (isNotFiltered(a0[i], b0[i], a1[i], b1[i], floatEnergy, pixelsStdDevPre[i], pixelsStdDevPost[i],
					threshold)) {
				impSrc.getStack().getVoxels(i % wid, i / wid, 0, 1, 1, voxels.length, voxels);
				for (int k = 0; k < voxels.length; k++) {
					voxels[k] = (voxels[k] - a0[i] - b0[i] * floatEnergy[k])
							/ (a1[i] - a0[i] + (b1[i] - b0[i]) * floatEnergy[k]);
					if (e0 == 0 && k > 1 && voxels[k] > 0.5F)
						e0 = floatEnergy[k - 1] + (floatEnergy[k] - floatEnergy[k - 1]) * (0.5F - voxels[k - 1])
								/ (voxels[k] - voxels[k - 1]);
				}
				impNorm.getStack().setVoxels(i % wid, i / wid, 0, 1, 1, voxels.length, voxels);
				pixelsFilter[i] = -1;// Byte signed integer -1 corresponds to unsigned integer 255 and HEX 0xFF.
			}
			pixelsE0[i] = e0;
			if (e0 != 0)
				pixelsDmut[i] = a1[i] - a0[i] + (b1[i] - b0[i]) * e0;
		}
		IJ.log("\\Update:Normalizing all pixels...done.");
		impE0.setDisplayRange(ImagingXAFSCommon.e0Min, ImagingXAFSCommon.e0Max);
		IJ.run(impE0, "Jet", "");
		impDmut.resetDisplayRange();
		if (show) {
			List<ImagePlus> imps = new ArrayList<ImagePlus>();
			Collections.addAll(imps, impFilter, impE0, impDmut);
			ImagingXAFSResultWindow.create("Normalization summary of " + impSrc.getTitle(), 3, 1, imps);
		}

		impSrc.show();
		impNorm.show();
		impE0.setTitle(impSrc.getTitle().replace("_corrected", "").replace(".tif", "") + "_E0.tif");
		impE0.show();
		impDmut.setTitle(impSrc.getTitle().replace("_corrected", "").replace(".tif", "") + "_Dmut.tif");
		impDmut.show();
		if (autoSave) {
			FileInfo fi = impSrc.getOriginalFileInfo();
			IJ.saveAsTiff(impNorm, fi.directory + impNorm.getTitle());
			IJ.saveAsTiff(impE0, fi.directory + impE0.getTitle());
			IJ.saveAsTiff(impDmut, fi.directory + impDmut.getTitle());
		}
	}

	static boolean isNotFiltered(float a0, float b0, float a1, float b1, float[] energy, float stdDev0, float stdDev1,
			float threshold) {
		return (a1 - a0 + (b1 - b0) * energy[0] > (stdDev0 + stdDev1) * threshold)
				&& (a1 - a0 + (b1 - b0) * energy[energy.length - 1] > (stdDev0 + stdDev1) * threshold);
	}

}
