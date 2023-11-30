package imagingXAFS.common;

import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.*;
import imagingXAFS.bl15a1.BL15A1Common;
import imagingXAFS.bl15a1.BL15A1Props;

public class CorrelationPlotMask_Menu implements PlugIn {

	private static List<CorrelationPlotMask> cpms = new ArrayList<CorrelationPlotMask>();
	private static final String[] modes = { "Scatter plot", "Heat map" };

	public void run(String arg) {
		if (arg.equalsIgnoreCase("plot")) {
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
			String mode = gd.getNextRadioButton();
			int size = (int) gd.getNextNumber();
			CorrelationPlotMask cpm = new CorrelationPlotMask(imp1, imp2);
			if (cpm.isSet) {
				if (mode == modes[0]) {
					cpm.showScatterPlot(cpms.size());
				} else {
					if (size < 32 || size > 4096) {
						IJ.error("Invalid heat map size.");
						return;
					}
					cpm.showHeatMap(cpms.size(), size, size);
				}
				cpms.add(cpm);
			}

		} else if (arg.substring(0, 4).equalsIgnoreCase("mask")) {
			if (cpms.size() < 1) {
				IJ.error("There is no correlation plot.");
				return;
			}
			int idx = getIndexFromID(WindowManager.getCurrentImage().getID());
			if (arg.contains("zoom")) {
				BL15A1Props prop = BL15A1Common.ReadProps();
				cpms.get(idx).createMask(prop.zoom);
			} else {
				cpms.get(idx).createMask(-1.0);
			}
		}
	}

	private int getIndexFromID(int id) {
		if (cpms.size() > 0) {
			for (int i = 0; i < cpms.size(); i++) {
				if (id == cpms.get(i).impPlot.getID())
					return i;
			}
		}
		return 0;
	}
}
