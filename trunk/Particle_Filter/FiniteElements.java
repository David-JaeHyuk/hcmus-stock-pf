package org.hcmus.stock.particlefilter;


import java.util.ArrayList;

import Jama.Matrix;

public class FiniteElements {

	ArrayList<Double> priceList;
	int futureInterval;

	ArrayList<double[]> cubicTrialFunctions = new ArrayList<double[]>();
	Matrix coMatrix1; // coefficient matrix of linear system: phi(0)=?; phi'(0)=?;phi(1)=?; phi'(1)=?
					  // where phi() is a cubic functions, this is the linear system used for finding phi()
	
	public FiniteElements() {
		priceList = null;
		futureInterval = 0;
		createTrialFunctions();
	}
	
	public FiniteElements(
			ArrayList<Double> priceList,
			int futureInterval) {
		this.priceList = priceList;
		this.futureInterval = futureInterval;
		createTrialFunctions();
	}
	
	public ArrayList<Double> getPriceList() {
		return priceList;
	}

	public void setPriceList(ArrayList<Double> priceList) {
		this.priceList = priceList;
	}

	private void createTrialFunctions() {
		double[][] array = {{1.,0.,0.,0.},{0.,1.,0.,0.},{1.,1.,1.,1.},{0.,1.,2.,3.}}; 
		coMatrix1 = new Matrix(array);
		Matrix b = new Matrix(4,1);
		Matrix coTrialFunction;
		
		//find 4 trial cubic functions
		
		//first cubic
		b.set(0, 0, 1);
		b.set(1, 0, 0);
		b.set(2, 0, 0);
		b.set(3, 0, 0);
		coTrialFunction = coMatrix1.solve(b);
		cubicTrialFunctions.add(new double[4]);
		cubicTrialFunctions.get(0)[0] = coTrialFunction.get(0, 0);
		cubicTrialFunctions.get(0)[1] = coTrialFunction.get(1, 0);
		cubicTrialFunctions.get(0)[2] = coTrialFunction.get(2, 0);
		cubicTrialFunctions.get(0)[3] = coTrialFunction.get(3, 0);
		
		//second cubic
		b.set(0, 0, 0);
		b.set(1, 0, 1);
		b.set(2, 0, 0);
		b.set(3, 0, 0);
		coTrialFunction = coMatrix1.solve(b);
		cubicTrialFunctions.add(new double[4]);
		cubicTrialFunctions.get(1)[0] = coTrialFunction.get(0, 0);
		cubicTrialFunctions.get(1)[1] = coTrialFunction.get(1, 0);
		cubicTrialFunctions.get(1)[2] = coTrialFunction.get(2, 0);
		cubicTrialFunctions.get(1)[3] = coTrialFunction.get(3, 0);

		//third cubic
		b.set(0, 0, 0);
		b.set(1, 0, 0);
		b.set(2, 0, 1);
		b.set(3, 0, 0);
		coTrialFunction = coMatrix1.solve(b);
		cubicTrialFunctions.add(new double[4]);
		cubicTrialFunctions.get(2)[0] = coTrialFunction.get(0, 0);
		cubicTrialFunctions.get(2)[1] = coTrialFunction.get(1, 0);
		cubicTrialFunctions.get(2)[2] = coTrialFunction.get(2, 0);
		cubicTrialFunctions.get(2)[3] = coTrialFunction.get(3, 0);

		//fourth cubic
		b.set(0, 0, 0);
		b.set(1, 0, 0);
		b.set(2, 0, 0);
		b.set(3, 0, 1);
		coTrialFunction = coMatrix1.solve(b);
		cubicTrialFunctions.add(new double[4]);
		cubicTrialFunctions.get(3)[0] = coTrialFunction.get(0, 0);
		cubicTrialFunctions.get(3)[1] = coTrialFunction.get(1, 0);
		cubicTrialFunctions.get(3)[2] = coTrialFunction.get(2, 0);
		cubicTrialFunctions.get(3)[3] = coTrialFunction.get(3, 0);
	}

	public int getFutureInterval() {
		return futureInterval;
	}

	public void setFutureInterval(int futureInterval) {
		this.futureInterval = futureInterval;
	}

	public ArrayList<Double> runAlgorithm() throws Exception {
		
		//approximate r'(x) (center)
		ArrayList<Double> firstDerivativeOfR = new ArrayList<Double>();
		firstDerivativeOfR.add(priceList.get(1)-priceList.get(0));
		for (int i = 1; i < priceList.size()-1; ++i) {
			firstDerivativeOfR.add((priceList.get(i+1)-priceList.get(i-1))/2);
		}
		firstDerivativeOfR.add(priceList.get(priceList.size()-1)-priceList.get(priceList.size()-2));
		
		//approximate r''(x) (center)
		ArrayList<Double> secondDerivativeOfR = new ArrayList<Double>();
		secondDerivativeOfR.add(firstDerivativeOfR.get(1)-firstDerivativeOfR.get(0));
		for (int i = 1; i < firstDerivativeOfR.size()-1; ++i) {
			secondDerivativeOfR.add((firstDerivativeOfR.get(i-1)-firstDerivativeOfR.get(i+1))/2);
		}
		secondDerivativeOfR.add(firstDerivativeOfR.get(firstDerivativeOfR.size()-1)-firstDerivativeOfR.get(firstDerivativeOfR.size()-2));
		
		Matrix K = new Matrix(2*secondDerivativeOfR.size(), 2*secondDerivativeOfR.size(), 0);
		Matrix F = new Matrix(2*secondDerivativeOfR.size(), 1, 0);
		Matrix elementMatrix = new Matrix(4,4);
		
		//calculate the element matrix
		for (int i = 0; i<4; ++i ) {
			for (int j = 0; j < 4; ++j) {
				double[] a1 = cubicTrialFunctions.get(i);
				double[] a2 = cubicTrialFunctions.get(j);
				double value = a1[1]*a2[1] + (a1[1]*a2[2] + a1[2]*a2[1]) + 
						(a1[1]*a2[3] + (4./3.)*a1[2]*a2[2] + a1[3]*a2[1]) +
						(6./4.)*(a1[2]*a2[3] + a1[3]*a2[2]) +
						(9./5.)*a1[3]*a2[3];
							
				elementMatrix.set(i, j, value);
			}
		}
		
		//calculate K and F matrices
		for (int i = 0; i < secondDerivativeOfR.size()-1; ++i ) {			
			for (int j = 0; j < 4; ++j) {
				for (int k = 0; k < 4; ++k) {
					K.set(i*2 + j, i*2 + k, K.get(i*2 + j, i*2 + k) + elementMatrix.get(j, k));
				}
			}
		}
		
		for (int i = 1; i < secondDerivativeOfR.size()-1; ++i) {
			double area1 = cubicTrialFunctions.get(2)[0] + cubicTrialFunctions.get(2)[1]/2 + cubicTrialFunctions.get(2)[2]/3 + cubicTrialFunctions.get(2)[3]/4;
			double area2 = cubicTrialFunctions.get(0)[0] + cubicTrialFunctions.get(0)[1]/2 + cubicTrialFunctions.get(0)[2]/3 + cubicTrialFunctions.get(0)[3]/4;
			double value = secondDerivativeOfR.get(i-1)*area1 + secondDerivativeOfR.get(i)*area2;
			F.set(2*i, 0, value);
	
			area1 = cubicTrialFunctions.get(3)[0] + cubicTrialFunctions.get(3)[1]/2 + cubicTrialFunctions.get(3)[2]/3 + cubicTrialFunctions.get(3)[3]/4;
			area2 = cubicTrialFunctions.get(1)[0] + cubicTrialFunctions.get(1)[1]/2 + cubicTrialFunctions.get(1)[2]/3 + cubicTrialFunctions.get(1)[3]/4;
			value = secondDerivativeOfR.get(i-1)*area1 + secondDerivativeOfR.get(i)*area2;
			F.set(2*i+1, 0, value);
		}
		
		//boundary conditions
		for (int i = 0; i < secondDerivativeOfR.size()*2; i++) {
			K.set(0, i, 0);
			K.set(secondDerivativeOfR.size()*2-2, i, 0);
		}
		K.set(0, 0, 1);
		K.set(secondDerivativeOfR.size()*2-2, secondDerivativeOfR.size()*2-2, 1);
		
		//
		F.set(0, 0, priceList.get(0));
		double value = secondDerivativeOfR.get(0)*(cubicTrialFunctions.get(3)[0] + cubicTrialFunctions.get(3)[1]/2 + cubicTrialFunctions.get(3)[2]/3 + cubicTrialFunctions.get(3)[3]/4);
		F.set(1, 0, value);
		F.set(secondDerivativeOfR.size()*2-2, 0, priceList.get(priceList.size()-1));
		value = secondDerivativeOfR.get(secondDerivativeOfR.size()-2)*(cubicTrialFunctions.get(1)[0] + cubicTrialFunctions.get(1)[1]/2 + cubicTrialFunctions.get(1)[2]/3 + cubicTrialFunctions.get(1)[3]/4);
		F.set(secondDerivativeOfR.size()*2-1, 0, value);
		
		//solve KU=F
		Matrix U = K.solve(F);
		
		ArrayList<Double> predictedPrices = new ArrayList<Double>();
		for (int i = 0; i < futureInterval; ++i) {
			double price = U.get(U.getRowDimension()-1, 0)*(cubicTrialFunctions.get(3)[0] + cubicTrialFunctions.get(3)[1]*(i+2) + cubicTrialFunctions.get(3)[2]*Math.pow((i+2),2) + cubicTrialFunctions.get(3)[3]*Math.pow((i+2),3));
			price += U.get(U.getRowDimension()-2, 0)*(cubicTrialFunctions.get(2)[0] + cubicTrialFunctions.get(2)[1]*(i+2) + cubicTrialFunctions.get(2)[2]*Math.pow((i+2),2) + cubicTrialFunctions.get(2)[3]*Math.pow((i+2),3));
			price += U.get(U.getRowDimension()-3, 0)*(cubicTrialFunctions.get(1)[0] + cubicTrialFunctions.get(1)[1]*(i+2) + cubicTrialFunctions.get(1)[2]*Math.pow((i+2),2) + cubicTrialFunctions.get(1)[3]*Math.pow((i+2),3));
			price += U.get(U.getRowDimension()-4, 0)*(cubicTrialFunctions.get(0)[0] + cubicTrialFunctions.get(0)[1]*(i+2) + cubicTrialFunctions.get(0)[2]*Math.pow((i+2),2) + cubicTrialFunctions.get(0)[3]*Math.pow((i+2),3));
			
			predictedPrices.add(price);
			
		}
		
		return predictedPrices;
	}
	
}
