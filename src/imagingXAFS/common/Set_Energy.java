package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Set_Energy implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		OpenDialog od = new OpenDialog("Select 9809 format file.");
		if (od.getPath() == null)
			return;
		double[] energy = ImagingXAFSCommon.readEnergies(od.getPath());
		if (imp.getNSlices() == energy.length) {
			ImagingXAFSCommon.setPropEnergies(imp, energy);
		} else {
			IJ.error("Energy points does not match the number of slices.");
			return;
		}
	}

}
