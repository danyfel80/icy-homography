package algorithms.danyfel80.homography;

import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;
import java.util.Comparator;
import java.util.List;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PerspectiveTransform;
import javax.media.jai.WarpPerspective;

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
 * @author Daniel Felipe Gonzalez Obando
 */
public class HomographyEstimation {
	// Input
	private Sequence seq1;
	private Sequence seq2;

	// Output

	// Internal variables
	private double[] h;
	private double[][] hm;
	private RealMatrix hMat;

	/**
	 * Default Constructor accepting 2 sequences. Both sequences should have the
	 * same amount of points and at least 8.
	 * 
	 * @param seq1
	 * @param seq2
	 */
	public HomographyEstimation(Sequence seq1, Sequence seq2) {
		this.seq1 = seq1;
		this.seq2 = seq2;
	}

	/**
	 * Computes the homography of the sequence 2 into the sequence 1.
	 * 
	 * @throws IllegalArgumentException
	 *           if sizes are less than 8 or if the amount of points is not the
	 *           same in both sequences.
	 */
	public void execute() throws IllegalArgumentException {
		Comparator<ROI> compFn = new Comparator<ROI>() {
			@Override
			public int compare(ROI o1, ROI o2) {
				return Integer.compare(Integer.parseInt(o1.getName()), Integer.parseInt(o2.getName()));
			}
		};

		List<? extends ROI> pts1 = this.seq1.getROIs(ROI2DPoint.class);
		List<? extends ROI> pts2 = this.seq2.getROIs(ROI2DPoint.class);
		pts1.sort(compFn);
		pts2.sort(compFn);

		if (seq1.getROICount(ROI2DPoint.class) < 8 || seq2.getROICount(ROI2DPoint.class) < 8) {
			throw new IllegalArgumentException(
			    String.format("Too few correspondence points (sizes: %d, %d). Should be at least 8.",
			        seq1.getROICount(ROI2DPoint.class), seq2.getROICount(ROI2DPoint.class)));
		}
		if (seq1.getROICount(ROI2DPoint.class) != seq2.getROICount(ROI2DPoint.class)) {
			throw new IllegalArgumentException(String.format("Inconsistent amount of correspondence points (sizes: %d, %d)",
			    seq1.getROICount(ROI2DPoint.class), seq2.getROICount(ROI2DPoint.class)));
		}

		System.out.println("Sizes: " + pts1.size() + ", " + pts2.size());

		// Create matrix A
		double[][] a = new double[pts1.size() * 2][];
		for (int i = 0; i < pts1.size(); i++) {
			ROI2DPoint pt1 = (ROI2DPoint) pts1.get(i);
			ROI2DPoint pt2 = (ROI2DPoint) pts2.get(i);
			a[i * 2] = new double[] { -pt1.getPosition().x, -pt1.getPosition().y, -1, 0, 0, 0,
			    pt2.getPosition().x * pt1.getPosition().x, pt2.getPosition().x * pt1.getPosition().y, pt2.getPosition().x };
			a[i * 2 + 1] = new double[] { 0, 0, 0, -pt1.getPosition().x, -pt1.getPosition().y, -1,
			    pt2.getPosition().y * pt1.getPosition().x, pt2.getPosition().y * pt1.getPosition().y, pt2.getPosition().y };
		}

		// Perform SVD on A
		RealMatrix matA = new Array2DRowRealMatrix(a);
		SingularValueDecomposition svd = new SingularValueDecomposition(matA);
		double[] sVals = svd.getSingularValues();
		System.out.print("singular values = [");
		for (double val : sVals) {
			System.out.print(val + ", ");
		}
		System.out.println("]");
		System.out.println("Goodness of fit = " + sVals[8]);

		// Get H matrix
		h = svd.getV().getColumn(8);
		hm = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				hm[i][j] = h[i * 3 + j];
			}
		}
		hMat = new Array2DRowRealMatrix(hm);
	}

	/**
	 * @return
	 */
	public double[] getTransformation() {
		return h;
	}
	
	public RealMatrix getTransformationMatrix() {
		return hMat;
	}

	/**
	 * @param seq2Transfromed
	 *          Sequence to store the transformed image without using
	 *          interpolation.
	 */
	public void getImageTransformationWithoutInterpolation(Sequence seq2Transfromed) {
		seq2Transfromed.beginUpdate();
		seq2Transfromed.setImage(0, 0,
		    new IcyBufferedImage(seq1.getWidth(), seq1.getHeight(), seq1.getSizeC(), seq1.getDataType_()));
		byte[][] seq2TData = seq2Transfromed.getDataXYCAsByte(0, 0);
		byte[][] seq2Data = seq2.getDataXYCAsByte(0, 0);
		for (int x = 0; x < seq2Transfromed.getSizeX(); x++) {
			for (int y = 0; y < seq2Transfromed.getSizeY(); y++) {
				Point2D ptT = transform(x, y);
				int ptTx = (int) Math.round(ptT.getX());
				int ptTy = (int) Math.round(ptT.getY());

				if (ptTx >= 0 && ptTx < seq2.getSizeX() && ptTy >= 0 && ptTy < seq2.getSizeY()) {
					for (int c = 0; c < seq2Transfromed.getSizeC(); c++) {
						seq2TData[c][x + y * seq2Transfromed.getSizeX()] = seq2Data[c][ptTx + ptTy * seq2.getSizeX()];
					}
				}
			}
		}
		seq2Transfromed.dataChanged();
		seq2Transfromed.endUpdate();
	}

	/**
	 * @param x
	 * @param y
	 * @return The point in Seq1 corresponding to the coordinates (x,y) in Seq2
	 */
	private Point2D transform(int x, int y) {
		double[][] mx1 = new double[3][1];
		mx1[0][0] = x;
		mx1[1][0] = y;
		mx1[2][0] = 1;
		RealMatrix x1 = new Array2DRowRealMatrix(mx1);
		RealMatrix mx2 = hMat.multiply(x1);
		return new Point2D.Double(mx2.getEntry(0, 0) / mx2.getEntry(2, 0), mx2.getEntry(1, 0) / mx2.getEntry(2, 0));
	}

	/**
	 * @param seq2Transfromed
	 *          Sequence to store the transformed image using interpolation.
	 */
	public void getImageTransformation(Sequence seq2Transfromed) {

		IcyBufferedImage im = seq2.getFirstImage();
		ParameterBlock pb = new ParameterBlock();
		pb.addSource(im);
		PerspectiveTransform pt = new PerspectiveTransform(hm);
		WarpPerspective wp = new WarpPerspective(pt);
		pb.add(wp);
		pb.add(Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2));

		seq2Transfromed.beginUpdate();
		seq2Transfromed.setImage(0, 0, JAI.create("warp", pb).getAsBufferedImage());
		seq2Transfromed.dataChanged();
		seq2Transfromed.endUpdate();
	}

}
