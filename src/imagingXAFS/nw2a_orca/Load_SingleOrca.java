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
		gd.addFileField("Image file", "");
		gd.addFileField("Reference image file (if exists)", "");
		gd.addNumericField("Constant dark offset", OrcaCommon.ofsInt);
		gd.addChoice("Binning", OrcaCommon.arrBinning, OrcaCommon.strBinning);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImgPath = gd.getNextString();
		Path pathImg = Paths.get(strImgPath);
		String strRefPath = gd.getNextString();
		Path pathRef = Paths.get(strRefPath);
		if (strImgPath.isEmpty() || !Files.exists(pathImg))
			return;
		int _ofsInt = (int) gd.getNextNumber();
		String _strBinning = gd.getNextChoice();

		ImagePlus impImg = OrcaCommon.LoadOrca(pathImg, prop);
		if (impImg == null)
			return;
		if (_ofsInt != 0)
			impImg.getProcessor().add(-_ofsInt);
		ImagePlus impRef;
		if (!strRefPath.isEmpty() && Files.exists(pathRef)) {
			impRef = OrcaCommon.LoadOrca(pathRef, prop);
			if (impRef == null)
				return;
			if (_ofsInt != 0)
				impRef.getProcessor().add(-_ofsInt);
			impTgt = ImageCalculator.run(impRef, impImg, "divide create 32-bit");
			impTgt.setTitle(impImg.getTitle().replace(".img", ""));
			impTgt.getProcessor().log();
		} else {
			impTgt = impImg;
		}
		int intBin = 1;
		if (_strBinning != OrcaCommon.arrBinning[0]) {
			try {
				intBin = Integer.parseInt(_strBinning);
				impTgt = impTgt.resize(prop.width / intBin, prop.height / intBin, "average");
			} catch (NumberFormatException e) {
			}
		}
		impTgt.setFileInfo(impImg.getOriginalFileInfo());
		OrcaCommon.setCalibration(impTgt, prop, intBin);
		OrcaCommon.WriteProps(prop);
		OrcaCommon.ofsInt = _ofsInt;
		OrcaCommon.strBinning = _strBinning;
		impTgt.show();
		IJ.run(impTgt, "Enhance Contrast...", "saturated=0.1");
		impTgt.updateAndDraw();
	}

}
