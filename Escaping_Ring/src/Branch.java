import java.util.ArrayList;
import java.util.Arrays;

import ij.IJ;

public class Branch extends ArrayList<Ring>{
	Network network;
	Volume vol;
	Volume test;
	Volume workingVol;
	double step;

	public Branch(Network network, Ring ring, Volume vol, Volume test, Volume workingVol, double step){
		this.add(ring);
		this.network = network;
		this.vol = vol;
		this.test = test;
		this.workingVol = workingVol;
		network.add(this);
		this.evolve(vol, ring, step, test, 0.7);
		//evolve in opposite direction
		workingVol.showTwoChannels("First round", test);
		this.evolve(vol, ring.flippedRing(), step, test, 0.7);
		workingVol.showTwoChannels("Second round", test);
		this.regression(workingVol, test, step);
	}
	
	public Branch(Network network, ArrayList<Ring> branch, Volume vol, Volume test, Volume workingVol, double step){
		this.addAll(branch);
		this.network = network;
		this.vol = vol;
		this.test = test;
		this.workingVol = workingVol;
		network.add(this);
		this.regression(workingVol, test, step);
	}

	public void regression(Volume workingVol, Volume test, double width){

		//erase the whole branch
		
		for(Ring ring: this) {
			ring.eraseVol(workingVol, width);
		}
		
		workingVol.showTwoChannels("Working volume", test);

		Branch branchCopy = (Branch) this.clone();
		for(int i = branchCopy.size()-1; i >=0 ; i--){
			IJ.log("checking ring: " + i);
			Ring nextRing = branchCopy.get(i);
			double step = width;
			nextRing.contrast = nextRing.contrast/2; //to lower the threshold of starting the new branch
			ArrayList<Ring> ringsAround = proposeCandidates(nextRing, step, workingVol, true);
			for(Ring r : ringsAround) {
				IJ.log("checking next from " + ringsAround.size());
				ArrayList<Ring> branchCand = evolve(workingVol, r, step, test, 0.7);
				if(branchCand.size()>2) {
					Branch newBranch = new Branch(network, branchCand, vol, test,  workingVol, step);
				}
			}
			//test.showTwoChannels("t", workingVol);
		}
	}

	public ArrayList<Ring> evolve(Volume vol, Ring initial, double step, Volume test, double breakValue) {

		Ring current = initial.duplicate();
		int iter = 0;
		double prevMax = initial.contrast*3; //later the contrast value is a sum of three rings

		ArrayList<Ring> newBranch = new ArrayList<Ring>();
		newBranch.add(current);
		MAINLOOP:
			do {
				ArrayList<Ring> candidates = proposeCandidates(current, step, vol);
				//keep x% best
				candidates = keepBestCandidates(candidates, 40);
				ArrayList<Ring[]> candidatesTriple = new ArrayList<Ring[]>();
				for ( Ring cand : candidates){
					ArrayList<Ring> candidates2 = proposeCandidates(cand, step, vol);
					//keep x% best
					candidates2 = keepBestCandidates(candidates2, 20);
					for (Ring cand2 : candidates2){
						ArrayList<Ring> candidates3 = proposeCandidates(cand2, step, vol);
						candidates3 = keepBestCandidates(candidates3, 20);
						for (Ring cand3 : candidates3){
							candidatesTriple.add(new Ring[]{cand,cand2, cand3});
						}	
					}
				}

				//calculating the best contrast out of those [three rings]
				Ring best = null; //first ring of couple
				double max = -Double.MAX_VALUE; //total contrast of three rings
				double rest = -Double.MAX_VALUE; //sum of contrast of second and third ring
				for(Ring[] cC : candidatesTriple) {
					double c = cC[0].contrast + cC[1].contrast + cC[2].contrast;
					//IJ.log(" c: " + c);
					if (c > max) {
						max = c;
						best = cC[0];	
						rest = cC[1].contrast + cC[2].contrast;
					}
				}


				//adjust the first ring with more subtle parameter change

				ArrayList<Ring> candidatesRefine = proposeCandidates(best, step, vol, "refine");
				//IJ.log("best: " + max);
				double currentContrast = best.contrast;
				for(Ring cand4 : candidatesRefine) {
					if (cand4.contrast > currentContrast) {
						currentContrast = cand4.contrast;
						best = cand4;
						//IJ.log("refined: " + currentContrast);
					}
				}

				max = currentContrast + rest; 

				//IJ.log("TotalContrast: " + max + "max: " + currentContrast + "rest: " + rest );

				//max = max+rest;
				if(max<prevMax*breakValue) break MAINLOOP;

				newBranch.add(best);
				best.drawMeasureArea(test, step);
				//drawCenterLine(vol, best);
				this.add(best);

				current = best.duplicate();
				IJ.log(" after iter"  + iter + " " + max );
				prevMax=max;
				
				//erase ring 2 places backwards
				if(newBranch.size()>3) {
					newBranch.get(newBranch.size()-2).eraseVol(workingVol, step);
				}
				iter++;
			}
			while (true);
		return newBranch;
	}

	private ArrayList<Ring> keepBestCandidates(ArrayList<Ring> rings, double percent) {
		//keeps percent of best candidates
		percent = 100 - percent;
		ArrayList<Ring> bestCands = new ArrayList<Ring>();	
		double[]contrasts = new double[rings.size()];
		int i=0;
		for(Ring ring : rings){
			contrasts[i] = ring.contrast;
			i++;
		}
		Arrays.sort(contrasts);
		//IJ.log(Arrays.toString(contrasts));
		int cutOffPos = (int) Math.round(percent*rings.size() /100);
		double cutOffContr = contrasts[cutOffPos];
		//IJ.log("cutoff: "+cutOffContr);
		for(Ring ring : rings){
			if(ring.contrast>=cutOffContr)
				bestCands.add(ring);
		}
		//IJ.log("cutoff: " + cutOffContr);
		return bestCands;

	}

	private ArrayList<Ring> proposeCandidates(Ring ring, double step, Volume volume) {
		ArrayList<Ring> cands = new ArrayList<Ring>();	
		double angleStep = Math.PI/10;
		int angleRange = 1;

		double initRadius = ring.radius;
		double maxRadius = 1.40;
		double maxMeasurmentArea = 2;
		double width = step;
		step = step/2;


		for(double dt = -angleRange*angleStep; dt<=angleRange*angleStep; dt+=angleStep) {
			for(double dp = -angleRange*angleStep; dp<=angleRange*angleStep; dp+=angleStep) {	
				//return the MeasurmentVolume
				Ring maxRing = ring.duplicate();
				maxRing.radius = initRadius*maxRadius*maxMeasurmentArea;
				double polar[] = maxRing.getAnglesFromDirection();
				maxRing.c = maxRing.getPositionFromSphericalAngles(step, polar[0] + dt, polar[1] + dp);
				maxRing.dir = new Point3D((maxRing.c.x-ring.c.x)/step, (maxRing.c.y-ring.c.y)/step, (maxRing.c.z-ring.c.z)/step);
				MeasurmentVolume mv = new MeasurmentVolume(volume, maxRing, width);
				//IJ.log("radius: " + maxRing.radius);
				//IJ.log(mv.toString());

				for(double r = initRadius*0.80; r<initRadius*maxRadius; r+=0.20*initRadius) {
					Ring cand = maxRing.duplicate();
					cand.radius = r;
					cand.calculateContrast(mv);
					//IJ.log("contrast: " + cand.contrast);
					cands.add(cand);
				}
			}
		}	
		return cands;
	}

	private ArrayList<Ring> proposeCandidates(Ring ring, double step, Volume volume, String refine) {
		ArrayList<Ring> cands = new ArrayList<Ring>();	
		double angleStep = Math.PI/40;
		int angleRange = 1;

		double initRadius = ring.radius;
		double maxRadius = 1.01;
		double maxMeasurmentArea = 2;
		double width = step;
		step = 0;


		for(double dt = -angleRange*angleStep; dt<=angleRange*angleStep; dt+=angleStep) {
			for(double dp = -angleRange*angleStep; dp<=angleRange*angleStep; dp+=angleStep) {	
				//return the MeasurmentVolume
				Ring maxRing = ring.duplicate();
				double polar[] = maxRing.getAnglesFromDirection();
				maxRing.radius = initRadius*maxRadius*maxMeasurmentArea;
				maxRing.dir = maxRing.getDirectionFromSphericalAngles(polar[0] + dt, polar[1] + dp);
				MeasurmentVolume mv = new MeasurmentVolume(volume, maxRing, width);
				//IJ.log("radius: " + maxRing.radius);
				//IJ.log(mv.toString());

				for(double r = initRadius*0.99; r<initRadius*maxRadius; r+=0.01*initRadius) {
					Ring cand = maxRing.duplicate();
					cand.radius = r;
					cand.calculateContrast(mv);
					//IJ.log("contrast: " + cand.contrast);
					cands.add(cand);
				}				
			}
		}	
		return cands;
	}

	private ArrayList<Ring> proposeCandidates(Ring ring, double step, Volume volume, boolean sparse) {
		//returns sparse rings in 3d space, which keep the initial contrast
		double keepContrast = ring.contrast;
		ArrayList<Ring> cands = new ArrayList<Ring>();	
		double angleStep = Math.PI/2;
		int angleRange = 1;

		double initRadius = ring.radius;
		double maxRadius = 1.75;
		double maxMeasurmentArea = 2;
		double width = step;
		step = 0;


		for(double dt = -angleRange*angleStep; dt<=angleRange*angleStep; dt+=angleStep) {
			for(double dp = -angleRange*angleStep; dp<=angleRange*angleStep; dp+=angleStep) {	
				Ring maxRing = ring.duplicate();
				maxRing.radius = initRadius*maxRadius*maxMeasurmentArea;
				maxRing.dir = maxRing.getDirectionFromSphericalAngles( dt, dp);

				for(double r = initRadius*0.25; r<initRadius*maxRadius; r+=0.75*initRadius) {
					Ring cand = maxRing.duplicate();
					cand.radius = r;
					cand.contrast = keepContrast;
					//IJ.log("contrast: " + cand.contrast);
					cands.add(cand);
				}				
			}
		}	
		return cands;
	}
}
