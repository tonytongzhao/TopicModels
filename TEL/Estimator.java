package TEM;

import java.io.File;
import java.util.Vector;

public class Estimator {
        
        // output model
        protected Model trnModel;
        LDACmdOption option;
        double [] ps;
        public boolean init(LDACmdOption option){
                this.option = option;
                trnModel = new Model();                
                if (option.est){
                        if (!trnModel.initNewModel(option))
                                return false;
                        trnModel.data.localDict.writeWordMap(option.dir + File.separator + option.wordMapFileName);
                        trnModel.data.writeAuthorMap(option.dir + File.separator + "Author_map.txt");
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
                for (trnModel.liter = lastIter + 1; trnModel.liter < trnModel.niters + lastIter; trnModel.liter++){
                        System.out.println("Iteration " + trnModel.liter + " ...");
                        
                        // for all z_i
                        for (int m = 0; m < trnModel.M; m++){                           
                                for (int n = 0; n < trnModel.data.docs[m].length; n++){
                                        // z_i = z[m][n]
                                        // sample from p(z_i, a_i|z_-i, a_-i w)
                                        int topic = sampling(m, n);
                                }// end for each word
                        }// end for each document        
                        if (option.savestep > 0){
                                if (trnModel.liter % option.savestep == 0){
                                        System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
                                        computeTheta();
                                        computePhi();
                                        computePi();
                                        trnModel.saveModel("model-" + Conversion.ZeroPad(trnModel.liter, 5));
                                }
                        }
                }// end iterations             
                System.out.println("Gibbs sampling completed!\n");
                System.out.println("Saving the final model!\n");
                computeTheta();
                computePhi();
                computePi();
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
                // remove z_i from the count variables
        		// get topic and author assigned to the nth word of Doc m prior 
                int topic = trnModel.z[m].get(n);
                int asker = trnModel.za[m].get(n);
                int replier= trnModel.zr[m].get(n);
                // get the authors of Doc m and word n
                int w = trnModel.data.docs[m].words[n];
                int num_authors = trnModel.dr[m].size();
                //int num_authors=trnModel.A;
                trnModel.nwz[w][topic]-=1;
                trnModel.nxz[asker][topic] -= 1 ;
                trnModel.nxzr[asker][topic][replier] -= 1 ;
                trnModel.nzsumw[topic] -= 1 ;
                trnModel.nxsumz[asker] -= 1;
                trnModel.nxzsumr[asker][topic]-=1;
               /* if( trnModel.naa[asker][replier]<0|| trnModel.naasum[asker]<0)
                {
                	System.out.println("GOD  DAMN  IT !!");
                }*/
                double Vbeta = trnModel.V * trnModel.beta;
                double Kalpha = trnModel.K * trnModel.alpha;
                
                
                ps= new double[num_authors*trnModel.K];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
                for (int b = 0; b < num_authors; ++b)
                {
                        int a = trnModel.dr[m].get(b);
                        for (int k = 0; k < trnModel.K; k++){
                        		double gamma = trnModel.ARTcount[asker][a][k]*0.01;
                        		double gammasum = 0.0;
                        		for(int res = 0; res < num_authors; res++)
                        			gammasum += trnModel.ARTcount[asker][res][k]*0.01;
                                ps[current_index] = (trnModel.nwz[w][k] + trnModel.beta)/(trnModel.nzsumw[k] + Vbeta) * (trnModel.nxz[asker][k] + trnModel.alpha)/(trnModel.nxsumz[asker] + Kalpha)*(trnModel.nxzr[asker][k][a] + gamma)/(trnModel.nxzsumr[asker][k] + gammasum);
                                total_prob += ps[current_index++];
                               
                        }
                }
                
                // turn the joint author/topic probabilities into a cumulative distribution
                int size = ps.length;
                for (int i = 1; i < size; ++i)
                        ps[i] += ps[i-1];
                
                // scaled sample from author/topic distribution because of unnormalized ps[]
                double u = Math.random() * total_prob;
                int _topic = 0;
                int _author = 0;
                for (int i = 0; i < size; ++i){
                        if (ps[i] > u)
                        {
                                _topic = i % trnModel.K;
                                _author = i - _topic;
                                _author = _author / trnModel.K;
                                break;
                        }
                }          
                int a = trnModel.dr[m].get(_author);
                topic = _topic;
                
                //set the author for the word (the topic is set in evaluate())   
                trnModel.zr[m].set(n, a);
                trnModel.z[m].set(n, topic);
                //trnModel.za[m].set(n, asker);

                // add newly estimated z_i to count variables
                trnModel.nwz[w][topic] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nxz[asker][topic] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nzsumw[topic] += 1 ;
                //* trnModel.word_weights[w][m];
                trnModel.nxsumz[asker] += 1;
                //* trnModel.word_weights[w][m];
        		trnModel.nxzr[asker][topic][a] += 1;
        		trnModel.nxzsumr[asker][topic] += 1;
        		
        		/*update*/
        		//sample y from beta(g, b). to be modified as Tang et al.
        		double y;
        		int gNum, bNum;
        		gNum = trnModel.GBCount[a][topic].getGeneralCount();
        		bNum = trnModel.GBCount[a][topic].getBestCount();
        		y = bNum/(gNum + bNum);
        		double p = Math.random();
        		if(_author==0 && p>y)
        			trnModel.GBCount[a][topic].bIncrement();
        		if(_author!=0 && p<y)
        			trnModel.GBCount[a][topic].gIncrement();
        		gNum = trnModel.GBCount[a][topic].getGeneralCount();
        		bNum = trnModel.GBCount[a][topic].getBestCount();
        		int numOfResponders = trnModel.dr[m].size();
        		trnModel.ARTcount[asker][a][topic]+=bNum/(gNum+bNum)/numOfResponders;
        		//trnModel.updateGamma();///modify
        		
                return topic;
        }
        
        
        public void computeTheta(){
                for (int m = 0; m < trnModel.A; m++){
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][k] = (trnModel.nxz[m][k] + trnModel.alpha) / (trnModel.nxsumz[m] + trnModel.K * trnModel.alpha);
                        }
                }
        }
        
        public void computePhi(){
                for (int k = 0; k < trnModel.K; k++){
                        for (int w = 0; w < trnModel.V; w++){
                                trnModel.phi[k][w] = (trnModel.nwz[w][k] + trnModel.beta) / (trnModel.nzsumw[k] + trnModel.V * trnModel.beta);
                        }
                }
        }
        public void computePi(){
        	for (int k = 0; k < trnModel.K; k++)
	            for (int a = 0; a < trnModel.A; a++){
            			double gammasum = 0.0;
            			for(int res = 0; res < trnModel.A; res++)
            				gammasum += trnModel.ARTcount[a][res][k]*0.01;
	                    for (int b = 0; b < trnModel.A; b++){
	                  		double gamma = trnModel.ARTcount[a][b][k]*0.01;
	                    	if(trnModel.nxzsumr[a][k]==0)
	                    	{
	                    		trnModel.pi[a][k][b] =0;
	                    	}
	                    	else
	                    	{
	                    		 trnModel.pi[a][k][b]= (trnModel.nxzr[a][k][b] + gamma) / (trnModel.nxzsumr[a][k] + gammasum);
	                             
	                    	}
	                    }
	            }
    }
}