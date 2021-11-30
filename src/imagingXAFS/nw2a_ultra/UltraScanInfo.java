package imagingXAFS.nw2a_ultra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import ij.plugin.PlugIn;
import ij.IJ;

public class UltraScanInfo implements PlugIn {

	public int version;
	public boolean energy;
	public boolean tomo;
	public boolean mosaic;
	public int multiExposure;
	public int nRepeatScan;
	public int waitNSecs;
	public int nExposures;
	public boolean averageOnTheFly;
	public int refNExposures;
	public boolean refForEveryExposures;
	public boolean refABBA;
	public boolean refAverageOnTheFly;
	public int mosaicUp;
	public int mosaicDown;
	public int mosaicLeft;
	public int mosaicRight;
	public double mosaicOverlap;
	public boolean mosaicCentralFile;
	public String directory;
	public String[] allFiles;
	public String[] imageFiles;
	public String[] referenceFiles;
	public double[] energies;
	public double[] angles;
	public int numMosaicX;
	public int numMosaicY;

	public UltraScanInfo(String path) throws IOException, NumberFormatException, IndexOutOfBoundsException {
		Path p = Paths.get(path);
		directory = IJ.addSeparator(p.getParent().toString());
		String[] lines = Files.lines(p).toArray(String[]::new);
		String[] elements;
		int idx = 0;
		forVariables: for (int i = 0; i < lines.length; i++) {
			elements = lines[i].split("\s");
			switch (elements[0]) {
			case "VERSION":
				version = Integer.parseInt(elements[1]);
				break;
			case "ENERGY":
				energy = Integer.parseInt(elements[1]) > 0;
				break;
			case "TOMO":
				tomo = Integer.parseInt(elements[1]) > 0;
				break;
			case "MOSAIC":
				mosaic = Integer.parseInt(elements[1]) > 0;
				break;
			case "MULTIEXPOSURE":
				multiExposure = Integer.parseInt(elements[1]);
				break;
			case "NREPEATSCAN":
				nRepeatScan = Integer.parseInt(elements[1]);
				break;
			case "WAITNSECS":
				waitNSecs = Integer.parseInt(elements[1]);
				break;
			case "NEXPOSURES":
				nExposures = Integer.parseInt(elements[1]);
				break;
			case "AVERAGEONTHEFLY":
				averageOnTheFly = Integer.parseInt(elements[1]) > 0;
				break;
			case "REFNEXPOSURES":
				refNExposures = Integer.parseInt(elements[1]);
				break;
			case "REF4EVERYEXPOSURES":
				refForEveryExposures = Integer.parseInt(elements[1]) > 0;
				break;
			case "REFABBA":
				refABBA = Integer.parseInt(elements[1]) > 0;
				break;
			case "REFAVERAGEONTHEFLY":
				refAverageOnTheFly = Integer.parseInt(elements[1]) > 0;
				break;
			case "MOSAICUP":
				mosaicUp = Integer.parseInt(elements[1]);
				break;
			case "MOSAICDOWN":
				mosaicDown = Integer.parseInt(elements[1]);
				break;
			case "MOSAICLEFT":
				mosaicLeft = Integer.parseInt(elements[1]);
				break;
			case "MOSAICRIGHT":
				mosaicRight = Integer.parseInt(elements[1]);
				break;
			case "MOSAICOVERLAP":
				mosaicOverlap = Double.parseDouble(elements[1]);
				break;
			case "MOSAICCENTRALFILE":
				mosaicCentralFile = Integer.parseInt(elements[1]) > 0;
				break;
			case "FILES":
				idx = i;
				break forVariables;
			default:
				break;
			}
		}

		ArrayList<String> listAllFiles = new ArrayList<String>();
		for (int i = idx + 1; i < lines.length; i++) {
			if (lines[i].endsWith(".xrm")) {
				listAllFiles.add(lines[i]);
			}
		}
		allFiles = listAllFiles.toArray(new String[listAllFiles.size()]);

		ArrayList<String> listImageFiles = new ArrayList<String>();
		ArrayList<String> listReferenceFiles = new ArrayList<String>();
		for (int i = 0; i < allFiles.length; i++) {
			if (allFiles[i].contains("_ref_"))
				listReferenceFiles.add(allFiles[i]);
			else
				listImageFiles.add(allFiles[i]);
		}
		imageFiles = listImageFiles.toArray(new String[listImageFiles.size()]);
		referenceFiles = listReferenceFiles.toArray(new String[listReferenceFiles.size()]);

		energies = energy ? getDistinctArray("eV", imageFiles) : null;
		angles = tomo ? getDistinctArray("Degree", imageFiles) : null;
		numMosaicX = mosaic ? (mosaicLeft + mosaicRight + (mosaicCentralFile ? 1 : 0)) : 1;
		numMosaicY = mosaic ? (mosaicUp + mosaicDown + (mosaicCentralFile ? 1 : 0)) : 1;
	}

	public void run(String arg) {
	}

	private double[] getDistinctArray(String key, String[] source) {
		ArrayList<Double> list = new ArrayList<Double>();
		double d1 = getNumberBeforeTheKey(key, source[0]);
		double d2 = d1;
		list.add(d1);
		for (int i = 1; i < source.length; i++) {
			d1 = getNumberBeforeTheKey(key, source[i]);
			if (d1 != d2) {
				list.add(d1);
				d2 = d1;
			}
		}
		double[] target = new double[list.size()];
		for (int i = 0; i < target.length; i++) {
			target[i] = list.get(i);
		}
		return target;
	}

	private double getNumberBeforeTheKey(String key, String filename) {
		String[] arr = filename.split("_");
		double d = Double.NaN;
		for (int i = 1; i < arr.length; i++) {
			if (arr[i].equals(key)) {
				try {
					d = Double.parseDouble(arr[i - 1]);
				} catch (Exception e) {
				}
				break;
			}
		}
		return d;
	}

}
