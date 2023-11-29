package imagingXAFS.nw2a_orca;

import java.util.Arrays;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;

import imagingXAFS.common.*;

public class Diag_EnergyCorrParam implements PlugIn {

	public void run(String arg) {
		OrcaProps readProps = OrcaCommon.ReadProps();

		GenericDialog gd = new GenericDialog("Energy Correction Diagnostics");
		gd.addMessage("Data source:");
		gd.addFileField("Image data file (9809 format)", "");
		gd.addFileField("Reference data file (9809 format, if exists)", "");
		gd.addNumericField("Constant dark offset", OrcaCommon.ofsInt);
		gd.addCheckbox("I0 normalization", Load_OrcaStack.getNorm());
		gd.addMessage("Normalization:");
		gd.addNumericField("Pre-edge from", ImagingXAFSCommon.normalizationParam[0], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[1], 2, 8, "eV");
		gd.addNumericField("Post-edge from", ImagingXAFSCommon.normalizationParam[2], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[3], 2, 8, "eV");
		gd.addNumericField("Filter threshold", 2.0, 1);
		gd.addNumericField("Normalized absorbance at E0", 0.5, 2);
		gd.addMessage("Correction parameters:");
		gd.addMessage("DCM direction = " + (readProps.dcmDirection == 0 ? "Down" : "Up"));
		gd.addNumericField("Detector position from", readProps.detectorPosition - 5000, 1, 8, "mm");
		gd.addNumericField("to", readProps.detectorPosition + 5000, 1, 8, "mm");
		gd.addNumericField("step", 1000, 1, 8, "mm");
		gd.addMessage("Pixel size = " + readProps.pixelSize + " um");
		gd.addMessage("Distance between crystals = " + readProps.dcmDistance + " mm");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg9809Path = gd.getNextString();
		String strRef9809Path = gd.getNextString();
		int _ofsInt = (int) gd.getNextNumber();
		boolean norm = gd.getNextBoolean();
		double preStart = gd.getNextNumber();
		double preEnd = gd.getNextNumber();
		double postStart = gd.getNextNumber();
		double postEnd = gd.getNextNumber();
		float threshold = (float) gd.getNextNumber();
		float e0Jump = (float) gd.getNextNumber();
		double from = gd.getNextNumber();
		double to = gd.getNextNumber();
		double step = gd.getNextNumber();
		if (from < 0 || to < 0 || step < 0 || (to - from) < step) {
			IJ.error("Invalid parameters.");
			return;
		}
		int num = (int) ((to - from) / step) + 1;
		double[] arrPosition = new double[num];
		for (int i = 0; i < num; i++) {
			arrPosition[i] = step * i + from;
		}
		Load_OrcaStack.setOptions(_ofsInt, 0.0d, "1", norm, false, false);
		Load_OrcaStack.Load(strImg9809Path, strRef9809Path);
		ImagePlus impSrc = Load_OrcaStack.impStack;
		IJ.setTool("rect");
		new WaitForUserDialog("Select rectangle region to analyze, then click OK.\nSelect none not to crop.").show();
		Roi roi = impSrc.getRoi();
		if (roi != null && roi.getType() != Roi.RECTANGLE) {
			IJ.error("Failed to specify region to analyze.");
			impSrc.close();
			return;
		}
		ImagePlus impCrop = impSrc.crop("stack");
		impCrop.setFileInfo(impSrc.getOriginalFileInfo());
		ImagingXAFSCommon.setPropEnergies(impCrop, ImagingXAFSCommon.getPropEnergies(impSrc));
		ImagePlus impCorr, impE0;
		ImageStack stack = new ImageStack();
		ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
		ImagingXAFSCommon.e0Jump = e0Jump;
		OrcaProps prop = OrcaCommon.getDuplicatedProp(readProps);
		int wid = impCrop.getDimensions()[0];
		float[] data1 = new float[wid];
		int hei = impCrop.getDimensions()[1];
		double[] dataX = new double[hei];
		double[] dataY = new double[hei];
		for (int i = 0; i < hei; i++) {
			dataX[i] = prop.pixelSize * (i - hei / 2);
		}
		CurveFitter cf;
		double[] arrE0Slope = new double[num];
		for (int i = 0; i < num; i++) {
			IJ.showStatus("Calculating detectorPosition=" + String.format("%.1f", arrPosition[i]));
			IJ.showProgress(i, num);
			prop.detectorPosition = arrPosition[i];
			OrcaCommon.WriteProps(prop);
			impCorr = Load_OrcaStack.GetCorrectedStack(impCrop, false);
			Normalization.Normalize(impCorr, threshold, false, false, false, false);
			impE0 = Normalization.impE0;
			stack.addSlice(impE0.getProcessor());
			for (int j = 0; j < hei; j++) {
				data1 = impE0.getProcessor().getRow(0, j, data1, wid);
				Arrays.sort(data1);
				dataY[j] = data1[wid / 2];
			}
			cf = new CurveFitter(dataX, dataY);
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			arrE0Slope[i] = cf.getParams()[1];
		}
		OrcaCommon.WriteProps(readProps);
		ImagePlus impResultStack = new ImagePlus("Result stack", stack);
		impResultStack.show();
		Plot plot = new Plot("Result graph", "Detector position (mm)", "Vertical E0 Slope (eV/um)");
		plot.add("connected circle", arrPosition, arrE0Slope);
		plot.show();
	}

}