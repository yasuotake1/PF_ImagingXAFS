package imagingXAFS.common;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.gui.MessageDialog;
import ij.plugin.PlugIn;

public class Stitching_Complement implements PlugIn {

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Complement tile positions");
		gd.addFileField("Configuration_file", "");
		gd.addNumericField("Grid_size_X", 2, 0);
		gd.addNumericField("Grid_size_Y", 2, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strPath = gd.getNextString();
		int sizeX = (int) gd.getNextNumber();
		int sizeY = (int) gd.getNextNumber();

		try {
			List<String> lines = Files.readAllLines(Paths.get(strPath));
			String[] entries, points;
			String str;
			List<String> listNames = new ArrayList<String>();
			List<Float> listPosX = new ArrayList<Float>();
			List<Float> listPosY = new ArrayList<Float>();
			for (int i = 0; i < lines.size(); i++) {
				entries = lines.get(i).split(";");
				if (entries.length == 3) {
					str = entries[2].trim();
					if (str.startsWith("(") && str.endsWith(")")) {
						listNames.add(entries[0].trim());
						points = str.substring(1, str.length() - 1).split(",");
						listPosX.add(Float.parseFloat(points[0]));
						listPosY.add(Float.parseFloat(points[1]));
					}
				}
			}
			if (sizeX < 3 || sizeY < 3 || listNames.size() != sizeX * sizeY) {
				// Show message without aborting.
				// IJ.error("Invalid grid size.");
				MessageDialog md = new MessageDialog(IJ.getInstance(), "Complement tile positions",
						"Invalid grid size.");
				if (md != null)
					md.dispose();
				return;
			}
			float[] arrStepX = new float[(sizeX - 1) * sizeY];
			for (int j = 0; j < sizeY; j++) {
				for (int i = 0; i < sizeX - 1; i++) {
					arrStepX[j * (sizeX - 1) + i] = listPosX.get(j * sizeX + i + 1) - listPosX.get(j * sizeX + i);
				}
			}
			float medianStepX = getMedian(arrStepX);
			float[] arrStepY = new float[sizeX * (sizeY - 1)];
			for (int j = 0; j < sizeX; j++) {
				for (int i = 0; i < sizeY - 1; i++) {
					arrStepY[j * (sizeY - 1) + i] = listPosY.get(i * sizeX + j + sizeX) - listPosY.get(i * sizeX + j);
				}
			}
			float medianStepY = getMedian(arrStepY);
			List<Boolean> listFailure = new ArrayList<Boolean>();
			boolean b;
			for (int i = 0; i < listNames.size(); i++) {
				b = false;
				if (listPosX.get(i) == 0.0 && listPosY.get(i) == 0.0) {
					int x = i % sizeX;
					b = (x == 0 || checkOutOfRange(i, i - 1, listPosX, medianStepX))
							&& (x == sizeX - 1 || checkOutOfRange(i + 1, i, listPosX, medianStepX));
					int y = i / sizeX;
					b = b || ((y == 0 || checkOutOfRange(i, i - sizeX, listPosY, medianStepY))
							&& (y == sizeY - 1 || checkOutOfRange(i + sizeX, i, listPosY, medianStepY)));
				}
				listFailure.add(b);
			}
			List<Boolean> listFailureNew = new ArrayList<>(listFailure);
			b = true;
			while (b) {
				b = false;
				for (int i = 0; i < listFailureNew.size(); i++) {
					if (listFailureNew.get(i)) {
						boolean b1 = false;
						int x = i % sizeX;
						float guessOffsetX = 0;
						if (x > 0 && !listFailureNew.get(i - 1)) {
							guessOffsetX = listPosX.get(i - 1) + medianStepX;
							b1 = true;
						} else if (x < sizeX - 1 && !listFailureNew.get(i + 1)) {
							guessOffsetX = listPosX.get(i + 1) - medianStepX;
							b1 = true;
						}
						int y = i / sizeX;
						boolean b2 = false;
						float guessOffsetY = 0;
						if (y > 0 && !listFailureNew.get(i - sizeX)) {
							guessOffsetY = listPosY.get(i - sizeX) + medianStepY;
							b2 = true;
						} else if (y < sizeY - 1 && !listFailureNew.get(i + sizeX)) {
							guessOffsetY = listPosY.get(i + sizeX) - medianStepY;
							b2 = true;
						}
						if (b1 && b2) {
							listPosX.set(i, (float) Math.round(guessOffsetX));
							listPosY.set(i, (float) Math.round(guessOffsetY));
							listFailureNew.set(i, false);
							b = true;
						}
					}
				}
			}
			b = false;
			String log = "Corrected tile position(s) ";
			for (int i = 0; i < listFailure.size(); i++) {
				if (listFailure.get(i) && !listFailureNew.get(i)) {
					log += String.format("#%03d ", i);
					b = true;
				}
			}
			log += String.format(" by using median step size (%.1f, %.1f).", medianStepX, medianStepY);
			if (b) {
				IJ.log(log);
				String output = "";
				for (int i = 0; i < lines.size(); i++) {
					str = lines.get(i);
					for (int j = 0; j < listNames.size(); j++) {
						if (str.startsWith(listNames.get(j))) {
							if (listFailure.get(j) && !listFailureNew.get(j)) {
								str = str.substring(0, str.indexOf("("))
										+ String.format("(%.1f, %.1f)", listPosX.get(j), listPosY.get(j));
							}
							break;
						}
					}
					output += str + System.lineSeparator();
				}
				FileWriter fw = new FileWriter(strPath);
				fw.write(output);
				fw.close();
			}
		} catch (Exception ex) {
			IJ.error("Failed to read/write configuration file.");
			return;
		}
	}

	private float getMedian(float[] arrSrc) {
		float[] arr = Arrays.copyOf(arrSrc, arrSrc.length);
		Arrays.sort(arr);
		int middle = arr.length / 2;
		return (arr.length % 2 > 0) ? arr[middle] : (arr[middle] + arr[middle - 1]) / 2;
	}

	private boolean checkOutOfRange(int idx1, int idx2, List<Float> list, float guess) {
		return Math.abs(list.get(idx1) - list.get(idx2) - guess) > Math.abs(guess / 2);
	}
}
