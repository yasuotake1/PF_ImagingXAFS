package imagingXAFS.nw2a_orca;

import ij.plugin.PlugIn;

public class OrcaProps implements PlugIn {
	public int dcmDirection;// Down = 0, Up = 1.
	public double detectorPosition;// from source point in mm.
	public double pixelSize;// in um.
	public double dcmDistance;// height distance between 1st and 2nd crystals in mm.
	public int width;
	public int height;
	public int bitDepth;

	public void run(String arg) {
	}
}
