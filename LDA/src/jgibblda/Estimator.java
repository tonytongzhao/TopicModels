/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package jgibblda;

import java.io.File;
import java.util.Date;
import java.util.Vector;

public class Estimator {
	
	// output model
	protected Model trnModel;
	LDACmdOption option;
	
	public boolean init(LDACmdOption option){
		this.option = option;
		trnModel = new Model();
		
		if (option.est){
			if (!trnModel.initNewModel(option))
				return false;
			trnModel.data.localDict.writeWordMap(option.dir + File.separator + option.wordMapFileName);
			trnModel.data.userDict.writeAuthormap(option.dir + File.separator +"authorMapFile");
		}
		else if (option.estc){
			if (!trnModel.initEstimatedModel(option))
				return false;
		}
		
		return true;
	}
	
	public void estimate(){
		System.out.println("Sampling " + trnModel.niters + " iteration!");
		
		int lastIter = trnModel.liter;
		java.util.Date d1 =new Date();
		int ms=d1.getMinutes();
		int ts=d1.getSeconds();
		int concept=0;
		int all=0;
		int perconcept=0;
		int persample=0;
		for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters + lastIter; trnModel.liter++){
			System.out.println("Iteration " + trnModel.liter + " ...");
			
			// for all z_i
			for (int m = 0; m < trnModel.M; m++){				
				
				for (int n = 0; n < trnModel.data.docs[m].length; n++){
					// z_i = z[m][n]       n is the nth word of Doc m
					// sample from p(z_i|z_-i, w)
					if(trnModel.data.id2concept.containsKey(trnModel.data.docs[m].words[n]))
					{
						concept++;
						perconcept++;
					}
					all++;
					persample++;
					int topic = sampling(m, n);
					trnModel.z[m].set(n, topic);
				}// end for each word
			}// end for each document
			
			if (option.savestep > 0){
				if (trnModel.liter % option.savestep == 0){
					System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
					computeTheta();
					computePhi();
					trnModel.saveModel("model-" + Conversion.ZeroPad(trnModel.liter, 5));
				}
			}
			System.out.println((double)perconcept/persample);
			perconcept=0;
			persample=0;
		}// end iterations		
		java.util.Date d2 =new Date();
		int mf=d2.getMinutes();
		int tf=d2.getSeconds();
		System.out.println("Start time: "+d1.getHours()+"."+ms+"."+ts);
		System.out.println("finish time: "+d2.getHours()+"."+mf+"."+tf);
		System.out.println(""+all+" "+concept+" "+(double)concept/all);
		System.out.println("Gibbs sampling completed!\n");
		System.out.println("Saving the final model!\n");
		computeTheta();
		computePhi();
		trnModel.liter--;
		trnModel.saveModel("model-final");
		
	}
	
	/**
	 * Do sampling
	 * @param m document number
	 * @param n word number
	 * @return topic id
	 */
	public int sampling(int m, int n){
		// extract topic from the nth word of Doc m
		// remove z_i from the count variable
		int topic = trnModel.z[m].get(n);
		//  w is the number of the word in local Dictionary
		int w = trnModel.data.docs[m].words[n];
		
		trnModel.nw[w][topic] -= 1;
		trnModel.nd[m][topic] -= 1;
		trnModel.nwsum[topic] -= 1;
		trnModel.ndsum[m] -= 1;
		
		double Vbeta = trnModel.V * trnModel.beta;
		double Kalpha = trnModel.K * trnModel.alpha;
		
		//do multinominal sampling via cumulative method,sample topic z from 1 to K on word di
		for (int k = 0; k < trnModel.K; k++){
			trnModel.p[k] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) *
					(trnModel.nd[m][k] + trnModel.alpha)/(trnModel.ndsum[m] + Kalpha);
		}
		
		// cumulate multinomial parameters
		for (int k = 1; k < trnModel.K; k++){
			trnModel.p[k] += trnModel.p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * trnModel.p[trnModel.K - 1];
		
		for (topic = 0; topic < trnModel.K; topic++){
			if (trnModel.p[topic] > u) //sample topic w.r.t distribution p
				break;
		}
		
		// add newly estimated z_i to count variables
		trnModel.nw[w][topic] += 1;
		trnModel.nd[m][topic] += 1;
		trnModel.nwsum[topic] += 1;
		trnModel.ndsum[m] += 1;
		
 		return topic;
	}
	
	public void computeTheta(){
		for (int m = 0; m < trnModel.M; m++){
			for (int k = 0; k < trnModel.K; k++){
				trnModel.theta[m][k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m]-trnModel.nd[m][k] + trnModel.K * trnModel.alpha);
			}
		}
	}
	
	public void computePhi(){
		for (int k = 0; k < trnModel.K; k++){
			for (int w = 0; w < trnModel.V; w++){
				trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k]-trnModel.nw[w][k]  + trnModel.V * trnModel.beta);
			}
		}
	}
	public void perplexitycal()
	{
		double per=0;
		int tokens=0;
		
		double per1=0;
		for(int i=0;i<trnModel.data.M;i++)
		{
			
			for(int j=0;j<trnModel.data.docs[i].length;j++)
			{
				int word=trnModel.data.docs[i].words[j];
				for(int k=0;k<trnModel.K;k++)
				{
					per1=per1+trnModel.theta[i][k]*trnModel.phi[k][word];
				}
				
				per=per+Math.log(per1);
				per1=0;
			}
			tokens+=trnModel.data.docs[i].length;
		}
		
		double perplexity=Math.exp(-per/tokens);
		System.out.println("Perplexity of "+" iterations : "+perplexity);
	}
}
