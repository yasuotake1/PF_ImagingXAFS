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
		Pattern p = Pattern.compile("rep[0-9]+_");
		Matcher m = p.matcher("rep01_00000_hogehoge");
		if (m.find())
			IJ.log(m.group());
	}
}
