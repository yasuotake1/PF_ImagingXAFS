package imagingXAFS.bl15a1;

import ij.plugin.PlugIn;

public class BL15A1Props implements PlugIn {
	public String[] listSuffixes = new String[16];
	public int stageConf;
	public String scaleConf;
	public double pulsePerMMX;
	public double pulsePerMMY;
	public double zoom;
	public boolean[] listUse = new boolean[16];
	public String defaultDir;

	public void run(String arg) {
	}

}
