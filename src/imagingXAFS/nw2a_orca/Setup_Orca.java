package imagingXAFS.nw2a_orca;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Setup_Orca implements PlugIn {

	public void run(String arg) {
		OrcaProps readProps = OrcaCommon.ReadProps();

		String[] choiceDirections = { "Down", "Up" };
		String[] choiceBitDepth = { " 8", "16", "32", "64" };

		GenericDialog gd = new GenericDialog("Imaging XAFS Setup");
		gd.addChoice("DCM direction: ", choiceDirections, choiceDirections[readProps.dcmDirection]);
		gd.addNumericField("Detector position: ", readProps.detectorPosition, 1, 8, "mm");
		gd.addNumericField("Pixel size: ", readProps.pixelSize, 3, 8, "um");
		gd.addNumericField("Distance between crystals: ", readProps.dcmDistance, 1, 8, "mm");
		gd.addNumericField("Width: ", readProps.width, 0);
		gd.addNumericField("Height: ", readProps.height, 0);
		gd.addChoice("Bit depth: ", choiceBitDepth, choiceBitDepth[1]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		OrcaProps target = new OrcaProps();
		target.dcmDirection = gd.getNextChoiceIndex();
		target.detectorPosition = gd.getNextNumber();
		target.pixelSize = gd.getNextNumber();
		target.dcmDistance = gd.getNextNumber();
		target.width = (int) gd.getNextNumber();
		target.height = (int) gd.getNextNumber();
		switch (gd.getNextChoiceIndex()) {
		case 0:
			target.bitDepth = 8;
			break;
		case 1:
			target.bitDepth = 16;
			break;
		case 2:
			target.bitDepth = 32;
			break;
		case 3:
			target.bitDepth = 64;
			break;
		}

		OrcaCommon.WriteProps(target);
	}
	
}
