package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
//		FileInfo fi = WindowManager.getCurrentImage().getOriginalFileInfo();
//		IJ.log(fi.directory);
//		IJ.log(fi.fileName);
		double[] arr = { 1.1, 2.2, 1.1, 3.3, 1.1, 4.4, 1.1, 5.5, 1.1 };
		for (int i = 0; i < arr.length; i++) {
			IJ.log(String.format("%.1f", arr[i]));
		}
		double[] arr1 = new double[arr.length];
		double val = arr[0] + arr[1] + arr[2];
		arr1[0] = val / 3;
		val += arr[3];
		arr1[1] = val / 4;
		val += arr[4];
		arr1[2] = val / 5;
		for (int i = 3; i < arr1.length - 2; i++) {
			val = val - arr[i - 3] + arr[i + 2];
			arr1[i] = val / 5;
		}
		val -= arr[arr.length - 5];
		arr1[arr1.length - 2] = val / 4;
		val -= arr[arr.length - 4];
		arr1[arr1.length - 1] = val / 3;
		for (int i = 0; i < arr1.length; i++) {
			IJ.log(String.format("%.1f", arr1[i]));
		}
	}

}
