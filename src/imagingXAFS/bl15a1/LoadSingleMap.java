package imagingXAFS.bl15a1;

import ij.*;
import ij.plugin.*;
import ij.process.FloatProcessor;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import java.io.File;

public class LoadSingleMap implements PlugIn {
	public String dir = "";
	public String prefix = "";

	public void run(String arg) {
	}

	public ImagePlus[] method1(String[] listSuffixes, String[] listChName, BL15A1Props prop) {
		boolean bRev = prop.stageConf != 0;

		OpenDialog od = new OpenDialog("Select one of the 2D ASCII data files.", prop.defaultDir, "");
		if (od.getDirectory() == null)
			return null;
		dir = od.getDirectory();
		prop.defaultDir = dir;
		BL15A1Common.WriteProps(prop);

		String nameTemp = od.getFileName();
		prefix = nameTemp;
		for (int i = 0; i < listSuffixes.length; i++) {
			if (listSuffixes[i].length() > 0 && nameTemp.endsWith(listSuffixes[i])) {
				prefix = nameTemp.replace(listSuffixes[i], "");
				break;
			}
		}
		if (prefix == nameTemp) {
			IJ.error("An invalid file is selected.");
			return null;
		}

		String[] listAll = new File(dir).list();
		String[] listFiles = new String[listSuffixes.length];
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i] = "";
		}
		for (int i = 0; i < listAll.length; i++) {
			for (int j = 0; j < listSuffixes.length; j++) {
				if ((listSuffixes[j].length() > 0) && (listAll[i].equals(prefix + listSuffixes[j]))) {
					listFiles[j] = listAll[i];
				}
			}
		}

		double[] scale = BL15A1Common.getScanInfo(dir, prefix, prop);
		ImagePlus[] listImps = new ImagePlus[listChName.length];

		for (int i = 0; i < listFiles.length; i++) {
			if (listFiles[i].length() > 0) {
				String file = dir + listFiles[i];
				listImps[i] = IJ.openImage(file);
				listImps[i].getProcessor().flipVertical();
				if (!bRev) {
					listImps[i].getProcessor().flipHorizontal();
				}
				listImps[i].show();
				listImps[i].setActivated();
				if (!Double.isNaN(scale[0]) && !Double.isNaN(scale[1]) && !Double.isNaN(scale[2])
						&& !Double.isNaN(scale[3])) {
					IJ.run("Properties...", "unit=" + prop.scaleConf + " pixel_width=" + scale[0] + " pixel_height="
							+ scale[1] + " origin=" + scale[2] + "," + scale[3]);
				} else {
					IJ.run("Properties...", "unit=pixel pixel_width=1 pixel_height=1 origin=0,0");
				}
				IJ.run("Set... ", "zoom=" + prop.zoom);
				IJ.run("Scale to Fit", "");
			} else {
				listImps[i] = null;
			}
		}

		for (int i = 0; i < listImps.length; i++) {
			if (listImps[i] != null) {
				listImps[i].setTitle(prefix + "_" + listChName[i] + ".tif");
				FileSaver fs = new FileSaver(listImps[i]);
				fs.saveAsTiff(dir + listImps[i].getTitle());
				fs.saveAsJpeg(dir + listImps[i].getTitle().replace(".tif", ".jpg"));
			}
		}

		return listImps;
	}

	public ImagePlus[] method2(ImagePlus[] listImps, String[] listI0CorrName, BL15A1Props prop) {
		ImagePlus[] listI0CorrImps = new ImagePlus[listI0CorrName.length];
		ImageCalculator ic = new ImageCalculator();
		double i0median = BL15A1Common.getMedian((FloatProcessor) listImps[0].getProcessor());

		for (int i = 0; i < listI0CorrImps.length; i++) {
			if (listImps[0] != null && listImps[i + 1] != null) {
				listI0CorrImps[i] = ic.run("Divide create 32-bit", listImps[i + 1], listImps[0]);
				listI0CorrImps[i].setTitle(prefix + "_" + listI0CorrName[i]);
				listI0CorrImps[i].show();
				if (i == 0) {
					IJ.run(listI0CorrImps[i], "Reciprocal", "");
					IJ.run(listI0CorrImps[i], "Log", "");
				} else {
					listI0CorrImps[i].getProcessor().multiply(i0median);
				}
				IJ.run(listI0CorrImps[i], "Set... ", "zoom=" + prop.zoom);
				IJ.run(listI0CorrImps[i], "Scale to Fit", "");
				IJ.run(listI0CorrImps[i], "Enhance Contrast...", "saturated=0.1");
				listI0CorrImps[i].updateAndDraw();
			} else {
				listI0CorrImps[i] = null;
			}
		}

		for (int i = 0; i < listI0CorrImps.length; i++) {
			if (listI0CorrImps[i] != null) {
				listI0CorrImps[i].setTitle(prefix + "_" + listI0CorrName[i] + ".tif");
				FileSaver fs = new FileSaver(listI0CorrImps[i]);
				fs.saveAsTiff(dir + listI0CorrImps[i].getTitle());
				fs.saveAsJpeg(dir + listI0CorrImps[i].getTitle().replace(".tif", ".jpg"));
			}
		}

		return listI0CorrImps;
	}
}
