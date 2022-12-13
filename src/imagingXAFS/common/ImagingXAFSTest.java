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
import imagingXAFS.nw2a_orca.Load_SingleOrca;
import imagingXAFS.nw2a_ultra.*;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		short[] pixels = (short[]) WindowManager.getCurrentImage().getProcessor().getPixels();
		IJ.log(String.valueOf(pixels[0]));
		IJ.log(String.valueOf(pixels[0]<0?65536+pixels[0]:pixels[0]));
	}

}
