import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;

import java.awt.Rectangle;

import javax.swing.DefaultListModel;



public class Espacing_Ring implements PlugIn {
	static Volume vol; //raw image, not changable in processing
	static Volume workingVol; //raw image, changed during processing
	static ImageCanvas iC;
	static ImagePlus imp; //display image
	static StackWindow imgS;

	@Override
	public void run(String arg0) {

		vol = null;
		workingVol = null;
		Gui dialog = new Gui();
		dialog.setVisible(true);

		//start();

	}

	public static void start(Network network, double step, double impInside, double impOutside, double threshold, double branchFacilitator,
			double firstLoop, double secondLoop, double thirdLoop,
			double maxIn, double minMem, double maxMem, double minOut, double maxOut) {
		IJ.log("start 21 nov 2017");

		imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.error("No open image.");
			return;
		}


		Roi roi = imp.getRoi();
		if (roi == null) {
			IJ.error("No selected ROI.");
			return;
		}

		if (roi.getType() != Roi.OVAL){
			IJ.error("No selected Oval ROI.");
			return;
		}

		OvalRoi oval = (OvalRoi)roi;
		Rectangle rect = oval.getBounds();
		int xc = rect.x + rect.width/2;
		int yc = rect.y + rect.height/2;
		int radius = (rect.width + rect.height) / 4;	
		int zc = imp.getSlice();

		Ring.setImpInside(impInside);
		Ring.setImpOutside(impOutside);
		Branch.setEvolveValue(threshold);
		Branch.setBranchFacilitator(branchFacilitator);
		Branch.setFirstLoopElimination(firstLoop);
		Branch.setSecondLoopElimination(secondLoop);
		Branch.setThirdLoopElimination(thirdLoop);
		Branch.stopAll(false);	
		Ring.setParameters(maxIn, minMem, maxMem, minOut, maxOut);


		Ring initial = new Ring(xc, yc, zc, 0, 0, 0, radius, step*2);
		IJ.log(" Initial Ring " + initial);
		Volume test = new Volume(imp.getWidth(), imp.getHeight(), imp.getNSlices());

		if(vol == null) vol = new Volume(imp);
		if(workingVol == null) workingVol = new Volume(imp); 

		generateView(true);
	
		Ring adjInitial = initial.adjustFirstRing(vol);
		network.recalculateContrast(adjInitial.getContrast());

		Branch firstBranch = new Branch(network, adjInitial, vol, test, workingVol, step);




	}
	/*	public static void showResult(Network network, double step){
		segmented = new Volume(imp.getWidth(), imp.getHeight(), imp.getNSlices());
		for(Branch branch : network) {
			for(Ring ring : branch) {
				ring.drawMeasureArea(segmented);
			}
		}
		generateView(false);

	}
	 */
	public static void drawNetwork(Network network){

		for(Branch branch : network) {
			for(Ring ring : branch) {
				ring.drawMeasureArea(iC.getImage(), java.awt.Color.BLUE);
			}
		}

	}

	public static void showResult(DefaultListModel<Branch> branchList, double step){
		//selected = new Volume(imp.getWidth(), imp.getHeight(), imp.getNSlices());
		for(int i=0; i< branchList.getSize(); i++){
			Branch branch = branchList.getElementAt(i);
			for(Ring ring : branch) {
				ring.drawMeasureArea(iC.getImage(), java.awt.Color.RED);
			}
		}
		//generateView(false);

	}

	public static void showRings(DefaultListModel<Ring> ringList){
		//selected = new Volume(imp.getWidth(), imp.getHeight(), imp.getNSlices());
		for(int i=0; i< ringList.getSize(); i++){
			Ring ring = ringList.getElementAt(i);
			ring.drawMeasureArea(iC.getImage(), java.awt.Color.YELLOW);
		}
		//generateView(false);
	}


	private double[] unit(double[] u) {
		double norm = 0.0;
		for(int i=0; i<u.length; i++)
			norm += u[i]*u[i];
		norm = Math.sqrt(norm);
		return new double[] {u[0]/norm, u[1]/norm, u[2]/norm};	
	}

	public void drawCenterLine(Volume volume, Ring ring) {

		double angles[] = ring.getAnglesFromDirection();
		double sint = Math.sin(angles[0]);
		double cost = Math.cos(angles[0]);
		double sinp = Math.sin(angles[1]);
		double cosp = Math.cos(angles[1]);
		double R[][] = 
			{{cosp*cost, -sinp, cosp*sint},
					{sinp*cost, cosp, sinp*sint},
					{-sint, 0, cost}};
		int i = 0;
		int j = 0;
		for(int k=0; k<=10; k++) {
			double dx = i*R[0][0] + j*R[0][1] + k*R[0][2];
			double dy = i*R[1][0] + j*R[1][1] + k*R[1][2];
			double dz = i*R[2][0]  + k*R[2][2];
			volume.setValue(ring.c, dx, dy, dz, 1000);
		}
	}

	public static void generateView(boolean setVisible){
		ImagePlus imp = new ImagePlus("VascRing3D", vol.createImageStackFrom3DArray());
		imp.setDisplayMode(IJ.COLOR);
		iC = new ImageCanvas(imp);
		imgS = new StackWindow (imp, iC);
		iC.setVisible(true);


	}
}
