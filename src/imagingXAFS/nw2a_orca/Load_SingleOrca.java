package imagingXAFS.nw2a_orca;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import ij.plugin.ImageCalculator;
import imagingXAFS.common.ImagingXAFSCommon;

public class Load_SingleOrca implements PlugIn {

	public static ImagePlus impTgt;
	private static final String msg = "Note:\n"
			+ "Select an image file for Reference file field to calculate absorbance, or enter an integer value to use constant I0."
			+ "\nSelect an image file for Dark file field to subtract dark image, or enter an integer value to subtract constant."
			+ "\nMultiple dark images (*_dk[0-9].img) are searched for automatically.";

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load single ORCA-Flash image");
		gd.addFileField("Transmission image", OrcaCommon.strImg);
		gd.addFileField("Reference image or constant", OrcaCommon.strRef);
		gd.addFileField("Dark image or constant", OrcaCommon.strDark);
		gd.addCheckbox("Avoid zero in raw images", OrcaCommon.avoidZero);
		gd.addChoice("Binning", OrcaCommon.arrBinning, OrcaCommon.strBinning);
		gd.addMessage(msg);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg = gd.getNextString();
		String strRef = gd.getNextString();
		String strDark = gd.getNextString();
		if (!ImagingXAFSCommon.isExistingPath(strImg))
			return;

		OrcaCommon.strImg = strImg;
		OrcaCommon.strRef = strRef;
		OrcaCommon.setDark(strDark);
		OrcaCommon.avoidZero = gd.getNextBoolean();
		OrcaCommon.strBinning = gd.getNextChoice();

		OrcaProps prop = OrcaCommon.readProps();
		ImagePlus impImg = OrcaCommon.loadOrca(strImg, prop, true);
		if (impImg == null)
			return;
		ImagePlus impRef;
		if (ImagingXAFSCommon.isExistingPath(strRef)) {
			impRef = OrcaCommon.loadOrca(strRef, prop, true);
			if (impRef == null)
				return;
			impTgt = ImageCalculator.run(impRef, impImg, "divide create 32-bit");
			impTgt.setTitle(impImg.getTitle().replace(".img", ""));
			impTgt.getProcessor().log();
		} else {
			impTgt = impImg;
			if (OrcaCommon.isInteger(strRef)) {
				double constRef = Integer.parseInt(strRef);
				ImageConverter iConv = new ImageConverter(impTgt);
				iConv.convertToGray32();
				float[] arrFloat = (float[]) impTgt.getProcessor().getPixels();
				for (int k = 0; k < arrFloat.length; k++) {
					arrFloat[k] = (float) Math.log(constRef / arrFloat[k]);
				}
			}
		}
		int intBin = OrcaCommon.getIntBinning();
		if (intBin > 1) {
			impTgt = impTgt.resize(prop.width / intBin, prop.height / intBin, "average");
		}
		impTgt.setFileInfo(impImg.getOriginalFileInfo());
		OrcaCommon.setCalibration(impTgt, prop, intBin);
		OrcaCommon.writeProps(prop);
		impTgt.show();
		IJ.run(impTgt, "Enhance Contrast...", "saturated=0.1");
		impTgt.updateAndDraw();
	}

}
