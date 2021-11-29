package imagingXAFS.nw2a_ultra;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.measure.Calibration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;

/**
 * A plugin for loading xrm files into ImageJ. These are proprietary files for
 * Zeiss Xradia XRay microscopes.
 *
 * @author Michael Sutherland with additions by Brian Metscher, July 2018 and
 *         modified by Yasuo Takeichi, November 2021.
 */
public class XRM_Reader implements PlugIn {

	private static int width, height, numberOfImages, type, binning;
	private static float pixelSize, OpticalMagnification, XrayMagnification, expTime;

	// -- Constants --
	private static final int FLOAT_TYPE = 10;
	private static final int INT16_TYPE = 5;
	private static final int UCHAR_TYPE = 3;

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Load XRM", "", "");
		String path = od.getPath();
		if (path != null) {
			ImagePlus imp = Load(path);
			if (imp == null)
				IJ.error("Error reading XRM file: " + path);
			else
				imp.show();
		}
	}

	public static ImagePlus Load(String path) {
		return Load(path, false);
	}

	public static ImagePlus Load(String path, boolean skipReadInfo) {
		FileInfo fi = new FileInfo();
		Path p = Paths.get(path);
		fi.directory = IJ.addSeparator(p.getParent().toString());
		fi.fileName = p.getFileName().toString();
		fi.intelByteOrder = true;
		try (InputStream inputStream = new FileInputStream(path);
				POIFSFileSystem fs = new POIFSFileSystem(inputStream)) {
			// read some parameters from the file
			DirectoryEntry root = fs.getRoot();
			DirectoryEntry imageInfo;
			DocumentEntry document;
			DocumentInputStream stream;
			if (!skipReadInfo) {
				imageInfo = (DirectoryEntry) root.getEntry("ImageInfo");
				// width
				document = (DocumentEntry) imageInfo.getEntry("ImageWidth");
				stream = new DocumentInputStream(document);
				width = stream.readInt();
				fi.width = width;
				stream.close();
				// height
				document = (DocumentEntry) imageInfo.getEntry("ImageHeight");
				stream = new DocumentInputStream(document);
				height = stream.readInt();
				fi.height = height;
				stream.close();
				// data type
				document = (DocumentEntry) imageInfo.getEntry("DataType");
				stream = new DocumentInputStream(document);
				type = stream.readInt();
				stream.close();
				// number of images
				document = (DocumentEntry) imageInfo.getEntry("NoOfImages");
				stream = new DocumentInputStream(document);
				numberOfImages = stream.readInt();
				stream.close();
				// pixel size
				document = (DocumentEntry) imageInfo.getEntry("PixelSize");
				stream = new DocumentInputStream(document);
				pixelSize = Float.intBitsToFloat(stream.readInt());
				stream.close();
				// binning
				document = (DocumentEntry) imageInfo.getEntry("HorizontalBin");
				stream = new DocumentInputStream(document);
				binning = stream.readInt();
				stream.close();
				// optical magnification
				document = (DocumentEntry) imageInfo.getEntry("OpticalMagnification");
				stream = new DocumentInputStream(document);
				OpticalMagnification = Float.intBitsToFloat(stream.readInt());
				stream.close();
				// x-ray magnification
				document = (DocumentEntry) imageInfo.getEntry("XrayMagnification");
				stream = new DocumentInputStream(document);
				XrayMagnification = Float.intBitsToFloat(stream.readInt());
				stream.close();
				// exposure time
				document = (DocumentEntry) imageInfo.getEntry("ExpTimes");
				stream = new DocumentInputStream(document);
				expTime = Float.intBitsToFloat(stream.readInt());
				stream.close();

				if (type == FLOAT_TYPE) {
					fi.fileType = FileInfo.GRAY32_FLOAT;
				}
				if (type == INT16_TYPE) {
					fi.fileType = FileInfo.GRAY16_UNSIGNED;
				}
				if (type == UCHAR_TYPE) {
					fi.fileType = FileInfo.GRAY8;
				}
			}
			// actual data
			// create stack even if there is only a single image (will open correctly).
			ImageStack stack = new ImageStack(width, height, numberOfImages);
			// NOTE: ImageStack and XRM directory naming are both one indexed
			for (int imageNumber = 1; imageNumber <= numberOfImages; imageNumber++) {
				try {
					// NOTE: ImageData# increments every 100
					imageInfo = (DirectoryEntry) root.getEntry("ImageData" + ((int) Math.ceil(imageNumber / 100.0)));
					document = (DocumentEntry) imageInfo.getEntry("Image" + (imageNumber));
					stream = new DocumentInputStream(document);
					ImageProcessor proc = null;
					if (type == FLOAT_TYPE) {
						// float
						float[] data = new float[width * height];
						for (int i = 0; i < width * height; i++) {
							byte[] bytes = new byte[4]; // space to store float
							stream.read(bytes);
							data[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
						}
						proc = new FloatProcessor(width, height, data, null);
					} else if (type == INT16_TYPE) {
						short[] data = new short[width * height];
						for (int i = 0; i < width * height; i++) {
							data[i] = stream.readShort();
						}
						proc = new ShortProcessor(width, height, data, null);
					} else if (type == UCHAR_TYPE) {
						byte[] data = new byte[width * height];
						for (int i = 0; i < width * height; i++) {
							data[i] = stream.readByte();
						}
						proc = new ByteProcessor(width, height, data, null);
					} else {
					}
					stream.close();
					if (proc == null)
						throw new IOException();
					stack.setProcessor(proc, imageNumber);
				} catch (IOException e) {
					return null;
				}
				IJ.showProgress(imageNumber, numberOfImages);
			}

			ImagePlus imp = new ImagePlus(fi.fileName, stack);
			imp.setFileInfo(fi);
			Calibration calib = new Calibration();
			calib.pixelWidth = calib.pixelHeight = pixelSize;
			calib.setUnit("um");
			imp.setCalibration(calib);
			imp.setProp("Binning", binning);
			imp.setProp("OpticalMagnification", OpticalMagnification);
			imp.setProp("XrayMagnification", XrayMagnification);
			imp.setProp("ExposureTime", expTime);

			return imp;
		} catch (Exception e) {
			IJ.log(e.getMessage());
			return null;
		}
	}

}
