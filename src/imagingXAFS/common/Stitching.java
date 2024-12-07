package imagingXAFS.common;

import java.io.FileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Stitching implements PlugIn {

	public static final String[] CHOICEORDER = { "Right & Down", "Left & Down", "Right & Up", "Left & Up" };
	private String order;
	public int sizeX;
	public int sizeY;
	private double overlap;
	private String dir = "";
	public String output1 = "TileConfiguration.txt";
	public String output2 = "TileConfiguration.registered.txt";
	private String firstFilePath = "";

	public void run(String arg) {
	}

	public Stitching() {
	}

	public boolean showDialog(int num) {
		GenericDialog gd = new GenericDialog("Grid stitching");
		gd.addChoice("Grid_order", CHOICEORDER, CHOICEORDER[0]);
		gd.addMessage("(Grid size X) * (Grid size Y) should be " + num);
		gd.addNumericField("Grid_size_X", 2, 0);
		gd.addNumericField("Grid_size_Y", 2, 0);
		gd.addSlider("Tile_overlap [%]", 0, 100, 10);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		order = gd.getNextChoice();
		sizeX = (int) gd.getNextNumber();
		sizeY = (int) gd.getNextNumber();
		overlap = gd.getNextNumber();
		return true;
	}

	public void setWithoutDialog(String _order, int _sizeX, int _sizeY, double _overlap) {
		order = _order;
		sizeX = _sizeX;
		sizeY = _sizeY;
		overlap = _overlap;
	}

	public boolean register(String pathToFirstFile) {
		firstFilePath = pathToFirstFile;
		dir = getDir(firstFilePath);
		try {
			String strOption = "type=[Grid: row-by-row] order=[" + order + "]";
			strOption += " grid_size_x=" + sizeX;
			strOption += " grid_size_y=" + sizeY;
			strOption += " tile_overlap=" + overlap;
			strOption += " first_file_index_i=" + getIndex(firstFilePath);
			strOption += " directory=" + dir;
			strOption += " file_names=" + getPrefix(firstFilePath, true) + "{iii}" + getSuffix(firstFilePath);
			strOption += " output_textfile_name=" + output1;
			strOption += " fusion_method=[Do not fuse images (only write TileConfiguration)]";
			strOption += " regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50 compute_overlap computation_parameters=[Save computation time (but use more RAM)]";
			IJ.run("Grid/Collection stitching", strOption);
		} catch (IndexOutOfBoundsException | NullPointerException | NumberFormatException ex) {
			return false;
		}
		return true;
	}

	public void doComplement() {
		String strOption = "configuration_file=" + dir + output2;
		strOption += " grid_size_x=" + sizeX + " grid_size_y=" + sizeY;
		IJ.run("Complement tile positions of refinement failure...", strOption);
		return;
	}

	public ImagePlus doStitching(String sufReplacement) {
		String modFileName = null;
		ImagePlus impCurrent = null;
		try {
			modFileName = makeModifiedConfigFile(sufReplacement);
		} catch (IOException ex) {
			IJ.error("Failed to read/write configuration file.");
			return null;
		}

		try {
			String strOption = "type=[Positions from file] order=[Defined by TileConfiguration]";
			strOption += " directory=" + dir + " layout_file=" + modFileName;
			strOption += " fusion_method=[Linear Blending] image_output=[Fuse and display]";
			IJ.run("Grid/Collection stitching", strOption);

			impCurrent = WindowManager.getCurrentImage();
			impCurrent.setTitle(getPrefix(firstFilePath, false) + sufReplacement.replace(".tif", "_stitched.tif"));
		} catch (IndexOutOfBoundsException ex) {
			IJ.error("Failed to stitch '" + sufReplacement + "' images.");
			return null;
		}

		return impCurrent;
	}

	private String makeModifiedConfigFile(String sufReplacement) throws IOException {
		String modFileName = "TileConfiguration" + sufReplacement.replace(".tif", "") + ".registered.txt";
		String strTileConfig = new String(Files.readAllBytes(Paths.get(dir + output2)));
		try (FileWriter fw = new FileWriter(dir + modFileName)) {
			fw.write(strTileConfig.replace(getSuffix(firstFilePath), sufReplacement));
		} catch (IOException ex) {
			throw ex;
		}
		return modFileName;
	}

	private String getDir(String strPath) {
		return Paths.get(strPath).getParent().toString() + "/";
	}

	private String getPrefix(String strPath, boolean withUnderScore) throws IndexOutOfBoundsException {
		String fileName = Paths.get(strPath).getFileName().toString();
		String[] strSplit = fileName.split("_[0-9]{3}");
		if (withUnderScore)
			return fileName.substring(0, fileName.length() - strSplit[strSplit.length - 1].length() - 3);
		else
			return fileName.substring(0, fileName.length() - strSplit[strSplit.length - 1].length() - 4);
	}

	private int getIndex(String strPath) throws NumberFormatException, IndexOutOfBoundsException {
		String fileName = Paths.get(strPath).getFileName().toString();
		String[] strSplit = fileName.split("_[0-9]{3}");
		int idxEndsAt = fileName.length() - strSplit[strSplit.length - 1].length();
		return Integer.parseInt(fileName.substring(idxEndsAt - 3, idxEndsAt));
	}

	private String getSuffix(String strPath) {
		String fileName = Paths.get(strPath).getFileName().toString();
		String[] strSplit = fileName.split("_[0-9]{3}");
		return strSplit[strSplit.length - 1];
	}

}
