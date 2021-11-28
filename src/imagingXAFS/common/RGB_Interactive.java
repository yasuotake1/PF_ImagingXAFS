package imagingXAFS.common;

import java.awt.AWTEvent;
import java.awt.TextField;

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
	ImageProcessor ipR, ipRByte, ipG, ipGByte, ipB, ipBByte;
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
		ipR = WindowManager.getImage(listId[1]).getProcessor().resize(previewWidth);
		ipRByte = ipR.convertToByte(true);
		ipG = WindowManager.getImage(listId[2]).getProcessor().resize(previewWidth);
		ipGByte = ipG.convertToByte(true);
		ipB = null;
		ipBByte = null;
		checkNullImages();
		impPreview = new ImagePlus("Preview", ipR.convertToRGB());

		GenericDialog gd = new GenericDialog("Make RGB phase map");
		gd.addChoice("Red image: ", listTitle, listTitle[1]);
		gd.addNumericField("Display range minimum", ipR.getMin(), 3);
		gd.addNumericField("Maximum", ipR.getMax(), 3);
		gd.addChoice("Green image: ", listTitle, listTitle[2]);
		gd.addNumericField("Display range minimum", ipG.getMin(), 3);
		gd.addNumericField("Maximum", ipG.getMax(), 3);
		gd.addChoice("Blue image: ", listTitle, listTitle[0]);
		gd.addNumericField("Display range minimum", Double.NaN, 3);
		gd.addNumericField("Maximum", Double.NaN, 3);
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
		double minR = gd.getNextNumber();
		double maxR = gd.getNextNumber();
		int idG = listId[gd.getNextChoiceIndex()];
		double minG = gd.getNextNumber();
		double maxG = gd.getNextNumber();
		int idB = listId[gd.getNextChoiceIndex()];
		double minB = gd.getNextNumber();
		double maxB = gd.getNextNumber();
		if (idR == 0) {
			ipR = null;
			ipRByte = null;
		} else {
			if (Double.isNaN(minR) || Double.isNaN(maxR)) {
				IJ.error("Invalid display range.");
				return;
			}
			ipR = WindowManager.getImage(idR).getProcessor();
			ipR.setMinAndMax(minR, maxR);
			ipRByte = ipR.convertToByte(true);
		}
		if (idG == 0) {
			ipG = null;
			ipGByte = null;
		} else {
			if (Double.isNaN(minG) || Double.isNaN(maxG)) {
				IJ.error("Invalid display range.");
				return;
			}
			ipG = WindowManager.getImage(idG).getProcessor();
			ipG.setMinAndMax(minG, maxG);
			ipGByte = ipG.convertToByte(true);
		}
		if (idB == 0) {
			ipB = null;
			ipBByte = null;
		} else {
			if (Double.isNaN(minB) || Double.isNaN(maxB)) {
				IJ.error("Invalid display range.");
				return;
			}
			ipB = WindowManager.getImage(idB).getProcessor();
			ipB.setMinAndMax(minB, maxB);
			ipBByte = ipB.convertToByte(true);
		}
		if (!checkNullImages()) {
			return;
		}
		if (!areSameSize()) {
			IJ.error("Selected images have different size.");
			return;
		}
		String strBg = gd.getNextRadioButton();
		double gamma = gd.getNextNumber();

		String nameResult = "R_"
				+ ((idR == 0) ? listTitle[0] : WindowManager.getImage(idR).getTitle().replace(".tif", ""));
		nameResult += "_G_" + ((idG == 0) ? listTitle[0] : WindowManager.getImage(idG).getTitle().replace(".tif", ""));
		nameResult += "_B_" + ((idB == 0) ? listTitle[0] : WindowManager.getImage(idB).getTitle().replace(".tif", ""));
		nameResult += "_" + strBg + ".png";

		ImagePlus impResult = new ImagePlus(nameResult, ipRByte.convertToRGB());
		setGammaImagePixels(gamma, strBg == choiceBg[0], (int[]) impResult.getProcessor().getPixels());
		impResult.show();
		IJ.saveAs(impResult, "png", null);

	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		if (gd.wasCanceled())
			return false;
		else if (gd.wasOKed())
			return true;

		int idR = listId[gd.getNextChoiceIndex()];
		double minR = gd.getNextNumber();
		double maxR = gd.getNextNumber();
		int idG = listId[gd.getNextChoiceIndex()];
		double minG = gd.getNextNumber();
		double maxG = gd.getNextNumber();
		int idB = listId[gd.getNextChoiceIndex()];
		double minB = gd.getNextNumber();
		double maxB = gd.getNextNumber();
		if (e.getSource() == gd.getChoices().get(0)) {
			if (impPreview != null)
				impPreview.close();
			if (idR == 0) {
				ipR = null;
				ipRByte = null;
				((TextField) gd.getNumericFields().get(0)).setText("");
				((TextField) gd.getNumericFields().get(1)).setText("");
			} else {
				ipR = WindowManager.getImage(idR).getProcessor().resize(previewWidth);
				ipRByte = ipR.convertToByte(true);
				((TextField) gd.getNumericFields().get(0)).setText(String.format("%.3f", ipR.getMin()));
				((TextField) gd.getNumericFields().get(1)).setText(String.format("%.3f", ipR.getMax()));
			}
			if (!checkNullImages())
				return false;
			impPreview = new ImagePlus("Preview", ipR.convertToRGB());
		} else if (e.getSource() == gd.getChoices().get(1)) {
			if (impPreview != null)
				impPreview.close();
			if (idG == 0) {
				ipG = null;
				ipGByte = null;
				((TextField) gd.getNumericFields().get(2)).setText("");
				((TextField) gd.getNumericFields().get(3)).setText("");
			} else {
				ipG = WindowManager.getImage(idG).getProcessor().resize(previewWidth);
				ipGByte = ipG.convertToByte(true);
				((TextField) gd.getNumericFields().get(2)).setText(String.format("%.3f", ipG.getMin()));
				((TextField) gd.getNumericFields().get(3)).setText(String.format("%.3f", ipG.getMax()));
			}
			if (!checkNullImages())
				return false;
			impPreview = new ImagePlus("Preview", ipR.convertToRGB());
		} else if (e.getSource() == gd.getChoices().get(2)) {
			if (impPreview != null)
				impPreview.close();
			if (idB == 0) {
				ipB = null;
				ipBByte = null;
				((TextField) gd.getNumericFields().get(4)).setText("");
				((TextField) gd.getNumericFields().get(5)).setText("");
			} else {
				ipB = WindowManager.getImage(idB).getProcessor().resize(previewWidth);
				ipBByte = ipB.convertToByte(true);
				((TextField) gd.getNumericFields().get(4)).setText(String.format("%.3f", ipB.getMin()));
				((TextField) gd.getNumericFields().get(5)).setText(String.format("%.3f", ipB.getMax()));
			}
			if (!checkNullImages())
				return false;
			impPreview = new ImagePlus("Preview", ipR.convertToRGB());
		}
		if (e.getSource() == gd.getNumericFields().get(0) || e.getSource() == gd.getNumericFields().get(1)) {
			if (Double.isNaN(minR) || Double.isNaN(maxR) || ipR == null)
				return false;
			ipR.setMinAndMax(minR, maxR);
			ipRByte = ipR.convertToByte(true);
		} else if (e.getSource() == gd.getNumericFields().get(2) || e.getSource() == gd.getNumericFields().get(3)) {
			if (Double.isNaN(minG) || Double.isNaN(maxG) || ipG == null)
				return false;
			ipG.setMinAndMax(minG, maxG);
			ipGByte = ipG.convertToByte(true);
		} else if (e.getSource() == gd.getNumericFields().get(4) || e.getSource() == gd.getNumericFields().get(5)) {
			if (Double.isNaN(minB) || Double.isNaN(maxB) || ipB == null)
				return false;
			ipB.setMinAndMax(minB, maxB);
			ipBByte = ipB.convertToByte(true);
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

		byte[] pixelsR = (byte[]) ipRByte.getPixels();
		byte[] pixelsG = (byte[]) ipGByte.getPixels();
		byte[] pixelsB = (byte[]) ipBByte.getPixels();

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
		ImageProcessor ipSrc = (ipRByte != null) ? ipRByte : ((ipGByte != null) ? ipGByte : ipBByte);
		if (ipSrc == null) {
			return false;
		}
		if (ipRByte == null) {
			ipRByte = getEmptyImageProcessor(ipSrc);
		}
		if (ipGByte == null) {
			ipGByte = getEmptyImageProcessor(ipSrc);
		}
		if (ipBByte == null) {
			ipBByte = getEmptyImageProcessor(ipSrc);
		}
		return true;
	}

	private boolean areSameSize() {
		return ipRByte.getWidth() == ipGByte.getWidth() && ipGByte.getWidth() == ipBByte.getWidth()
				&& ipRByte.getHeight() == ipGByte.getHeight() && ipGByte.getHeight() == ipBByte.getHeight();
	}

	private ImageProcessor getEmptyImageProcessor(ImageProcessor ipSrc) {
		int wid = ipSrc.getWidth();
		int hei = ipSrc.getHeight();
		byte[] pixels = new byte[wid * hei];
		return new ByteProcessor(wid, hei, pixels, null);
	}

}
