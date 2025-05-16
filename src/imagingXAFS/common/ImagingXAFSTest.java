package imagingXAFS.common;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Method;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.Measurements;
import ij.plugin.CanvasResizer;
import ij.plugin.ContrastEnhancer;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteStatistics;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imagingXAFS.nw2a_orca.Load_OrcaStack;
import imagingXAFS.nw2a_orca.Load_SingleOrca;
import imagingXAFS.nw2a_orca.OrcaCommon;
import imagingXAFS.nw2a_orca.OrcaProps;
import imagingXAFS.nw2a_ultra.*;

public class ImagingXAFSTest implements PlugIn {
	ImagePlus impTgt;
	private int widTgt, heiTgt;
	private double centXTgt, centYTgt;

	public void run(String arg) {
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		Integer[] list2dId = ImagingXAFSCommon.getDataIds(false);
		String[] list2dTitle = ImagingXAFSCommon.getDataTitles(false);
		if (listStackId.length == 0 || list2dId.length == 0) {
			IJ.error("Could not find data image(s).");
			return;
		}

		GenericDialog gd = new GenericDialog("Find slice in volume");
		gd.addChoice("Volume stack: ", listStackTitle, listStackTitle[0]);
		gd.addChoice("Slice image: ", list2dTitle, list2dTitle[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impVolume = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		impTgt = WindowManager.getImage(list2dId[gd.getNextChoiceIndex()]);
		if (impVolume.getType() != impTgt.getType()) {
			IJ.error("Different image type.");
			return;
		}
		Instant startTime = Instant.now();
		impTgt.resetRoi();
		impVolume.resetRoi();
		widTgt = impTgt.getWidth();
		heiTgt = impTgt.getHeight();
		ByteStatistics bs = new ByteStatistics(impTgt.getProcessor(), Measurements.CENTER_OF_MASS, null);
		centXTgt = bs.xCenterOfMass;
		centYTgt = bs.yCenterOfMass;
		int marginL = (int) centXTgt;
		int newWid = impVolume.getWidth() + widTgt;
		int marginT = (int) centYTgt;
		int newHei = impVolume.getHeight() + heiTgt;
		double[] centXVolume = new double[impVolume.getStackSize()];
		double[] centYVolume = new double[impVolume.getStackSize()];
		for (int i = 0; i < impVolume.getStackSize(); i++) {
			impVolume.setSlice(i + 1);
			bs = new ByteStatistics(impVolume.getProcessor(), Measurements.CENTER_OF_MASS, null);
			centXVolume[i] = bs.xCenterOfMass;
			centYVolume[i] = bs.yCenterOfMass;
		}
		CanvasResizer cr = new CanvasResizer();
		Duplicator dup = new Duplicator();
		ImagePlus impRotate;
		double[] angle = new double[360];
		double[] similarity = new double[angle.length];
		double angleRad;
		for (int i = 0; i < angle.length; i++) {
			angle[i] = i;
			angleRad = angle[i] / 180 * Math.PI;
			impRotate = impVolume.duplicate();
			ImageProcessor ip;
			for (int j = 0; j < impRotate.getStackSize(); j++) {
				impRotate.setSlice(j + 1);
				impRotate.setRoi(0, 0, impRotate.getWidth(), impRotate.getHeight());
				ip = impRotate.getProcessor();
				ip.setInterpolationMethod(ImageProcessor.BILINEAR);
				ip.setBackgroundValue(0.0);
				ip.rotate(angle[i]);
			}
			ImagePlus impResize = new ImagePlus("Resized",
					cr.expandStack(impRotate.getStack(), newWid, newHei, marginL, marginT));
//			impResize.show();
			double[] arrSim = new double[impResize.getStackSize()];
			double sim = 0.0;
			for (int j = 0; j < arrSim.length; j++) {
				impResize.setSlice(j + 1);
//				ByteStatistics bs1 = new ByteStatistics(impResize.getProcessor(), Measurements.CENTER_OF_MASS, null);
//				int x = (int) (bs1.xCenterOfMass-centXTgt);
//				int y = (int) (bs1.yCenterOfMass-centYTgt);
				int x = (int) ((centXVolume[j] - impVolume.getWidth() / 2) * Math.cos(angleRad)
						+ (impVolume.getHeight() / 2 - centYVolume[j]) * Math.sin(angleRad) + impVolume.getWidth() / 2);
				int y = (int) ((centXVolume[j] - impVolume.getWidth() / 2) * Math.sin(angleRad)
						- (impVolume.getHeight() / 2 - centYVolume[j]) * Math.cos(angleRad)
						+ impVolume.getHeight() / 2);
				if (x < 0 || y < 0)
					arrSim[i] = 0.0;
				else {
					impResize.setRoi(x, y, widTgt, heiTgt);
					arrSim[i] = cosineSimilarity8bit(dup.crop(impResize), impTgt);
				}
				if (arrSim[j] > sim)
					sim = arrSim[j];
			}
			similarity[i] = sim;
		}

		Plot pSimilarity = new Plot("Similarity", "Rotation angle", "Cosine similarity");
		pSimilarity.add("line", angle, similarity);
		pSimilarity.show();
		Instant endTime = Instant.now();
		long elapsed = Duration.between(startTime, endTime).getSeconds();
		IJ.log("Finished. Elapsed time: " + elapsed + " seconds.");
	}

	private double cosineSimilarity8bit(ImagePlus imp1, ImagePlus imp2) {
		byte[] arr1 = (byte[]) imp1.getProcessor().getPixels();
		byte[] arr2 = (byte[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		double arr1d, arr2d;
		for (int i = 0; i < arr1.length; i++) {
			arr1d = arr1[i] & 255;
			arr2d = arr2[i] & 255;
			temp0 += arr1d * arr2d;
			temp1 += arr1d * arr1d;
			temp2 += arr2d * arr2d;
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}

	private double cosineSimilarity16bit(ImagePlus imp1, ImagePlus imp2) {
		short[] arr1 = (short[]) imp1.getProcessor().getPixels();
		short[] arr2 = (short[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		double arr1d, arr2d;
		for (int i = 0; i < arr1.length; i++) {
			arr1d = arr1[i] & 0xffff;
			arr2d = arr2[i] & 0xffff;
			temp0 += arr1d * arr2d;
			temp1 += arr1d * arr1d;
			temp2 += arr2d * arr2d;
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}

	private double cosineSimilarity32bit(ImagePlus imp1, ImagePlus imp2) {
		float[] arr1 = (float[]) imp1.getProcessor().getPixels();
		float[] arr2 = (float[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		for (int i = 0; i < arr1.length; i++) {
			temp0 += arr1[i] * arr2[i];
			temp1 += arr1[i] * arr1[i];
			temp2 += arr2[i] * arr2[i];
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}
}
