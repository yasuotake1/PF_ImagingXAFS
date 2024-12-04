package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.io.*;
import ij.plugin.PlugIn;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class SVD implements PlugIn {

	static RealMatrix D;
	static int width;
	static int height;
	static String title;
	static double[] energies;
	static RealMatrix M;
	static RealMatrix invS;
	static RealMatrix X;
	static RealMatrix total;
	static RealMatrix residual;
	static ImagePlus impCurr;
	static List<String> fileNamesStd = new ArrayList<String>();
	static List<ImagePlus> results = new ArrayList<ImagePlus>();

	public void run(String arg) {
	}

	public static void setDataMatrix(double[] energies, List<double[]> ald) {
		ImagePlus imp = NewImage.createFloatImage("data", ald.size(), 1, ald.get(0).length, NewImage.FILL_BLACK);
		for (int i = 0; i < ald.size(); i++) {
			for (int j = 0; j < ald.get(i).length; j++) {
				imp.getStack().setVoxel(i, 0, j, ald.get(i)[j]);
			}
		}
		ImagingXAFSCommon.setPropEnergies(imp, energies);
		setDataMatrix(imp);
	}

	public static boolean setDataMatrix(ImagePlus imp) {
		energies = ImagingXAFSCommon.getPropEnergies(imp);
		if (energies == null) {
			return false;
		}
		width = imp.getWidth();
		height = imp.getHeight();
		title = imp.getTitle();
		D = MatrixUtils.createRealMatrix(imp.getNSlices(), width * height);
		for (int i = 0; i < imp.getNSlices(); i++) {
			imp.setSlice(i + 1);
			D.setRow(i, f2d((float[]) imp.getProcessor().getPixels()));
		}
		return true;
	}

	public static boolean setStandards(boolean showPlot, double[] energy) {
		energies = Arrays.copyOf(energy, energy.length);
		return setStandards(showPlot);
	}

	public static boolean setStandards(boolean showPlot) {
		int num = fileNamesStd.size();
		if (num > 0) {
			String msg = "Currently loaded standards:";
			for (int i = 0; i < fileNamesStd.size(); i++) {
				msg += "\n   " + fileNamesStd.get(i);
			}
			msg += "\nUse these standards?";
			YesNoCancelDialog yncd = new YesNoCancelDialog(IJ.getInstance(), "Set standards", msg, "Use these",
					"Load new standards");
			if (yncd.cancelPressed()) {
				return false;
			} else if (yncd.yesPressed()) {
				return true;
			}
		} else {
			num = 3;// Set default number of components.
		}

		String pathTemp = OpenDialog.getLastDirectory();
		GenericDialog gd = new GenericDialog("Set standards");
		gd.addNumericField("Number of components: ", num, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		num = (int) gd.getNextNumber();
		if (num < 1)
			return false;
		if (showPlot)
			IJ.log("Loading standards...");

		fileNamesStd.clear();
		double[] arrInt;
		for (int i = 0; i < num; i++) {
			OpenDialog od = new OpenDialog("Open component " + (i + 1) + ".");
			if (od.getPath() == null)
				return false;
			arrInt = getInterpolatedSpectrum(od.getPath(), energies);
			if (arrInt == null)
				return false;
			if (i == 0) {
				M = MatrixUtils.createRealMatrix(energies.length, num);
			}
			M.setColumn(i, arrInt);
			fileNamesStd.add(od.getFileName());
		}
		OpenDialog.setLastDirectory(pathTemp);
		if (showPlot) {
			showStandards();
			IJ.log("\\Update:Loading standards...loaded " + num + " spectra.");
		}

		return true;
	}

	public static void showStandards() {
		ImagingXAFSPlot.clear();
		for (int i = 0; i < fileNamesStd.size(); i++) {
			ImagingXAFSPlot.addData(energies, M.getColumn(i), fileNamesStd.get(i));
		}
		ImagingXAFSPlot.show(false);
	}

	public static int numComponents() {
		if (M == null)
			return 0;
		return Math.min(M.getColumnDimension(), fileNamesStd.size());
	}

	public static void doSVD(boolean showLog) {
		if (X != null)
			X = null;
		results.clear();

		if (showLog)
			IJ.log("Decomposing singular value...");
		SingularValueDecomposition svd = new SingularValueDecomposition(M);
		invS = MatrixUtils.createRealMatrix(svd.getS().getRowDimension(), svd.getS().getColumnDimension());
		for (int i = 0; i < invS.getRowDimension(); i++) {
			invS.setEntry(i, i, 1.0 / svd.getS().getEntry(i, i));
		}
		X = D.preMultiply(svd.getUT()).preMultiply(invS).preMultiply(svd.getV());
		total = X.preMultiply(M);
		residual = D.subtract(total);
		if (showLog)
			IJ.log("\\Update:Decomposing singular value...done.");
		/*
		 * IJ.showStatus("Applying edge jump filter..."); for(int i = 0; i <
		 * pixels(impDmut).length; i++) { IJ.showProgress(i, pixels(impDmut).length);
		 * if(pixels(impE0)[i] == 0) D.setColumn(i, new double[D.getRowDimension()]); }
		 */
	}

	public static double[] getCoefsAt(int idx) {
		if (X == null)
			return null;
		else
			return X.getColumn(idx);
	}

	public static double[] getCurveAt(int idx) {
		if (total == null)
			return null;
		else
			return total.getColumn(idx);
	}

	public static double[] getResidualAt(int idx) {
		if (residual == null)
			return null;
		else
			return residual.getColumn(idx);
	}

	public static List<String> getNames() {
		return fileNamesStd;
	}

	public static List<String> getNames(int length) {
		ArrayList<String> al = new ArrayList<String>();
		for (int i = 0; i < fileNamesStd.size(); i++) {
			if (fileNamesStd.get(i).length() > length)
				al.add(fileNamesStd.get(i).substring(0, length));
			else
				al.add(fileNamesStd.get(i));
		}
		return al;
	}

	public static void showResults(ImagePlus impDmut, boolean bClip, boolean showSummary, boolean showImages,
			boolean saveImages) {
		if (X == null)
			return;

		IJ.log("Calculating component distributions...");
		float[] pixelsDmut = (float[]) impDmut.getProcessor().getPixels();
		ImagePlus impx;
		float[] pixelsImpX;
		for (int i = 0; i < numComponents(); i++) {
			IJ.showProgress(i, numComponents() + 1);
			impx = NewImage.createFloatImage(fileNamesStd.get(i), width, height, 1, NewImage.FILL_BLACK);
			pixelsImpX = (float[]) impx.getProcessor().getPixels();
			if (bClip) {
				for (int j = 0; j < width * height; j++) {
					pixelsImpX[j] = (float) Math.max(X.getEntry(i, j), 0) * pixelsDmut[j];
				}
			} else {
				for (int j = 0; j < width * height; j++) {
					pixelsImpX[j] = (float) X.getEntry(i, j) * pixelsDmut[j];
				}
			}
			results.add(impx);
		}
		IJ.showProgress(numComponents(), numComponents() + 1);
		impx = NewImage.createFloatImage("Residual", width, height, 1, NewImage.FILL_BLACK);
		pixelsImpX = (float[]) impx.getProcessor().getPixels();
		for (int i = 0; i < width * height; i++) {
			pixelsImpX[i] = getRms(residual.getColumn(i));
		}
		results.add(impx);
		if (showSummary)
			ImagingXAFSResultWindow.create("Singular value decomposition of " + title, results);
		if (showImages) {
			for (ImagePlus imp : results) {
				imp.show();
			}
		}
		FileInfo fi = impDmut.getOriginalFileInfo();
		if (saveImages && fi != null) {
			for (ImagePlus imp : results) {
				IJ.saveAsTiff(imp, fi.directory + title.replace("_normalized", "").replace(".tif", "") + "_"
						+ imp.getTitle() + ".tif");
			}
		}
		IJ.showProgress(1, 1);
		IJ.log("\\Update:Calculating component distributions...done.");
	}

	private static double[] f2d(float[] arrFloat) {
		double[] arrDouble = new double[arrFloat.length];
		for (int i = 0; i < arrDouble.length; i++) {
			arrDouble[i] = (double) arrFloat[i];
		}
		return arrDouble;
	}

	private static double[] getInterpolatedSpectrum(String strPath, double[] energies) {
		double[] arrEne = { 0 };
		double[] arrInt = { 0 };
		try {
			List<String> lines = ImagingXAFSCommon.linesFromFile(strPath);
			int columnInt = 1;
			if (strPath.endsWith(".nor")) {
				columnInt = 3;// Use 'flat' in case of Athena .nor file.
				lines = lines.stream().filter(s -> !s.startsWith("#")).map(s -> s.trim()).collect(Collectors.toList());
			} else {
				lines.remove(0);
			}
			arrEne = new double[lines.size()];
			arrInt = new double[lines.size()];
			String[] values;
			for (int i = 0; i < lines.size(); i++) {
				values = lines.get(i).split("[,\\s]+");
				arrEne[i] = Double.parseDouble(values[0]);
				arrInt[i] = Double.parseDouble(values[columnInt]);
			}
			if (arrEne[0] > energies[0] || arrEne[arrEne.length - 1] < energies[energies.length - 1]) {
				throw new Exception("Insufficient energy range");
			}
		} catch (Exception e) {
			IJ.error("Failed to load " + Paths.get(strPath).getFileName().toString() + ".");
			IJ.log(e.getMessage());
			return null;
		}

		double[] arrIntInterp = new double[energies.length];
		double interpIdx;
		double ratio;
		for (int i = 0; i < arrIntInterp.length; i++) {
			interpIdx = ImagingXAFSCommon.getInterpIndex(energies[i], arrEne);
			if (ImagingXAFSCommon.doInterp(interpIdx)) {
				ratio = interpIdx - Math.floor(interpIdx);
				arrIntInterp[i] = arrInt[(int) interpIdx] * (1.0 - ratio) + arrInt[(int) interpIdx + 1] * ratio;
			} else {
				arrIntInterp[i] = arrInt[(int) (interpIdx + 0.5)];
			}
		}
		return arrIntInterp;

	}

	private static float getRms(double[] arr) {
		double square = 0;
		int len = arr.length;
		for (int i = 0; i < len; i++) {
			square += arr[i] * arr[i];
		}
		return (float) Math.sqrt(square / len);
	}

}
