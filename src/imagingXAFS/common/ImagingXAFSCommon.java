package imagingXAFS.common;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class ImagingXAFSCommon implements PlugIn {

	public static final double HC = 12398.52;
	public static final double SPAC_SI111 = 3.13551;
	public static final double SPAC_SI220 = 1.92010;
	public static final double SPAC_SI311 = 1.63747;
	public static final double SPAC_SI511 = 1.04517;
	static double spacCrystal = SPAC_SI111;
	public static final String KEYENERGY = "Energies";
	public static final double THRESHOLD_INTERP = 0.005;
	public static double[] normalizationParam = { 7015.0, 7095.0, 7140.0, 7310.0 };
	public static float e0Jump = 0.5F;
	public static double e0Min = 7116.0;
	public static double e0Max = 7124.0;
	public static final Color[] LIST_PLOTCOLORS = { new Color(0x8b0000), new Color(0x8b8b00), new Color(0x008b00),
			new Color(0x008b8b), new Color(0x00008b), new Color(0x8b008b), Color.DARK_GRAY, Color.BLACK };
	private static final Pattern p1 = Pattern.compile(".*D\\=\\s+([0-9\\.]+)\\s+A.*");
	private static final Pattern p2 = Pattern.compile("\\s*Mono :\\s+([a-zA-Z0-9\\(\\)]+)\\s+.*");

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
			IJ.error("This is not an imagestack.\n" + imp.getTitle());
			return null;
		}

		double[] energies;
		try {
			String[] tempEnergies = imp.getProp(KEYENERGY).split(",", 9999);
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
	 * @param energies Array of photon energy
	 */
	public static void setPropEnergies(ImagePlus imp, double[] energies) {
		String[] temp_energies = new String[energies.length];
		for (int i = 0; i < temp_energies.length; i++) {
			temp_energies[i] = String.format("%.2f", energies[i]);
		}
		imp.setProp(KEYENERGY, String.join(",", temp_energies));
	}

	/**
	 * Reads the list of photon energy from a 9809 XAFS data file or comma-separated
	 * text.
	 * 
	 * @param path Path of the file
	 * @return Array of photon energy
	 */
	public static double[] readEnergies(String path) {
		return readValues(path, true, 0);
	}

	/**
	 * Reads values from a 9809 XAFS data file or comma-separated text. This
	 * overload assumes I0 intensity recorded at column=3.
	 * 
	 * @param path Path of the file
	 * @return Array of values
	 */
	public static double[] readIntensities(String path) {
		return readValues(path, false, 3);
	}

	/**
	 * Reads values from a 9809 XAFS data file or comma-separated text.
	 * 
	 * @param path   Path of the file
	 * @param column column to read
	 * @return Array of values
	 */
	public static double[] readIntensities(String path, int column) {
		return readValues(path, false, column);
	}

	public static List<String> linesFromFile(String path) throws IOException {
		try (Stream<String> lines = Files.lines(Paths.get(path))) {
			return lines.filter(s -> !s.isEmpty()).collect(Collectors.toList());
		} catch (IOException e) {
			throw e;
		}
	}

	private static double[] readValues(String path, boolean applyAtoEfor9809, int column) {
		double[] values;
		try {
			List<String> lines = linesFromFile(path);
			boolean is9809 = lines.get(0).trim().startsWith("9809");
			if (is9809) {
				do {
					setSpacCrystal(lines.get(0));
					lines.remove(0);
				} while (!(lines.get(0)).trim().startsWith("Offset"));
				lines.remove(0);
				values = new double[lines.size()];
				for (int i = 0; i < values.length; i++) {
					values[i] = Double.parseDouble((lines.get(i)).substring(column * 10, column * 10 + 10).trim());
				}
			} else if (path.endsWith(".nor")) {
				lines = lines.stream().filter(s -> !s.startsWith("#")).map(s -> s.trim()).collect(Collectors.toList());
				values = new double[lines.size()];
				for (int i = 0; i < lines.size(); i++) {
					values[i] = Double.parseDouble(lines.get(i).split("[,\\s]+")[column]);
				}
			} else {
				values = new double[lines.size() - 1];
				for (int i = 0; i < values.length; i++) {
					values[i] = Double.parseDouble(lines.get(i + 1).split("[,\\s]+")[column]);
				}
			}
			if (applyAtoEfor9809 && is9809) {
				for (int i = 0; i < values.length; i++) {
					values[i] = AtoE(values[i]);
				}
			}
		} catch (Exception e) {
			logStackTrace(e);
			return null;
		}
		return values;
	}

	/**
	 * Checks if strLine is a monochromator description of 9809 format. It first
	 * tries to read and store the crystal d spacing. If failed, it tries to find a
	 * name of crystal plane such as "Si(111)", and stores the pre-defined values.
	 * 
	 * @param strLine
	 * @return true if successfully stored the crystal d spacing.
	 */
	public static boolean setSpacCrystal(String strLine) {
		if (strLine.trim().startsWith("Mono :")) {
			Matcher m1 = p1.matcher(strLine);
			if (m1.matches()) {
				try {
					spacCrystal = Double.parseDouble(m1.group(1));
					return true;
				} catch (Exception e) {
				}
			}
			Matcher m2 = p2.matcher(strLine);
			if (m2.matches()) {
				switch (m2.group(1)) {
				case "Si(111)":
					spacCrystal = SPAC_SI111;
					return true;
				case "Si(220)":
					spacCrystal = SPAC_SI220;
					return true;
				case "Si(311)":
					spacCrystal = SPAC_SI311;
					return true;
				case "Si(511)":
					spacCrystal = SPAC_SI511;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Calculates the photon energy from the DCM angle.
	 * 
	 * @param angle DCM angle in degree
	 * @return Photon energy in eV
	 */
	public static double AtoE(double angle) {
		return HC / (2 * spacCrystal * Math.sin(angle / 180 * Math.PI));
	}

	/**
	 * Calculates the DCM angle from the photon energy.
	 * 
	 * @param ene Photon energy in eV
	 * @return DCM angle in degree
	 */
	public static double EtoA(double ene) {
		return Math.asin(HC / (2 * spacCrystal * ene)) / Math.PI * 180;
	}

	/**
	 * Returns the fractional index representing the position of target value in
	 * energies. Example: target = 7113 and energies = {7110, 7112, 7114} leads to
	 * index = 1.5.
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
		return Math.abs(idx - Math.round(idx)) > THRESHOLD_INTERP;
	}

	/**
	 * Loads a standard spectrum from strPath and returns an array of intensities by
	 * interpolating the spectrum according to energies.
	 * 
	 * @param strPath
	 * @param energies
	 * @return
	 */
	public static double[] getInterpolatedSpectrum(String strPath, double[] energies) {
		double[] arrEne = readEnergies(strPath);
		double[] arrInt = readIntensities(strPath, strPath.endsWith(".nor") ? 3 : 1);
		if (arrEne == null || arrInt == null) {
			IJ.error("Failed to load " + Paths.get(strPath).getFileName().toString() + ".");
			return null;
		}

		double[] arrIntInterp = new double[energies.length];
		double interpIdx;
		double ratio;
		for (int i = 0; i < arrIntInterp.length; i++) {
			interpIdx = getInterpIndex(energies[i], arrEne);
			if (doInterp(interpIdx)) {
				ratio = interpIdx - Math.floor(interpIdx);
				arrIntInterp[i] = arrInt[(int) interpIdx] * (1.0 - ratio) + arrInt[(int) interpIdx + 1] * ratio;
			} else {
				arrIntInterp[i] = arrInt[(int) (interpIdx + 0.5)];
			}
		}
		return arrIntInterp;
	}

	/**
	 * Searches for indices those specify pre- and post-edge regions based on
	 * normalizationParam in photon energy.
	 * 
	 * @param energy Array of photon energy
	 * @return indices[0], [1], [2], [3] are: Starting index of pre-edge region,
	 *         final of pre-edge, starting of post-edge, final of post-edge,
	 *         respectively.
	 */
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
			indices[0] = Math.abs(normalizationParam[0] - energy[i]) < Math
					.abs(normalizationParam[0] - energy[indices[0]]) ? i : indices[0];
			indices[1] = Math.abs(normalizationParam[1] - energy[i]) < Math
					.abs(normalizationParam[1] - energy[indices[1]]) ? i : indices[1];
			indices[2] = Math.abs(normalizationParam[2] - energy[i]) < Math
					.abs(normalizationParam[2] - energy[indices[2]]) ? i : indices[2];
			indices[3] = Math.abs(normalizationParam[3] - energy[i]) < Math
					.abs(normalizationParam[3] - energy[indices[3]]) ? i : indices[3];
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

	double getCloserNumber(double target, double candidate1, double candidate2) {
		return Math.abs(target - candidate1) > Math.abs(target - candidate2) ? candidate2 : candidate1;
	}

	public static boolean isExistingPath(String path) {
		return path != null && !path.isEmpty() && Files.exists(Paths.get(path));
	}

	public static void logStackTrace(Exception e) {
		(new ImageJ.ExceptionHandler()).handle(e);
	}
}
