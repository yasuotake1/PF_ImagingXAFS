package imagingXAFS.nw2a_orca;

import imagingXAFS.common.*;

import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;

public class Load_OrcaStack implements PlugIn {

	public static ImagePlus impStack;
	static boolean i0Corr = true;
	static boolean eneCorr = true;
	static boolean autoSave = true;
	private static final String msg = "Note:\n"
			+ "Select a 9809 file for Reference file field to calculate absorbance, or enter an integer value to use constant I0."
			+ "\nSelect an image file for Dark file field to subtract dark image, or enter an integer value to subtract constant."
			+ "\nMultiple dark images (*_dk[0-9].img) are searched for automatically.";

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load ORCA-Flash imagestack");
		gd.addFileField("Transmission images (9809 format)", OrcaCommon.strImg);
		gd.addFileField("Reference images (9809 format) or constant", OrcaCommon.strRef);
		gd.addFileField("Dark image or constant", OrcaCommon.strDark);
		gd.addChoice("Binning", OrcaCommon.arrBinning, OrcaCommon.strBinning);
		gd.addMessage(msg);
		gd.addNumericField("Energy offset", OrcaCommon.ofsEne, 2);
		gd.addCheckbox("I0 correction", i0Corr);
		gd.addCheckbox("Energy correction", eneCorr);
		gd.addCheckbox("Save stack files", autoSave);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg9809 = gd.getNextString();
		String strRef9809 = gd.getNextString();
		String strDark = gd.getNextString();
		if (!ImagingXAFSCommon.isExistingPath(strImg9809))
			return;
		OrcaCommon.strImg = strImg9809;
		OrcaCommon.strRef = strRef9809;
		OrcaCommon.setDark(strDark);
		OrcaCommon.strBinning = gd.getNextChoice();
		OrcaCommon.ofsEne = gd.getNextNumber();
		i0Corr = gd.getNextBoolean();
		eneCorr = gd.getNextBoolean();
		autoSave = gd.getNextBoolean();
		load(strImg9809, strRef9809);
	}

	public static void load(String strImg9809Path, String strRef9809Path) {
		if (!ImagingXAFSCommon.isExistingPath(strImg9809Path))
			return;
		double[] energies = ImagingXAFSCommon.readEnergies(strImg9809Path);
		if (OrcaCommon.ofsEne <= -0.01 || OrcaCommon.ofsEne >= 0.01) {
			for (int i = 0; i < energies.length; i++) {
				energies[i] += OrcaCommon.ofsEne;
			}
		}
		double[] intImg = ImagingXAFSCommon.readIntensities(strImg9809Path);
		double[] intRef = new double[intImg.length];

		int i = 0;
		int j = 0;
		boolean multi = false;
		String strImg = strImg9809Path + (intImg.length > 999 ? "_0000.img" : "_000.img");
		if (!ImagingXAFSCommon.isExistingPath(strImg)) {
			strImg = strImg9809Path + (intImg.length > 999 ? "_0000_000.img" : "_000_000.img");
			if (!ImagingXAFSCommon.isExistingPath(strImg))
				return;
			multi = true;
		}

		boolean multiRef = false;
		String strRef = null;
		if (ImagingXAFSCommon.isExistingPath(strRef9809Path)) {
			intRef = ImagingXAFSCommon.readIntensities(strRef9809Path);
			strRef = strRef9809Path + (intImg.length > 999 ? "_0000.img" : "_000.img");
			if (!ImagingXAFSCommon.isExistingPath(strRef)) {
				strRef = strRef9809Path + (intImg.length > 999 ? "_0000_000.img" : "_000_000.img");
				if (ImagingXAFSCommon.isExistingPath(strRef))
					multiRef = true;
			}
		}
		boolean constRef = OrcaCommon.isInteger(strRef9809Path);
		double constRefValue = constRef ? Integer.parseInt(strRef9809Path) : 0.0;

		OrcaProps prop = OrcaCommon.readProps();
		ImageStack stack = null;
		FileInfo fi = null;
		ImagePlus impImg, impRef, impTgt;
		ImageCalculator iCal = new ImageCalculator();
		ImageConverter iConv;
		short[] arrShort;
		int[] arrInt;
		float[] arrFloat;
		while (ImagingXAFSCommon.isExistingPath(strImg)) {
			IJ.showStatus("Loading image " + String.format(intImg.length > 999 ? "%04d" : "%03d", i));
			IJ.showProgress(i, energies.length);
			impImg = OrcaCommon.loadOrca(strImg, prop, true);
			if (multi) {
				j = 0;
				arrInt = new int[((short[]) impImg.getProcessor().getPixels()).length];
				do {
					impImg = OrcaCommon.loadOrca(strImg, prop, true);
					arrShort = (short[]) impImg.getProcessor().getPixels();
					for (int k = 0; k < arrInt.length; k++) {
						arrInt[k] += arrShort[k] < 0 ? 65536 + arrShort[k] : arrShort[k];
					}
					j++;
					strImg = strImg9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i) + "_"
							+ String.format("%03d", j) + ".img";
				} while (ImagingXAFSCommon.isExistingPath(strImg));
				for (int k = 0; k < arrInt.length; k++) {
					arrInt[k] /= j;
					arrShort[k] = (short) (arrInt[k] > 32767 ? arrInt[k] - 65536 : arrInt[k]);
				}
				impImg.setTitle(impImg.getTitle().substring(0, impImg.getTitle().length() - 9));
			}
			if (impImg == null)
				break;
			if (i == 0) {
				stack = new ImageStack(impImg.getWidth(), impImg.getHeight());
				fi = impImg.getOriginalFileInfo();
			}

			if (ImagingXAFSCommon.isExistingPath(strRef)) {
				impRef = OrcaCommon.loadOrca(strRef, prop, true);
				if (multiRef) {
					j = 0;
					arrInt = new int[((short[]) impRef.getProcessor().getPixels()).length];
					do {
						impRef = OrcaCommon.loadOrca(strRef, prop, true);
						arrShort = (short[]) impRef.getProcessor().getPixels();
						for (int k = 0; k < arrInt.length; k++) {
							arrInt[k] += arrShort[k] < 0 ? 65536 + arrShort[k] : arrShort[k];
						}
						j++;
						strRef = strRef9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i)
								+ "_" + String.format("%03d", j) + ".img";
					} while (ImagingXAFSCommon.isExistingPath(strRef));
					for (int k = 0; k < arrInt.length; k++) {
						arrInt[k] /= j;
						arrShort[k] = (short) (arrInt[k] > 32767 ? arrInt[k] - 65536 : arrInt[k]);
					}
					impImg.setTitle(impImg.getTitle().substring(0, impImg.getTitle().length() - 9));
				}
				impTgt = iCal.run("divide create 32-bit", impRef, impImg);
				impTgt.setTitle(
						impImg.getTitle().replace(".img", "") + " (" + String.format("%.2f", energies[i]) + " eV)");
				impTgt.getProcessor().log();
				if (i0Corr) {
					impTgt.getProcessor().add(Math.log(intImg[i] / intRef[i]));
				}
				strRef = strRef9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
						+ (multiRef ? "_000.img" : ".img");
			} else if (constRef) {
				impTgt = impImg;
				iConv = new ImageConverter(impTgt);
				iConv.convertToGray32();
				arrFloat = (float[]) impTgt.getProcessor().getPixels();
				for (int k = 0; k < arrFloat.length; k++) {
					arrFloat[k] = (float) Math.log(constRefValue / arrFloat[k]);
				}
				if (i0Corr) {
					impTgt.getProcessor().add(Math.log(intImg[i] / intImg[0]));
				}
				impTgt.setTitle(
						impImg.getTitle().replace(".img", "") + " (" + String.format("%.2f", energies[i]) + " eV)");
			} else {
				impTgt = impImg;
				if (i0Corr) {
					impTgt.getProcessor().multiply(intImg[0] / intImg[i]);
				}
				impTgt.setTitle(impImg.getTitle() + "(" + String.format("%.2f", energies[i]) + " eV)");
			}
			stack.addSlice(impTgt.getTitle(), impTgt.getProcessor());
			strImg = strImg9809Path + "_" + String.format(intImg.length > 999 ? "%04d" : "%03d", i + 1)
					+ (multi ? "_000.img" : ".img");
			i++;
		}
		OpenDialog.setDefaultDirectory(Paths.get(strImg).getParent().toString());

		int intBin = OrcaCommon.getIntBinning();
		if (intBin > 1) {
			impStack = impStack.resize(prop.width / intBin, prop.height / intBin, "average");
			impStack.setTitle(Paths.get(strImg9809Path).getFileName().toString());
		} else {
			impStack = new ImagePlus(Paths.get(strImg9809Path).getFileName().toString(), stack);
		}
		ImagingXAFSCommon.setPropEnergies(impStack, energies);
		OrcaCommon.setCalibration(impStack, prop, intBin);
		OrcaCommon.writeProps(prop);
		impStack.changes = false;
		if (eneCorr) {
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

	/**
	 * Sets private variables for loading an ORCA imagestack.
	 * 
	 * @param _i0Corr   If true, it performs intensity correction of each images by
	 *                  I0 ion chamber intensity.
	 * @param _eneCorr  If true, it performs photon energy correction in vertical
	 *                  direction.
	 * @param _autoSave If true, it saves the loaded stack as TIFF.
	 */
	public static void setOptions(boolean _i0Corr, boolean _eneCorr, boolean _autoSave) {
		i0Corr = _i0Corr;
		eneCorr = _eneCorr;
		autoSave = _autoSave;
	}

	public static boolean getI0Corr() {
		return i0Corr;
	}

	/**
	 * Performs photon energy correction in vertical direction, based on M. Katayama
	 * et al. (https://doi.org/10.1107/S0909049512028282).
	 * 
	 * @param impSrc
	 * @param showStatus
	 * @return Energy-corrected version of impSrc.
	 */
	public static ImagePlus GetCorrectedStack(ImagePlus impSrc, boolean showStatus) {
		int[] Dimensions = impSrc.getDimensions();
		int nSlices = Dimensions[3];
		int currentSliceNumber = impSrc.getSlice();
		double[] energies = ImagingXAFSCommon.getPropEnergies(impSrc);
		if (energies == null)
			return impSrc;

		double[] correctedEnergies = new double[nSlices];
		OrcaProps prop = OrcaCommon.readProps();
		ImagePlus impTgt = impSrc.duplicate();
		impTgt.setFileInfo(impSrc.getOriginalFileInfo());
		double correctedIdx;
		impSrc.hide();
		float[] data1 = new float[Dimensions[0]];
		float[] data2 = new float[Dimensions[0]];
		float[] data3 = new float[Dimensions[0]];
		for (int i = 0; i < Dimensions[1]; i++) {
			if (showStatus) {
				IJ.showStatus("Processing energy correction at y = " + i + " in " + impSrc.getTitle() + "...");
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
