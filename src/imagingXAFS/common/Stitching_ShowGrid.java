package imagingXAFS.common;

import java.awt.*;

import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;

public class Stitching_ShowGrid implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Show stitching grid overlay");
//		gd.addFileField("Configuration_file", "");
		gd.addNumericField("Single image width", 4096, 0);
		gd.addNumericField("Single image height", 2304, 0);
		gd.addSlider("Tile overlap [%]", 0, 100, 10);
		gd.addCheckbox("Show grid index", true);
		gd.addChoice("Grid_order", Stitching.CHOICEORDER, Stitching.CHOICEORDER[0]);
		gd.addNumericField("Start index", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int wid = (int) gd.getNextNumber();
		int hei = (int) gd.getNextNumber();
		double overlap = gd.getNextNumber();
		boolean showIndex = gd.getNextBoolean();
		int order = gd.getNextChoiceIndex();
		int idx = (int) gd.getNextNumber();

		ImagePlus imp = WindowManager.getCurrentImage();
		int widTarget = imp.getWidth();
		int heiTarget = imp.getHeight();
		if (widTarget <= wid && heiTarget <= hei)
			IJ.error("Current image is not a stitched image.");
		int overlapHor = (int) (overlap / 100 * wid);
		int overlapVer = (int) (overlap / 100 * hei);

		Overlay overlay = new Overlay();
		int posX = wid;
		while (true) {
			if (posX >= widTarget)
				break;
			overlay.add(new Line(posX, 0, posX, heiTarget));
			posX -= overlapHor;
			overlay.add(new Line(posX, 0, posX, heiTarget));
			posX += wid;
		}
		int posY = hei;
		while (true) {
			if (posY >= heiTarget)
				break;
			overlay.add(new Line(0, posY, widTarget, posY));
			posY -= overlapVer;
			overlay.add(new Line(0, posY, widTarget, posY));
			posY += hei;
		}
		if (showIndex) {
			int size = wid > hei ? hei / 4 : wid / 4;
			Font font = new Font("SansSerif", Font.PLAIN, size);
			int cntX = (widTarget - wid / 2) / (wid - overlapHor) + 1;
			int cntY = (heiTarget - hei / 2) / (hei - overlapVer) + 1;
			switch (order) {
			case 0:// Right & Down
				for (int i = 0; i < cntY; i++) {
					posY = (hei - overlapVer) * i + hei / 2 - size / 2;
					for (int j = 0; j < cntX; j++) {
						posX = (wid - overlapHor) * j + wid / 2 - size / 2;
						overlay.add(new TextRoi(posX, posY, String.valueOf(idx), font));
						idx++;
					}
				}
				break;
			case 1:// Left & Down
				for (int i = 0; i < cntY; i++) {
					posY = (hei - overlapVer) * i + hei / 2 - size / 2;
					for (int j = cntX - 1; j >= 0; j--) {
						posX = (wid - overlapHor) * j + wid / 2 - size / 2;
						overlay.add(new TextRoi(posX, posY, String.valueOf(idx), font));
						idx++;
					}
				}
				break;
			case 2:// Right & Up
				for (int i = cntY - 1; i >= 0; i--) {
					posY = (hei - overlapVer) * i + hei / 2 - size / 2;
					for (int j = 0; j < cntX; j++) {
						posX = (wid - overlapHor) * j + wid / 2 - size / 2;
						overlay.add(new TextRoi(posX, posY, String.valueOf(idx), font));
						idx++;
					}
				}
				break;
			case 3:// Left & Up
				for (int i = cntY - 1; i >= 0; i--) {
					posY = (hei - overlapVer) * i + hei / 2 - size / 2;
					for (int j = cntX - 1; j >= 0; j--) {
						posX = (wid - overlapHor) * j + wid / 2 - size / 2;
						overlay.add(new TextRoi(posX, posY, String.valueOf(idx), font));
						idx++;
					}
				}
				break;
			default:
				break;
			}
		}

		imp.setOverlay(null);
		imp.setOverlay(overlay);
	}
}
