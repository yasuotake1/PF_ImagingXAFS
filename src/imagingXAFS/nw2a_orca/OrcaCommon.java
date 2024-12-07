package imagingXAFS.nw2a_orca;

import imagingXAFS.common.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.Raw;

public class OrcaCommon implements PlugIn {
	public static int dcmDirection = 0;// Down = 0, Up = 1.
	public static double detectorPosition = 30800;// from source point in mm.
	public static double pixelSize = 4.6;// in um.
	public static double dcmDistance = 25;// height distance between 1st and 2nd crystals in mm.
	public static int width = 2048;
	public static int height = 1024;
	public static int bitDepth = 16;

	public static String PropPath = "plugins/PF_ImagingXAFS/OrcaProps.config";
	public static final String[] LIST_BINNING = { "1", "2", "4", "8" };
	static String strImg = "";
	static String strRef = "";
	static String strDark = "100";
	static int modeDark = 0;// 0: No dark subtraction, 1: Dark subtraction by constDark, 2: Dark subtraction
							// by impDark
	private static ImagePlus impDark;
	private static int constDark = 0;
	static boolean avoidZero = false;
	static String strBinning = LIST_BINNING[0];
	static double ofsEne = 0.0;

	public void run(String arg) {
	}

	/**
	 * Reads parameters for loading ORCA images from the file specified in PropPath.
	 * 
	 * @return OrcaProps object contains parameters.
	 */
	public static OrcaProps readProps() {
		Properties prop = new Properties();

		OrcaProps target = new OrcaProps();
		try (InputStream is = new FileInputStream(new File(PropPath))) {
			prop.load(is);
			target.dcmDirection = Integer.parseInt(prop.getProperty("dcmDirection"));
			target.detectorPosition = Double.parseDouble(prop.getProperty("detectorPosition"));
			target.pixelSize = Double.parseDouble(prop.getProperty("pixelSize"));
			target.dcmDistance = Double.parseDouble(prop.getProperty("dcmDistance"));
			target.width = Integer.parseInt(prop.getProperty("width"));
			target.height = Integer.parseInt(prop.getProperty("height"));
			target.bitDepth = Integer.parseInt(prop.getProperty("bitDepth"));
		} catch (FileNotFoundException e) {
			target.dcmDirection = dcmDirection;
			target.detectorPosition = detectorPosition;
			target.pixelSize = pixelSize;
			target.dcmDistance = dcmDistance;
			target.width = width;
			target.height = height;
			target.bitDepth = bitDepth;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return target;
	}

	/**
	 * Writes parameters for loading ORCA images to the file specified in PropPath.
	 * 
	 * @param target OrcaProps object that contains parameters to be written.
	 */
	public static void writeProps(OrcaProps target) {
		Properties prop = new Properties();

		prop.setProperty("dcmDirection", String.valueOf(target.dcmDirection));
		prop.setProperty("detectorPosition", String.valueOf(target.detectorPosition));
		prop.setProperty("pixelSize", String.valueOf(target.pixelSize));
		prop.setProperty("dcmDistance", String.valueOf(target.dcmDistance));
		prop.setProperty("width", String.valueOf(target.width));
		prop.setProperty("height", String.valueOf(target.height));
		prop.setProperty("bitDepth", String.valueOf(target.bitDepth));

		try {
			Prefs.savePrefs(prop, PropPath);
		} catch (IOException e) {
			IJ.error("Failed to write properties.");
			return;
		}
	}

	/**
	 * Reads single ITEX file. Image width, heights, data offset, and bitdepth are
	 * read from heading 64 bytes. It subtracts constant or dark image set by
	 * modeDark if subtactDark=true.
	 * 
	 * @param path         Path specifying the .img file to be read.
	 * @param prop         OrcaProps object that contains loading parameters.
	 * @param subtractDark It subtracts dark image/counts predefined by
	 *                     setDark(String, OrcaProps).
	 * @return ImagePlus 2D image.
	 */
	public static ImagePlus loadOrca(String path, OrcaProps prop, boolean subtractDark) {
		Path p = Paths.get(path);
		FileInfo fi = new FileInfo();
		fi.intelByteOrder = true;
		try {
			if (!ImagingXAFSCommon.isExistingPath(path)) {
				return null;
			}
			if (prop == null)
				prop = new OrcaProps();
			byte[] buffer = readBytes(path, 0, 64);
			fi.width = prop.width = (buffer[4] & 0xff) + ((buffer[5] & 0xff) << 8);
			fi.height = prop.height = (buffer[6] & 0xff) + ((buffer[7] & 0xff) << 8);
			fi.longOffset = (long) ((buffer[2] & 0xff) + ((buffer[3] & 0xff) << 8) + 64);
			switch ((buffer[12] & 0xff) + ((buffer[13] & 0xff) << 8)) {
			case 0:
				prop.bitDepth = 8;
				fi.fileType = FileInfo.GRAY8;
				break;
			case 2:
				prop.bitDepth = 16;
				fi.fileType = FileInfo.GRAY16_UNSIGNED;
				break;
			case 3:
				prop.bitDepth = 32;
				fi.fileType = FileInfo.GRAY32_UNSIGNED;
				break;
			}
		} catch (IOException ex) {
//			IJ.error("Failed to load an ORCA image.");
			ImagingXAFSCommon.logStackTrace(ex);
			return null;
		}
		ImagePlus imp = Raw.open(path, fi);
		if (subtractDark) {
			switch (modeDark) {
			case 1:
				imp.getProcessor().add(-constDark);
				break;
			case 2:
				if (impDark != null && imp.getWidth() == impDark.getWidth() && imp.getHeight() == impDark.getHeight())
					ImageCalculator.run(imp, impDark, "subtract");
				break;
			default:
				break;
			}
		}
		if (avoidZero) {
			imp.getProcessor().add(-1);
			imp.getProcessor().add(1);
		}
		fi.directory = IJ.addSeparator(p.getParent().toString());
		fi.fileName = p.getFileName().toString();
		imp.setFileInfo(fi);
		return imp;
	}

	/**
	 * Reads binary file.
	 * 
	 * @param strPath
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public static byte[] readBytes(String strPath, int offset, int length) throws IOException {
		try (FileInputStream fis = new FileInputStream(strPath)) {
			byte[] buffer = new byte[length];
			fis.read(buffer, offset, length);
			return buffer;
		} catch (IOException ex) {
			throw ex;
		}
	}

	/**
	 * Sets the subtraction method in LoadOrca(String, OrcaProps, boolean). Constant
	 * dark subtraction is set when strDark could be parsed to an integer. If
	 * strDark specifies a path to dark image, it searches for up-to-ten dark files
	 * (*_dk[0-9].img), load, average, and stores in impDark. It returns modeDark,
	 * that is, 0: No dark subtraction, 1: Dark subtraction by constDark, 2: Dark
	 * subtraction by impDark.
	 * 
	 * @param _strDark
	 * @return
	 */
	public static int setDark(String _strDark) {
		impDark = null;
		constDark = 0;
		modeDark = 0;
		if (isInteger(_strDark)) {
			constDark = Integer.parseInt(_strDark);
			impDark = null;
			modeDark = constDark == 0 ? 0 : 1;
		} else if (ImagingXAFSCommon.isExistingPath(_strDark)) {
			if (_strDark.matches(".*_dk[0-9].img")) {
				String strDarkTry;
				int cnt = 0;
				ImagePlus imp;
				for (int i = 0; i < 10; i++) {
					strDarkTry = _strDark.substring(0, _strDark.length() - 8) + "_dk" + i + ".img";
					imp = loadOrca(strDarkTry, null, false);
					if (imp != null) {
						if (i == 0) {
							impDark = imp;
						} else {
							ImageCalculator.run(impDark, imp, "add");
							cnt++;
						}
					}
				}
				impDark.getProcessor().multiply(1d / cnt);
			} else {
				impDark = loadOrca(_strDark, null, false);
			}
			constDark = 0;
			modeDark = 2;
		}
		strDark = _strDark;
		return modeDark;
	}

	/**
	 * Returns corrected photon energy according to M. Katayama et al. (2015).
	 * Example: At y = 0, y0 = 512, ene = 7111.20, pixelSize = 4.6, DetectorPosition
	 * = 29500, DCMDistance = 25, corr = 0.004573 degree leads to correctedE =
	 * 7109.24 eV (difference = -1.96 eV).
	 * 
	 * @param y     Pixel position where the correction is calculated.
	 * @param y0    Correction center position, i.e., correctedE = ene at y = y0.
	 * @param ene   Nominal DCM energy in eV.
	 * @param prop  OrcaProps.
	 * @param calib Pixels size in Calibration is used if the unit is µm.
	 * @return Corrected photon energy in eV.
	 * @see <a href="https://doi.org/10.1107/S1600577515012990">M. Katayama et al.,
	 *      J. Synchrotron Rad. 22, 1227 (2015).</a>
	 */
	public static double getCorrectedE(double y, double y0, double ene, OrcaProps prop, Calibration calib) {
		double coefDCMDirection = -1;
		if (prop.dcmDirection == 1)
			coefDCMDirection = 1;
		double pixelSize = prop.pixelSize;
		if (calib.getUnit() == "µm" || calib.getUnit() == "um" || calib.getUnit() == "micron")
			pixelSize = calib.pixelHeight;
		double angle = ImagingXAFSCommon.EtoA(ene);
		double corr = coefDCMDirection * Math.atan(
				(y - y0) * pixelSize / 1000 / (prop.detectorPosition + prop.dcmDistance / Math.sin(angle / 90 * Math.PI)
						- prop.dcmDistance / Math.tan(angle / 90 * Math.PI)))
				/ Math.PI * 180;
		return ImagingXAFSCommon.AtoE(angle + corr);
	}

	/**
	 * Sets the calibration to an image by using the specified pixel size and
	 * binning.
	 * 
	 * @param imp
	 * @param prop OrcaProps object that contains pixel size.
	 * @param bin  Image binning applied when loading.
	 */
	public static void setCalibration(ImagePlus imp, OrcaProps prop, int bin) {
		Calibration calib = new Calibration();
		double pixelSize = prop.pixelSize;
		if (bin != 1) {
			pixelSize *= bin;
		}
		calib.pixelWidth = calib.pixelHeight = pixelSize;
		calib.setUnit("um");
		imp.setCalibration(calib);
	}

	/**
	 * Returns a deep copy of sourceProp.
	 * 
	 * @param sourceProp OrcaProps object.
	 * @return OrcaProps object.
	 */
	public static OrcaProps getDuplicatedProp(OrcaProps sourceProp) {
		OrcaProps prop = new OrcaProps();
		prop.bitDepth = sourceProp.bitDepth;
		prop.dcmDirection = sourceProp.dcmDirection;
		prop.dcmDistance = sourceProp.dcmDistance;
		prop.detectorPosition = sourceProp.detectorPosition;
		prop.height = sourceProp.height;
		prop.pixelSize = sourceProp.pixelSize;
		prop.width = sourceProp.width;
		return prop;
	}

	/**
	 * Returns number of pixels in one direction to be binned.
	 * 
	 * @return
	 */
	public static int getIntBinning() {
		try {
			return Integer.parseInt(strBinning);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	public static boolean isInteger(String value) {
		return value != null && !value.isEmpty() && value.matches("[0-9]+");
	}

	/**
	 * Returns a parent path string.
	 * 
	 * @return
	 */
	public static String getStrParent(String p) {
		return Paths.get(p).getParent().toString();
	}

	/**
	 * Returns a grandparent path string.
	 * 
	 * @return
	 */
	public static String getStrGrandParent(String p) {
		return Paths.get(p).getParent().getParent().toString();
	}

	/**
	 * Returns a path string without trailing index.
	 * 
	 * @return
	 */
	public static String removeTrailingIdx(String str) {
		while (Character.isDigit(str.charAt(str.length() - 1))) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	public static String getNextName(String name) {
		try {
			int idx = Integer.parseInt(name.substring(name.length() - 3));
			return OrcaCommon.removeTrailingIdx(name) + String.format("%03d", idx + 1);
		} catch (NumberFormatException | IndexOutOfBoundsException ex) {
			return OrcaCommon.removeTrailingIdx(name);
		}
	}

	public static String getNextPath(String path) {
		String name = Paths.get(path).getFileName().toString();
		return Paths.get(getStrGrandParent(path), getNextName(name), getNextName(name)).toString();
	}

	public static int getRepetition(String firstPath) {
		String str = firstPath;
		int rep = 0;
		while (ImagingXAFSCommon.isExistingPath(str)) {
			rep++;
			str = getNextPath(str);
		}
		return rep;
	}
}
