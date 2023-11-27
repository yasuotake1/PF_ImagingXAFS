package imagingXAFS.common;

import ij.plugin.*;
import imagingXAFS.bl15a1.BL15A1Common;
import imagingXAFS.bl15a1.BL15A1Props;

public class CorrelationMask_Menu implements PlugIn {

	public void run(String arg) {
		BL15A1Props prop = BL15A1Common.ReadProps();
		CorrelationPlotMask.createMask(prop.zoom);
	}

}
