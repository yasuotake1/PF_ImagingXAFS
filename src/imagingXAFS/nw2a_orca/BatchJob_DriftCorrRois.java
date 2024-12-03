package imagingXAFS.nw2a_orca;

import java.io.FileWriter;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import imagingXAFS.common.ImagingXAFSCommon;

public class BatchJob_DriftCorrRois implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Specify drift correction ROIs");
		gd.addFileField("First transmission images (9809 format)", OrcaCommon.strImg);
		gd.addFileField("Reference images (9809 format) or constant", OrcaCommon.strRef);
		gd.addFileField("Dark image or constant", OrcaCommon.strDark);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strImg9809Path = gd.getNextString();
		String strRef9809Path = gd.getNextString();
		String strDark = gd.getNextString();
		double[] energy = ImagingXAFSCommon.readEnergies(strImg9809Path);
		int rep = OrcaCommon.getRepetition(strImg9809Path);
		String content = "";

		for (int i = 0; i < rep; i++) {
			String strImgPath = strImg9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
			String strRefPath = strRef9809Path + "_" + String.format("%03d", energy.length - 1) + ".img";
			String strOption = "transmission=" + strImgPath + " reference=" + strRefPath;
			strOption += " dark=" + strDark + (OrcaCommon.avoidZero ? " avoid" : "") + " binning="
					+ OrcaCommon.strBinning;
			IJ.run("Load single ORCA image...", strOption);
			ImagePlus impRoi = Load_SingleOrca.impTgt;
			IJ.setTool("rect");
			new WaitForUserDialog("Select rectangle region for drift correction, then click OK.\nSelect none to skip.")
					.show();
			Roi roi = impRoi.getRoi();
			if (roi != null && roi.getType() != Roi.RECTANGLE) {
				IJ.error("Failed to specify region for drift correction.");
				impRoi.close();
				return;
			}
			if (roi == null)
				content += "0,0,0,0\n";
			else
				content += String.format("%d,%d,%d,%d\n", roi.getBounds().x, roi.getBounds().y, roi.getBounds().width,
						roi.getBounds().height);
			impRoi.close();
			strImg9809Path = OrcaCommon.getNextPath(strImg9809Path);
		}
		SaveDialog sd = new SaveDialog("Save ROIs...",
				Paths.get(OrcaCommon.getStrGrandParent(strImg9809Path)).toString(), "DriftCorrRois", ".txt");
		if (sd.getFileName() == null)
			return;
		try (FileWriter fw = new FileWriter(sd.getDirectory()+sd.getFileName())) {
			fw.write(content);
		} catch (Exception ex) {
			IJ.log("Failed to write ROIs.");
		}
	}

}
