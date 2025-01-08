package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class SVD_Menu implements PlugIn {
	static boolean bClip = true;
	static boolean autoSave = true;

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
		gd.addCheckbox("Clip at zero", bClip);
		gd.addCheckbox("Save automatically", autoSave);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impNorm = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		ImagePlus impDmut = WindowManager.getImage(list2dId[gd.getNextChoiceIndex()]);
		bClip = gd.getNextBoolean();
		autoSave = gd.getNextBoolean();

		if (!SVD.setDataMatrix(impNorm))
			return;
		if (!SVD.setStandards(true))
			return;
		SVD.performSVD(true);
		SVD.showResults(impDmut, bClip, true, true, autoSave);
	}

}
