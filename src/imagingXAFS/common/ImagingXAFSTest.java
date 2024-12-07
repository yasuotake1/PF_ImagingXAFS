package imagingXAFS.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Method;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
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
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import imagingXAFS.nw2a_orca.Load_OrcaStack;
import imagingXAFS.nw2a_orca.Load_SingleOrca;
import imagingXAFS.nw2a_orca.OrcaCommon;
import imagingXAFS.nw2a_orca.OrcaProps;
import imagingXAFS.nw2a_ultra.*;

public class ImagingXAFSTest implements PlugIn {

	public void run(String arg) {
		ImagePlus imp0 = WindowManager.getCurrentImage();
		int wid = imp0.getWidth();
		int hei = imp0.getHeight();
		int slc = imp0.getNSlices();
		ImagePlus imp1 = NewImage.createFloatImage("copy", wid, hei, slc, NewImage.FILL_BLACK);
		float[] pixels0, pixels1, voxels0, voxels1;
		Instant t0 = Instant.now();
		for (int i = 0; i < slc; i++) {
			imp0.setSlice(i + 1);
			pixels0 = (float[]) imp0.getProcessor().getPixels();
			imp1.setSlice(i + 1);
			pixels1 = (float[]) imp1.getProcessor().getPixels();
			for (int j = 0; j < pixels0.length; j++) {
				pixels1[j] = pixels0[j];
			}
		}
		Instant t1 = Instant.now();
		voxels0 = imp0.getStack().getVoxels(0, 0, 0, wid, hei, slc, null);
		voxels1 = imp1.getStack().getVoxels(0, 0, 0, wid, hei, slc, null);
		for (int i = 0; i < voxels0.length; i++) {
			voxels0[i] += voxels1[i];
		}
		Instant t2 = Instant.now();
		IJ.log(Duration.between(t0, t1).toString());
		IJ.log(Duration.between(t1, t2).toString());
	}
}
