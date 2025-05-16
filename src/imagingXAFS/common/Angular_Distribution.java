package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.PlugIn;

public class Angular_Distribution implements PlugIn{
	public void run(String arg) {
		Integer[] listId = ImagingXAFSCommon.getDataIds(false);
		String[] listTitle = ImagingXAFSCommon.getDataTitles(false);
		if (listId.length < 2) {
			IJ.error("Could not find two or more data images.");
			return;
		}

		GenericDialog gd = new GenericDialog("Plot angular distribution");
		gd.addChoice("Image Y", listTitle, listTitle[0]);
		gd.addChoice("Image X", listTitle, listTitle[1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impY = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		ImagePlus impX = WindowManager.getImage(listId[gd.getNextChoiceIndex()]);
		if (impY.getWidth() != impX.getWidth() || impY.getHeight() != impX.getHeight()) {
			IJ.error("Invalid source images.");
			return;
		}
		ImagePlus impResult = NewImage.createFloatImage("AngularDistribution", impY.getWidth(), impY.getHeight(), 1,
				NewImage.FILL_BLACK);
		float[] arrY = (float[]) impY.getProcessor().getPixels();
		float[] arrX = (float[]) impX.getProcessor().getPixels();
		float[] arrResult = (float[]) impResult.getProcessor().getPixels();
		for (int i = 0; i < arrY.length; i++) {
			IJ.showProgress(i, arrY.length);
			arrResult[i] = (float) (Math.atan2(arrY[i], arrX[i]) / Math.PI * 180);
		}
		IJ.run(impResult, "Spectrum", "");
		impResult.resetDisplayRange();
		impResult.show();
	}

}
