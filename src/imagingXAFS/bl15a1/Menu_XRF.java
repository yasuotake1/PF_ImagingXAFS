package imagingXAFS.bl15a1;

import ij.*;
import ij.plugin.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

@SuppressWarnings("serial")
public class Menu_XRF extends JFrame implements WindowListener, PlugIn {

	ImagePlus[] impsRaw;
	ImagePlus[] impsI0Corr;
	static final String btRawTextHide = "Hide Raw Data";
	static final String btRawTextShow = "Show Raw Data";
	static final String btI0CorrTextHide = "Hide I0-Corrected Data";
	static final String btI0CorrTextShow = "Show I0-Corrected Data";

	public void run(String arg) {
		BL15A1Props prop = BL15A1Common.ReadProps();
		LoadSingleMap singleMap = new LoadSingleMap();
		impsRaw = singleMap.method1(prop.listSuffixes, BL15A1Common.listChName, prop);
		if (checkNull(impsRaw))
			return;
		impsI0Corr = singleMap.method2(impsRaw, BL15A1Common.listI0CorrName, prop);
		IJ.run("Tile");

		final JFrame MenuFrame = new JFrame(singleMap.prefix);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setAlwaysOnTop(true);
		MenuFrame.addWindowListener(this);
		MenuFrame.setBounds(150, 150, 280, 135); // MenuFrame.setBounds(x,y,width,height of the frame in the display);
		MenuFrame.setLayout(null);
		try {
			MenuFrame.setAlwaysOnTop(true);
		} catch (SecurityException se) {
		}

		final JButton btRaw = new JButton();
		btRaw.setText(btRawTextHide);
		btRaw.setBounds(20, 20, 240, 30);
		btRaw.setFont(new Font("Arial", Font.PLAIN, 15));
		btRaw.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btRaw.getText().equals(btRawTextShow)) {
					btRaw.setText(btRawTextHide);
					// Show raw data
					for (int i = 0; i < impsRaw.length; i++) {
						if (impsRaw[i] != null) {
							impsRaw[i].show();
							impsRaw[i].setActivated();
							IJ.run("Set... ", "zoom=" + prop.zoom);
						}
					}
				} else {
					btRaw.setText(btRawTextShow);
					// Close raw data
					for (int i = 0; i < impsRaw.length; i++) {
						if (impsRaw[i] != null)
							impsRaw[i].hide();
					}
				}
				IJ.getInstance().toFront();
				MenuFrame.toFront();
				if (WindowManager.getWindowCount() > 0)
					IJ.run("Tile");
			}
		});

		final JButton btI0Corr = new JButton();
		btI0Corr.setText(btI0CorrTextHide);
		btI0Corr.setBounds(20, 60, 240, 30);
		btI0Corr.setFont(new Font("Arial", Font.PLAIN, 15));
		btI0Corr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (btI0Corr.getText().equals(btI0CorrTextShow)) {
					btI0Corr.setText(btI0CorrTextHide);
					// Show I0-corrected data
					for (int i = 0; i < impsI0Corr.length; i++) {
						if (impsI0Corr[i] != null) {
							impsI0Corr[i].show();
							impsI0Corr[i].setActivated();
							IJ.run("Set... ", "zoom=" + prop.zoom);
						}
					}
				} else {
					btI0Corr.setText(btI0CorrTextShow);
					;
					// Close I0-corrected data
					for (int i = 0; i < impsI0Corr.length; i++) {
						if (impsI0Corr[i] != null)
							impsI0Corr[i].hide();
					}
				}
				IJ.getInstance().toFront();
				MenuFrame.toFront();
				if (WindowManager.getWindowCount() > 0)
					IJ.run("Tile");
			}
		});

		MenuFrame.add(btRaw);
		MenuFrame.add(btI0Corr);
		IJ.getInstance().toFront();
		MenuFrame.setVisible(true);
	}

	public void windowOpened(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		for (int i = 0; i < impsRaw.length; i++) {
			if (impsRaw[i] != null)
				impsRaw[i].close();
		}
		for (int i = 0; i < impsI0Corr.length; i++) {
			if (impsI0Corr[i] != null)
				impsI0Corr[i].close();
		}
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	private boolean checkNull(ImagePlus[] imps) {
		boolean b = true;
		if (imps == null)
			return b;
		for (int i = 0; i < imps.length; i++) {
			if (imps[i] != null)
				b = false;
		}
		return b;
	}
}
