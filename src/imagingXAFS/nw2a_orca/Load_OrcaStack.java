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
	static boolean norm = true;
	static boolean corr = true;
	static boolean autoSave = true;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load ORCA-Flash imagestack");
		gd.addFileField("Image data file (9809 format)", "");
		gd.addFileField("Reference data file (9809 format, if exists)", "");
		gd.addNumericField("Constant dark offset", OrcaCommon.ofsInt);
		gd.addNumericField("Energy offset", OrcaCommon.ofsEne, 2);
		gd.addChoice("Binning", OrcaCommon.arrBinning, OrcaCommon.strBinning);
		gd.addCheckbox("I0 normalization", norm);
		gd.addCheckbox("Energy correction", corr);
		gd.addCheckbox("Save automatically", autoSave);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg9809Path = gd.getNextString();
		String strRef9809Path = gd.getNextString();
		int ofsInt = (int) gd.getNextNumber();
		double ofsEne = gd.getNextNumber();
		String strBinning = gd.getNextChoice();
		norm = gd.getNextBoolean();
		corr = gd.getNextBoolean();
		autoSave = gd.getNextBoolean();
		setOptions(ofsInt, ofsEne, strBinning, norm, corr, autoSave);
		Load(strImg9809Path, strRef9809Path);
	}

	public static void Load(String strImg9809Path, String strRef9809Path) {
		Path pathImg9809 = Paths.get(strImg9809Path);
		Path pathRef9809 = Paths.get(strRef9809Path);
		if (strImg9809Path.isEmpty() || !Files.exists(pathImg9809))
			return;
		double[] energies = ImagingXAFSCommon.readEnergies(pathImg9809);
		if (OrcaCommon.ofsEne <= -0.01 || OrcaCommon.ofsEne >= 0.01) {
			for (int i = 0; i < energies.length; i++) {
				energies[i] += OrcaCommon.ofsEne;
			}
		}
		double[] intImg = ImagingXAFSCommon.readIntensities(pathImg9809);
		double[] intRef = pathRef9809 != null ? ImagingXAFSCommon.readIntensities(pathRef9809) : null;
		OrcaProps prop = OrcaCommon.ReadProps();

		int i = 0;
		int j = 0;
		boolean multi = false;
		Path pathImg = Paths.get(strImg9809Path + (intImg.length > 999 ? "_0000.img" : "_000.img"));
		if (!Files.exists(pathImg)) {
			pathImg = Paths.get(strImg9809Path + (intImg.length > 999 ? "_0000_000.img" : "_000_000.img"));
			if (!Files.exists(pathImg))
				return;
			multi = true;
		}

		boolean multiRef = false;
		Path pathRef = null;
		if (!strRef9809Path.isEmpty() && Files.exists(pathRef9809)) {
			pathRef = Paths.get(strRef9809Path + (intImg.length > 999 ? "_0000.img" : "_000.img"));
			if (!Files.exists(pathRef)) {
				pathRef = Paths.get(strRef9809Path + (intImg.length > 999 ? "_0000_000.img" : "_000_000.img"));
				if (Files.exists(pathRef))
					multiRef = true;
			}
		}
		ImageStack stack = null;
		FileInfo fi = null;
		ImagePlus impImg, impRef, impTgt;
		ImageCalculator ic = new ImageCalculator();
		short[] pixels;
		int[] arr;
		while (Files.exists(pathImg)) {
			IJ.showStatus("Loading image " + String.format(intImg.length > 999 ? "%04d" : "%03d", i));
			IJ.showProgress(i, energies.length);
			impImg = OrcaCommon.LoadOrca(pathImg, prop);
			if (multi) {
				j = 0;
				arr = new int[((short[]) impImg.getProcessor().getPixels()).length];
				do {
					impImg = OrcaCommon.LoadOrca(pathImg, prop);
					pixels = (short[]) impImg.getProcessor().getPixels();
					for (int k = 0; k < arr.length; k++) {
						arr[k] += pixels[k] < 0 ? 65536 + pixels[k] : pixels[k];
					}
					j++;
					pathImg = Paths
							.get(strImg9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
									+ "_" + String.format("%03d", j) + ".img");
				} while (Files.exists(pathImg));
				for (int k = 0; k < arr.length; k++) {
					arr[k] /= j;
					pixels[k] = (short) (arr[k] > 32767 ? arr[k] - 65536 : arr[k]);
				}
				impImg.setTitle(impImg.getTitle().substring(0, impImg.getTitle().length() - 9));
			}
			if (impImg == null)
				break;
			if (OrcaCommon.ofsInt != 0)
				impImg.getProcessor().add(-OrcaCommon.ofsInt);
			if (i == 0) {
				stack = new ImageStack(impImg.getWidth(), impImg.getHeight());
				fi = impImg.getOriginalFileInfo();
			}

			if (pathRef != null && Files.exists(pathRef)) {
				impRef = OrcaCommon.LoadOrca(pathRef, prop);
				if (multiRef) {
					j = 0;
					arr = new int[((short[]) impRef.getProcessor().getPixels()).length];
					do {
						impRef = OrcaCommon.LoadOrca(pathRef, prop);
						pixels = (short[]) impRef.getProcessor().getPixels();
						for (int k = 0; k < arr.length; k++) {
							arr[k] += pixels[k] < 0 ? 65536 + pixels[k] : pixels[k];
						}
						j++;
						pathRef = Paths
								.get(strRef9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
										+ "_" + String.format("%03d", j) + ".img");
					} while (Files.exists(pathRef));
					for (int k = 0; k < arr.length; k++) {
						arr[k] /= j;
						pixels[k] = (short) (arr[k] > 32767 ? arr[k] - 65536 : arr[k]);
					}
					impImg.setTitle(impImg.getTitle().substring(0, impImg.getTitle().length() - 9));
				}
				if (OrcaCommon.ofsInt != 0)
					impRef.getProcessor().add(-OrcaCommon.ofsInt);
				impTgt = ic.run("divide create 32-bit", impRef, impImg);
				impTgt.setTitle(
						impImg.getTitle().replace(".img", "") + " (" + String.format("%.2f", energies[i]) + " eV)");
				impTgt.getProcessor().log();
				if (norm) {
					impTgt.getProcessor().add(Math.log(intImg[i] / intRef[i]));
				}
				pathRef = Paths.get(strRef9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
						+ (multiRef ? "_000.img" : ".img"));
			} else {
				impTgt = impImg;
				impTgt.setTitle(impImg.getTitle() + "(" + String.format("%.2f", energies[i]) + " eV)");
				if (norm) {
					impTgt.getProcessor().multiply(intImg[0] / intImg[i]);
				}
			}
			pathImg = Paths.get(strImg9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
					+ (multi ? "_000.img" : ".img"));
			stack.addSlice(impTgt.getTitle(), impTgt.getProcessor());

			i++;
		}
		OpenDialog.setDefaultDirectory(pathImg.getParent().toString());
		impStack = new ImagePlus(pathImg9809.getFileName().toString(), stack);
		int intBin = 1;
		if (OrcaCommon.strBinning != OrcaCommon.arrBinning[0]) {
			try {
				intBin = Integer.parseInt(OrcaCommon.strBinning);
				impStack = impStack.resize(prop.width / intBin, prop.height / intBin, "average");
			} catch (NumberFormatException e) {
			}
		}
		impStack.setTitle(pathImg9809.getFileName().toString());
		ImagingXAFSCommon.setPropEnergies(impStack, energies);
		OrcaCommon.setCalibration(impStack, prop, intBin);
		OrcaCommon.WriteProps(prop);
		impStack.changes = false;
		if (corr) {
			impStack = GetCorrectedStack(impStack, true);
		}
		fi.fileName = impStack.getTitle();
		impStack.setFileInfo(fi);
		if (autoSave) {
			IJ.saveAsTiff(impStack, fi.getFilePath());
		}
		impStack.show();
		IJ.run(impStack, "Enhance Contrast...", "saturated=0.1 use");
		impStack.updateAndDraw();
		IJ.setTool("multipoint");
	}

	public static void setOptions(int _ofsInt, double _ofsEne, String _strBinning, boolean _norm, boolean _corr,
			boolean _autoSave) {
		OrcaCommon.ofsInt = _ofsInt;
		OrcaCommon.ofsEne = _ofsEne;
		OrcaCommon.strBinning = _strBinning;
		norm = _norm;
		corr = _corr;
		autoSave = _autoSave;
	}

	public static boolean getNorm() {
		return norm;
	}

	public static ImagePlus GetCorrectedStack(ImagePlus impSrc, boolean showStatus) {
		int[] Dimensions = impSrc.getDimensions();
		int nSlices = Dimensions[3];
		int currentSliceNumber = impSrc.getSlice();
		double[] energies = ImagingXAFSCommon.getPropEnergies(impSrc);
		if (energies == null)
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
			if (showStatus) {
				IJ.showStatus("Processing energy correction at y = " + String.valueOf(i) + " in " + impSrc.getTitle()
						+ "...");
				IJ.showProgress(i, Dimensions[1]);
			}
			for (int j = 0; j < nSlices; j++) {
				correctedEnergies[j] = OrcaCommon.getCorrectedE((double) i, (double) Dimensions[1] / 2, energies[j],
						prop, impSrc.getCalibration());
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
			impTgt.setTitle(impSrc.getTitle() + "_corrected");
		impTgt.setSlice(currentSliceNumber);
		impSrc.close();
		return impTgt;
	}
}
