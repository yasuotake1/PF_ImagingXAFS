package imagingXAFS.bl15a1;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.io.OpenDialog;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConvertRaw2DMap implements PlugIn {

	public void run(String arg) {
		String[] labels;
		BL15A1Props prop = BL15A1Common.ReadProps();

		OpenDialog od = new OpenDialog("Select raw 2D mapping data file.", prop.defaultDir, "");
		if (od.getPath() == null)
			return;
		String strPreview = "";
		try (BufferedReader br = Files.newBufferedReader(Paths.get(od.getPath()))) {
			strPreview = br.readLine();
			labels = strPreview.split(",");
			for (int i = 0; i < labels.length; i++) {
				labels[i] = labels[i].trim();
			}
			strPreview += "\n";
			for (int i = 1; i < 5; i++) {
				strPreview += br.readLine() + "\n";
			}
		} catch (IOException e) {
			return;
		}
		boolean[] listUse = new boolean[labels.length];
		int offset = (listUse.length > prop.listUse.length) ? listUse.length - prop.listUse.length : 0;
		for (int i = 0; i < prop.listUse.length; i++) {
			if ((i + offset) >= listUse.length)
				break;
			listUse[i + offset] = prop.listUse[i];
		}

		GenericDialog gd = new GenericDialog("Raw 2D mapping data -> Text image");
		gd.addMessage("Content of " + od.getFileName() + ":");
		gd.addTextAreas(strPreview, null, 3, 90);
		gd.addChoice("Axis X: ", labels, labels[0]);
		gd.addChoice("Axis Y: ", labels, labels[1]);
		gd.addCheckboxGroup(2, 20, labels, listUse);
		gd.addCheckbox("Convert all imagestack files", false);
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		int idxX = gd.getNextChoiceIndex();
		int idxY = gd.getNextChoiceIndex();
		for (int i = 0; i < listUse.length; i++) {
			listUse[i] = gd.getNextBoolean();
		}
		boolean convAll = gd.getNextBoolean();

		for (int i = 0; i < prop.listUse.length; i++) {
			if ((i + offset) >= listUse.length)
				break;
			prop.listUse[i] = listUse[i + offset];
		}
		prop.defaultDir = od.getDirectory();
		BL15A1Common.WriteProps(prop);

		int countSrc = 0;
		int countTgt = 0;
		String strScanInfo = "";
		if (convAll) {
			String key = "_qscan_";
			String basePath, suffix;
			if (od.getPath().contains(key)) {
				basePath = od.getPath().substring(0, od.getPath().indexOf(key) + key.length());
				suffix = od.getPath().substring(od.getPath().indexOf(key) + key.length() + 3);
			} else {
				basePath = od.getPath().substring(0, od.getPath().length() - 3);
				suffix = "";
			}
			String currentBasePath = basePath + String.format("%03d", countSrc + 1);
			IJ.log(currentBasePath);
			IJ.log(suffix);
			File f = new File(currentBasePath + suffix);
			while (f.exists()) {
				if (DataTable.assign(f.toString(), idxX, labels[idxX], idxY, labels[idxY])) {
					countSrc++;

					strScanInfo = DataTable.strScanInfo;
					for (int i = 0; i < labels.length; i++) {
						if (listUse[i]) {
							writeTextFile(currentBasePath + "_" + labels[i] + ".txt",
									DataTable.getSpreadSheetString(i, ","));
							strScanInfo += "\n\"" + currentBasePath + "_" + labels[i] + ".txt\"";
							countTgt++;
						}
					}

					writeTextFile(currentBasePath + "_ScanInfo.txt", strScanInfo);

					IJ.showStatus("Converted " + Integer.toString(countSrc) + " file(s) into "
							+ Integer.toString(countTgt) + " text image(s).");

				} else {
					IJ.error("Invalid raw data file.");
				}

				currentBasePath = basePath + String.format("%03d", countSrc + 1);
				f = new File(currentBasePath + suffix);
			}

		} else {
			if (DataTable.assign(od.getPath(), idxX, labels[idxX], idxY, labels[idxY])) {
				countSrc = 1;
				strScanInfo = DataTable.strScanInfo;
				String name = od.getFileName();
				int idx = name.lastIndexOf('.');
				if (idx > 0)
					name = name.substring(0, idx);
				String path = "";
				for (int i = 0; i < labels.length; i++) {
					if (listUse[i]) {
						path = od.getDirectory() + name + "_" + labels[i] + ".txt";
						writeTextFile(path, DataTable.getSpreadSheetString(i, ","));
						strScanInfo += "\n\"" + path + "\"";
						countTgt++;
					}
				}
				writeTextFile(od.getDirectory() + name + "_ScanInfo.txt", strScanInfo);
			} else {
				IJ.error("Invalid raw data file.");
			}
		}
		IJ.showMessage("Converted " + Integer.toString(countSrc) + " file(s) into " + Integer.toString(countTgt)
				+ " text image(s).");
	}

	void writeTextFile(String path, String content) {
		try (FileWriter fw = new FileWriter(path)) {
			fw.write(content);
		} catch (IOException e) {
			IJ.error("Failed to write a text file.");
		}

		return;
	}
}
