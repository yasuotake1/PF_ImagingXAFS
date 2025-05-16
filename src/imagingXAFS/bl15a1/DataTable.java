package imagingXAFS.bl15a1;
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class DataTable implements PlugIn {
	static long[][] rawData;
	static String strScanInfo = "";
	static int[] arrIdxX;
	static int[] arrIdxY;
	static long[][] convData;
	
	public void run(String arg) {
	}

	public static boolean assign(String path, int idxX, String labelX, int idxY, String labelY) {
		int countRow = 0;
		int columns = 0;
		List<String> strRaw = new ArrayList<String>();

		try {
			strRaw = Files.readAllLines(Paths.get(path));
			columns = strRaw.get(0).split(",").length;
			for(int i=1; i<strRaw.size(); i++) {
				if(strRaw.get(i).split(",").length == columns) {
					countRow++;
				} else {
					break;
				}
			}
		} catch(IOException e) {
			return false;
		}
		rawData = new long[columns][countRow];
		for(int i=0; i<countRow; i++) {
			String[] strSplit = strRaw.get(i + 1).split(",");
			for(int j=0; j<columns; j++) {
				rawData[j][i] = Long.parseLong(strSplit[j].trim()); //2D data table, transposed from original file content.
			}
		}
		long[] uniqueX = Arrays.stream(rawData[idxX]).distinct().toArray();
		long stepX = uniqueX[1] - uniqueX[0];
		int numX = uniqueX.length;
		long startX = uniqueX[0];
		long endX = uniqueX[0];
		for(int i=0;i<uniqueX.length;i++) {
			startX=(stepX>0?uniqueX[i]<startX:uniqueX[i]>startX)?uniqueX[i]:startX;
			endX=(stepX>0?uniqueX[i]>endX:uniqueX[i]<endX)?uniqueX[i]:endX;
		}
		long[] uniqueY = Arrays.stream(rawData[idxY]).distinct().toArray();
		long stepY = uniqueY[1] - uniqueY[0];
		int numY = uniqueY.length;
		long startY = uniqueY[0];
		long endY = uniqueY[0];
		for(int i=0;i<uniqueY.length;i++) {
			startY=(stepY>0?uniqueY[i]<startY:uniqueY[i]>startY)?uniqueY[i]:startY;
			endY=(stepY>0?uniqueY[i]>endY:uniqueY[i]<endY)?uniqueY[i]:endY;
		}
		arrIdxX = new int[countRow];
		arrIdxY = new int[countRow];
		for(int i=0; i<countRow; i++) {
			arrIdxX[i] = (int)((rawData[idxX][i] - startX) / stepX);
			arrIdxY[i] = (int)((rawData[idxY][i] - startY) / stepY);
			IJ.log("IdxX="+arrIdxX[i]+",IdxY="+arrIdxY[i]);
		}
		strScanInfo = "Axis 1: \"" + labelX + "\"";
		strScanInfo += "\nStart=" + Long.toString(startX) + ", End=" + Long.toString(endX) + ", Step=" + Long.toString(stepX) + ", Points=" + Integer.toString(numX);
		strScanInfo += "\nAxis 2: \"" + labelY + "\"";
		strScanInfo += "\nStart=" + Long.toString(startY) + ", End=" + Long.toString(endY) + ", Step=" + Long.toString(stepY) + ", Points=" + Integer.toString(numY);
		convData = new long[numY][numX];
		return true;
	}
	
	public static String getSpreadSheetString(int idx, String delimiter) {
		int num = arrIdxX.length;
		for(int i=0; i<num; i++) {
			convData[arrIdxY[i]][arrIdxX[i]] = rawData[idx][i];
		}
		String[] arrStr = new String[convData.length];
		for(int i=0; i<arrStr.length; i++) {
			arrStr[i] = String.join(delimiter, Arrays.stream(convData[i]).mapToObj(String::valueOf).toArray(String[]::new));
		}
		return String.join("\n", arrStr);
	}
	
}
