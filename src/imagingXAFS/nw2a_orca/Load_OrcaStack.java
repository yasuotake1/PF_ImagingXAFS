package imagingXAFS.nw2a_orca;

import imagingXAFS.common.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;

public class Load_OrcaStack implements PlugIn {
	public static ImagePlus impStack;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load ORCA-Flash imagestack");
		gd.addFileField("Image data file (9809 format)", "");
		gd.addFileField("Reference data file (9809 format, if exists)", "");
		gd.addChoice("Binning", OrcaCommon.strBinning, OrcaCommon.strBinning[0]);
		gd.addCheckbox("Energy correction", true);
		gd.addCheckbox("Save automatically", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg9809Path = gd.getNextString();
		Path pathImg9809 = Paths.get(strImg9809Path);
		String strRef9809Path = gd.getNextString();
		Path pathRef9809 = Paths.get(strRef9809Path);
		if (strImg9809Path.isEmpty() || !Files.exists(pathImg9809))
			return;
		String strBinning = gd.getNextChoice();
		boolean corr = gd.getNextBoolean();
		boolean autoSave = gd.getNextBoolean();
		double[] energies = ImagingXAFSCommon.readEnergies(pathImg9809);
		OrcaProps prop = OrcaCommon.ReadProps();

		int i = 0;
		Path pathImg = Paths.get(strImg9809Path + "_" + String.format("%03d", i) + ".img");
		Path pathRef = null;
		if (!strRef9809Path.isEmpty() && Files.exists(pathRef9809)) {
			pathRef = Paths.get(strRef9809Path + "_" + String.format("%03d", i) + ".img");
		}
		ImageStack stack = null;
		FileInfo fi = null;
		ImagePlus impImg, impRef, impTgt;
		ImageCalculator ic = new ImageCalculator();
		while (Files.exists(pathImg)) {
			IJ.showStatus("Loading image " + String.format("%03d", i));
			IJ.showProgress(i, energies.length);
			impImg = OrcaCommon.LoadOrca(pathImg, prop);
			if (impImg == null)
				break;
			if (i == 0) {
				stack = new ImageStack(impImg.getWidth(), impImg.getHeight());
				fi = impImg.getOriginalFileInfo();
			}

			if (pathRef != null && Files.exists(pathRef)) {
				impRef = OrcaCommon.LoadOrca(pathRef, prop);
				impTgt = ic.run("divide create 32-bit", impRef, impImg);
				impTgt.setTitle(impImg.getTitle().replace(".img", "") + " (" + String.format("%.2f", energies[i])
						+ " eV)");
				impTgt.getProcessor().log();
				pathRef = Paths.get(strRef9809Path + "_" + String.format("%03d", i + 1) + ".img");
			} else {
				impTgt = impImg;
				impTgt.setTitle(impImg.getTitle() + "(" + String.format("%.2f", energies[i]) + " eV)");
			}
			pathImg = Paths.get(strImg9809Path + "_" + String.format("%03d", i + 1) + ".img");
			stack.addSlice(impTgt.getTitle(), impTgt.getProcessor());

			i++;
		}
		OpenDialog.setDefaultDirectory(pathImg.getParent().toString());
		impStack = new ImagePlus(pathImg9809.getFileName().toString(), stack);
		int intBin = 1;
		if (strBinning != OrcaCommon.strBinning[0]) {
			try {
				intBin = Integer.parseInt(strBinning);
				impStack = impStack.resize(prop.width / intBin, prop.height / intBin, "average");
			} catch (NumberFormatException e) {
			}
		}
		impStack.setTitle(pathImg9809.getFileName().toString());
		ImagingXAFSCommon.setPropEnergies(impStack, energies);

		OrcaCommon.setCalibration(impStack, prop, intBin);
		impStack.changes = false;
		fi.fileName = impStack.getTitle();
		impStack.setFileInfo(fi);
		if(autoSave) {
			IJ.saveAsTiff(impStack, fi.getFilePath() + ".tif");
		}
		if(corr) {
			impStack = GetCorrectedStack(impStack);
			if(autoSave) {
				IJ.saveAsTiff(impStack, fi.getFilePath() + "_corrected.tif");
			}
		}
		impStack.show();
		IJ.run(impStack, "Enhance Contrast...", "saturated=0.1 use");
		impStack.updateAndDraw();
		IJ.setTool("multipoint");
	}
	
	public static ImagePlus GetCorrectedStack(ImagePlus impSrc) {
		int[] Dimensions = impSrc.getDimensions();
		int nSlices = Dimensions[3];
		int currentSliceNumber = impSrc.getSlice();
		double[] energies = ImagingXAFSCommon.getPropEnergies(impSrc);
		if(energies==null)
			return impSrc;
		
		double[] correctedEnergies = new double[nSlices];
		OrcaProps prop = OrcaCommon.ReadProps();
		ImagePlus impTgt = impSrc.duplicate();
		impTgt.setFileInfo(impSrc.getOriginalFileInfo());
		double correctedIdx;
		impSrc.hide();
		float[] data1 = new float[Dimensions[0]];
		float[] data2 = new float[Dimensions[0]];
		float[] data3 = new float[Dimensions[0]];
		for (int i = 0; i < Dimensions[1]; i++) {
			IJ.showStatus(
					"Processing energy correction at y = " + String.valueOf(i) + " in " + impSrc.getTitle() + "...");
			IJ.showProgress(i, Dimensions[1]);
			for (int j = 0; j < nSlices; j++) {
				correctedEnergies[j] = OrcaCommon.getCorrectedE((double) i, (double) Dimensions[1] / 2,
						energies[j], prop, impSrc.getCalibration());
			}

			for (int j = 0; j < nSlices; j++) {
				impTgt.setSlice(j + 1);
				correctedIdx = ImagingXAFSCommon.getInterpIndex(energies[j], correctedEnergies);
				if (ImagingXAFSCommon.doInterp(correctedIdx)) {
					float ratio = (float) (correctedIdx - Math.floor(correctedIdx));
					impSrc.setSlice((int) Math.floor(correctedIdx) + 1);
					data1 = impSrc.getProcessor().getRow(0, i, data1, Dimensions[0]);
					impSrc.setSlice((int) Math.floor(correctedIdx) + 2);
					data2 = impSrc.getProcessor().getRow(0, i, data2, Dimensions[0]);
					for (int k = 0; k < Dimensions[0]; k++) {
						data3[k] = data1[k] + (data2[k] - data1[k]) * ratio;
					}
					impTgt.getProcessor().putRow(0, i, data3, Dimensions[0]);
				} else {
					impSrc.setSlice((int) (correctedIdx + 1.5));
					data1 = impSrc.getProcessor().getRow(0, i, data1, Dimensions[0]);
					impTgt.getProcessor().putRow(0, i, data1, Dimensions[0]);
				}
			}
		}

		impSrc.setSlice(currentSliceNumber);
		if (impSrc.getTitle().endsWith(".tif"))
			impTgt.setTitle(impSrc.getTitle().replace(".tif", "_corrected.tif"));
		else
			impTgt.setTitle(impSrc.getTitle() + "_corrected.tif");
		impTgt.setSlice(currentSliceNumber);
		impSrc.close();
		return impTgt;
	}
}
