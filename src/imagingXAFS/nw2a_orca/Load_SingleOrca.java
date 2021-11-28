package imagingXAFS.nw2a_orca;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.ImageCalculator;

public class Load_SingleOrca implements PlugIn {
	
	public static ImagePlus impTgt;
	
	public void run(String arg) {
		OrcaProps prop = OrcaCommon.ReadProps();

		GenericDialog gd = new GenericDialog("Load single ORCA-Flash image");
		gd.addFileField("Image file: ", "");
		gd.addFileField("Reference image file (if exists): ", "");
		gd.addChoice("Binning: ", OrcaCommon.strBinning, OrcaCommon.strBinning[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImgPath = gd.getNextString();
		Path pathImg = Paths.get(strImgPath);
		String strRefPath = gd.getNextString();
		Path pathRef = Paths.get(strRefPath);
		if (strImgPath.isEmpty() || !Files.exists(pathImg))
			return;
		String strBinning = gd.getNextChoice();

		ImagePlus impImg = OrcaCommon.LoadOrca(pathImg, prop);
		if (impImg == null)
			return;
		ImagePlus impRef;
		impTgt = new ImagePlus();
		if (!strRefPath.isEmpty() && Files.exists(pathRef)) {
			impRef = OrcaCommon.LoadOrca(pathRef, prop);
			if (impRef == null)
				return;
			impTgt = ImageCalculator.run(impRef, impImg, "divide create 32-bit");
			impTgt.setTitle(impImg.getTitle().replace(".img", ""));
			impTgt.getProcessor().log();
		} else {
			impTgt = impImg;
		}
		int intBin = 1;
		if (strBinning != OrcaCommon.strBinning[0]) {
			try {
				intBin = Integer.parseInt(strBinning);
				impTgt = impTgt.resize(prop.width / intBin, prop.height / intBin, "average");
			} catch (NumberFormatException e) {
			}
		}
		impTgt.setFileInfo(impImg.getOriginalFileInfo());
		OrcaCommon.setCalibration(impTgt, prop, intBin);
		impTgt.show();
		IJ.run(impTgt, "Enhance Contrast...", "saturated=0.1");
		impTgt.updateAndDraw();
	}
	
}
