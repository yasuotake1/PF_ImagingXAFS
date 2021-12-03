package imagingXAFS.nw2a_ultra;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.NewImage;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;

public class Monitor_UltraScript implements PlugIn {

	public void run(String arg) {
		MonitorWindow mw = new MonitorWindow(NewImage.createShortImage("Empty", 1024, 1024, 1, NewImage.FILL_BLACK));
		mw.startMonitor();
	}

	@SuppressWarnings("serial")
	public class MonitorWindow extends ImageWindow {

		private UltraScanInfo usi;
		private int currentIdx = 0;
		private MonitorTimer mt;

		MonitorWindow(ImagePlus imp) {
			super(imp, new ImageCanvas(imp));
			imp = NewImage.createShortImage("Empty", 1024, 1024, 1, NewImage.FILL_BLACK);
			setTitle("UltraXRM script monitor");
		}

		public boolean startMonitor() {
			OpenDialog od = new OpenDialog("Choose ScanInfo file...");
			String path = od.getPath();
			if (path == null)
				return false;
			try {
				usi = new UltraScanInfo(path);
			} catch (Exception e) {
				return false;
			}
			mt = new MonitorTimer(this);
			mt.Start();
			return true;
		}

		public void showLatestImage() {
			if (usi == null)
				return;
			int tempIdx = currentIdx;
			File f = new File(usi.directory);
			List<String> ls = Arrays.asList(f.list());
			while (currentIdx < usi.allFiles.length - 1 && ls.contains(usi.allFiles[currentIdx + 1])) {
				currentIdx++;
			}
			if (currentIdx == 0 || currentIdx != tempIdx) {
				try {
					super.setImage(XRM_Reader.Load(usi.getPath(currentIdx)));
					setTitle("UltraXRM script monitor: " + usi.allFiles[currentIdx]);
				} catch (Exception e) {
					currentIdx = (currentIdx == 0) ? 0 : currentIdx - 1;
				}
			}
		}

		public void StopMonitor() {
			mt.Stop();
		}
	}

	class MonitorTimer {

		MonitorWindow mw;
		TimerTask task;
		Timer timer;

		MonitorTimer(MonitorWindow _mw) {
			if (_mw != null) {
				mw = _mw;
				task = new TimerTask() {
					public void run() {
						mw.showLatestImage();
					}
				};
			}
		}

		void Start() {
			timer = new Timer();
			timer.schedule(task, 100, 1000);
		}

		void Stop() {
			if (timer != null)
				timer.cancel();
		}

	}

}
