package imagingXAFS.bl15a1;

import java.util.Arrays;
import ij.*;
import ij.plugin.*;
import ij.io.OpenDialog;
import ij.io.DirectoryChooser;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imagingXAFS.common.ImagingXAFSCommon;

import java.io.File;
import java.nio.file.Paths;

public class LoadXANESMap implements PlugIn {
	String dirImg = "";
	String prefix = "";
	double[] energies = null;

	public void run(String arg) {
	}

	public ImagePlus[] method1(String[] listSuffixes, String[] listChName, BL15A1Props prop) {
		boolean bRev = prop.stageConf != 0;

		IJ.run("Close All");

		OpenDialog.setDefaultDirectory(prop.defaultDir);
		DirectoryChooser dc = new DirectoryChooser("Choose directory for XANES mapping dataset...");
		if (dc.getDirectory() == null) {
			return null;
		}
		dirImg = dc.getDirectory();
		prop.defaultDir = dirImg;
		BL15A1Common.WriteProps(prop);

		String[] listAll = new File(dirImg).list();
		String[] listFiles = new String[listSuffixes.length];
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i] = "";
		}

		for (int i = 0; i < listSuffixes.length; i++) {
			if ((listSuffixes[i]).length() > 0) {
				for (int j = 0; j < listAll.length; j++) {
					if ((listAll[j]).endsWith(listSuffixes[i])) {
						if (listFiles.length == 0) {
							listFiles[i] = listAll[j];
						} else {
							listFiles[i] = listFiles[i] + "," + listAll[j];
						}
					}
				}
			}
		}

		int[] arrCount = new int[listFiles.length];
		for (int i = 0; i < listFiles.length; i++) {
			arrCount[i] = listFiles[i].length();

		}
		int max1 = arrCount[0];
		for (int i = 1; i < arrCount.length; i++) {
			int v = arrCount[i];
			if (v > max1) {
				max1 = v;
			}
		}
		if (max1 == 0) {
			IJ.error("No files to load.");
		}
		String FileTitle = "";

		double[] scale = { Double.NaN, Double.NaN, Double.NaN, Double.NaN };
		ImagePlus[] listImps = new ImagePlus[listChName.length];

		for (int i = 0; i < listSuffixes.length; i++) {
			if (listFiles[i] != "") {

				String[] sublist = (listFiles[i]).split(",", 0);
				int[] Subsublist = new int[sublist.length];

				Subsublist[0] = 0;
				for (int h = 1; h < sublist.length; h++) {
					sublist[h] = (sublist[h]).replace(listSuffixes[i], "");
					int StrIndex = (sublist[h]).lastIndexOf("_");
					int StrLength = (sublist[h]).length();
					Subsublist[h] = Integer.parseInt((sublist[h]).substring(StrIndex + 1, StrLength));
					FileTitle = (sublist[h]).substring(0, StrIndex + 1);
				}
				Arrays.sort(Subsublist);
				for (int k = 1; k < sublist.length; k++) {
					sublist[k] = FileTitle + String.format("%03d", Subsublist[k]) + listSuffixes[i];
				}

				prefix = FileTitle.substring(0, FileTitle.indexOf("_qscan_"));
				if (Double.isNaN(scale[0]) && Double.isNaN(scale[1]) && Double.isNaN(scale[2])
						&& Double.isNaN(scale[3]))
					scale = BL15A1Common.getScanInfo(dirImg, prefix + "_qscan_001", prop);
				energies = ImagingXAFSCommon.readEnergies(Paths.get(dirImg + prefix));
				if (energies == null) {
					energies = ImagingXAFSCommon
							.readEnergies(Paths.get(dirImg + prefix.substring(0, prefix.length() - 4)));
				}
				ImageStack stack = new ImageStack();
				TextReader tr = new TextReader();
				ImageProcessor ip;
				for (int j = 1; j < sublist.length; j++) {
					String file = dirImg + sublist[j];
					ip = tr.open(file);
					if (j == 1) {
						stack = new ImageStack(ip.getWidth(), ip.getHeight());
					}
					if (!bRev) {
						ip.flipHorizontal();
					}
					ip.flipVertical();
					stack.addSlice(ip);
				}
				listImps[i] = new ImagePlus(prefix + "_" + listChName[i], stack);
				listImps[i].show();
				listImps[i].setActivated();
				if (!Double.isNaN(scale[0]) && !Double.isNaN(scale[1]) && !Double.isNaN(scale[2])
						&& !Double.isNaN(scale[3])) {
					IJ.run("Properties...", "unit=" + prop.scaleConf + " pixel_width=" + scale[0] + " pixel_height="
							+ scale[1] + " origin=" + scale[2] + "," + scale[3]);
				} else {
					IJ.run("Properties...", "unit=pixel pixel_width=1 pixel_height=1 origin=0,0");
				}
				if (energies != null) {
					ImagingXAFSCommon.setPropEnergies(listImps[i], energies);
				}
				IJ.run("Set... ", "zoom=" + prop.zoom);
				IJ.run("Scale to Fit", "");
			} else {
				listImps[i] = null;
			}
		}
		FileInfo fi;
		for (int i = 0; i < listImps.length; i++) {
			if (listImps[i] != null) {
				listImps[i].setTitle(prefix + "_" + listChName[i] + ".tif");
				fi = listImps[i].getFileInfo();
				fi.directory = dirImg;
				fi.fileName = listImps[i].getTitle();
				listImps[i].setFileInfo(fi);
				FileSaver fs = new FileSaver(listImps[i]);
				fs.saveAsTiff(fi.getFilePath());
			}
		}

		return listImps;
	}

	public ImagePlus[] method2(ImagePlus[] listImps, String[] listI0CorrName, BL15A1Props prop) {
		ImagePlus[] listI0CorrImps = new ImagePlus[listI0CorrName.length];
		ImageCalculator ic = new ImageCalculator();
		int slc = listImps[0].getNSlices();
		double[] i0median = new double[slc];
		for (int i = 0; i < slc; i++) {
			listImps[0].setSlice(i + 1);
			i0median[i] = BL15A1Common.getMedian((FloatProcessor) listImps[0].getProcessor());
		}

		for (int i = 0; i < listI0CorrImps.length; i++) {
			if (listImps[0] != null && listImps[i + 1] != null) {
				listI0CorrImps[i] = ic.run("Divide create 32-bit stack", listImps[i + 1], listImps[0]);
				listI0CorrImps[i].setTitle(prefix + "_" + listI0CorrName[i]);
				listI0CorrImps[i].show();
				if (i == 0) {
					IJ.run(listI0CorrImps[i], "Reciprocal", "stack");
					IJ.run(listI0CorrImps[i], "Log", "stack");
				} else {
					for (int j = 0; j < slc; j++) {
						listI0CorrImps[i].setSlice(j + 1);
						listI0CorrImps[i].getProcessor().multiply(i0median[i]);
					}
				}
				IJ.run(listI0CorrImps[i], "Set... ", "zoom=" + prop.zoom);
				IJ.run(listI0CorrImps[i], "Scale to Fit", "");
				IJ.run(listI0CorrImps[i], "Enhance Contrast...", "saturated=0.1");
				if (energies != null) {
					ImagingXAFSCommon.setPropEnergies(listI0CorrImps[i], energies);
				}
				listI0CorrImps[i].updateAndDraw();
			} else {
				listI0CorrImps[i] = null;
			}
		}
		FileInfo fi;
		for (int i = 0; i < listI0CorrImps.length; i++) {
			if (listI0CorrImps[i] != null) {
				listI0CorrImps[i].setTitle(prefix + "_" + listI0CorrName[i] + ".tif");
				fi = listI0CorrImps[i].getFileInfo();
				fi.directory = dirImg;
				fi.fileName = listI0CorrImps[i].getTitle();
				listI0CorrImps[i].setFileInfo(fi);
				FileSaver fs = new FileSaver(listI0CorrImps[i]);
				fs.saveAsTiff(fi.getFilePath());
			}
		}

		return listI0CorrImps;
	}
}
