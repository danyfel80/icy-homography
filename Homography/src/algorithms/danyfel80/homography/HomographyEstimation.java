package algorithms.danyfel80.homography;

import java.awt.geom.Point2D;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.sequence.Sequence;
import plugins.kernel.roi.roi2d.ROI2DPoint;

/**
 * Computes the homography based on two 2D images containing corresponding
 * points. Transforms image 2 to match image 1 using point correspondence.
 * 
 * @author Daniel
 */
public class HomographyEstimation {
	// Input
	private Sequence seq1;
	private Sequence seq2;

	// Output

	// Internal variables
	private double[] h;
	private RealMatrix hMat;

	public HomographyEstimation(Sequence seq1, Sequence seq2) {
		this.seq1 = seq1;
		this.seq2 = seq2;
	}

	public void execute() {
		List<? extends ROI> pts1 = this.seq1.getROIs(ROI2DPoint.class);
		List<? extends ROI> pts2 = this.seq2.getROIs(ROI2DPoint.class);

		double[][] a = new double[pts1.size() * 2][];
		for (int i = 0; i < a.length; i++) {
			ROI2DPoint pt1 = (ROI2DPoint) pts1.get(i);
			ROI2DPoint pt2 = (ROI2DPoint) pts2.get(i);
			a[i * 2] = new double[] { -pt1.getPosition().x, -pt1.getPosition().y, -1, 0, 0, 0, pt2.getPosition().x * pt1
			    .getPosition().x, pt2.getPosition().x * pt1.getPosition().y, pt2.getPosition().x };
			a[i * 2 + 1] = new double[] { 0, 0, 0, -pt1.getPosition().x, -pt1.getPosition().y, -1, pt2.getPosition().y * pt1
			    .getPosition().x, pt2.getPosition().y * pt1.getPosition().y, pt2.getPosition().y };
		}
		RealMatrix matA = new Array2DRowRealMatrix(a);
		SingularValueDecomposition svd = new SingularValueDecomposition(matA);
		double[] sVals = svd.getSingularValues();
		System.out.println(sVals);
		System.out.println("Goodness of fit = " + sVals[8]);
		h = svd.getV().getColumn(8);
		double[][] hm = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				hm[i][j] = h[i * 3 + j];
			}
		}
		hMat = new Array2DRowRealMatrix(hm);
	}

	public double[] getTransformation() {
		return h;
	}

	public void getImageTransformation(Sequence seq2Transfromed) {
		seq2Transfromed.beginUpdate();
		seq2Transfromed.setImage(0, 0, new IcyBufferedImage(seq1.getWidth(), seq1.getHeight(), seq1.getSizeC(), seq1
		    .getDataType_()));
		byte[][] seq2TData = seq2Transfromed.getDataXYCAsByte(0, 0);
		byte[][] seq2Data = seq2.getDataXYCAsByte(0, 0);
		for (int x = 0; x < seq2Transfromed.getSizeX(); x++) {
			for (int y = 0; y < seq2Transfromed.getSizeY(); y++) {
				Point2D ptT = transform(x, y);
				if (x >= 0 && x < seq2.getSizeX() && y >= 0 && y < seq2.getSizeY()) {
					for (int c = 0; c < seq2Transfromed.getSizeC(); c++) {
						seq2TData[c][x + y * seq2Transfromed.getSizeX()] = seq2Data[c][(int) Math.round(ptT.getX()) + (int) Math
						    .round(ptT.getY()) * seq2.getSizeX()];
					}
				}
			}
		}
		seq2Transfromed.dataChanged();
		seq2Transfromed.endUpdate();
	}

	private Point2D transform(int x, int y) {
		double[][] mx1 = new double[3][1];
		mx1[0][0] = x;
		mx1[0][1] = y;
		mx1[0][2] = 1;
		RealMatrix x1 = new Array2DRowRealMatrix(mx1);
		RealMatrix mx2 = hMat.multiply(x1);
		return new Point2D.Double(mx2.getEntry(0, 0) / mx2.getEntry(2, 0), mx2.getEntry(1, 0) / mx2.getEntry(2, 0));
	}

}
