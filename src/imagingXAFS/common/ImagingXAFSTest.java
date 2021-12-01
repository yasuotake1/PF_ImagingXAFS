package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.Measurements;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageStatistics;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		ImageStack stackSrc = imp.crop("stack").getStack();
		int slc = imp.getNSlices();
		GaussianBlur gb = new GaussianBlur();
		ContrastEnhancer ce = new ContrastEnhancer();
		ce.setNormalize(true);
		for (int i = 1; i <= slc; i++) {
			gb.blurGaussian(stackSrc.getProcessor(i), 2.0);
			ImageStatistics stats = ImageStatistics.getStatistics(stackSrc.getProcessor(i), Measurements.MIN_MAX, null);
			ce.stretchHistogram(stackSrc.getProcessor(i), 0.1, stats);
		}
		ImagePlus impResult=new ImagePlus("Result",stackSrc);
		impResult.show();
	}

}
