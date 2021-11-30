package imagingXAFS.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.RankFilters;
import imagingXAFS.nw2a_ultra.UltraScanInfo;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("ScanInfo");
		String path = od.getPath();
		if (path == null)
			return;
		try {
			UltraScanInfo si = new UltraScanInfo(path);
			for (int i = 0; i < si.imageFiles.length; i++) {
				IJ.log(si.imageFiles[i].replaceAll("_[0-9]{3}of[0-9]{3}", ""));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
