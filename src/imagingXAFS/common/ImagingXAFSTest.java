package imagingXAFS.common;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
import imagingXAFS.nw2a_ultra.*;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Dialog");
		String path = od.getPath();
		if (path == null)
			return;
		UltraScanInfo usi = null;
		try {
			usi = new UltraScanInfo(path);
		} catch (NumberFormatException | IndexOutOfBoundsException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File f = new File(usi.directory);
		List<String> ls = Arrays.asList(f.list());
		ls.forEach(item -> IJ.log(item));
	}

}
