package imagingXAFS.nw2a_orca;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Create_SequenceList implements PlugIn {
	
	public void run(String arg) {
		OpenDialog od = new OpenDialog("Choose XAFS data (9809 format) of 1st imagestack");
		if(od.getDirectory() == null) return;
		
		String dirFirst = od.getDirectory();
		String pathFirst = od.getPath();
		String nameFirst = od.getFileName();
		String line = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(pathFirst)));
			line = br.readLine();			
			br.close();
		} catch(FileNotFoundException e) {
			IJ.error(e.getMessage());
			return;
		} catch(IOException e) {
			IJ.error(e.getMessage());
			return;
		}
		if(!line.trim().startsWith("9809")) {
			IJ.error("Invalid 9809 format file.");
			return;
		}

		String prefix = nameFirst.substring(0, nameFirst.lastIndexOf("_"));
		String content = pathFirst;
		int i = Integer.parseInt(nameFirst.substring(nameFirst.lastIndexOf("_") + 1)) + 1;
		String pathNext = dirFirst.substring(0, dirFirst.lastIndexOf("_") + 1) + String.format("%03d", i) + File.separator + prefix + "_" + String.format("%03d", i);
		while(Files.exists(Paths.get(pathNext))) {
			content += "\r\n";
			content += pathNext;
			i++;
			pathNext = dirFirst.substring(0, dirFirst.lastIndexOf("_") + 1) + String.format("%03d", i) + File.separator + prefix + "_" + String.format("%03d", i);
		}
		
		String pathSeqList = (Paths.get(dirFirst)).getParent() + File.separator + "seqList_" + prefix + ".txt";
		try {
			FileWriter fw = new FileWriter(pathSeqList);
			fw.write(content);
			fw.close();
		} catch(IOException e) {
			IJ.error("Failed to write a text file.");
			return;
		}
		IJ.showMessage("Sequence list saved as:\n" + pathSeqList);

	}

}
