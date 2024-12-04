package imagingXAFS.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
		try {
			String option = "-align";
			ImagePlus source = WindowManager.getImage("source.tif");
			ImagePlus target = WindowManager.getImage("target.tif");
			int wid = source.getWidth();
			int hei = source.getHeight();
			option += String.format(" -window %s %d %d %d %d", source.getTitle(), 0, 0, wid - 1, hei - 1);
			option += String.format(" -window %s %d %d %d %d", target.getTitle(), 0, 0, wid - 1, hei - 1);
			option += String.format(" -translation %d %d %d %d", wid / 2, hei / 2, wid / 2, hei / 2);
			option += " -hideOutput";
			Object turboreg = IJ.runPlugIn("TurboReg_", option);
			Method method = turboreg.getClass().getMethod("getSourcePoints", (Class[]) null);
			double[][] sourcePoints = (double[][]) method.invoke(turboreg);
			IJ.log(String.format("sourcePoint %f %f", sourcePoints[0][0], sourcePoints[0][1]));
			method = turboreg.getClass().getMethod("getTargetPoints", (Class[]) null);
			double[][] targetPoints = (double[][]) method.invoke(turboreg);
			IJ.log(String.format("targetPoint %f %f", targetPoints[0][0], targetPoints[0][1]));
			ImagePlus aligned = source.duplicate();
			aligned.setTitle("aligned");
			ImageProcessor ip = aligned.getProcessor();
			ip.setInterpolationMethod(ImageProcessor.BILINEAR);
			ip.translate(targetPoints[0][0] - sourcePoints[0][0], targetPoints[0][1] - sourcePoints[0][1]);
			source.show();
			target.show();
			aligned.show();
		} catch (Exception e) {
			IJ.log(e.getMessage());
		}
	}
}
