package imagingXAFS.common;

import ij.*;
import ij.gui.*;
import ij.plugin.*;

public class CorrelationPlot_Menu implements PlugIn {

	public void run(String arg) {
		Integer[] listId = ImagingXAFSCommon.getDataIds(false);
		String[] listTitle = ImagingXAFSCommon.getDataTitles(false);
		if (listId.length < 2) {
			IJ.error("This method requires at least two images open.");
			return;
		}

		GenericDialog gd = new GenericDialog("Correlation plot");
		gd.addChoice("Image 1: ", listTitle, listTitle[0]);
		gd.addChoice("Image 2: ", listTitle, listTitle[1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		ImagePlus imp1 = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		ImagePlus imp2 = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		CorrelationPlotMask.showPlot(0, imp1, imp2);
	}

}
