package imagingXAFS.bl15a1;

import ij.*;
import ij.plugin.*;
import imagingXAFS.common.ImagingXAFSCommon;

import java.io.*;
import ij.io.*;

public class Save4TXW_ implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		String fName = imp.getTitle();
		double[] energies = ImagingXAFSCommon.getPropEnergies(imp);
		if (energies == null)
			return;
		FileInfo fi = imp.getOriginalFileInfo();
		if (fi == null)
			return;

		ImageStack stack = imp.getStack();
		String dirTXW = fi.directory + "TXW" + File.separator;
		File target = new File(dirTXW);
		if (!target.exists()) {
			if (!target.mkdir())
				IJ.error("Unable to create directory.");
		}
		String prefix = fName.replace(".tif", "") + "_";
		String suffix = "_eV.tif";
		ImagePlus imp2;
		FileSaver fs;
		try {
			for (int i = 0; i < energies.length; i++) {
				imp2 = new ImagePlus(stack.getSliceLabel(i + 1), stack.getProcessor(i + 1));
				fs = new FileSaver(imp2);
				fs.saveAsTiff(dirTXW + prefix + String.format("%08.2f", energies[i]) + suffix);
				imp2.close();
			}
			IJ.showMessage(fName + "\nwas saved for TXM XANES Wizard in\n" + dirTXW);
		} catch (Exception e) {
			IJ.error("Failed to save files.");
		}
	}
}
