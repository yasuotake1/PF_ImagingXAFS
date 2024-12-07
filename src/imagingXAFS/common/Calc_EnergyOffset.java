package imagingXAFS.common;

import java.awt.Color;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.PlugIn;

public class Calc_EnergyOffset implements PlugIn {
	static String style = "line";
	static Color color = ImagingXAFSCommon.LIST_PLOTCOLORS[0];

	public void run(String arg) {
		if (ImagingXAFSPlot.getNumPlots() != 1) {
			IJ.error("One spectrum must be present in ImagingXAFS plot window.");
			return;
		}
		float[][] spectrum = ImagingXAFSPlot.getSpectrum(0);
		double[] eSrc = new double[spectrum[0].length];
		double[] iSrc = new double[spectrum[0].length];
		for (int i = 0; i < eSrc.length; i++) {
			eSrc[i] = spectrum[0][i];
			iSrc[i] = spectrum[1][i];
		}
		GenericDialog gd = new GenericDialog("Calculate energy offset");
		gd.addFileField("Target spectrum", "");
		gd.addNumericField("Search from", -5.000, 3);
		gd.addNumericField("to", 5.000, 3);
		gd.addNumericField("step", 0.010, 3);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		String path = gd.getNextString();
		double from = gd.getNextNumber();
		double to = gd.getNextNumber();
		double step = gd.getNextNumber();
		if (from > to || step > (to - from)) {
			IJ.error("Invalid parameters.");
			return;
		}
		double[] eTgt = ImagingXAFSCommon.readEnergies(path);
		double[] iTgt = ImagingXAFSCommon.readIntensities(path, path.endsWith(".nor") ? 3 : 1);
		double[] offsets = new double[(int) Math.ceil((to - from) / step)];
		double[] distances = new double[offsets.length];
		double[] eSrcOfs = new double[eSrc.length];
		double[] iTgtInterp = new double[eSrc.length];
		double max = 0.0;
		int idx = 0;
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = step * i + from;
			double interpIdx;
			double ratio;
			for (int j = 0; j < eSrcOfs.length; j++) {
				eSrcOfs[j] = eSrc[j] + offsets[i];
				interpIdx = ImagingXAFSCommon.getInterpIndex(eSrcOfs[j], eTgt);
				if (ImagingXAFSCommon.doInterp(interpIdx)) {
					ratio = interpIdx - Math.floor(interpIdx);
					iTgtInterp[j] = iTgt[(int) interpIdx] * (1.0 - ratio) + iTgt[(int) interpIdx + 1] * ratio;
				} else {
					iTgtInterp[j] = iTgt[(int) (interpIdx + 0.5)];
				}
			}
			distances[i] = cosineDistance(iSrc, iTgtInterp);
			if (distances[i] > max) {
				idx = i;
				max = distances[i];
			}
		}
		Plot p = new Plot("Energy offset", "Energy offset (eV)", "Cosine similarity");
		p.setColor(color);
		p.add(style, offsets, distances);
		p.addLabel(((double) idx) / offsets.length, 0.5, String.format("Offset = %.3f eV", offsets[idx]));
		p.show();
	}

	private double cosineDistance(double[] A, double[] B) {
		if (A.length != B.length)
			return 0;
		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		for (int i = 0; i < A.length; i++) {
			temp0 += A[i] * B[i];
			temp1 += A[i] * A[i];
			temp2 += B[i] * B[i];
		}
		return temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}
}
