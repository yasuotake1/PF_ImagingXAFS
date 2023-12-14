package imagingXAFS.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.CurveFitter;
import ij.measure.Measurements;
import ij.plugin.ContrastEnhancer;
import ij.plugin.PlugIn;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import imagingXAFS.nw2a_orca.Load_OrcaStack;
import imagingXAFS.nw2a_orca.Load_SingleOrca;
import imagingXAFS.nw2a_orca.OrcaCommon;
import imagingXAFS.nw2a_orca.OrcaProps;
import imagingXAFS.nw2a_ultra.*;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("test");
		gd.addFileField("Path", "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		String path = gd.getNextString();
		try {
			byte[] buffer = OrcaCommon.readBytes(path, 0, 64);
			int intFileType = (buffer[12] & 0xff) + ((buffer[13] & 0xff) << 8);
			IJ.log(String.valueOf(intFileType));
			switch (intFileType) {
			case 0:
				IJ.log("8bit");
				break;
			case 2:
				IJ.log("16bit");
				break;
			case 3:
				IJ.log("32bit");
				break;
			}
		} catch (Exception e) {

		}

	}
}
