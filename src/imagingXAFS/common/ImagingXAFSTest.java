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
		Integer[] listImageId = ImagingXAFSCommon.getDataIds(false);
		String[] listImageTitle = ImagingXAFSCommon.getDataTitles(false);
		Integer[] listStackId = ImagingXAFSCommon.getDataIds(true);
		String[] listStackTitle = ImagingXAFSCommon.getDataTitles(true);
		if (listImageId.length < 1 || listStackId.length < 1) {
			IJ.error("Could not find data image(s).");
			return;
		}
		GenericDialog gd = new GenericDialog("map");
		gd.addChoice("Normalized imagestack ", listStackTitle, listStackTitle[0]);
		gd.addChoice("Dmut image ", listImageTitle, listImageTitle[0]);
		gd.addCheckbox("Weight with Dmut ", true);
		gd.addNumericField("Normalized absorption max. ", 1.5, 1);
		gd.addNumericField("Normalized absorption min. ", -0.1, 1);
		gd.addNumericField("Normalized absorption step ", 0.005, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus impNorm = WindowManager.getImage(listStackId[gd.getNextChoiceIndex()]);
		ImagePlus impDmut = WindowManager.getImage(listImageId[gd.getNextChoiceIndex()]);
		int wid = impNorm.getWidth();
		int hei = impNorm.getHeight();
		int slc = impNorm.getNSlices();
		if (impDmut.getWidth() != wid || impDmut.getHeight() != hei) {
			IJ.error("Invalid data size.");
			return;
		}
		boolean weight = gd.getNextBoolean();
		double max = gd.getNextNumber();
		double min = gd.getNextNumber();
		double step = gd.getNextNumber();
		if (max < min || step > (max - min)) {
			IJ.error("Invalid range.");
			return;
		}
		int bin = (int) Math.ceil((max - min) / step);
		ImagePlus impMatrix = NewImage.createFloatImage("Spectral map of " + impNorm.getTitle(), slc, bin, 1,
				NewImage.FILL_BLACK);
		float[] intensity;
		float[] matrix = (float[]) impMatrix.getProcessor().getPixels();
		float[] dmut = (float[]) impDmut.getProcessor().getPixels();
		int pos;
		if (weight) {
			for (int i = 0; i < slc; i++) {
				impNorm.setSlice(i + 1);
				intensity = (float[]) impNorm.getProcessor().getPixels();
				for (int j = 0; j < intensity.length; j++) {
					pos = (int) ((intensity[j] - min) / step);
					if (dmut[j] > 0 && pos > 0 && pos <= bin) {
						matrix[i + slc * (bin - pos)] += dmut[j];
					}
				}
			}
		} else {
			for (int i = 0; i < slc; i++) {
				impNorm.setSlice(i + 1);
				intensity = (float[]) impNorm.getProcessor().getPixels();
				for (int j = 0; j < intensity.length; j++) {
					pos = (int) ((intensity[j] - min) / step);
					if (dmut[j] > 0 && pos > 0 && pos <= bin) {
						matrix[i + slc * (bin - pos)] += 1;
					}
				}
			}

		}
		impMatrix.resetDisplayRange();
		impMatrix.show();
	}
}
