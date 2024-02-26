package imagingXAFS.nw2a_ultra;

import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Show_UltraScanInfo implements PlugIn {
	public void run(String arg) {
		OpenDialog od = new OpenDialog("Open UltraXRM ScanInfo...", null);
		String path = od.getPath();
		if (path == null)
			return;

		try {
			UltraScanInfo si = new UltraScanInfo(path);
			IJ.log(path);
			IJ.log("Version=" + si.version);
			IJ.log("Energy=" + si.energy + ",Tomo=" + si.tomo + ",Mosaic=" + si.mosaic + ",NRepeatScan="
					+ si.nRepeatScan);
			IJ.log("MultiExposure=" + si.multiExposure + ",WaitNSecs=" + si.waitNSecs + ",NExposures=" + si.nExposures
					+ ",AverageOnTheFly=" + si.averageOnTheFly);
			IJ.log("RefNExposures=" + si.refNExposures + ",RefForEveryExposures=" + si.refForEveryExposures
					+ ",RefABBA=" + si.refABBA + ",RefAverageOnTheFly=" + si.refAverageOnTheFly);
			IJ.log("MosaicUp=" + si.mosaicUp + ",MosaicDown=" + si.mosaicDown + ",MosaicLeft=" + si.mosaicLeft
					+ ",MosaicRight=" + si.mosaicRight + ",MosaicOverlap=" + si.mosaicOverlap + ",MosaicCentralFile="
					+ si.mosaicCentralFile);
			if (si.energy) {
				String str1 = "";
				for (int i = 0; i < si.energies.length; i++) {
					str1 += si.energies[i] + ",";
				}
				IJ.log("Energies=" + str1.substring(0, str1.length() - 1));
			}
			if (si.tomo) {
				String str2 = "";
				for (int i = 0; i < si.angles.length; i++) {
					str2 += si.angles[i] + ",";
				}
				IJ.log("Angles=" + str2.substring(0, str2.length() - 1));
			}
		} catch (Exception ex) {
			IJ.error("Failed to load a ScanInfo.");
			return;
		}
	}
}