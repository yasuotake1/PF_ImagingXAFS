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
		gd.addNumericField("Start index", 0, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int wid = (int) gd.getNextNumber();
		int hei = (int) gd.getNextNumber();
		double overlap = gd.getNextNumber();
		boolean showIndex = gd.getNextBoolean();
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
			posY = hei / 2 - size / 2;
			while (true) {
				if (posY >= heiTarget)
					break;
				posX = wid / 2 - size / 2;
				while (true) {
					if (posX >= widTarget)
						break;
					overlay.add(new TextRoi(posX, posY, String.valueOf(idx), font));
					posX += wid - overlapHor;
					idx++;
				}
				posY += hei - overlapVer;
			}
		}

		imp.setOverlay(null);
		imp.setOverlay(overlay);
	}
}
