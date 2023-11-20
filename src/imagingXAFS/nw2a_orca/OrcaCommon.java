package imagingXAFS.nw2a_orca;

import imagingXAFS.common.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileInfo;
import ij.measure.Calibration;
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
	public static final String[] strBinning = { "1", "2", "4", "8" };

	public void run(String arg) {
	}

	public static OrcaProps ReadProps() {
		Properties prop = new Properties();

		InputStream is;
		OrcaProps target = new OrcaProps();
		try {
			is = new FileInputStream(new File(PropPath));
			prop.load(is);
			is.close();

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

	public static void WriteProps(OrcaProps target) {
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

	public static ImagePlus LoadOrca(Path path, OrcaProps prop) {
		FileInfo fi = new FileInfo();
		fi.intelByteOrder = true;
		try {
			if (!Files.exists(path)) {
				return null;
			}
			byte[] buffer = readBytes(path.toString(), 0, 64);
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
			IJ.error("Failed to load an ORCA-Flash image.");
			return null;
		}
		ImagePlus imp = Raw.open(path.toString(), fi);
		fi.directory = IJ.addSeparator(path.getParent().toString());
		fi.fileName = path.getFileName().toString();
		imp.setFileInfo(fi);
		return imp;
	}

	public static byte[] readBytes(String strPath, int offset, int length) throws IOException {
		try {
			byte[] buffer = new byte[length];
			InputStream is = new FileInputStream(strPath);
			is.skip(offset);
			is.read(buffer, 0, length);
			is.close();
			return buffer;
		} catch (IOException ex) {
			throw ex;
		}
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
	
	public static OrcaProps getDuplicatedProp(OrcaProps sourceProp) {
		OrcaProps prop=new OrcaProps();
		prop.bitDepth=sourceProp.bitDepth;
		prop.dcmDirection=sourceProp.dcmDirection;
		prop.dcmDistance=sourceProp.dcmDistance;
		prop.detectorPosition=sourceProp.detectorPosition;
		prop.height=sourceProp.height;
		prop.pixelSize=sourceProp.pixelSize;
		prop.width=sourceProp.width;
		return prop;
	}
}
