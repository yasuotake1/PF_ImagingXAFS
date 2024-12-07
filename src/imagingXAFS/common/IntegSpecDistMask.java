package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.Roi;
import ij.plugin.PlugIn;

public class IntegSpecDistMask implements PlugIn {

	private ImagePlus impNorm, impDmut;
	private int slcSrc;
	public ImagePlus impPlot;
	private double iMin, iMax, iStep;
	private int[] eIdx;
	private float[] intensity;

	public void run(String arg) {
	}

	public IntegSpecDistMask(ImagePlus _impNorm, ImagePlus _impDmut) {
		impNorm = _impNorm;
		slcSrc = impNorm.getNSlices();
		impDmut = _impDmut;
	}

	public void showPlot(boolean weight, double _iMin, double _iMax, double _iStep, double eMin, double eMax,
			double eStep) {
		iMin = _iMin;
		iMax = _iMax;
		iStep = _iStep;
		int iPts = (int) Math.ceil((iMax - iMin) / iStep);
		int ePts = (int) Math.ceil((eMax - eMin) / eStep);
		double[] eGrid = new double[ePts];
		eIdx = new int[ePts];
		for (int i = 0; i < ePts; i++) {
			eGrid[i] = eStep * i + eMin;
			eIdx[i] = (int) Math
					.round(ImagingXAFSCommon.getInterpIndex(eGrid[i], ImagingXAFSCommon.getPropEnergies(impNorm)));
		}
		float[] dmut = (float[]) impDmut.getProcessor().getPixels();
		float[][] data = new float[slcSrc][];
		int pos;
		for (int i = 0; i < slcSrc; i++) {
			impNorm.setSlice(i + 1);
			intensity = (float[]) impNorm.getProcessor().getPixels();
			data[i] = new float[iPts];
			for (int j = 0; j < intensity.length; j++) {
				pos = (int) ((intensity[j] - iMin) / iStep);
				if (dmut[j] > 0 && pos >= 0 && pos < iPts) {
					data[i][pos] += weight ? dmut[j] : 1;
				}
			}
		}
		impPlot = NewImage.createFloatImage("Spectral map of " + impNorm.getTitle(), ePts, iPts, 1,
				NewImage.FILL_BLACK);
		float[] matrix = (float[]) impPlot.getProcessor().getPixels();
		for (int i = 0; i < ePts; i++) {
			for (int j = 0; j < iPts; j++) {
				matrix[(iPts - 1 - j) * ePts + i] = data[eIdx[i]][j];
			}
		}
		impPlot.resetDisplayRange();
		impPlot.show();
		IJ.run("Fire");
	}

	public void createMask() {
		if (impNorm == null) {
			IJ.error("The source of current correlation plot is closed.");
			return;
		}
		Roi roi = impPlot.getRoi();
		if (roi == null) {
			IJ.error("There is no ROI selected.");
			return;
		}
		int top = roi.getBounds().y;
		int bottom = top + roi.getBounds().height;
		int middle = (int) Math.floor(roi.getBounds().width / 2 + roi.getBounds().x);
		IJ.log("top=" + top + " bottom=" + bottom + " middle=" + middle);
		impPlot.setRoi(new Roi(top, middle, 1, bottom - top));
		impNorm.setSlice(eIdx[middle] + 1);
		intensity = (float[]) impNorm.getProcessor().getPixels();
		int pos;
		int iPts = (int) Math.ceil((iMax - iMin) / iStep);
		ImagePlus impMask = NewImage.createByteImage("Spectral Distribution Mask", impNorm.getWidth(),
				impNorm.getHeight(), 1, NewImage.FILL_BLACK);
		byte[] pixels = (byte[]) impMask.getProcessor().getPixels();
		for (int i = 0; i < intensity.length; i++) {
			pos = iPts - 1 - (int) ((intensity[i] - iMin) / iStep);
			pixels[i] = (byte) ((pos >= top && pos < bottom) ? 255 : 0);
		}
		impMask.show();
		impMask.setActivated();
		IJ.run("Invert LUT");
	}
}
