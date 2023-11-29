package imagingXAFS.nw2a_orca;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

/**
 * This class reads header information of an ITEX image file and shows them in
 * the ImageJ Log window.
 */
public class Show_ITEXFileInfo implements PlugIn {

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open ITEX Image...", null);
		String path = od.getPath();
		if (path == null)
			return;

		try {
			byte[] buffer = OrcaCommon.readBytes(path, 0, 64);
			int lenComment = (buffer[2] & 0xff) + ((buffer[3] & 0xff) << 8);
			int width = (buffer[4] & 0xff) + ((buffer[5] & 0xff) << 8);
			int height = (buffer[6] & 0xff) + ((buffer[7] & 0xff) << 8);
			int type = (buffer[12] & 0xff) + ((buffer[13] & 0xff) << 8);
			IJ.log("Directory=\"" + od.getDirectory() + "\"");
			IJ.log("FileName=\"" + od.getFileName() + "\"");
			IJ.log("Width=" + width);
			IJ.log("Height=" + height);
			IJ.log("FileType=" + getFileTypeString(type) + "");
			IJ.log("CommentLength=" + lenComment);
			buffer = OrcaCommon.readBytes(path, 64, lenComment);
			List<String> list = parseCSV(new String(buffer));
			for (int i = 0; i < list.size(); i++) {
				IJ.log(list.get(i));
			}
		} catch (IOException ex) {
			IJ.error("Failed to load an ITEX image.");
			return;
		}

	}

	String getFileTypeString(int type) {
		switch (type) {
		case 0:
			return "0: 8 Bit";
		case 1:
			return "1: Compressed";
		case 2:
			return "2: 16 Bit";
		case 3:
			return "3: 32 Bit";
		default:
			return type + ": N/A";
		}
	}

	List<String> parseCSV(String str) {
		List<String> list = new ArrayList<String>();
		list.add("");
		boolean withinQuot = false;
		int idx = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == ',' && !withinQuot) {
				list.add("");
				idx++;
			} else {
				list.set(idx, list.get(idx) + str.charAt(i));
				if (str.charAt(i) == '"')
					withinQuot = !withinQuot;
			}
		}
		return list;
	}
}
