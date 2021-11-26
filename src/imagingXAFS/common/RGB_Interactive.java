package imagingXAFS.common;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class RGB_Interactive implements PlugIn, DialogListener {

	final private String[] choiceBg = { "White", "Black" };
	Integer[] listId;
	ImageProcessor ipR;
	ImageProcessor ipG;
	ImageProcessor ipB;
	ImagePlus impPreview = null;
	static int previewWidth = 600;

	public void run(String arg) {
		previewWidth = ImagingXAFSCommon.getCurrentScreenWidth() / 3;

		listId = ImagingXAFSCommon.getDataIds(false);
		if(listId.length < 2) {
			IJ.error("Could not find open data image(s).");
			return;
		}
		String[] listTitle = ImagingXAFSCommon.getDataTitles(false);
		ipR = WindowManager.getImage(listId[0]).getProcessor().resize(previewWidth).convertToByte(true);
		ipG = WindowManager.getImage(listId[1]).getProcessor().resize(previewWidth).convertToByte(true);
		ipB = WindowManager.getImage(listId[2]).getProcessor().resize(previewWidth).convertToByte(true);
		impPreview = new ImagePlus("Preview", ipR.convertToRGB());

		GenericDialog gd = new GenericDialog("Make RGB phase map");
		gd.addChoice("Red image: ", listTitle, listTitle[0]);
		gd.addChoice("Green image: ", listTitle, listTitle[1]);
		gd.addChoice("Blue image: ", listTitle, listTitle[2]);
		gd.addRadioButtonGroup("Background: ", choiceBg, 1, 2, choiceBg[0]);
		gd.addSlider("Gamma: ", 0.5, 2, 1.0, 0.05);
		gd.addCheckbox("Preview ", false);
		gd.addDialogListener(this);
		gd.showDialog();
		if (impPreview.isVisible())
			impPreview.close();
		if (gd.wasCanceled())
			return;

		int idR = listId[gd.getNextChoiceIndex()];
		int idG = listId[gd.getNextChoiceIndex()];
		int idB = listId[gd.getNextChoiceIndex()];
		ipR = WindowManager.getImage(idR).getProcessor().convertToByte(true);
		ipG = WindowManager.getImage(idG).getProcessor().convertToByte(true);
		ipB = WindowManager.getImage(idB).getProcessor().convertToByte(true);
		if (!areSameSize()) {
			IJ.error("Selected images have different size.");
			return;
		}
		String strBg = gd.getNextRadioButton();
		double gamma = gd.getNextNumber();

		String nameResult = "R_" + WindowManager.getImage(idR).getTitle().replace(".tif", "");
		nameResult += "_G_" + WindowManager.getImage(idG).getTitle().replace(".tif", "");
		nameResult += "_B_" + WindowManager.getImage(idB).getTitle().replace(".tif", "");
		nameResult += "_" + strBg + ".png";

		ImagePlus impResult = new ImagePlus(nameResult, ipR.convertToRGB());
		setGammaImagePixels(gamma, strBg == choiceBg[0], (int[]) impResult.getProcessor().getPixels());
		impResult.show();
		IJ.saveAs(impResult, "png", null);

	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		// TODO Auto-generated method stub

		if (gd.wasOKed() || gd.wasCanceled())
			return false;

		if (e.getSource() == gd.getChoices().get(0) || e.getSource() == gd.getChoices().get(1)
				|| e.getSource() == gd.getChoices().get(2)) {
			if (impPreview != null)
				impPreview.close();
			ipR = WindowManager.getImage(listId[gd.getNextChoiceIndex()]).getProcessor().resize(previewWidth)
					.convertToByte(true);
			ipG = WindowManager.getImage(listId[gd.getNextChoiceIndex()]).getProcessor().resize(previewWidth)
					.convertToByte(true);
			ipB = WindowManager.getImage(listId[gd.getNextChoiceIndex()]).getProcessor().resize(previewWidth)
					.convertToByte(true);
			impPreview = new ImagePlus("Preview", ipR.convertToRGB());
		}
		boolean isWhiteBg = gd.getNextRadioButton() == choiceBg[0];
		double gamma = gd.getNextNumber();

		if (gd.getNextBoolean()) {
			if (!areSameSize())
				return false;
			setGammaImagePixels(gamma, isWhiteBg, (int[]) impPreview.getProcessor().getPixels());
			if (impPreview.isVisible())
				impPreview.updateAndDraw();
			else
				impPreview.show();
		} else {
			impPreview.hide();
		}
		return true;
	}

	private void setGammaImagePixels(double gamma, boolean isWhiteBg, int[] pixelsTarget) {
		double w;
		int r;
		int g;
		int b;
		int bg = 0;
		if (isWhiteBg)
			bg = 255;

		byte[] pixelsR = (byte[]) ipR.getPixels();
		byte[] pixelsG = (byte[]) ipG.getPixels();
		byte[] pixelsB = (byte[]) ipB.getPixels();

		for (int i = 0; i < pixelsTarget.length; i++) {
			r = Byte.toUnsignedInt(pixelsR[i]);
			g = Byte.toUnsignedInt(pixelsG[i]);
			b = Byte.toUnsignedInt(pixelsB[i]);
			w = Math.exp(Math.log((double) (Math.max(Math.max(r, g), b) + 1) / 256) * gamma);
			pixelsTarget[i] = ((int) (w * r + (1 - w) * bg) << 16) + ((int) (w * g + (1 - w) * bg) << 8)
					+ (int) (w * b + (1 - w) * bg);
		}
	}

	private boolean areSameSize() {
		return ipR.getWidth() == ipG.getWidth() && ipG.getWidth() == ipB.getWidth()
				&& ipR.getHeight() == ipG.getHeight() && ipG.getHeight() == ipB.getHeight();
	}
	
}
