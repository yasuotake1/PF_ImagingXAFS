package imagingXAFS.common;

import ij.*;
import ij.gui.*;
import ij.plugin.*;

public class CorrelationPlot_Menu implements PlugIn {

	static final String[] modes = { "Scatter plot", "Heat map" };

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
		gd.addRadioButtonGroup("", modes, 1, 2, modes[0]);
		gd.addNumericField("Heat map size", 512, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		ImagePlus imp1 = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		ImagePlus imp2 = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		if (gd.getNextRadioButton() == modes[0]) {
			CorrelationPlotMask.showScatterPlot(0, imp1, imp2);
		} else {
			int size = (int) gd.getNextNumber();
			if (size < 32 || size > 4096) {
				IJ.error("Invalid heat map size.");
				return;
			}
			CorrelationPlotMask.showHeatMap(0, imp1, imp2, size, size);
		}
	}

}
