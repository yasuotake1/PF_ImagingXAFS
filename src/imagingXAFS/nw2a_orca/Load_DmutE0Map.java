package imagingXAFS.nw2a_orca;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import imagingXAFS.common.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.plugin.PlugIn;

public class Load_DmutE0Map implements PlugIn {

	public static int dataLength = 10;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Load Dmut and E0 map text");
		gd.addFileField("Analyzed map data file: ", "");
		gd.addMessage("E0 display range (can be modified afterwards with 'Brightness/Contrast')");
		gd.addNumericField("Minimum: ", ImagingXAFSCommon.e0Min);
		gd.addNumericField("Maximum: ", ImagingXAFSCommon.e0Max);
		gd.addCheckbox("Sequence", false);
		gd.addCheckbox("Grid stitching", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String pathSrc = gd.getNextString();
		String nameSrc;
		String pathFirst = pathSrc;
		double e0Min = gd.getNextNumber();
		double e0Max = gd.getNextNumber();
		if (Double.isNaN(e0Min) || Double.isNaN(e0Max) || e0Min > e0Max) {
			IJ.error("Invalid E0 minimum and/or maximum.");
			return;
		}
		ImagingXAFSCommon.e0Min = e0Min;
		ImagingXAFSCommon.e0Max = e0Max;
		String dir = Paths.get(pathSrc).getParent().toString() + File.separator;
		boolean isSeq = gd.getNextBoolean();
		boolean doStitch = gd.getNextBoolean() & isSeq;
		int width;
		int height;
		String[] lines;
		ImagePlus impDmut;
		ImagePlus impE0;

		int idx = isSeq
				? Integer.parseInt(pathSrc.substring(pathSrc.lastIndexOf("_map") - 3, pathSrc.lastIndexOf("_map")))
				: 0;
		int rep = 0;

		do {
			nameSrc = pathSrc.substring(dir.length());
			IJ.showStatus("Loading " + nameSrc + "...");
			width = 0;
			height = 0;

			try {
				lines = Files.lines(Paths.get(pathSrc)).toArray(String[]::new);
			} catch (IOException e) {
				IJ.error("Failed to load an analyzed map data.");
				return;
			}

			int line = 0;
			int lineDataStart = 0;
			do {
				if (line == lines.length) {
					IJ.error("Invalid Dmut and E0 map text.");
					return;
				}
				if (lines[line].indexOf("Map Data") > 0) {
					width = Integer.parseInt(
							lines[line].substring(lines[line].lastIndexOf(":") + 1, lines[line].lastIndexOf(",")));
					height = Integer.parseInt(lines[line].substring(lines[line].lastIndexOf(",") + 1));
				}
				if (!lines[line].startsWith("#") && lineDataStart == 0)
					lineDataStart = line;
				line++;
			} while (width == 0 || height == 0 || lineDataStart == 0);

			impDmut = NewImage.createFloatImage(nameSrc.replace("_map.txt", "_Dmut.tif"), width, height, 1,
					NewImage.FILL_BLACK);
			float[] pixels = (float[]) impDmut.getProcessor().getPixels();
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					pixels[width * i + j] = Float.parseFloat(
							lines[lineDataStart + i].substring(j * dataLength, (j + 1) * dataLength).trim());
				}
			}
			IJ.saveAsTiff(impDmut, dir + impDmut.getTitle());

			impE0 = NewImage.createFloatImage(nameSrc.replace("_map.txt", "_E0.tif"), width, height, 1,
					NewImage.FILL_BLACK);
			pixels = (float[]) impE0.getProcessor().getPixels();
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					pixels[width * i + j] = Float.parseFloat(lines[lineDataStart + height + 1 + i]
							.substring(j * dataLength, (j + 1) * dataLength).trim());
				}
			}
			IJ.saveAsTiff(impE0, dir + impE0.getTitle());

			if (!isSeq) {
				impDmut.show();
				IJ.run("Enhance Contrast", "saturated=0.1");
				impE0.setDisplayRange(e0Min, e0Max);
				impE0.show();
				IJ.run("Jet");
			}

			idx++;
			rep++;
			pathSrc = pathSrc.substring(0, pathSrc.lastIndexOf("_map") - 3) + String.format("%03d", idx)
					+ pathSrc.substring(pathSrc.lastIndexOf("_map"));
		} while (isSeq && ImagingXAFSCommon.isExistingPath(pathSrc));

		if (isSeq)
			IJ.showStatus("Finished " + rep + " files.");

		if (doStitch) {
			Stitching sti = new Stitching();
			if (!sti.showDialog(rep))
				return;

			if (!sti.register(pathFirst.replace("_map.txt", "_Dmut.tif"))) {
				IJ.error("Failed to calculate stitching configuration.");
				return;
			}

			ImagePlus impCurrent = sti.doStitching("_Dmut.tif");
			if (impCurrent == null)
				return;
			IJ.run("Enhance Contrast", "saturated=0.35");
			IJ.saveAsTiff(impCurrent, dir + impCurrent.getTitle());

			impCurrent = sti.doStitching("_E0.tif");
			if (impCurrent == null)
				return;
			impCurrent.setDisplayRange(e0Min, e0Max);
			IJ.run("Jet");
			IJ.saveAsTiff(impCurrent, dir + impCurrent.getTitle());
		}

		IJ.run("Brightness/Contrast...");

	}

}
