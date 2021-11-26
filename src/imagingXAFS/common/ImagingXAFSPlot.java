package imagingXAFS.common;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ij.IJ;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.measure.CurveFitter;
import ij.plugin.PlugIn;

public class ImagingXAFSPlot implements PlugIn {

	static boolean hasData = false;
	static Plot plot = new Plot("ImagingXAFS plot", "Photon energy (eV)", "Intensity or absorption");
	static ImagingXAFSPlotWindow window;
	static final Color[] colors = { new Color(0x8b0000), new Color(0x8b8b00), new Color(0x008b00), new Color(0x008b8b),
			new Color(0x00008b), new Color(0x8b008b), Color.DARK_GRAY, Color.BLACK };
	static int idxColor = 0;
	static final String styleData = "connected circle";
	static final String styleFit = "line";
	static List<String> labels = new ArrayList<String>();
	static List<Integer> idxColors = new ArrayList<Integer>();
	static List<double[]> energies = new ArrayList<double[]>();
	static List<double[]> intensities = new ArrayList<double[]>();
	static List<double[]> normalized = new ArrayList<double[]>();
	static List<double[]> coefs = new ArrayList<double[]>();
	static CurveFitter cf;

	public void run(String arg) {
	}

	public static boolean hasData() {
		return hasData;
	}

	/**
	 * Adds a data to the ImagingXAFSPlot and stores the plot objects for the
	 * following processes.
	 * 
	 * @param energy
	 * @param intensity
	 * @param label
	 */
	public static void addData(double[] energy, double[] intensity, String label) {
		hasData = true;
		plot.restorePlotObjects();

		labels.add(label);
		idxColors.add(idxColor);
		energies.add(energy);
		intensities.add(intensity);

		plot.setColor(colors[idxColor], colors[idxColor]);
		idxColor = (idxColor + 1) % colors.length;
		plot.add(styleData, energy, intensity);

		plot.savePlotObjects();
	}

	/**
	 * Restores the plot objects to those when added finally.
	 */
	public static void redrawData() {
		plot.restorePlotObjects();
		show(true);
	}

	/**
	 * Draws pre-edge and post-edge normalization lines.
	 */
	static void addNormalizationLines() {
		if (!hasData)
			return;
		plot.restorePlotObjects();

		double eStart, eEnd;
		for (int i = 0; i < labels.size(); i++) {
			eStart = energies.get(i)[0];
			eEnd = energies.get(i)[energies.get(i).length - 1];
			double[] xPrePost = { eStart, eEnd };
			double[] yPre = { getPreEdgeLineValue(i, eStart), getPreEdgeLineValue(i, eEnd) };
			double[] yPost = { getPostEdgeLineValue(i, eStart), getPostEdgeLineValue(i, eEnd) };
			plot.setColor(colors[idxColors.get(i)]);
			plot.drawLine(xPrePost[0], yPre[0], xPrePost[1], yPre[1]);
			plot.drawLine(xPrePost[0], yPost[0], xPrePost[1], yPost[1]);
		}
		show(true);
	}

	/**
	 * Replaces the plots with those are substracted with pre-edge lines.
	 */
	static void drawSubtractedData() {
		if (!hasData)
			return;
		plot.restorePlotObjects();

		for (int i = 0; i < labels.size(); i++) {
			double[] intensity = new double[energies.get(i).length];
			for (int j = 0; j < intensity.length; j++) {
				intensity[j] = intensities.get(i)[j] - getPreEdgeLineValue(i, energies.get(i)[j]);
			}
			plot.setColor(colors[idxColors.get(i)], colors[idxColors.get(i)]);
			plot.replace(i, styleData, energies.get(i), intensity);
		}
		show(true);
	}

	/**
	 * Replaces the plots with those are normalized using pre-edge and post-edge
	 * lines.
	 */
	static void drawNormalizedData() {
		if (!hasData)
			return;
		plot.restorePlotObjects();

		normalized.clear();
		for (int i = 0; i < labels.size(); i++) {
			double[] intensity = new double[energies.get(i).length];
			for (int j = 0; j < intensity.length; j++) {
				intensity[j] = (intensities.get(i)[j] - getPreEdgeLineValue(i, energies.get(i)[j]))
						/ (getPostEdgeLineValue(i, energies.get(i)[j]) - getPreEdgeLineValue(i, energies.get(i)[j]));
			}
			normalized.add(intensity);
			plot.setColor(colors[idxColors.get(i)], colors[idxColors.get(i)]);
			plot.replace(i, styleData, energies.get(i), intensity);

		}
		show(true);
	}

	/**
	 * Performs SVD on the plots and shows the sum of the components (i.e., fitting
	 * results) along with the normalized spectra. Returns false when failed.
	 * 
	 * @return
	 */
	static boolean drawLinearCombination() {
		if (!hasData)
			return false;

		SVD.setDataMatrix(energies.get(0), normalized);
		if (!SVD.setStandards(false))
			return false;
		SVD.doSVD(false);

		IJ.log("Linear combination result:");
		String log = "               ";// fifteen blanks
		for (int i = 0; i < SVD.numComponents(); i++) {
			log += String.format("%-15s", SVD.getNames(15).get(i));
		}
		IJ.log(log);
		double[] weight;
		for (int i = 0; i < labels.size(); i++) {
			log = String.format("%-15s", labels.get(i));
			weight = SVD.getCoefsAt(i);
			for (int j = 0; j < weight.length; j++) {
				log += String.format("%15.3f", weight[j]);
			}
			IJ.log(log);
			plot.setColor(colors[idxColors.get(i)]);
			plot.add(styleFit, energies.get(i), SVD.getCurveAt(i));
		}
		plot.setLimitsToFit(true);
		return true;
	}

	/**
	 * Calculates and stores the coefficients of pre-edge and post-edge lines using
	 * ij.measure.CurveFitter.CurveFitter(double[] xData, double[] yData).
	 */
	static void calcCoefs() {
		if (!hasData) {
			return;
		}

		coefs.clear();
		for (int i = 0; i < labels.size(); i++) {
			int[] indices = ImagingXAFSCommon.searchNormalizationIndices(energies.get(i));
			if (indices == null)
				return;
			double[] coef = new double[4];
			cf = new CurveFitter(Arrays.copyOfRange(energies.get(i), indices[0], indices[1]),
					Arrays.copyOfRange(intensities.get(i), indices[0], indices[1]));
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			coef[0] = cf.getParams()[0];
			coef[1] = cf.getParams()[1];
			cf = new CurveFitter(Arrays.copyOfRange(energies.get(i), indices[2], indices[3]),
					Arrays.copyOfRange(intensities.get(i), indices[2], indices[3]));
			cf.doFit(CurveFitter.STRAIGHT_LINE);
			coef[2] = cf.getParams()[0];
			coef[3] = cf.getParams()[1];
			coefs.add(coef);
		}
	}

	static double getPreEdgeLineValue(int idx, double ene) {
		return coefs.get(idx)[0] + coefs.get(idx)[1] * ene;
	}

	static double getPostEdgeLineValue(int idx, double ene) {
		return coefs.get(idx)[2] + coefs.get(idx)[3] * ene;
	}

	public static void resetIdxColor() {
		idxColor = 0;
	}

	public static void show(boolean enableNormalization) {
		plot.setColor(Color.black);
		plot.addLegend(String.join("\t", labels));

		if (window == null) {
			window = new ImagingXAFSPlotWindow(plot, enableNormalization);
		} else {
			plot.update();
		}
		plot.setLimitsToFit(true);
	}

	public static void clear() {
		if (window != null) {
			window.close();
			window = null;
		}
		plot = new Plot("ImagingXAFS plot", "Photon energy (eV)", "Intensity or absorption");
		hasData = false;
		idxColor = 0;
		labels.clear();
		idxColors.clear();
		energies.clear();
		intensities.clear();
		normalized.clear();
		coefs.clear();
	}

	@SuppressWarnings("serial")
	static class ImagingXAFSPlotWindow extends PlotWindow implements ItemListener {
		private static Choice choice;
		private final String[] choices = { "Raw data", "Show pre- and post-edge lines", "Subtract pre-edge",
				"Normalized", "Linear combination of standards" };
		private static int selectedIndexOld;
		private final Label labelPreStart = new Label("Pre-edge from");
		private final Label labelPreEnd = new Label("  to");
		private final Label labelPostStart = new Label("       Post-edge from");
		private final Label labelPostEnd = new Label("  to");
		private static TextField tfPreStart, tfPreEnd, tfPostStart, tfPostEnd;

		ImagingXAFSPlotWindow(Plot plot, boolean enableNormalization) {
			super(plot.getImagePlus(), plot);
			if (enableNormalization)
				addNormalizationPanel();
		}

		private void addNormalizationPanel() {
			Panel panel = new Panel();
			panel.setLayout(new BorderLayout());
			choice = new Choice();
			for (int i = 0; i < choices.length; i++) {
				choice.add(choices[i]);
			}
			panel.add("North", choice);
			choice.addItemListener(this);
			Panel panel2nd = new Panel();
			panel2nd.setLayout(new FlowLayout(FlowLayout.LEFT));
			panel2nd.add(labelPreStart);
			tfPreStart = new TextField(String.format("%.2f", ImagingXAFSCommon.normalizationParam[0]), 7);
			panel2nd.add(tfPreStart);
			panel2nd.add(labelPreEnd);
			tfPreEnd = new TextField(String.format("%.2f", ImagingXAFSCommon.normalizationParam[1]), 7);
			panel2nd.add(tfPreEnd);
			panel2nd.add(labelPostStart);
			tfPostStart = new TextField(String.format("%.2f", ImagingXAFSCommon.normalizationParam[2]), 7);
			panel2nd.add(tfPostStart);
			panel2nd.add(labelPostEnd);
			tfPostEnd = new TextField(String.format("%.2f", ImagingXAFSCommon.normalizationParam[3]), 7);
			panel2nd.add(tfPostEnd);
			panel.add("South", panel2nd);
			add(panel);
			pack();

			selectedIndexOld = 0;

		}

		public void itemStateChanged(ItemEvent e) {
			int selectedIndex = choice.getSelectedIndex();

			if (selectedIndex == 0) {
				redrawData();

			} else {
				Double preStart = getPreStart();
				Double preEnd = getPreEnd();
				Double postStart = getPostStart();
				Double postEnd = getPostEnd();
				if (preStart.isNaN() || preEnd.isNaN() || postStart.isNaN() || postEnd.isNaN()) {
					choice.select(selectedIndexOld);
					return;
				}
				if (preStart > preEnd || preEnd > postStart || postStart > postEnd) {
					IJ.error("Invalid pre-edge and post-edge region.");
					choice.select(selectedIndexOld);
					return;
				}

				ImagingXAFSCommon.normalizationParam = new double[] { preStart, preEnd, postStart, postEnd };
				calcCoefs();
				if (coefs.size() == 0) {
					choice.select(selectedIndexOld);
					return;
				}

				if (selectedIndex == 1) {
					addNormalizationLines();
				}

				if (selectedIndex == 2) {
					drawSubtractedData();
				}

				if (selectedIndex == 3) {
					drawNormalizedData();
				}

				if (selectedIndex == 4) {
					drawNormalizedData();
					if (!drawLinearCombination()) {
						choice.select(selectedIndexOld);
						return;
					}
				}

				if (selectedIndex > 4) {
					choice.select(selectedIndexOld);
					return;
				}
			}
			selectedIndexOld = selectedIndex;

		}

		static Double getValue(String str) {
			Double d;
			if (str == null)
				d = Double.NaN;
			try {
				d = Double.parseDouble(str);
			} catch (NumberFormatException e) {
				d = Double.NaN;
			}
			return d;
		}

		static Double getPreStart() {
			Double d;
			if (tfPreStart == null)
				d = Double.NaN;
			else
				d = getValue(tfPreStart.getText());
			return d;
		}

		static Double getPreEnd() {
			Double d;
			if (tfPreEnd == null)
				d = Double.NaN;
			else
				d = getValue(tfPreEnd.getText());
			return d;
		}

		static Double getPostStart() {
			Double d;
			if (tfPostStart == null)
				d = Double.NaN;
			else
				d = getValue(tfPostStart.getText());
			return d;
		}

		static Double getPostEnd() {
			Double d;
			if (tfPostEnd == null)
				d = Double.NaN;
			else
				d = getValue(tfPostEnd.getText());
			return d;
		}
	}
	
}
