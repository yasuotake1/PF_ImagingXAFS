package imagingXAFS.common;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class AlphaImage_Interactive implements PlugIn, DialogListener {

	private static final String[] choiceBg = { "White", "Black" };
	Integer[] listId;
	ImageProcessor ipE0;
	ImageProcessor ipDmut;
	ImagePlus impPreview = null;
	static int previewWidth = 600;

	public void run(String arg) {
		previewWidth = ImagingXAFSCommon.getCurrentScreenWidth() / 3;

		listId = ImagingXAFSCommon.getDataIds(false);
		if (listId.length < 2) {
			IJ.error("Could not find data image(s).");
			return;
		}
		String[] listTitle = ImagingXAFSCommon.getDataTitles(false);
		ipE0 = WindowManager.getImage(listId[0]).getProcessor().resize(previewWidth).convertToRGB();
		ipDmut = WindowManager.getImage(listId[1]).getProcessor().resize(previewWidth).convertToByte(true);
		impPreview = new ImagePlus("Preview", ipE0.duplicate());

		GenericDialog gd = new GenericDialog("Make E0 @Color - Dmut @alpha channel image");
		gd.addChoice("E0 image: ", listTitle, listTitle[0]);
		gd.addChoice("Dmut image: ", listTitle, listTitle[1]);
		gd.addRadioButtonGroup("Background: ", choiceBg, 1, 2, choiceBg[0]);
		gd.addSlider("Gamma: ", 0.5, 2, 1.0, 0.05);
		gd.addCheckbox("Preview ", false);
		gd.addDialogListener(this);
		gd.showDialog();
		if (impPreview.isVisible())
			impPreview.close();
		if (gd.wasCanceled())
			return;

		int idE0 = listId[gd.getNextChoiceIndex()];
		int idDmut = listId[gd.getNextChoiceIndex()];
		ipE0 = WindowManager.getImage(idE0).getProcessor().convertToRGB();
		ipDmut = WindowManager.getImage(idDmut).getProcessor().convertToByte(true);
		if (!isSameSize()) {
			IJ.error("E0 and Dmut images have different size.");
			return;
		}
		String strBg = gd.getNextRadioButton();
		double gamma = gd.getNextNumber();

		String nameResult = WindowManager.getImage(idE0).getTitle();
		if (nameResult.endsWith(".tif"))
			nameResult = nameResult.replace(".tif", "_" + strBg + ".png");
		else
			nameResult += "_" + strBg + ".png";

		ImagePlus impResult = new ImagePlus(nameResult, ipE0.duplicate());
		setAlphaImagePixels(gamma, strBg == choiceBg[0], (int[]) impResult.getProcessor().getPixels());
		impResult.show();
		IJ.saveAs(impResult, "png", null);

	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// TODO Auto-generated method stub

		if (gd.wasOKed() || gd.wasCanceled())
			return false;

		if (e.getSource() == gd.getChoices().get(0) || e.getSource() == gd.getChoices().get(1)) {
			if (impPreview != null)
				impPreview.close();
			ipE0 = WindowManager.getImage(listId[gd.getNextChoiceIndex()]).getProcessor().resize(previewWidth)
					.convertToRGB();
			ipDmut = WindowManager.getImage(listId[gd.getNextChoiceIndex()]).getProcessor().resize(previewWidth)
					.convertToByte(true);
			impPreview = new ImagePlus("Preview", ipE0.duplicate());
		}
		boolean isWhiteBg = gd.getNextRadioButton() == choiceBg[0];
		double gamma = gd.getNextNumber();

		if (gd.getNextBoolean()) {
			if (!isSameSize())
				return false;
			setAlphaImagePixels(gamma, isWhiteBg, (int[]) impPreview.getProcessor().getPixels());
			if (impPreview.isVisible())
				impPreview.updateAndDraw();
			else
				impPreview.show();
		} else {
			impPreview.hide();
		}
		return true;
	}

	private void setAlphaImagePixels(double gamma, boolean isWhiteBg, int[] pixelsTarget) {
		double w;
		int r;
		int g;
		int b;
		int bg = 0;
		if (isWhiteBg)
			bg = 255;
		int[] pixelsValue = (int[]) ipE0.getPixels();
		byte[] pixelsAlpha = (byte[]) ipDmut.getPixels();

		for (int i = 0; i < pixelsTarget.length; i++) {
			w = Math.exp(Math.log(((double) Byte.toUnsignedInt(pixelsAlpha[i]) + 1) / 256) * gamma);
			r = (pixelsValue[i] >> 16) & 0xFF;
			g = (pixelsValue[i] >> 8) & 0xFF;
			b = pixelsValue[i] & 0xFF;
			pixelsTarget[i] = ((int) (w * r + (1 - w) * bg) << 16) + ((int) (w * g + (1 - w) * bg) << 8)
					+ (int) (w * b + (1 - w) * bg);
		}
	}

	private boolean isSameSize() {
		return ipE0.getWidth() == ipDmut.getWidth() && ipE0.getHeight() == ipDmut.getHeight();
	}

}
