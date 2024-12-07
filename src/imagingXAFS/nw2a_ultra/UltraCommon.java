package imagingXAFS.nw2a_ultra;

import java.util.regex.Pattern;

import ij.plugin.PlugIn;

public class UltraCommon implements PlugIn {

	public static final String[] LIST_BINNING = { "1", "2", "4", "8" };
	public static final String[] lOADINGMODES = { "Apply reference", "Do not apply reference", "Load reference only" };
	public static final Pattern P_REP = Pattern.compile("rep[0-9]{2}_");
	public static final Pattern P_ENERGY = Pattern.compile("_[0-9]{5}\\.[0-9]{2}_eV");
	public static final Pattern P_ANGLE = Pattern.compile("_[0-9\\-][0-9]{3}\\.[0-9]{2}");
	public static final Pattern P_MOSAIC = Pattern.compile("_X[0-9]{2}_Y[0-9]{2}");
	public static final Pattern P_NEXP = Pattern.compile("_[0-9]{3}of[0-9]{3}");
	
	public void run(String arg) {
	}
	
	public static String removePattern(Pattern p, String str) {
		return p.matcher(str).replaceAll("");
	}
}
