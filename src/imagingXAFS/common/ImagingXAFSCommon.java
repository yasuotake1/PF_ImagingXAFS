package imagingXAFS.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class ImagingXAFSCommon implements PlugIn {

	public static final double hc = 12398.52;
	public static final double SpacSi111 = 3.13551;
	public static final String keyEnergy = "Energies";
	public static final double thresInterp = 0.005;
	public static double[] normalizationParam = { 7080.0, 7105.0, 7140.0, 7310.0 };
	public static float e0Jump = 0.5F;
	public static double e0Min = 7116.0;
	public static double e0Max = 7124.0;

	public void run(String arg) {
	}

	/**
	 * Reads the photon energy list recorded in an string property associated with
	 * keyEnergy. Returns null if failed.
	 * 
	 * @param imp
	 * @return Array of photon energy
	 */
	public static double[] getPropEnergies(ImagePlus imp) {
		if (imp.getNSlices() < 2) {
			IJ.error("This is not an imagestack.");
			return null;
		}

		double[] energies;
		try {
			String[] tempEnergies = imp.getProp(keyEnergy).split(",", 1000);
			energies = new double[tempEnergies.length];
			for (int i = 0; i < energies.length; i++) {
				energies[i] = Double.parseDouble(tempEnergies[i]);
			}
			if (imp.getNSlices() != energies.length)
				throw new Exception();
		} catch (Exception e) {
			IJ.error("Energy is not correctly set.");
			return null;
		}
		return energies;
	}

	/**
	 * Writes the photon energy list as an string property associated with
	 * keyEnergy.
	 * 
	 * @param imp
	 * @param energies
	 */
	public static void setPropEnergies(ImagePlus imp, double[] energies) {
		String[] temp_energies = new String[energies.length];
		for (int i = 0; i < temp_energies.length; i++) {
			temp_energies[i] = String.format("%.2f", energies[i]);
		}
		imp.setProp(keyEnergy, String.join(",", temp_energies));
	}

	/**
	 * Reads the list of photon energy from a 9809 XAFS data file or comma-separated
	 * text.
	 * 
	 * @param path Path of the file
	 * @return Array of photon energy
	 */
	public static double[] readEnergies(Path path) {
		return readValues(path, true, 0);
	}

	/**
	 * Reads values from a 9809 XAFS data file or comma-separated text. This
	 * overload assumes I0 intensity recorded at column=3.
	 * 
	 * @param path Path of the file
	 * @return Array of values
	 */
	public static double[] readIntensities(Path path) {
		return readValues(path, false, 3);
	}

	/**
	 * Reads values from a 9809 XAFS data file or comma-separated text.
	 * 
	 * @param path   Path of the file
	 * @param column column to read
	 * @return Array of values
	 */
	public static double[] readIntensities(Path path, int column) {
		return readValues(path, false, column);
	}

	private static double[] readValues(Path path, boolean applyAtoEfor9809, int column) {
		List<String> rows = new ArrayList<String>();
		double[] values;
		if (path == null) {
			return null;
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(path.toString()));
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty())
					rows.add(line);
			}
			br.close();
			boolean is9809 = rows.get(0).trim().startsWith("9809");
			if (is9809) {
				do {
					rows.remove(0);
				} while (!(rows.get(0)).trim().startsWith("Offset"));
				values = new double[rows.size() - 1];
				for (int i = 0; i < values.length; i++) {
					values[i] = Double.parseDouble((rows.get(i + 1)).substring(column * 10, column * 10 + 10).trim());
				}
			} else {
				values = new double[rows.size() - 1];
				String[] arrSplit;
				for (int i = 0; i < values.length; i++) {
					arrSplit = rows.get(i + 1).split(",");
					values[i] = Double.parseDouble(arrSplit[column]);
				}
			}
			if (applyAtoEfor9809 && is9809) {
				for (int i = 0; i < values.length; i++) {
					values[i] = AtoE(values[i]);
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return null;
		}
		return values;
	}

	/**
	 * Calculates the photon energy from the DCM angle.
	 * 
	 * @param angle DCM angle in degree
	 * @return Photon energy in eV
	 */
	public static double AtoE(double angle) {
		return hc / (2 * SpacSi111 * Math.sin(angle / 180 * Math.PI));
	}

	/**
	 * Calculates the DCM angle from the photon energy.
	 * 
	 * @param ene Photon energy in eV
	 * @return DCM angle in degree
	 */
	public static double EtoA(double ene) {
		return Math.asin(hc / (2 * SpacSi111 * ene)) / Math.PI * 180;
	}

	/**
	 * Returns the index representing the position of target value in energies.
	 * Example: target = 7113 and energies = {7110, 7112, 7114} leads to index =
	 * 1.5.
	 * 
	 * @param target
	 * @param energies
	 * @return index
	 */
	public static double getInterpIndex(double target, double[] energies) {
		if (target < energies[0])
			return 0;
		if (target > energies[energies.length - 1])
			return energies.length - 1;

		int lowerIdx = 0;
		while (lowerIdx < energies.length - 1 && energies[lowerIdx + 1] < target) {
			lowerIdx++;
		}
		return (double) lowerIdx + (target - energies[lowerIdx]) / (energies[lowerIdx + 1] - energies[lowerIdx]);
	}

	/**
	 * Determines if the interpolation of two points is necessary.
	 * 
	 * @param idx
	 * @return
	 */
	public static boolean doInterp(double idx) {
		return Math.abs(idx - Math.round(idx)) > thresInterp;
	}

	public static int[] searchNormalizationIndices(double[] energy) {
		int[] indices = new int[4];
		boolean bNaN = false;
		for (int i = 0; i < 4; i++) {
			bNaN |= Double.isNaN(normalizationParam[i]);
		}
		if (bNaN) {
			IJ.error("Invalid pre-edge and post-edge region.");
			return null;
		}
		for (int i = 0; i < energy.length; i++) {
			if (indices[0] == 0 && energy[i] >= normalizationParam[0])
				indices[0] = i;
			if (indices[1] == 0 && energy[i] >= normalizationParam[1])
				indices[1] = i - 1;
			if (indices[2] == 0 && energy[i] >= normalizationParam[2])
				indices[2] = i;
			if (indices[3] == 0 && energy[i] >= normalizationParam[3])
				indices[3] = i - 1;
		}
		if (indices[0] >= indices[1] || indices[2] >= indices[3]) {
			IJ.error("Invalid pre-edge and post-edge region.");
			return null;
		}
		return indices;
	}

	/**
	 * Returns the ImageIDs of open stacks if stack = true. Works on 2D images if
	 * stack = false.
	 * 
	 * @param stack
	 * @return Array of image IDs
	 */
	public static Integer[] getDataIds(boolean stack) {
		int[] idOpenImages = WindowManager.getIDList();
		if (idOpenImages == null) {
			return new Integer[0];
		}

		List<Integer> listId = new ArrayList<Integer>();
		ImagePlus impTemp;
		for (int i = 0; i < idOpenImages.length; i++) {
			impTemp = WindowManager.getImage(idOpenImages[i]);
			if (impTemp.getBitDepth() != 24 && (impTemp.getNSlices() < 2 ^ stack))
				listId.add(impTemp.getID());
		}
		Integer[] arr = new Integer[listId.size()];
		listId.toArray(arr);
		return arr;
	}

	/**
	 * Returns the titles of open stacks if stack = true. Works on 2D images if
	 * stack = false.
	 * 
	 * @param stack
	 * @return Array of image titles
	 */
	public static String[] getDataTitles(boolean stack) {
		Integer[] arrId = getDataIds(stack);
		String[] arr = new String[arrId.length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = WindowManager.getImage(arrId[i]).getTitle();
		}
		return arr;
	}

	/**
	 * Returns the screen width. In a multi-monitor environment, this works on the
	 * display in which ImageJ menu bar is located.
	 * 
	 * @return Screen width
	 */
	public static int getCurrentScreenWidth() {
		return IJ.getInstance().getGraphicsConfiguration().getDevice().getDisplayMode().getWidth();
	}

	/**
	 * Returns the screen height. In a multi-monitor environment, this works on the
	 * display in which ImageJ menu bar is located.
	 * 
	 * @return Screen height
	 */
	public static int getCurrentScreenHeight() {
		return IJ.getInstance().getGraphicsConfiguration().getDevice().getDisplayMode().getHeight();
	}

}
