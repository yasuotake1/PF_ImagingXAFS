package imagingXAFS.common;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Stitching_KillBadPos implements PlugIn {

	public static String[] choiceKillOp = { "Set positions to (0.0,0.0)", "Remove bad tiles" };

	public void run(String arg) {
		GenericDialog gd = new GenericDialog("Complement tile positions");
		gd.addFileField("Configuration_file", "");
		gd.addChoice("Operation", choiceKillOp, choiceKillOp[0]);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String strPath = gd.getNextString();
		int op = gd.getNextChoiceIndex();
		String str;
		Pattern p = Pattern.compile("[^#].*;.*;\\s*\\([-0-9]+\\.0+,\\s*[-0-9]+\\.0+\\)");// Starts without #, two ';',
																							// ends with (...0, ..,0)
		String output = "";
		try {
			List<String> lines = Files.readAllLines(Paths.get(strPath));
			int num = 0;
			List<String> newLines = new ArrayList<String>();
			for (int i = 0; i < lines.size(); i++) {
				str = lines.get(i);
				if (p.matcher(str).matches()) {
					num++;
					if (op == 0) {
						newLines.add(str.substring(0, str.lastIndexOf(';') + 1) + " (0.0, 0.0)");
					}
				} else {
					newLines.add(lines.get(i));
				}
			}
			for (int i = 0; i < newLines.size(); i++) {
				output += newLines.get(i) + System.lineSeparator();
			}
			String log = op == 0 ? "Set " : "Removed ";
			log += String.valueOf(num);
			log += op == 0 ? " tile positions to (0.0, 0.0)." : " tiles.";
			IJ.log(log);
			FileWriter fw = new FileWriter(strPath);
			fw.write(output);
			fw.close();
		} catch (Exception ex) {
			IJ.error("Failed to read/write configuration file.");
			return;
		}
	}
}