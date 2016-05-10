/**
 * 
 */
package plugins.danyfel80.homography;

import algorithms.danyfel80.homography.HomographyEstimation;
import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * Plugin to compute the homography based on two 2D images containing
 * corresponding points. Transforms image 2 to match image 1 using point
 * correspondence.
 * 
 * @author Daniel Felipe Gonzalez Obando
 */
public class Homography extends EzPlug implements Block {

	// Input
	private EzVarSequence inSequence1;
	private EzVarSequence inSequence2;

	// Output
	private EzVarSequence outSequence;

	// Internal variables
	private Sequence seq1;
	private Sequence seq2;
	private Sequence seq2Transfromed;

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.adufour.ezplug.EzPlug#initialize()
	 */
	@Override
	protected void initialize() {
		inSequence1 = new EzVarSequence("Sequence 1");
		inSequence2 = new EzVarSequence("Sequence 2");
		
		addEzComponent(inSequence1);
		addEzComponent(inSequence2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.adufour.ezplug.EzPlug#execute()
	 */
	@Override
	protected void execute() {
		validateInput();
		seq1 = inSequence1.getValue();
		seq2 = inSequence2.getValue();
		
		// Compute homography
		HomographyEstimation he = new HomographyEstimation(seq1, seq2);
		long startTime = System.nanoTime();
		he.execute();
		long endTime = System.nanoTime();
		System.out.print("The transformation parameters are: [");
		for (double d : he.getTransformation()) {
			System.out.print("" + d + ", ");
		}
		System.out.println(String.format("] in %f msecs.", (endTime - startTime) / 1000000d));
		
		// non-interpolated transformation
		startTime = System.nanoTime();
		seq2Transfromed = new Sequence(seq2.getName() + "_Transformed");
		he.getImageTransformationWithoutInterpolation(seq2Transfromed);
		endTime = System.nanoTime();
		System.out.println(String.format("Transformation without interpolation = %f msecs.", (endTime - startTime) / 1000000d));
		
		// interpolated transformation
		startTime = System.nanoTime();
		Sequence seq2TransformedInterpolated = new Sequence(seq2.getName() + "_TransformedInt");
		he.getImageTransformation(seq2TransformedInterpolated);
		endTime = System.nanoTime();
		System.out.println(String.format("Transformation with interpolation = %f msecs.", (endTime - startTime) / 1000000d));
		
		// Show results
		if (outSequence != null) {
			outSequence.setValue(seq2Transfromed);
		} else {
			addSequence(seq2Transfromed);
			addSequence(seq2TransformedInterpolated);
		}
	}

	/**
	 * Validates input parameters
	 */
	private void validateInput() {
		if (inSequence1.getValue() == null || inSequence1.getValue().isEmpty()) {
			MessageDialog.showDialog("Wrong input image 1", "Error", MessageDialog.ERROR_MESSAGE);
		}
		if (inSequence2.getValue() == null || inSequence2.getValue().isEmpty()) {
			MessageDialog.showDialog("Wrong input image 2", "Error", MessageDialog.ERROR_MESSAGE);
		}
		if (inSequence1.getValue().getROICount(ROI2DPoint.class) < 8 || inSequence2.getValue().getROICount(ROI2DPoint.class) < 8) {
			MessageDialog.showDialog("insufficient amount of corresponding points", "Error", MessageDialog.ERROR_MESSAGE);
		}
		if (inSequence1.getValue().getROICount(ROI2DPoint.class) != inSequence2.getValue().getROICount(ROI2DPoint.class)) {
			MessageDialog.showDialog("incoherent corresponding points", "Error", MessageDialog.ERROR_MESSAGE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see plugins.adufour.ezplug.EzPlug#clean()
	 */
	@Override
	public void clean() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareInput(plugins.adufour.blocks.util.
	 * VarList)
	 */
	@Override
	public void declareInput(VarList inputMap) {
		inSequence1 = new EzVarSequence("Sequence 1");
		inSequence2 = new EzVarSequence("Sequence 2");
		inputMap.add(inSequence1.name, inSequence1.getVariable());
		inputMap.add(inSequence2.name, inSequence2.getVariable());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * plugins.adufour.blocks.lang.Block#declareOutput(plugins.adufour.blocks.util
	 * .VarList)
	 */
	@Override
	public void declareOutput(VarList outputMap) {
		outSequence = new EzVarSequence("HomographyTransformation");
		outputMap.add(outSequence.name, outSequence.getVariable());
	}

}
