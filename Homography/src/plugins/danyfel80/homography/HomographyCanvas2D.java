package plugins.danyfel80.homography;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import algorithms.danyfel80.homography.HomographyEstimation;
import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvasEvent;
import icy.canvas.IcyCanvasEvent.IcyCanvasEventType;
import icy.gui.viewer.Viewer;
import icy.sequence.DimensionId;

public class HomographyCanvas2D extends Canvas2D {

	/**
	 * 
	 */
	private static final long serialVersionUID = -803268059026090722L;
	private Map<Integer, RealMatrix> homographyMats; 

	
	public HomographyCanvas2D(Viewer viewer) {
		super(viewer);
		homographyMats = new HashMap<>();
	}

	/* (non-Javadoc)
	 * @see icy.canvas.Canvas2D#synchronizeCanvas(java.util.List, icy.canvas.IcyCanvasEvent, boolean)
	 */
	@Override
	protected void synchronizeCanvas(List<IcyCanvas> canvasList, IcyCanvasEvent event, boolean processAll) {
		final IcyCanvasEventType type = event.getType();
    final DimensionId dim = event.getDim();

    // position synchronization
    if (isSynchOnSlice())
    {
        if (processAll || (type == IcyCanvasEventType.POSITION_CHANGED))
        {
            // no information about dimension --> set all
            if (processAll || (dim == DimensionId.NULL))
            {
                // only support T and Z positioning
                final int z = getPositionZ();
                final int t = getPositionT();

                for (IcyCanvas cnv : canvasList)
                {
                    if (z != -1)
                        cnv.setPositionZ(z);
                    if (t != -1)
                        cnv.setPositionT(t);
                }
            }
            else
            {
                for (IcyCanvas cnv : canvasList)
                {
                    final int pos = getPosition(dim);
                    if (pos != -1)
                        cnv.setPosition(dim, pos);
                }
            }
        }
    }

    // view synchronization
    if (isSynchOnView())
    {
        if (processAll || (type == IcyCanvasEventType.SCALE_CHANGED))
        {
            // no information about dimension --> set all
            if (processAll || (dim == DimensionId.NULL))
            {
                final double sX = getScaleX();
                final double sY = getScaleY();

                for (IcyCanvas cnv : canvasList)
                    ((Canvas2D) cnv).setScale(sX, sY, false);
            }
            else
            {
                for (IcyCanvas cnv : canvasList)
                    cnv.setScale(dim, getScale(dim));
            }
        }

        if (processAll || (type == IcyCanvasEventType.ROTATION_CHANGED))
        {
            // no information about dimension --> set all
            if (processAll || (dim == DimensionId.NULL))
            {
                final double rot = getRotationZ();

                for (IcyCanvas cnv : canvasList)
                    ((Canvas2D) cnv).setRotation(rot, false);
            }
            else
            {
                for (IcyCanvas cnv : canvasList)
                    cnv.setRotation(dim, getRotation(dim));
            }
        }

        // process offset in last as it can be limited depending destination scale value
        if (processAll || (type == IcyCanvasEventType.OFFSET_CHANGED))
        {
            // no information about dimension --> set all
            if (processAll || (dim == DimensionId.NULL))
            {
                final int offX = getOffsetX();
                final int offY = getOffsetY();

                for (IcyCanvas cnv : canvasList)
                    ((Canvas2D) cnv).setOffset(offX, offY, false);
            }
            else
            {
                for (IcyCanvas cnv : canvasList)
                    cnv.setOffset(dim, getOffset(dim));
            }
        }

    }

    // cursor synchronization
    if (isSynchOnCursor())
    { // mouse synchronization
        if (processAll || (type == IcyCanvasEventType.MOUSE_IMAGE_POSITION_CHANGED))
        {
            // no information about dimension --> set all
            if (processAll || (dim == DimensionId.NULL))
            {   
                
                
                for (IcyCanvas cnv : canvasList) {
                	RealMatrix hm = homographyMats.get(cnv.getSequence().getId());
                	if (hm == null) {
                		try {
	                		HomographyEstimation he = new HomographyEstimation(this.getSequence(), cnv.getSequence());
	                		he.execute();
	                		homographyMats.put(cnv.getSequence().getId(), he.getTransformationMatrix());
	                		hm = he.getTransformationMatrix();
                		} catch (IllegalArgumentException e) {
                			hm = MatrixUtils.createRealIdentityMatrix(3);
                			homographyMats.put(cnv.getSequence().getId(), hm);
                		}
                	}
                	
                	Point2D posTransformed = transform(getMouseImagePosX(), getMouseImagePosY(), hm);

                  final double mouseImagePosX = posTransformed.getX();
                  final double mouseImagePosY = posTransformed.getY();
                  
                  ((Canvas2D) cnv).setMouseImagePos(mouseImagePosX, mouseImagePosY);
                }
            }
            else
            {
                for (IcyCanvas cnv : canvasList)
                    cnv.setMouseImagePos(dim, getMouseImagePos(dim));
            }
        }
    }
	}

	/**
	 * @param x
	 * @param y
	 * @param hm
	 * @return The point in Seq1 corresponding to the coordinates (x,y) in Seq2
	 */
	private Point2D transform(double x, double y, RealMatrix hm) {
		double[][] mx1 = new double[3][1];
		mx1[0][0] = x;
		mx1[1][0] = y;
		mx1[2][0] = 1;
		RealMatrix x1 = new Array2DRowRealMatrix(mx1);
		RealMatrix mx2 = hm.multiply(x1);
		return new Point2D.Double(mx2.getEntry(0, 0) / mx2.getEntry(2, 0), mx2.getEntry(1, 0) / mx2.getEntry(2, 0));
	}
	

}
