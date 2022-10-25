package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Clip_Values implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Clip at min/max values...");
		gd.addNumericField("Maximum", 5, 2);
		gd.addNumericField("Minimum", 0, 2);
		gd.addNumericField("Set outliers to", 0, 2);
		gd.addCheckbox("Process entire stack", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		float max = (float) gd.getNextNumber();
		float min = (float) gd.getNextNumber();
		float val = (float) gd.getNextNumber();
		boolean stack = gd.getNextBoolean();
		ImagePlus imp = WindowManager.getCurrentImage();

		ClipValues(imp, max, min, val, stack);
	}

	public static void ClipValues(ImagePlus imp, float max, float min, float val, boolean stack) {
		if (imp.getNSlices() > 1 && stack) {
			int slc = imp.getNSlices();
			for (int i = 1; i <= slc; i++) {
				IJ.showProgress(i, slc);
				imp.setSliceWithoutUpdate(i);
				ClipValuesSlice((float[]) imp.getProcessor().getPixels(), max, min, val);
			}
		} else {
			ClipValuesSlice((float[]) imp.getProcessor().getPixels(), max, min, val);
		}
		imp.resetDisplayRange();
	}

	private static void ClipValuesSlice(float[] pixels, float max, float min, float val) {
		int len = pixels.length;
		for (int i = 0; i < len; i++) {
			pixels[i] = pixels[i] < min ? val : (pixels[i] > max ? val : pixels[i]);
		}
	}
}
