package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.PlugIn;

public class Plot_IntegSpecDist implements PlugIn {
	public void run(String arg) {
		Integer[] listImageId = ImagingXAFSCommon.getDataIds(false);
		String[] listImageTitle = ImagingXAFSCommon.getDataTitles(false);
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		if (listImageId.length < 1 || listStackId.length < 1) {
			IJ.error("Could not find data image(s).");
			return;
		}
		GenericDialog gd = new GenericDialog("map");
		gd.addChoice("Normalized imagestack", listStackTitle, listStackTitle[0]);
		gd.addChoice("Dmut image", listImageTitle, listImageTitle[0]);
		gd.addCheckbox("Weight with Dmut", true);
		gd.addMessage("Plot range");
		gd.addNumericField("Normalized absorption from", -0.1, 1);
		gd.addNumericField("to", 1.5, 1);
		gd.addNumericField("step", 0.005, 2);
		gd.addNumericField("Photon energy from", 7009.20, 2);
		gd.addNumericField("to", 7310.20, 2);
		gd.addNumericField("step", 0.35, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impNorm = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		ImagePlus impDmut = WindowManager.getImage(listImageId[gd.getNextChoiceIndex()]);
		int wid = impNorm.getWidth();
		int hei = impNorm.getHeight();
		int slc = impNorm.getNSlices();
		if (impDmut.getWidth() != wid || impDmut.getHeight() != hei) {
			IJ.error("Invalid data size.");
			return;
		}
		double[] energies = ImagingXAFSCommon.getPropEnergies(impNorm);
		if (energies == null) {
			IJ.error("Invalid imagestack.");
			return;
		}
		boolean weight = gd.getNextBoolean();
		double iMin = gd.getNextNumber();
		double iMax = gd.getNextNumber();
		double iStep = gd.getNextNumber();
		if (iMax < iMin || iStep > (iMax - iMin)) {
			IJ.error("Invalid range.");
			return;
		}
		int iPts = (int) Math.ceil((iMax - iMin) / iStep);
		double eMin = gd.getNextNumber();
		double eMax = gd.getNextNumber();
		double eStep = gd.getNextNumber();
		if (eMax < eMin || eStep > (eMax - eMin)) {
			IJ.error("Invalid range.");
			return;
		}
		int ePts = (int) Math.ceil((eMax - eMin) / eStep);
		double[] eGrid = new double[ePts];
		int[] eIdx = new int[ePts];
		for (int i = 0; i < ePts; i++) {
			eGrid[i] = eStep * i + eMin;
			eIdx[i] = (int) Math.round(ImagingXAFSCommon.getInterpIndex(eGrid[i], energies));
		}
		float[] intensity;
		float[] dmut = (float[]) impDmut.getProcessor().getPixels();
		float[][] data = new float[slc][];
		int pos;
		for (int i = 0; i < slc; i++) {
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
		ImagePlus impMatrix = NewImage.createFloatImage("Spectral map of " + impNorm.getTitle(), ePts, iPts, 1,
				NewImage.FILL_BLACK);
		float[] matrix = (float[]) impMatrix.getProcessor().getPixels();
		for (int i = 0; i < ePts; i++) {
			for (int j = 0; j < iPts; j++) {
				matrix[(iPts - 1 - j) * ePts + i] = data[eIdx[i]][j];
			}
		}
		impMatrix.resetDisplayRange();
		impMatrix.show();
		IJ.run("Fire");
	}
}
