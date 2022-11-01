package imagingXAFS.nw2a_orca;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import imagingXAFS.common.*;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.plugin.GaussianBlur3D;
import ij.plugin.PlugIn;

public class BatchJob_Orca implements PlugIn {

	private String strImg9809Path;
	private String strRef9809Path;

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Batch job: NW2A ImagingXAFS");
		gd.addMessage("Source:");
		gd.addFileField("First image data file (9809 format)", "");
		gd.addFileField("Reference data file (9809 format)", "");
		gd.addNumericField("Constant dark offset", 100);
		gd.addNumericField("Energy offset", 0, 2);
		gd.addChoice("Binning", OrcaCommon.strBinning, OrcaCommon.strBinning[0]);
		gd.addMessage("Preprocess:");
		gd.addCheckbox("Apply 3D Gaussian blur", false);
		gd.addMessage("Normalization:");
		gd.addNumericField("Pre-edge from", ImagingXAFSCommon.normalizationParam[0], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[1], 2, 8, "eV");
		gd.addNumericField("Post-edge from", ImagingXAFSCommon.normalizationParam[2], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[3], 2, 8, "eV");
		gd.addNumericField("Filter threshold", 2.0, 1);
		gd.addNumericField("Normalized absorbance at E0", 0.5, 2);
		gd.addNumericField("E0 plot range minimum", ImagingXAFSCommon.e0Min, 2, 8, "eV");
		gd.addNumericField("maximum", ImagingXAFSCommon.e0Max, 2, 8, "eV");
		gd.addCheckbox("Create statistics images", false);
		gd.addMessage("Singular value decomposition:");
		gd.addCheckbox("Do SVD", true);
		gd.addCheckbox("Clip at zero", true);
		gd.addMessage("Postprocess:");
		gd.addCheckbox("Copy files for stitching", true);
		gd.addCheckbox("Perform grid stitching", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		strImg9809Path = gd.getNextString();
		strRef9809Path = gd.getNextString();
		if (strImg9809Path.isEmpty() || !Files.exists(getPathImg9809()) || strRef9809Path.isEmpty()
				|| !Files.exists(getPathRef9809()))
			return;
		double constantOffset = gd.getNextNumber();
		double energyOffset = gd.getNextNumber();
		String strBinning = gd.getNextChoice();
		boolean filter = gd.getNextBoolean();
		double[] energy = ImagingXAFSCommon.readEnergies(getPathImg9809());
		double preStart = gd.getNextNumber();
		double preEnd = gd.getNextNumber();
		double postStart = gd.getNextNumber();
		double postEnd = gd.getNextNumber();
		ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
		float threshold = (float) gd.getNextNumber();
		float e0Jump = (float) gd.getNextNumber();
		if (e0Jump < 0 || e0Jump > 1) {
			IJ.error("Normalized absorbance at E0 must be within 0 and 1.");
			return;
		}
		double e0Min = gd.getNextNumber();
		double e0Max = gd.getNextNumber();
		if (Double.isNaN(e0Min) || Double.isNaN(e0Max) || e0Min > e0Max) {
			IJ.error("Invalid E0 minimum and/or maximum.");
			return;
		}
		ImagingXAFSCommon.e0Jump = e0Jump;
		ImagingXAFSCommon.e0Min = e0Min;
		ImagingXAFSCommon.e0Max = e0Max;
		boolean statsImages = gd.getNextBoolean();
		boolean doSVD = gd.getNextBoolean();
		boolean clip = gd.getNextBoolean();
		boolean copy = gd.getNextBoolean();
		boolean stitch = gd.getNextBoolean();

		String strImgPath = strImg9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
		String strRefPath = strRef9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
		String strOption = "image=" + strImgPath + " reference=" + strRefPath + " binning=" + strBinning;
		IJ.run("Load single ORCA-Flash image", strOption);
		ImagePlus impRoi = Load_SingleOrca.impTgt;
		IJ.setTool("rect");
		new WaitForUserDialog("Select rectangle region to analyze, then click OK.\nSelect none not to crop.").show();
		Roi roi = impRoi.getRoi();
		if (roi != null && roi.getType() != Roi.RECTANGLE) {
			IJ.error("Failed to specify region to analyze.");
			impRoi.close();
			return;
		}
		int roiX = roi == null ? 0 : roi.getBounds().x;
		int roiY = roi == null ? 0 : roi.getBounds().y;
		int roiWidth = roi == null ? impRoi.getWidth() : roi.getBounds().width;
		int roiHeight = roi == null ? impRoi.getHeight() : roi.getBounds().height;
		impRoi.close();
		double xsigma = 1.0, ysigma = 1.0, zsigma = 7.0;
		if (filter) {
			gd = new GenericDialog("3D Gaussian Blur");
			gd.addNumericField("X sigma:", xsigma, 1);
			gd.addNumericField("Y sigma:", ysigma, 1);
			gd.addNumericField("Z sigma:", zsigma, 1);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			xsigma = gd.getNextNumber();
			ysigma = gd.getNextNumber();
			zsigma = gd.getNextNumber();
		}
		if (doSVD && !SVD.setStandards(false, energy))
			return;
		IJ.run("Close All");
		System.gc();

		ImagePlus impMut, impCrop, impNorm, impDmut, impLast;
		String baseName;
		Instant startTime = Instant.now();
		String first9809Path = strImg9809Path;
		int rep = getRepetition();
		Path dirStitch = Paths.get(getImg9809Root(), "stitching");
		ArrayList<String> sufList = new ArrayList<String>();
		Stitching sti = new Stitching();

		if (copy) {
			if (!Files.exists(dirStitch)) {
				try {
					Files.createDirectory(dirStitch);
				} catch (IOException ex) {
					IJ.error("Failed to create temporary folder for stitching.");
					return;
				}
			}
		}
		if (stitch) {
			if (!sti.showDialog(rep))
				return;
		}

		for (int i = 0; i < rep; i++) {
			IJ.log("Loading " + getImg9809Name() + "...");
			strOption = "image=" + strImg9809Path + " reference=" + strRef9809Path + " constant="
					+ String.valueOf((int) constantOffset) + " energy=" + String.valueOf(energyOffset) + " binning="
					+ strBinning + " i0 energy_0 save";
			IJ.run("Load ORCA-Flash imagestack", strOption);
			impMut = Load_OrcaStack.impStack;
			baseName = impMut.getTitle().replace("_corrected", "").replace(".tif", "");
			if (roi == null) {
				impCrop = impMut;
			} else {
				IJ.makeRectangle(roiX, roiY, roiWidth, roiHeight);
				impCrop = impMut.crop("stack");
				impCrop.setFileInfo(impMut.getOriginalFileInfo());
				impMut.close();
			}
			impCrop.setTitle(baseName);
			impCrop.show();
			IJ.log("\\Update:Loading " + getImg9809Name() + "...done.");
			if (filter) {
				IJ.log("Applying filter...");
				GaussianBlur3D.blur(impCrop, xsigma, ysigma, zsigma);
				IJ.log("\\Update:Applying filter...done.");
			}
			impCrop.show();
			Normalization.Normalize(impCrop, threshold, false, statsImages, true);
			impNorm = Normalization.impNorm;
			impDmut = Normalization.impDmut;
			if (clip) {
				Clip_Values.ClipValues(impDmut, 5F, 0F, 0F, false);
				IJ.saveAsTiff(impDmut, impCrop.getOriginalFileInfo().directory + impDmut.getTitle());
			}
			if (doSVD) {
				SVD.setDataMatrix(impNorm);
				SVD.doSVD(true);
				SVD.showResults(impDmut, clip, false, true, true);
			}

			if (copy) {
				if (i == 0) {
					sufList.add("_LastImage.tif");
					sufList.add("_Dmut.tif");
					sufList.add("_E0.tif");
					if(statsImages) {
						sufList.add("_PreEdgeMean.tif");
						sufList.add("_PreEdgeSlope.tif");
						sufList.add("_PreEdgeStdDev.tif");
						sufList.add("_PostEdgeMean.tif");
						sufList.add("_PostEdgeSlope.tif");
						sufList.add("_PostEdgeStdDev.tif");
					}
					if (doSVD) {
						List<String> listComponents = SVD.getNames();
						for (int j = 0; j < listComponents.size(); j++) {
							sufList.add("_" + listComponents.get(j) + ".tif");
						}
					}
				}
				impCrop.setSlice(impCrop.getNSlices());
				impLast = new ImagePlus(baseName + "_LastImage.tif", impCrop.getProcessor());
				IJ.saveAsTiff(impLast, impCrop.getOriginalFileInfo().directory + impLast.getTitle());
				for (int j = 0; j < sufList.size(); j++) {
					Path srcCopy = Paths.get(getImg9809Dir(), baseName + sufList.get(j));
					try {
						Files.copy(srcCopy, dirStitch.resolve(srcCopy.getFileName()),
								StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ex) {
						IJ.error("Failed to copy result file(s).\n" + ex.getMessage());
						return;
					}
				}
			}

			IJ.run("Close All");
			System.gc();
			setNextImg9809();
		}

		strImg9809Path = first9809Path;
		if (stitch) {
			if (!sti.register(dirStitch.toString() + "/" + getImg9809Name() + sufList.get(0))) {
				IJ.error("Failed to calculate stitching configuration.");
				return;
			}

			ImagePlus impCurrent;
			for (int i = 0; i < sufList.size(); i++) {
				impCurrent = sti.doStitching(sufList.get(i));
				if (impCurrent == null)
					return;
				if (sufList.get(i).contains("E0")) {
					impCurrent.setDisplayRange(e0Min, e0Max);
					IJ.run("Jet");
				} else {
					IJ.run("Enhance Contrast", "saturated=0.35");
				}
				IJ.saveAsTiff(impCurrent, dirStitch.toString() + "/" + impCurrent.getTitle());
			}
		}

		Instant endTime = Instant.now();
		long elapsed = Duration.between(startTime, endTime).getSeconds();
		IJ.log("Finished batch job. Elapsed time: " + String.valueOf(elapsed) + " seconds.");
	}

	private Path getPathImg9809() {
		return Paths.get(strImg9809Path);
	}

	private Path getPathRef9809() {
		return Paths.get(strRef9809Path);
	}

	private String getImg9809Name() {
		return getPathImg9809().getFileName().toString();
	}

	private String getImg9809NameWithoutIdx() {
		try {
			return getImg9809Name().substring(0, getImg9809Name().length() - 3);
		} catch (IndexOutOfBoundsException ex) {
			return getImg9809Name().replaceAll("[0-9]", "");
		}
	}

	private String getImg9809Dir() {
		return getPathImg9809().getParent().toString() + "/";
	}

	private String getImg9809Root() {
		return getPathImg9809().getParent().getParent().toString();
	}

	private int getImg9809Idx() {
		try {
			return Integer.parseInt(getImg9809Name().substring(getImg9809Name().length() - 3));
		} catch (IndexOutOfBoundsException ex) {
			return -1;
		} catch (NumberFormatException ex) {
			return -1;
		}
	}

	private void setNextImg9809() {
		strImg9809Path = Paths.get(getImg9809Root(), getNextName(), getNextName()).toString();
		return;
	}

	private String getNextName() {
		int idx = getImg9809Idx();
		if (idx < 0) {
			return getImg9809NameWithoutIdx();
		} else {
			return getImg9809NameWithoutIdx() + String.format("%03d", idx + 1);
		}

	}

	private int getRepetition() {
		String tmp = strImg9809Path;
		int rep = 0;
		do {
			rep++;
			setNextImg9809();
		} while (Files.exists(getPathImg9809()));
		strImg9809Path = tmp;
		return rep;
	}

}
