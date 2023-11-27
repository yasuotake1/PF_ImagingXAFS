package imagingXAFS.bl15a1;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Setup_BL15A1 implements PlugIn {

	public void run(String arg) {
		
		String[] listStage = { "Default", "Reversed" };
		BL15A1Props readProps = BL15A1Common.ReadProps();

		GenericDialog gd = new GenericDialog("Setup XRF/XANES mapping");
		String strGuide1 = "Enter the file suffixes.";
		strGuide1 += "\r\nLeave the field empty to ignore its channel.";
		gd.addMessage(strGuide1);
		for (int i = 0; i < 16; i++) {
			gd.addStringField(BL15A1Common.listChName[i] + ": ", readProps.listSuffixes[i], 10);
		}
		String strGuide3 = "Note: i0 and i1 will be used to calculate mu = ln(i0/i1).";
		strGuide3 += "\r\nSCA1-8 and AUX1-6 will be normalized by i/i0.";
		gd.addMessage(strGuide3);
		if (readProps.stageConf == 1) {
			gd.addRadioButtonGroup("Stage configuration: ", listStage, 1, 2, listStage[1]);
		} else {
			gd.addRadioButtonGroup("Stage configuration: ", listStage, 1, 2, listStage[0]);
		}
		gd.addChoice("Image scaling: ", BL15A1Common.listScale, readProps.scaleConf);
		gd.addNumericField("Default zoom (%): ", readProps.zoom, 1);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		BL15A1Props target = new BL15A1Props();
		for (int i = 0; i < 16; i++) {
			target.listSuffixes[i] = gd.getNextString();
		}
		if (gd.getNextRadioButton() == listStage[0]) {
			target.stageConf = 0;
		} else {
			target.stageConf = 1;
		}

		target.scaleConf = gd.getNextChoice();
		target.pulsePerMMX = readProps.pulsePerMMX;
		target.pulsePerMMY = readProps.pulsePerMMY;
		target.zoom = gd.getNextNumber();
		target.listUse = readProps.listUse;

		BL15A1Common.WriteProps(target);
	}

}
