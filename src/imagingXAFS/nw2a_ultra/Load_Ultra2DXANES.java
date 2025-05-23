package imagingXAFS.nw2a_ultra;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imagingXAFS.common.ImagingXAFSCommon;

public class Load_Ultra2DXANES implements PlugIn {

	public static ImagePlus impStack;
	private static boolean autoSave = true;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load UltraXRM 2D XANES");
		gd.addFileField("ScanInfo file", "");
		gd.addChoice("Binning", UltraCommon.LIST_BINNING, UltraCommon.LIST_BINNING[0]);
		gd.addChoice("Mode", UltraCommon.lOADINGMODES, UltraCommon.lOADINGMODES[0]);
		gd.addCheckbox("Save automatically", autoSave);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String pathInfo = gd.getNextString();
		String strBinning = gd.getNextChoice();
		int intMode = gd.getNextChoiceIndex();
		autoSave = gd.getNextBoolean();

		try {
			IJ.showStatus("Loading ScanInfo...");
			UltraScanInfo si = new UltraScanInfo(pathInfo);
			if (!si.energy) {
				IJ.error("Invalid ScanInfo file.");
				return;
			}
			String[] arrImg = si.imageFiles;
			String[] arrRef = si.referenceFiles;
			if (si.nRepeatScan > 1) {
				String[] listRep = new String[si.nRepeatScan];
				Pattern p = Pattern.compile("rep[0-9]+_");
				String strCheck = "";
				int counter = 0;
				for (int i = 0; i < arrImg.length; i++) {
					Matcher m = p.matcher(arrImg[i]);
					if (m.find() && !strCheck.equals(m.group())) {
						listRep[counter] = strCheck = m.group();
						counter++;
					}
					if (counter == listRep.length)
						break;
				}

				GenericDialog gdRepeat = new GenericDialog("Repetition");
				gdRepeat.addMessage("Loading single 2D XANES in a repetition...");
				gdRepeat.addChoice("", listRep, listRep[0]);
				gdRepeat.showDialog();
				if (gdRepeat.wasCanceled())
					return;
				String choice = gdRepeat.getNextChoice();
				arrImg = extractArray(choice, arrImg);
				arrRef = extractArray(choice, arrRef);
			}
			if (si.mosaic && intMode != 2) {
				String[] listX = new String[si.numMosaicX];
				for (int i = 0; i < listX.length; i++) {
					listX[i] = String.format("%02d", i + 1);
				}
				String[] listY = new String[si.numMosaicY];
				for (int i = 0; i < listY.length; i++) {
					listY[i] = String.format("%02d", i + 1);
				}
				GenericDialog gdMosaic = new GenericDialog("Mosaic");
				gdMosaic.addMessage("Loading single 2D XANES from a mosaic scan...");
				gdMosaic.addChoice("Index X", listX, listX[0]);
				gdMosaic.addChoice("Index Y", listY, listY[0]);
				gdMosaic.showDialog();
				if (gdMosaic.wasCanceled())
					return;
				String keyMosaic = "X" + gdMosaic.getNextChoice();
				keyMosaic += "_Y" + gdMosaic.getNextChoice();
				arrImg = extractArray(keyMosaic, arrImg);
			}
			if (si.tomo && intMode != 2) {
				String[] listAngle = new String[si.angles.length];
				for (int i = 0; i < listAngle.length; i++) {
					listAngle[i] = String.format("%07.2f", si.angles[i]);
				}
				GenericDialog gdAngle = new GenericDialog("Tomography");
				gdAngle.addMessage("Loading single 2D XANES from a tomography...");
				gdAngle.addChoice("Angle", listAngle, listAngle[0]);
				gdAngle.showDialog();
				if (gdAngle.wasCanceled())
					return;
				arrImg = extractArray(gdAngle.getNextChoice() + "_Degree", arrImg);
			}
			ImagePlus impImg, impRef, impTgt;
			impImg = XRM_Reader.Load(si.directory + arrImg[0], false);// Load one image to read info, to skip reading
																		// info later on.
			String title = UltraCommon.removePattern(UltraCommon.P_ENERGY, impImg.getTitle());
			title = UltraCommon.removePattern(UltraCommon.P_NEXP, title).replace(".xrm", ".tif");
			Calibration calib = impImg.getCalibration();
			FileInfo fi = impImg.getOriginalFileInfo();
			ImageStack stack = new ImageStack(impImg.getWidth(), impImg.getHeight());
			ImageCalculator ic = new ImageCalculator();
			for (int i = 0; i < si.energies.length; i++) {
				IJ.showStatus("Loading image " + String.format("%03d", i));
				switch (intMode) {
				case 0:// "Apply reference"
					impImg = loadAndAverage(arrImg, si.directory, i, si.nExposures);
					impRef = loadAndAverage(arrRef, si.directory, i, si.refNExposures);
					impTgt = ic.run("divide create 32-bit", impRef, impImg);
					impTgt.getProcessor().log();
					impTgt.setTitle(impImg.getTitle().replace(".xrm", ""));
					stack.addSlice(impTgt.getTitle(), impTgt.getProcessor());
					break;
				case 1:// "Do not apply reference"
					impImg = loadAndAverage(arrImg, si.directory, i, si.nExposures);
					stack.addSlice(impImg.getTitle(), impImg.getProcessor());
					break;
				case 2:// "Load reference only"
					impRef = loadAndAverage(si.referenceFiles, si.directory, i, si.refNExposures);
					stack.addSlice(impRef.getTitle(), impRef.getProcessor());
					break;
				}
			}
			impStack = new ImagePlus(title, stack);
			int intBin = 1;
			if (strBinning != UltraCommon.LIST_BINNING[0]) {
				try {
					intBin = Integer.parseInt(strBinning);
					impStack = impStack.resize(fi.width / intBin, fi.height / intBin, "average");
					calib.pixelWidth *= intBin;
					calib.pixelHeight *= intBin;
				} catch (NumberFormatException e) {
				}
			}
			impStack.setTitle(title);
			impStack.setCalibration(calib);
			fi.fileName = title.replace(".xrm", ".tif");
			impStack.setFileInfo(fi);
			ImagingXAFSCommon.setPropEnergies(impStack, si.energies);
			impStack.changes = false;
			if (autoSave) {
				IJ.saveAsTiff(impStack, fi.getFilePath());
			}
			impStack.show();
		} catch (Exception e) {
			ImagingXAFSCommon.logStackTrace(e);
			return;
		}
	}

	private String[] extractArray(String key, String[] arr) {
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < arr.length; i++) {
			if (arr[i].contains(key))
				list.add(arr[i]);
		}
		return list.toArray(new String[list.size()]);
	}

	private ImagePlus loadAndAverage(String[] list, String directory, int idxEne, int numExp) {
		ImagePlus source[] = new ImagePlus[numExp];
		for (int i = 0; i < source.length; i++) {
			source[i] = XRM_Reader.Load(directory + list[idxEne * numExp + i], true);
		}
		return averageImagePlus(source);
	}

	private ImagePlus averageImagePlus(ImagePlus[] imps) {
		int len = imps.length;
		if (len == 0)
			return null;

		ImageProcessor ip = imps[0].getProcessor().convertToFloat();
		if (len > 1) {
			float[] tgt = (float[]) ip.getPixels();
			switch (imps[0].getType()) {
			case ImagePlus.GRAY8:
				byte[] srcB;
				for (int i = 1; i < len; i++) {
					srcB = (byte[]) imps[i].getProcessor().getPixels();
					for (int j = 0; j < tgt.length; j++) {
						tgt[j] += (int) (srcB[j] & 0xFF);
					}
				}
				break;
			case ImagePlus.GRAY16:
				short[] srcS;
				for (int i = 1; i < len; i++) {
					srcS = (short[]) imps[i].getProcessor().getPixels();
					for (int j = 0; j < tgt.length; j++) {
						tgt[j] += (int) (srcS[j] & 0xFFFF);
					}
				}
				break;
			default:// GRAY32
				float[] srcF;
				for (int i = 1; i < len; i++) {
					srcF = (float[]) imps[i].getProcessor().getPixels();
					for (int j = 0; j < tgt.length; j++) {
						tgt[j] += srcF[j];
					}
				}
				break;
			}
			for (int i = 0; i < tgt.length; i++) {
				tgt[i] /= len;
			}
		}
		ImagePlus result = new ImagePlus(UltraCommon.removePattern(UltraCommon.P_NEXP, imps[0].getTitle()), ip);
		result.setCalibration(imps[0].getCalibration());
		result.setFileInfo(imps[0].getOriginalFileInfo());
		return result;
	}

}
