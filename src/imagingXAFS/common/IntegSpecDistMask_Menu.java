package imagingXAFS.common;

import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class IntegSpecDistMask_Menu implements PlugIn {

	private static List<IntegSpecDistMask> isdms = new ArrayList<IntegSpecDistMask>();

	public void run(String arg) {
		if (arg.equalsIgnoreCase("plot")) {
			Integer[] listImageId = ImagingXAFSCommon.getDataIds(false);
			String[] listImageTitle = ImagingXAFSCommon.getDataTitles(false);
			Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
			String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
			if (listImageTitle.length < 1 || listStackId.length < 1) {
				IJ.error("Could not find data image(s).");
				return;
			}
			GenericDialog gd = new GenericDialog("map");
			gd.addChoice("Normalized imagestack", listStackTitle, listStackTitle[0]);
			gd.addChoice("Dmut image", listImageTitle, listImageTitle[0]);
			gd.addCheckbox("Weight with Dmut", true);
			gd.addMessage("Plot range");
			gd.addNumericField("Normalized absorption from", -0.1, 1);
			gd.addNumericField("to", 1.5, 1);
			gd.addNumericField("step", 0.005, 2);
			gd.addNumericField("Photon energy from", 7009.20, 2);
			gd.addNumericField("to", 7310.20, 2);
			gd.addNumericField("step", 0.1, 2);
			gd.showDialog();
			if (gd.wasCanceled())
				return;

			ImagePlus impNorm = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
			ImagePlus impDmut = WindowManager.getImage(listImageId[gd.getNextChoiceIndex()]);
			if (impDmut.getWidth() != impNorm.getWidth() || impDmut.getHeight() != impNorm.getHeight()) {
				IJ.error("Invalid data size.");
				return;
			}
			if (ImagingXAFSCommon.getPropEnergies(impNorm) == null) {
				IJ.error("Invalid imagestack.");
				return;
			}
			IntegSpecDistMask isdm = new IntegSpecDistMask(impNorm, impDmut);
			boolean weight = gd.getNextBoolean();
			double iMin = gd.getNextNumber();
			double iMax = gd.getNextNumber();
			double iStep = gd.getNextNumber();
			if (iMax < iMin || iStep > (iMax - iMin)) {
				IJ.error("Invalid range.");
				return;
			}
			double eMin = gd.getNextNumber();
			double eMax = gd.getNextNumber();
			double eStep = gd.getNextNumber();
			if (eMax < eMin || eStep > (eMax - eMin)) {
				IJ.error("Invalid range.");
				return;
			}
			isdm.showPlot(weight, iMin, iMax, iStep, eMin, eMax, eStep);
			isdms.add(isdm);
		} else if (arg.substring(0, 4).equalsIgnoreCase("mask")) {
			if (isdms.size() < 1) {
				IJ.error("There is no integrated spectral distribution plot.");
				return;
			}
			int idx = getIndexFromID(WindowManager.getCurrentImage().getID());
			if (idx < 0) {
				IJ.error("Current image is not a correlation plot.");
				return;
			}
			isdms.get(idx).createMask();
		}
	}

	private int getIndexFromID(int id) {
		if (isdms.size() > 0) {
			for (int i = 0; i < isdms.size(); i++) {
				if (id == isdms.get(i).impPlot.getID())
					return i;
			}
		}
		return -1;
	}
}
