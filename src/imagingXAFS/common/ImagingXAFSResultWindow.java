package imagingXAFS.common;

import java.awt.Color;
import java.util.List;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class ImagingXAFSResultWindow implements PlugIn {

	public static int margin = 5;
	public static int heightLabelArea = 15;// Assume drawString() uses font ij.IJ.font12.

	public void run(String arg) {
	}

	public static void create(String title, List<ImagePlus> imps) {
		if (imps == null)
			return;
		int num = imps.size();
		if (num < 1)
			return;
		else if (num == 1)
			create(title, 1, 1, imps);
		else if (num == 2)
			create(title, 2, 1, imps);
		else if (num == 3)
			create(title, 3, 1, imps);
		else if (num < 7)
			create(title, 3, 2, imps);
		else {
			int rep = (num - 1) / 6 + 1;
			for (int i = 0; i < rep; i++) {
				create(title + "-" + String.valueOf(i + 1), 3, 2,
						imps.subList(i * 6, Math.min(i * 6 + 6, imps.size())));
			}
		}
	}

	public static void create(String title, int numX, int numY, List<ImagePlus> imps) {
		int width = ImagingXAFSCommon.getCurrentScreenWidth() / (numX + 1);
		int height = (int) ((double) width / imps.get(0).getWidth() * imps.get(0).getHeight());
		ImagePlus impResult = NewImage.createRGBImage(title, (width + margin) * numX + margin,
				(height + heightLabelArea + margin) * numY + margin, 1, NewImage.FILL_WHITE);
		ImageProcessor ip = impResult.getProcessor();
		for (int i = 0; i < numY; i++) {
			for (int j = 0; j < numX; j++) {
				if (i * numX + j < imps.size()) {
					addDataPanel(imps.get(i * numX + j), width, height, ip, (width + margin) * j + margin,
							(height + heightLabelArea + margin) * i + heightLabelArea + margin);
				}
			}
		}
		impResult.show();
	}

	static void addDataPanel(ImagePlus imp, int width, int height, ImageProcessor ip, int xOffset, int yOffset) {
		ImagePlus impScaled = imp.resize(width, height, "average");
		double min = impScaled.getDisplayRangeMin();
		double max = impScaled.getDisplayRangeMax();
		String label = imp.getTitle();
		if (imp.getBitDepth() == 16) {
			label += "     " + String.format("%d", min) + " --- " + String.format("%d", max);
		} else if (imp.getBitDepth() == 32) {
			label += "     " + String.format("%.3f", min) + " --- " + String.format("%.3f", max);
		}
		ip.setColor(Color.BLACK);
		ip.setJustification(ImageProcessor.LEFT_JUSTIFY);
		ip.drawString(label, xOffset, yOffset);
		ip.insert(impScaled.getProcessor(), xOffset, yOffset);
	}

}
