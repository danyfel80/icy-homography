/**
 * 
 */
package plugins.danyfel80.homography;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarSequence;

/**
 * Plugin to compute the homography based on two images containing corresponding points.
 * Transforms image 2 to match image 1 using point correspondance. 
 * @author Daniel Felipe Gonzalez Obando
 */
public class Homography extends EzPlug implements Block {
	
	// Input
	EzVarSequence inSequence1;
	EzVarSequence inSequence2;
	
	// Output
	EzVarSequence outSequence;

	/* (non-Javadoc)
	 * @see plugins.adufour.ezplug.EzPlug#initialize()
	 */
	@Override
	protected void initialize() {
		// TODO Auto-generated method stub

	}	
	/* (non-Javadoc)
	 * @see plugins.adufour.ezplug.EzPlug#execute()
	 */
	@Override
	protected void execute() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see plugins.adufour.ezplug.EzPlug#clean()
	 */
	@Override
	public void clean() {
		// TODO Auto-generated method stub

	}
	
	/* (non-Javadoc)
	 * @see plugins.adufour.blocks.lang.Block#declareInput(plugins.adufour.blocks.util.VarList)
	 */
	@Override
	public void declareInput(VarList inputMap) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see plugins.adufour.blocks.lang.Block#declareOutput(plugins.adufour.blocks.util.VarList)
	 */
	@Override
	public void declareOutput(VarList outputMap) {
		// TODO Auto-generated method stub
		
	}

}
