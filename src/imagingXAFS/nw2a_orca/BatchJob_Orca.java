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

	private static String strImg9809Path;
	private static String strRef9809Path;
	private static boolean saveStack = false;
	private static boolean driftCorr = false;
	private static boolean filter = false;
	private static boolean zeroSlope = false;
	private static double threshold = 2.0;
	private static boolean statsImages = false;
	private static boolean doSVD = true;
	private static boolean bClip = true;
	private static boolean bCopy = true;
	private static boolean bStitch = true;
	private static boolean bComplement = false;
	private static boolean showStitched = true;

	public void run(String arg) {
		boolean macroMode = arg.equalsIgnoreCase("macro");
		GenericDialog gd = new GenericDialog("Batch job: NW2A ImagingXAFS");
		gd.addMessage("Data source:");
		gd.addFileField("First transmission images (9809 format)", strImg9809Path);
		gd.addFileField("Reference images (9809 format) or constant", strRef9809Path);
		gd.addFileField("Dark image or constant", OrcaCommon.strDark);
		gd.addCheckbox("Avoid zero in raw images", OrcaCommon.avoidZero);
		gd.addChoice("Binning", OrcaCommon.LIST_BINNING, OrcaCommon.strBinning);
		gd.addNumericField("Energy offset", OrcaCommon.ofsEne, 2);
		gd.addCheckbox("I0 correction", Load_OrcaStack.getI0Corr());
		gd.addCheckbox("Save stack files", saveStack);
		gd.addMessage("Preprocess:");
		gd.addCheckbox("Drift correction (Requires specified ROI list)", driftCorr);
		gd.addCheckbox("Apply 3D Gaussian blur", filter);
		gd.addMessage("Normalization:");
		gd.addNumericField("Pre-edge from", ImagingXAFSCommon.normalizationParam[0], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[1], 2, 8, "eV");
		gd.addNumericField("Post-edge from", ImagingXAFSCommon.normalizationParam[2], 2, 8, "eV");
		gd.addNumericField("to", ImagingXAFSCommon.normalizationParam[3], 2, 8, "eV");
		gd.addCheckbox("Zero-slope pre-edge", zeroSlope);
		gd.addNumericField("Filter threshold", threshold, 1);
		gd.addNumericField("Normalized absorbance at E0", ImagingXAFSCommon.e0Jump, 2);
		gd.addNumericField("E0 plot range minimum", ImagingXAFSCommon.e0Min, 2, 8, "eV");
		gd.addNumericField("maximum", ImagingXAFSCommon.e0Max, 2, 8, "eV");
		gd.addCheckbox("Create statistics images", statsImages);
		gd.addMessage("Singular value decomposition:");
		gd.addCheckbox("Perform_SVD", doSVD);
		gd.addCheckbox("Clip at zero", bClip);
		gd.addMessage("Postprocess:");
		gd.addCheckbox("Copy files for stitching", bCopy);
		gd.addCheckbox("Perform_grid_stitching", bStitch);
		gd.addCheckbox("Complement tile positions of refinement failure", bComplement);
		gd.addCheckbox("Show stitched images", showStitched);
		if (macroMode) {
			gd.addNumericField("Crop_x", 0, 1);
			gd.addNumericField("Crop_y", 0, 1);
			gd.addNumericField("Crop_width", 0, 1);
			gd.addNumericField("Crop_height", 0, 1);
			gd.addCheckbox("Use_ROI for calculation", true);
			gd.addFileField("ROI_list", "");
			gd.addNumericField("Gaussian blur sigma (radius)", 1.0, 1);
			gd.addCheckbox("Edge detection", false);
			gd.addChoice("Optimization", ImagingXAFSDriftCorrection.OPTIMIZATION,
					ImagingXAFSDriftCorrection.OPTIMIZATION[0]);
			gd.addChoice("Calculate_drift_to", ImagingXAFSDriftCorrection.CALC_MODE,
					ImagingXAFSDriftCorrection.CALC_MODE[0]);
			gd.addNumericField("Filter_sigmaX", 1.0, 1);
			gd.addNumericField("Filter_sigmaY", 1.0, 1);
			gd.addNumericField("Filter_sigmaZ", 1.0, 1);
			gd.addStringField("Standard_files", "");
			gd.addChoice("Grid_order", Stitching.CHOICEORDER, Stitching.CHOICEORDER[0]);
			gd.addNumericField("Grid_size_X", 2, 0);
			gd.addNumericField("Grid_size_Y", 2, 0);
			gd.addSlider("Tile_overlap", 0, 100, 10);
		}
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		strImg9809Path = gd.getNextString();
		strRef9809Path = gd.getNextString();
		String strDark = gd.getNextString();
		if (!ImagingXAFSCommon.isExistingPath(strImg9809Path)
				|| !(ImagingXAFSCommon.isExistingPath(strRef9809Path) || OrcaCommon.isInteger(strRef9809Path)))
			return;
		OrcaCommon.avoidZero = gd.getNextBoolean();
		OrcaCommon.strBinning = gd.getNextChoice();
		OrcaCommon.ofsEne = gd.getNextNumber();
		boolean i0Corr = gd.getNextBoolean();
		saveStack = gd.getNextBoolean();
		driftCorr = gd.getNextBoolean();
		filter = gd.getNextBoolean();
		double[] energy = ImagingXAFSCommon.readEnergies(strImg9809Path);
		if (OrcaCommon.ofsEne <= -0.01 || OrcaCommon.ofsEne >= 0.01) {
			for (int i = 0; i < energy.length; i++) {
				energy[i] += OrcaCommon.ofsEne;
			}
		}
		int rep = OrcaCommon.getRepetition(strImg9809Path);
		double preStart = gd.getNextNumber();
		double preEnd = gd.getNextNumber();
		double postStart = gd.getNextNumber();
		double postEnd = gd.getNextNumber();
		ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
		zeroSlope = gd.getNextBoolean();
		threshold = gd.getNextNumber();
		double e0Jump = gd.getNextNumber();
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
		ImagingXAFSCommon.e0Jump = (float) e0Jump;
		ImagingXAFSCommon.e0Min = e0Min;
		ImagingXAFSCommon.e0Max = e0Max;
		statsImages = gd.getNextBoolean();
		doSVD = gd.getNextBoolean();
		bClip = gd.getNextBoolean();
		bCopy = gd.getNextBoolean();
		bStitch = gd.getNextBoolean();
		bComplement = gd.getNextBoolean();
		showStitched = gd.getNextBoolean();
		int cropX = macroMode ? (int) gd.getNextNumber() : 0;
		int cropY = macroMode ? (int) gd.getNextNumber() : 0;
		int cropWidth = macroMode ? (int) gd.getNextNumber() : 0;
		int cropHeight = macroMode ? (int) gd.getNextNumber() : 0;
		boolean useRoi = macroMode ? gd.getNextBoolean() : false;
		String pathRoiList = macroMode ? gd.getNextString() : "";
		double sigmaDrift = macroMode ? gd.getNextNumber() : 1.0;
		boolean edge = macroMode ? gd.getNextBoolean() : false;
		int driftOpt = macroMode ? gd.getNextChoiceIndex() : 0;
		int driftMode = macroMode ? gd.getNextChoiceIndex() : 0;
		double sigmaX = macroMode ? gd.getNextNumber() : 1.0;
		double sigmaY = macroMode ? gd.getNextNumber() : 1.0;
		double sigmaZ = macroMode ? gd.getNextNumber() : 7.0;
		String standardFiles = macroMode ? gd.getNextString() : "";
		String gridOrder = macroMode ? gd.getNextChoice() : "";
		int gridSizeX = macroMode ? (int) gd.getNextNumber() : 0;
		int gridSizeY = macroMode ? (int) gd.getNextNumber() : 0;
		double tileOverlap = macroMode ? gd.getNextNumber() : 0.0;

		Roi roi = null;
		if (macroMode) {
			if (cropWidth > 0 && cropHeight > 0)
				roi = new Roi(cropX, cropY, cropWidth, cropHeight);
		} else {
			String strImgPath = strImg9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
			String strRefPath = strRef9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
			String strOption = "transmission=" + strImgPath + " reference=" + strRefPath;
			strOption += " dark=" + strDark + (OrcaCommon.avoidZero ? " avoid" : "") + " binning="
					+ OrcaCommon.strBinning;
			IJ.run("Load single ORCA image...", strOption);
			ImagePlus impRoi = Load_SingleOrca.impTgt;
			IJ.setTool("rect");
			new WaitForUserDialog("Select rectangle region to analyze, then click OK.\nSelect none not to crop.")
					.show();
			roi = impRoi.getRoi();
			if (roi != null && roi.getType() != Roi.RECTANGLE) {
				IJ.error("Failed to specify region to analyze.");
				impRoi.close();
				return;
			}
			impRoi.close();
		}

		ImagingXAFSDriftCorrection udc = new ImagingXAFSDriftCorrection();
		Roi[] driftRois = new Roi[rep];
		if (driftCorr) {
			if (!macroMode) {
				gd = new GenericDialog("Drift correction");
				gd.addCheckbox("Use_ROI for calculation", true);
				gd.addFileField("ROI_list", "");
				gd.addNumericField("Gaussian blur sigma (radius)", 1.0, 1);
				gd.addCheckbox("Edge detection", false);
				gd.addChoice("Optimization", ImagingXAFSDriftCorrection.OPTIMIZATION,
						ImagingXAFSDriftCorrection.OPTIMIZATION[0]);
				gd.addChoice("Calculate_drift_to", ImagingXAFSDriftCorrection.CALC_MODE,
						ImagingXAFSDriftCorrection.CALC_MODE[0]);
				gd.showDialog();
				if (gd.wasCanceled())
					return;
				useRoi = gd.getNextBoolean();
				pathRoiList = gd.getNextString();
				sigmaDrift = gd.getNextNumber();
				edge = gd.getNextBoolean();
				driftOpt = gd.getNextChoiceIndex();
				driftMode = gd.getNextChoiceIndex();
			}
			if (useRoi) {
				try {
					List<String> strDriftRois = Files.readAllLines(Paths.get(pathRoiList));
					int tmpX, tmpY, tmpW, tmpH;
					for (int i = 0; i < rep; i++) {
						String[] bounds = strDriftRois.get(i).split(",");
						tmpX = Integer.parseInt(bounds[0]);
						tmpY = Integer.parseInt(bounds[1]);
						tmpW = Integer.parseInt(bounds[2]);
						tmpH = Integer.parseInt(bounds[3]);
						driftRois[i] = (tmpW == 0 || tmpH == 0) ? null : new Roi(tmpX, tmpY, tmpW, tmpH);
					}
				} catch (Exception ex) {
					ImagingXAFSCommon.logStackTrace(ex);
					return;
				}
			}
		}

		if (!macroMode && filter) {
			gd = new GenericDialog("3D Gaussian Blur");
			gd.addNumericField("X sigma:", sigmaX, 1);
			gd.addNumericField("Y sigma:", sigmaY, 1);
			gd.addNumericField("Z sigma:", sigmaZ, 1);
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			sigmaX = gd.getNextNumber();
			sigmaY = gd.getNextNumber();
			sigmaZ = gd.getNextNumber();
		}

		if (doSVD) {
			if (macroMode ? !SVD.setStandards(standardFiles.split(","), energy) : !SVD.setStandards(false, energy))
				return;
		}

		String first9809Path = strImg9809Path;
		String dirStitch = OrcaCommon.getStrGrandParent(first9809Path) + "/stitching";
		ArrayList<String> sufList = new ArrayList<String>();
		Stitching sti = new Stitching();
		if (bCopy) {
			if (!Files.exists(Paths.get(dirStitch))) {
				try {
					Files.createDirectory(Paths.get(dirStitch));
				} catch (IOException ex) {
					IJ.error("Failed to create temporary folder for stitching.");
					return;
				}
			}
		}
		if (bStitch) {
			if (macroMode) {
				sti.setWithoutDialog(gridOrder, gridSizeX, gridSizeY, tileOverlap);
			} else {
				if (!sti.showDialog(rep))
					return;
			}
		}

		IJ.run("Close All");
		System.gc();
		ImagePlus impMut, impCrop, impCorr, impNorm, impDmut, impLast;
		String baseName;
		Instant startTime = Instant.now();
		for (int i = 0; i < rep; i++) {
			IJ.log("Loading " + getImg9809Name() + "...");
			Load_OrcaStack.setOptions(i0Corr, true, saveStack);
			Load_OrcaStack.load(strImg9809Path, strRef9809Path);
			impMut = Load_OrcaStack.impStack;
			baseName = impMut.getTitle().replace("_corrected", "").replace(".tif", "");
			if (roi == null) {
				impCrop = impMut;
			} else {
				IJ.makeRectangle(roi.getBounds().x, roi.getBounds().y, roi.getBounds().width, roi.getBounds().height);
				impCrop = impMut.crop("stack");
				impCrop.setFileInfo(impMut.getOriginalFileInfo());
				ImagingXAFSCommon.setPropEnergies(impCrop, ImagingXAFSCommon.getPropEnergies(impMut));
				impMut.close();
			}
			impCrop.setTitle(baseName);
			impCrop.show();
			IJ.log("\\Update:Loading " + getImg9809Name() + "...done.");
			if (driftCorr && !(useRoi && driftRois[i] == null)) {
				impCorr = udc.GetCorrectedStack(impCrop, driftOpt, driftMode, sigmaDrift, edge, driftRois[i], true);
				impCorr.setTitle(impCrop.getTitle());
				impCrop.close();
			} else {
				impCorr = impCrop;
			}
			if (filter) {
				IJ.log("Applying filter...");
				GaussianBlur3D.blur(impCorr, sigmaX, sigmaY, sigmaZ);
				IJ.log("\\Update:Applying filter...done.");
			}
			impCorr.show();
			Normalization.Normalize(impCorr, zeroSlope, (float) threshold, false, statsImages, true, saveStack);
			impNorm = Normalization.impNorm;
			impDmut = Normalization.impDmut;
			if (bClip) {
				Clip_Values.ClipValues(impDmut, 5F, 0F, 0F, false);
				IJ.saveAsTiff(impDmut, impCorr.getOriginalFileInfo().directory + impDmut.getTitle());
			}
			if (doSVD) {
				SVD.setDataMatrix(impNorm);
				SVD.performSVD(true);
				SVD.showResults(impDmut, bClip, false, true, true);
			}

			if (bCopy) {
				if (i == 0) {
					sufList.add("_LastImage.tif");
					sufList.add("_Dmut.tif");
					sufList.add("_E0.tif");
					if (statsImages) {
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
				impCorr.setSlice(impCorr.getNSlices());
				impLast = new ImagePlus(baseName + "_LastImage.tif", impCorr.getProcessor());
				IJ.saveAsTiff(impLast, impCorr.getOriginalFileInfo().directory + impLast.getTitle());
				for (int j = 0; j < sufList.size(); j++) {
					Path srcCopy = Paths.get(OrcaCommon.getStrParent(strImg9809Path), baseName + sufList.get(j));
					Path tgtCopy = Paths.get(dirStitch, baseName + sufList.get(j));
					try {
						Files.copy(srcCopy, tgtCopy, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ex) {
						IJ.error("Failed to copy result file(s).\n" + ex.getMessage());
						return;
					}
				}
			}

			IJ.run("Close All");
			System.gc();
			strImg9809Path = OrcaCommon.getNextPath(strImg9809Path);
		}

		strImg9809Path = first9809Path;
		if (bStitch) {
			if (!sti.register(dirStitch + "/" + getImg9809Name() + sufList.get(0))) {
				IJ.error("Failed to calculate stitching configuration.");
				return;
			}

			if (bComplement) {
				sti.doComplement();
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
				IJ.saveAsTiff(impCurrent, dirStitch + "/" + impCurrent.getTitle());
				if (!showStitched) {
					impCurrent.close();
				}
				System.gc();
			}
		}

		Instant endTime = Instant.now();
		long elapsed = Duration.between(startTime, endTime).getSeconds();
		IJ.log("Finished batch job. Elapsed time: " + elapsed + " seconds.");
	}

	private String getImg9809Name() {
		return Paths.get(strImg9809Path).getFileName().toString();
	}

}
