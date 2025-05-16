package imagingXAFS.common;

import ij.ImagePlus;
import ij.WindowManager;

public class Cosine_Similarity {
	public static String get(String arg) {
		String[] args = arg.split(" ");
		ImagePlus impSrc = WindowManager.getImage(args[0]);
		ImagePlus impTgt = WindowManager.getImage(args[1]);
		if (impSrc == null || impTgt == null)
			return "";
		int type = impSrc.getType();
		if (type != impTgt.getType())
			return "";
		switch (type) {
		case ImagePlus.GRAY8:
			return String.valueOf(cosineSimilarity8bit(impSrc, impTgt));
		case ImagePlus.GRAY16:
			return String.valueOf(cosineSimilarity16bit(impSrc, impTgt));
		case ImagePlus.GRAY32:
			return String.valueOf(cosineSimilarity32bit(impSrc, impTgt));
		default:
			return "";
		}
	}

	public static double cosineSimilarity8bit(ImagePlus imp1, ImagePlus imp2) {
		byte[] arr1 = (byte[]) imp1.getProcessor().getPixels();
		byte[] arr2 = (byte[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		double arr1d, arr2d;
		for (int i = 0; i < arr1.length; i++) {
			arr1d = arr1[i] & 255;
			arr2d = arr2[i] & 255;
			temp0 += arr1d * arr2d;
			temp1 += arr1d * arr1d;
			temp2 += arr2d * arr2d;
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}

	public static double cosineSimilarity16bit(ImagePlus imp1, ImagePlus imp2) {
		short[] arr1 = (short[]) imp1.getProcessor().getPixels();
		short[] arr2 = (short[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		double arr1d, arr2d;
		for (int i = 0; i < arr1.length; i++) {
			arr1d = arr1[i] & 0xffff;
			arr2d = arr2[i] & 0xffff;
			temp0 += arr1d * arr2d;
			temp1 += arr1d * arr1d;
			temp2 += arr2d * arr2d;
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}

	public static double cosineSimilarity32bit(ImagePlus imp1, ImagePlus imp2) {
		float[] arr1 = (float[]) imp1.getProcessor().getPixels();
		float[] arr2 = (float[]) imp2.getProcessor().getPixels();
		if (arr1.length != arr2.length)
			return 0.0;

		double temp0 = 0.0, temp1 = 0.0, temp2 = 0.0;
		for (int i = 0; i < arr1.length; i++) {
			temp0 += arr1[i] * arr2[i];
			temp1 += arr1[i] * arr1[i];
			temp2 += arr2[i] * arr2[i];
		}
		return (double) temp0 / Math.sqrt(temp1) / Math.sqrt(temp2);
	}
}
