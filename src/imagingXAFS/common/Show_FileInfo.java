package imagingXAFS.common;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

public class Show_FileInfo implements PlugIn {

	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		IJ.log("<Method getOriginalFileInfo() of " + imp.getTitle() + ">");
		String[] arr = imp.getOriginalFileInfo().toString().split(",");
		for (int i = 0; i < arr.length; i++) {
			IJ.log(arr[i].trim());
		}
	}

}
