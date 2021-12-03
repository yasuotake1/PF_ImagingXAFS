package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class SVD_Menu implements PlugIn {

	public void run(String arg) {
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		Integer[] list2dId = ImagingXAFSCommon.getDataIds(false);
		String[] list2dTitle = ImagingXAFSCommon.getDataTitles(false);
		if (listStackId.length == 0 || list2dId.length == 0) {
			IJ.error("Could not find data image(s).");
			return;
		}

		GenericDialog gd = new GenericDialog("Singular value decomposition");
		gd.addChoice("Normalized stack: ", listStackTitle, listStackTitle[0]);
		gd.addChoice("Dmut image: ", list2dTitle, list2dTitle[0]);
		gd.addCheckbox("Clip at zero", true);
		gd.addCheckbox("Save automatically", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impNorm = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		ImagePlus impDmut = WindowManager.getImage(list2dId[gd.getNextChoiceIndex()]);
		boolean bClip = gd.getNextBoolean();
		boolean autoSave = gd.getNextBoolean();
		
		if (!SVD.setDataMatrix(impNorm))
			return;
			if (!SVD.setStandards(true))
				return;
		SVD.doSVD(true);
		SVD.showResults(impDmut, bClip, true, true, autoSave);
	}

}
