package imagingXAFS.common;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ij.gui.Plot;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Stitching_PlotTilePositions implements PlugIn {
	Color c = ImagingXAFSCommon.LIST_PLOTCOLORS[0];

	public void run(String arg) {
		OpenDialog od = new OpenDialog("Plot tile positions");
		String strPath = od.getPath();
		if (strPath == null)
			return;

		String fileName = od.getFileName();
		try {
			List<String> lines = Files.readAllLines(Paths.get(strPath));
			int bra, comma, ket;
			String str;
			List<Double> listX = new ArrayList<Double>();
			List<Double> listY = new ArrayList<Double>();
			for (int i = 0; i < lines.size(); i++) {
				str = lines.get(i);
				bra = str.lastIndexOf('(');
				comma = str.lastIndexOf(',');
				ket = str.lastIndexOf(')');
				if (bra > 0 && comma > 0 && ket > 0) {
					listX.add(Double.parseDouble(str.substring(bra + 1, comma)));
					listY.add(Double.parseDouble(str.substring(comma + 1, ket)));
				}
			}
			if (listX.size() > 0) {
				double[] arrX = new double[listX.size()];
				double[] arrY = new double[listY.size()];
				for (int i = 0; i < arrX.length; i++) {
					arrX[i] = listX.get(i);
					arrY[i] = listY.get(i);
				}
				Plot plt = new Plot("Tile positions of " + fileName, "X", "Y");
				plt.setColor(c, c);
				plt.add("connected circle", arrX, arrY);
				plt.show();
				double[] arrLimits = plt.getLimits();
				plt.setLimits(arrLimits[0], arrLimits[1], arrLimits[3], arrLimits[2]);
			}
		} catch (Exception ex) {

		}
	}
}