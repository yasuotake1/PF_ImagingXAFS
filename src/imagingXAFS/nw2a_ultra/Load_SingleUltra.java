package imagingXAFS.nw2a_ultra;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import imagingXAFS.common.ImagingXAFSCommon;

public class Load_SingleUltra implements PlugIn {

	public static ImagePlus impTgt;

	public void run(String arg) {

		GenericDialog gd = new GenericDialog("Load single UltraXRM image");
		gd.addFileField("Image file", "");
		gd.addFileField("Reference image file (if exists)", "");
		gd.addChoice("Binning", UltraCommon.LIST_BINNING, UltraCommon.LIST_BINNING[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImgPath = gd.getNextString();
		String strRefPath = gd.getNextString();
		if (ImagingXAFSCommon.isExistingPath(strImgPath))
			return;
		String strBinning = gd.getNextChoice();

		ImagePlus impImg = XRM_Reader.Load(strImgPath, false);
		if (impImg == null)
			return;
		ImagePlus impRef;
		if (ImagingXAFSCommon.isExistingPath(strRefPath)) {
			impRef = XRM_Reader.Load(strRefPath, true);
			if (impRef == null)
				return;
			impTgt = ImageCalculator.run(impRef, impImg, "divide create 32-bit");
			impTgt.setTitle(impImg.getTitle().replace(".xrm", ""));
			impTgt.getProcessor().log();
		} else {
			impTgt = impImg;
		}
		int intBin = 1;
		if (strBinning != UltraCommon.LIST_BINNING[0]) {
			try {
				intBin = Integer.parseInt(strBinning);
				impTgt = impTgt.resize(impTgt.getWidth() / intBin, impTgt.getHeight() / intBin, "average");
			} catch (NumberFormatException e) {
			}
		}
		impTgt.setFileInfo(impImg.getOriginalFileInfo());
		Calibration calib = impImg.getCalibration();
		calib.pixelHeight = calib.pixelWidth = calib.pixelWidth * intBin;
		impTgt.setCalibration(calib);
		impTgt.show();
		IJ.run(impTgt, "Enhance Contrast...", "saturated=0.1");
		impTgt.updateAndDraw();
	}

}
