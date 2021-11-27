package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		imp.hide();
		RankFilters rf = new RankFilters();
		int slc = imp.getNSlices();
		if (slc > 1) {
			for (int i = 1; i <= slc; i++) {
				IJ.showStatus("Applying filter " + String.valueOf(i) + "/" + String.valueOf(slc));
				imp.setSlice(i);
				rf.rank(imp.getProcessor(), 1.0, RankFilters.MEDIAN);
			}
		} else {
			rf.rank(imp.getProcessor(), 1.0, RankFilters.MEDIAN);
		}
		imp.show();
	}

}
