package imagingXAFS.nw2a_ultra;

import java.util.regex.Pattern;

import ij.plugin.PlugIn;

public class UltraCommon implements PlugIn {

	public static final String[] strBinning = { "1", "2", "4", "8" };
	public static final String[] loadingModes = { "Apply reference", "Do not apply reference", "Load reference only" };
	public static final Pattern pRep = Pattern.compile("rep[0-9]{2}_");
	public static final Pattern pEnergy = Pattern.compile("_[0-9]{5}\\.[0-9]{2}_eV");
	public static final Pattern pAngle = Pattern.compile("_[0-9\\-][0-9]{3}\\.[0-9]{2}");
	public static final Pattern pMosaic = Pattern.compile("_X[0-9]{2}_Y[0-9]{2}");
	public static final Pattern pNExp = Pattern.compile("_[0-9]{3}of[0-9]{3}");
	
	public void run(String arg) {
	}
	
	public static String removePattern(Pattern p, String str) {
		return p.matcher(str).replaceAll("");
	}
}
