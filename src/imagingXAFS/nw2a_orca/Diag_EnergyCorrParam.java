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
		OrcaProps readProps = OrcaCommon.readProps();

		GenericDialog gd = new GenericDialog("Energy Correction Diagnostics");
		gd.addMessage("Data source:");
		gd.addFileField("Transmission images (9809 format)", OrcaCommon.strImg);
		gd.addFileField("Reference images (9809 format) or constant", OrcaCommon.strRef);
		gd.addFileField("Dark image or constant", OrcaCommon.strDark);
		gd.addCheckbox("Avoid zero in raw images", OrcaCommon.avoidZero);
		gd.addMessage("");
		gd.addCheckbox("I0 correction", Load_OrcaStack.getI0Corr());
		gd.addMessage("Normalization:");
		gd.addNumericField("Pre-edge from", ImagingXAFSCommon.normalizationParam[0], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[1], 2, 8, "eV");
		gd.addNumericField("Post-edge from", ImagingXAFSCommon.normalizationParam[2], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[3], 2, 8, "eV");
		gd.addCheckbox("Zero-slope pre-edge", false);
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

		String strImg9809 = gd.getNextString();
		String strRef9809 = gd.getNextString();
		String strDark = gd.getNextString();
		if (!ImagingXAFSCommon.isExistingPath(strImg9809))
			return;
		OrcaCommon.setDark(strDark);
		OrcaCommon.avoidZero = gd.getNextBoolean();
		OrcaCommon.strBinning = OrcaCommon.arrBinning[0];
		OrcaCommon.ofsEne = 0d;
		boolean i0Corr = gd.getNextBoolean();
		double preStart = gd.getNextNumber();
		double preEnd = gd.getNextNumber();
		double postStart = gd.getNextNumber();
		double postEnd = gd.getNextNumber();
		boolean zeroSlope = gd.getNextBoolean();
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
		Load_OrcaStack.setOptions(i0Corr, false, false);
		Load_OrcaStack.load(strImg9809, strRef9809);
		ImagePlus impSrc = Load_OrcaStack.impStack;
		IJ.setTool("rect");
		new WaitForUserDialog("Select rectangle region to analyze, then click OK.\nSelect none not to crop.").show();
		Roi roi = impSrc.getRoi();
		if (roi != null && roi.getType() != Roi.RECTANGLE) {
			IJ.error("Failed to specify region to analyze.");
			impSrc.close();
			return;
		}

		OrcaProps prop = OrcaCommon.getDuplicatedProp(readProps);
		ImagePlus impCrop = impSrc.crop("stack");
		impCrop.setFileInfo(impSrc.getOriginalFileInfo());
		ImagingXAFSCommon.setPropEnergies(impCrop, ImagingXAFSCommon.getPropEnergies(impSrc));
		ImagePlus impCorr, impE0;
		ImageStack stack = new ImageStack();
		ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
		ImagingXAFSCommon.e0Jump = e0Jump;
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
			OrcaCommon.writeProps(prop);
			impCorr = Load_OrcaStack.GetCorrectedStack(impCrop, false);
			Normalization.Normalize(impCorr, zeroSlope, threshold, false, false, false, false);
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
		OrcaCommon.writeProps(readProps);
		ImagePlus impResultStack = new ImagePlus("Result stack", stack);
		impResultStack.show();
		Plot plot = new Plot("Result graph", "Detector position (mm)", "Vertical E0 Slope (eV/um)");
		plot.add("connected circle", arrPosition, arrE0Slope);
		plot.show();
	}

}