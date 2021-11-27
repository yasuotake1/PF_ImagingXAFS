package imagingXAFS.common;

import java.awt.AWTEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
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

		Integer[] listIdTemp = ImagingXAFSCommon.getDataIds(false);
		if (listIdTemp.length < 2) {
			IJ.error("Could not find two or more data images.");
			return;
		}
		listId = new Integer[listIdTemp.length + 1];
		System.arraycopy(listIdTemp, 0, listId, 1, listIdTemp.length);
		listId[0] = 0;
		String[] listTitleTemp = ImagingXAFSCommon.getDataTitles(false);
		String[] listTitle = new String[listTitleTemp.length + 1];
		System.arraycopy(listTitleTemp, 0, listTitle, 1, listTitleTemp.length);
		listTitle[0] = "None";

		GenericDialog gd = new GenericDialog("Make RGB phase map");
		gd.addChoice("Red image: ", listTitle, listTitle[1]);
		gd.addChoice("Green image: ", listTitle, listTitle[2]);
		gd.addChoice("Blue image: ", listTitle, listTitle[0]);
		ipR = WindowManager.getImage(listId[1]).getProcessor().resize(previewWidth).convertToByte(true);
		ipG = WindowManager.getImage(listId[2]).getProcessor().resize(previewWidth).convertToByte(true);
		ipB = null;
		checkNullImages();
		impPreview = new ImagePlus("Preview", ipR.convertToRGB());
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
		ipR = (idR == 0) ? null : WindowManager.getImage(idR).getProcessor().convertToByte(true);
		int idG = listId[gd.getNextChoiceIndex()];
		ipG = (idG == 0) ? null : WindowManager.getImage(idG).getProcessor().convertToByte(true);
		int idB = listId[gd.getNextChoiceIndex()];
		ipB = (idB == 0) ? null : WindowManager.getImage(idB).getProcessor().convertToByte(true);
		if (!checkNullImages()) {
			return;
		}
		if (!areSameSize()) {
			IJ.error("Selected images have different size.");
			return;
		}
		String strBg = gd.getNextRadioButton();
		double gamma = gd.getNextNumber();

		String nameResult = "R_" + ((idR == 0) ? "None" : WindowManager.getImage(idR).getTitle().replace(".tif", ""));
		nameResult += "_G_" + ((idG == 0) ? "None" : WindowManager.getImage(idG).getTitle().replace(".tif", ""));
		nameResult += "_B_" + ((idB == 0) ? "None" : WindowManager.getImage(idB).getTitle().replace(".tif", ""));
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
			int idR = listId[gd.getNextChoiceIndex()];
			ipR = (idR == 0) ? null
					: WindowManager.getImage(idR).getProcessor().resize(previewWidth).convertToByte(true);
			int idG = listId[gd.getNextChoiceIndex()];
			ipG = (idG == 0) ? null
					: WindowManager.getImage(idG).getProcessor().resize(previewWidth).convertToByte(true);
			int idB = listId[gd.getNextChoiceIndex()];
			ipB = (idB == 0) ? null
					: WindowManager.getImage(idB).getProcessor().resize(previewWidth).convertToByte(true);
			if (!checkNullImages()) {
				return false;
			}
			impPreview = new ImagePlus("Preview", ipR.convertToRGB());
		}
		boolean isWhiteBg = gd.getNextRadioButton() == choiceBg[0];
		double gamma = gd.getNextNumber();

		if (gd.getNextBoolean()) {
			if (!areSameSize()) {
				return false;
			}
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
		int bg = isWhiteBg ? 255 : 0;

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

	private boolean checkNullImages() {
		ImageProcessor ipSrc = (ipR != null) ? ipR : ((ipG != null) ? ipG : ipB);
		if (ipSrc == null) {
			return false;
		}
		if (ipR == null) {
			ipR = getEmptyImageProcessor(ipSrc);
		}
		if (ipG == null) {
			ipG = getEmptyImageProcessor(ipSrc);
		}
		if (ipB == null) {
			ipB = getEmptyImageProcessor(ipSrc);
		}
		return true;
	}

	private boolean areSameSize() {
		return ipR.getWidth() == ipG.getWidth() && ipG.getWidth() == ipB.getWidth()
				&& ipR.getHeight() == ipG.getHeight() && ipG.getHeight() == ipB.getHeight();
	}

	private ImageProcessor getEmptyImageProcessor(ImageProcessor ipSrc) {
		int wid = ipSrc.getWidth();
		int hei = ipSrc.getHeight();
		byte[] pixels = new byte[wid * hei];
		return new ByteProcessor(wid, hei, pixels, null);
	}

}
