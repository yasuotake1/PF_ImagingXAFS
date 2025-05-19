package imagingXAFS.common;

import java.awt.AWTEvent;
import java.awt.TextField;

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
	ImageProcessor ipE0, ipE0RGB, ipDmut, ipDmutByte;
	ImagePlus impPreview = null;
	static int previewWidth = 600;
	static String strBg = choiceBg[0];
	static double gamma = 1.0;

	public void run(String arg) {
		boolean macroMode = arg.equalsIgnoreCase("macro");
		String nameResult = "";
		double minE0, maxE0, minDmut, maxDmut;

		if (macroMode) {
			GenericDialog gd = new GenericDialog("Make E0 @Color - Dmut @alpha channel image");
			gd.addStringField("E0Image", "");
			gd.addNumericField("E0Min", 0.0);
			gd.addNumericField("E0Max", 1.0);
			gd.addStringField("DmutImage", "");
			gd.addNumericField("DmutMin", 0.0);
			gd.addNumericField("DmutMax", 1.0);
			gd.addRadioButtonGroup("Background", choiceBg, 1, 2, choiceBg[0]);
			gd.addSlider("Gamma", 0.5, 2, 1.0, 0.05);
			gd.showDialog();

			nameResult = gd.getNextString();
			ipE0 = WindowManager.getImage(nameResult).getProcessor();
			minE0 = gd.getNextNumber();
			maxE0 = gd.getNextNumber();
			ipDmut = WindowManager.getImage(gd.getNextString()).getProcessor();
			minDmut = gd.getNextNumber();
			maxDmut = gd.getNextNumber();
			strBg = gd.getNextRadioButton();
			gamma = gd.getNextNumber();
		} else {
			previewWidth = ImagingXAFSCommon.getCurrentScreenWidth() / 3;

			listId = ImagingXAFSCommon.getDataIds(false);
			if (listId.length < 2) {
				IJ.error("Could not find data image(s).");
				return;
			}
			String[] listTitle = ImagingXAFSCommon.getDataTitles(false);
			ipE0 = WindowManager.getImage(listId[0]).getProcessor().resize(previewWidth);
			ipE0RGB = ipE0.convertToRGB();
			ipDmut = WindowManager.getImage(listId[1]).getProcessor().resize(previewWidth);
			ipDmutByte = ipDmut.convertToByte(true);
			impPreview = new ImagePlus("Preview", ipE0RGB.duplicate());

			GenericDialog gd = new GenericDialog("Make E0 @Color - Dmut @alpha channel image");
			gd.addChoice("E0: Image", listTitle, listTitle[0]);
			gd.addNumericField("Display range minimum", ipE0.getMin(), 2, 8, "eV");
			gd.addNumericField("Maximum", ipE0.getMax(), 2, 8, "eV");
			gd.addChoice("Dmut: Image", listTitle, listTitle[1]);
			gd.addNumericField("Display range minimum", ipDmut.getMin(), 3);
			gd.addNumericField("Maximum", ipDmut.getMax(), 3);
			gd.addRadioButtonGroup("Background", choiceBg, 1, 2, choiceBg[0]);
			gd.addSlider("Gamma", 0.5, 2, 1.0, 0.05);
			gd.addCheckbox("Preview", false);
			gd.addDialogListener(this);
			gd.showDialog();
			if (impPreview.isVisible())
				impPreview.close();
			if (gd.wasCanceled())
				return;

			int idE0 = listId[gd.getNextChoiceIndex()];
			minE0 = gd.getNextNumber();
			maxE0 = gd.getNextNumber();
			int idDmut = listId[gd.getNextChoiceIndex()];
			minDmut = gd.getNextNumber();
			maxDmut = gd.getNextNumber();
			strBg = gd.getNextRadioButton();
			gamma = gd.getNextNumber();

			ipE0 = WindowManager.getImage(idE0).getProcessor();
			ipDmut = WindowManager.getImage(idDmut).getProcessor();
			nameResult = WindowManager.getImage(idE0).getTitle();
		}

		if (!isSameSize()) {
			IJ.error("E0 and Dmut images have different size.");
			return;
		}
		if (Double.isNaN(minE0) || Double.isNaN(maxE0) || Double.isNaN(minDmut) || Double.isNaN(maxDmut)) {
			IJ.error("Invalid display range.");
			return;
		}

		IJ.log("Making E0 @Color - Dmut @alpha channel image...");
		IJ.log("    E0: " + String.format("%.2f", minE0) + " - " + String.format("%.2f", maxE0) + " eV");
		IJ.log("    Dmut: " + String.format("%.3f", minDmut) + " - " + String.format("%.3f", maxDmut));
		ipE0.setMinAndMax(minE0, maxE0);
		ipE0RGB = ipE0.convertToRGB();
		ipDmut.setMinAndMax(minDmut, maxDmut);
		ipDmutByte = ipDmut.convertToByte(true);
		if (nameResult.endsWith(".tif"))
			nameResult = nameResult.replace(".tif", "_" + strBg);
		else
			nameResult += "_" + strBg;
		ImagePlus impResult = new ImagePlus(nameResult, ipE0RGB.duplicate());
		setAlphaImagePixels(gamma, strBg == choiceBg[0], (int[]) impResult.getProcessor().getPixels());
		impResult.show();

		if (!macroMode) {
			IJ.log("Saving...");
			IJ.saveAs(impResult, "png", null);
			IJ.log("\\Update:Saving...done.");
		}
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		if (gd.wasCanceled())
			return false;
		else if (gd.wasOKed())
			return true;

		int idE0 = listId[gd.getNextChoiceIndex()];
		int idDmut = listId[gd.getNextChoiceIndex()];
		if (e.getSource() == gd.getChoices().get(0)) {
			if (impPreview != null)
				impPreview.close();
			ipE0 = WindowManager.getImage(idE0).getProcessor().resize(previewWidth);
			ipE0RGB = ipE0.convertToRGB();
			impPreview = new ImagePlus("Preview", ipE0RGB.duplicate());
			((TextField) gd.getNumericFields().get(0)).setText(String.format("%.3f", ipE0.getMin()));
			((TextField) gd.getNumericFields().get(1)).setText(String.format("%.3f", ipE0.getMax()));
		} else if (e.getSource() == gd.getChoices().get(1)) {
			ipDmut = WindowManager.getImage(idDmut).getProcessor().resize(previewWidth);
			ipDmutByte = ipDmut.convertToByte(true);
			((TextField) gd.getNumericFields().get(2)).setText(String.format("%.3f", ipDmut.getMin()));
			((TextField) gd.getNumericFields().get(3)).setText(String.format("%.3f", ipDmut.getMax()));
		}
		double minE0 = gd.getNextNumber();
		double maxE0 = gd.getNextNumber();
		double minDmut = gd.getNextNumber();
		double maxDmut = gd.getNextNumber();
		if (Double.isNaN(minE0) || Double.isNaN(maxE0) || Double.isNaN(minDmut) || Double.isNaN(maxDmut))
			return false;
		if (e.getSource() == gd.getNumericFields().get(0) || e.getSource() == gd.getNumericFields().get(1)) {
			ipE0.setMinAndMax(minE0, maxE0);
			ipE0RGB = ipE0.convertToRGB();
		} else if (e.getSource() == gd.getNumericFields().get(2) || e.getSource() == gd.getNumericFields().get(3)) {
			ipDmut.setMinAndMax(minDmut, maxDmut);
			ipDmutByte = ipDmut.convertToByte(true);
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
		int[] pixelsValue = (int[]) ipE0RGB.getPixels();
		byte[] pixelsAlpha = (byte[]) ipDmutByte.getPixels();

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
