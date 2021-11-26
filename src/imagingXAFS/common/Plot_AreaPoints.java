package imagingXAFS.common;

import java.awt.Point;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;

public class Plot_AreaPoints implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		int currentSliceNumber = imp.getSlice();
		double[] energies = ImagingXAFSCommon.getPropEnergies(imp);
		if (energies == null)
			return;

		RoiManager roiManager = RoiManager.getInstance();
		Roi roi;
		ImagingXAFSPlot.clear();

		if (roiManager == null || roiManager.getCount() == 0) {
			roi = imp.getRoi();
			if (roi == null) {
				IJ.error("No selection in current image.");
				return;
			}
			IJ.showStatus("Plotting spectrum at current ROI...");
			addPlots(imp, energies, 0);

		} else {
			IJ.showStatus("Plotting spectrum at ROIs stored in ROI Manager...");
			for (int idx = 0; idx < roiManager.getRoisAsArray().length; idx++) {
				roi = roiManager.getRoisAsArray()[idx];
				if (roi != null) {
					imp.setRoi(roi);
					addPlots(imp, energies, idx);
				}
			}
			roiManager.runCommand("Show All");
		}

		ImagingXAFSPlot.show(true);
		imp.setSlice(currentSliceNumber);
	}

	private void addPlots(ImagePlus imp, double[] energies, int idxOffset) {
		int slc = imp.getNSlices();

		if (imp.getRoi().isArea()) {
			double[] arrInt = new double[slc];
			for (int i = 0; i < slc; i++) {
				imp.setSlice(i + 1);
				ImageStatistics stats = imp.getStatistics(Measurements.MEAN + Measurements.CENTROID);
				arrInt[i] = stats.mean;
			}
			ImagingXAFSPlot.addData(energies, arrInt, "ROI " + String.valueOf(1 + idxOffset));
		} else {
			Point[] points = imp.getRoi().getContainedPoints();
			for (int j = 0; j < points.length; j++) {
				double[] arrInt = new double[slc];
				for (int i = 0; i < slc; i++) {
					imp.setSlice(i + 1);
					arrInt[i] = (double) imp.getProcessor().getPixelValue(points[j].x, points[j].y);
				}
				ImagingXAFSPlot.addData(energies, arrInt, "Point " + String.valueOf(1 + idxOffset + j));
			}
		}
	}

}
