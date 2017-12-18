import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultListModel;


public class Network extends ArrayList<Branch> implements Serializable{

	private static final long serialVersionUID = 1L;
	private int totalNumberRings = 0;
	private double totalContrast = 0;
	private double meanContrast = -Double.MAX_VALUE;
	protected DefaultListModel<Branch> branchList = null;

	private int lastBranchNo = 0;

	public Network(){	
	}
	
	public Network(DefaultListModel<Branch> branchList){
		this.branchList = branchList;
		lastBranchNo = branchList.size();
	}
	
	public Network(ArrayList<Branch> branchList){
		this.addAll(branchList);
	}

	public void save(String filename) {
	}

	public void load(String filename) {
	}

	public void recalculateContrast(double c) {
		++totalNumberRings;
		totalContrast += c;
		this.meanContrast = totalContrast/totalNumberRings;
	}

	public double getMeanContrast() {
		return this.meanContrast;
	}

	public void resetContrast(){
		this.meanContrast = -Double.MAX_VALUE;
		this.totalContrast = 0;
		this.totalNumberRings = 0;
	}

	public void lowerMeanContrast(double percent) {
		this.meanContrast = meanContrast*percent;
	}

	public void generateSkeleton(Volume vol) {
		for(Branch branch : this) {
			for(int n = 0; n<branch.size()-1; n++) {
				double[] angles = new double[2];
				Point3D first = branch.get(n).getC();
				Point3D second = branch.get(n+1).getC();
				Point3D dir = first.middlePointDir(second);
				angles[0] = Math.acos(dir.getZ());
				angles[1] = Math.atan2(dir.getY(), dir.getX());
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
				int maxK = (int)first.distance(second);
				for(int k=0; k<=maxK; k++) {
					double dx = i*R[0][0] + j*R[0][1] + k*R[0][2];
					double dy = i*R[1][0] + j*R[1][1] + k*R[1][2];
					double dz = i*R[2][0]  + k*R[2][2];
					vol.setValue(first, dx, dy, dz, 1000);
				}
			}
		}
	}
	
	public void generateBinary(Volume vol) {
		for(Branch branch : this) {
			for(int n = 0; n<branch.size()-1; n++) {
				double[] angles = new double[2];
				Point3D first = branch.get(n).getC();
				Point3D second = branch.get(n+1).getC();
				Point3D dir = first.middlePointDir(second);
				angles[0] = Math.acos(dir.getZ());
				angles[1] = Math.atan2(dir.getY(), dir.getX());
				double sint = Math.sin(angles[0]);
				double cost = Math.cos(angles[0]);
				double sinp = Math.sin(angles[1]);
				double cosp = Math.cos(angles[1]);
				double R[][] = 
					{{cosp*cost, -sinp, cosp*sint},
							{sinp*cost, cosp, sinp*sint},
							{-sint, 0, cost}};

				int maxK = (int)Math.ceil(first.distance(second))+1; //without this +1 there is a gap in the binary
				for(int ki=0; ki<=2*maxK; ki++) {
					double k = ki/2.0;
					double radius =  branch.get(n).getRadius();
					int rad = (int) radius;
					for(int ji=-4*rad; ji<=4*rad; ji++) {
						for(int ii=-4*rad; ii<=4*rad; ii++) {
							double j = ji/2.0;
							double i = ii/2.0;
							
							double dx = i*R[0][0] + j*R[0][1] + k*R[0][2];
							double dy = i*R[1][0] + j*R[1][1] + k*R[1][2];
							double dz = i*R[2][0]  + k*R[2][2];
							
							double d = Math.sqrt(i*i+j*j);
							if(d<=radius) vol.setValue(first, dx, dy, dz, 1000);
						}
					}
				}
			}
		}
	}

	public void exportData(String csvFile) throws IOException{
		List<String> header = Arrays.asList("BranchNo", "Length", "Width");
		List<List<String>> data = new ArrayList<List<String>>();

		FileWriter writer = new FileWriter(csvFile);

		for(Branch branch : this) {
			int BranchNo = branch.getBranchNo();
			double branchLength = 0;
			double totalBranchWidth = 0;
			int ringNumber = 0;
			for(int n = 0; n<branch.size()-1; n++) {
				++ringNumber;
				if(n>0){
					branchLength += branch.get(n-1).getC().distance(branch.get(n).getC());
				}
				totalBranchWidth += branch.get(n).getRadius();

			}
			double branchWidth = totalBranchWidth/ringNumber;
			List<String> row = Arrays.asList( String.valueOf(BranchNo), String.valueOf(branchLength), String.valueOf(branchWidth));
			data.add(row);

		}
		CSVUtils.writeLine(writer, header);
		for(List<String> row : data){
			CSVUtils.writeLine(writer, row);
		}
		writer.flush();
		writer.close();

	}


	@Override public boolean add(Branch branch) {
		branchList.addElement(branch);
		++this.lastBranchNo;
		return super.add(branch);
	}

	public int getLastBranchNo() {
		return lastBranchNo;
	}
	public int getTotalNumberRings() {
		return totalNumberRings;
	}

	public double getTotalContrast() {
		return totalContrast;
	}

	public void setTotalNumberRings(int totalNumberRings) {
		this.totalNumberRings = totalNumberRings;
	}

	public void setTotalContrast(double totalContrast) {
		this.totalContrast = totalContrast;
	}

	public void setMeanContrast(double meanContrast) {
		this.meanContrast = meanContrast;
	}

	public void setLastBranchNo(int lastBranchNo) {
		this.lastBranchNo = lastBranchNo;
	}
	public DefaultListModel<Branch> getBranchList() {
		return branchList;
	}

	public void setBranchList(DefaultListModel<Branch> branchList) {
		this.branchList = branchList;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}


}
