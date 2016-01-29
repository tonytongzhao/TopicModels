package jUSTM_NRI;

import java.io.File;

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
                        trnModel.data.writeAuthorMap(option.dir + File.separator + "Author_map.txt");
                        trnModel.data.writeWordSentimentMap(option.dir + File.separator + "Word_Sentiment_map.txt");
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
                                        trnModel.z[m].set(n, topic);
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
                int current_author = trnModel.za[m].get(n);
                int senti_label=trnModel.zl[m].get(n);
                // get the authors of Doc m and word n
                int w = trnModel.data.docs[m].words[n];
                int num_authors = trnModel.da[m].size();
               
              
               if(trnModel.nal[current_author][topic][senti_label]<0||  trnModel.nalsum[current_author][senti_label]<0)
               { System.out.println("nal "+trnModel.nal[current_author][topic][senti_label]);
               	System.out.println( "nalsum "+ trnModel.nalsum[current_author][senti_label]);
               } 
               
               
             
                trnModel.nwl[w][topic][senti_label] -= 1 ;
              
                trnModel.nal[current_author][topic][senti_label] -= 1 ;
              
                trnModel.nwlsum[topic][senti_label] -= 1 ;
                
                trnModel.nalsum[current_author][senti_label] -= 1;
              
                
                double Vbeta = trnModel.V * trnModel.beta;
                double Kalpha = trnModel.K * trnModel.alpha;
                double Sgamma = trnModel.S * trnModel.gamma;
                double [] ps = new double[num_authors*trnModel.K*trnModel.S];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
              
                for (int b = 0; b < num_authors; ++b)
                {
                	int a = trnModel.da[m].get(b);
                	for(int s = 0; s<trnModel.S; s++)
                	{
                        
                        for (int k = 0; k < trnModel.K; k++){
                        	
                        		ps[current_index] = (trnModel.nwl[w][k][s] + trnModel.beta)/(trnModel.nwlsum[k][s] + Vbeta) * (trnModel.nal[a][k][s] + trnModel.alpha)/(trnModel.nalsum[a][s] + Kalpha)*(trnModel.nalsum[a][s]+trnModel.gamma)/(trnModel.nasum[a]+Sgamma);
                                total_prob += ps[current_index++];
                        	}              
                        }
                }
                int _topic = 0;
                int _author = 0;
                int _sentiment=0;
     
               
                
                // turn the joint author/topic probabilities into a cumulative distribution
                int size = ps.length;
                for (int i = 1; i < size; ++i)
                        ps[i] += ps[i-1];
                
                // scaled sample from author/topic distribution because of unnormalized ps[]
                double u = Math.random() * total_prob;
             
                for (int i = 0; i < size; ++i){
                        if (ps[i] > u)
                        {
                                _topic = i % trnModel.K;
                                _sentiment = (i / trnModel.K)%trnModel.S;
                                _author = i /(trnModel.K*trnModel.S);
                                break;
                        }
                }  
               
                int a = trnModel.da[m].get(_author);
                topic = _topic;  
                //set the author for the word (the topic is set in evaluate())
                trnModel.za[m].set(n, a);
                trnModel.zl[m].set(n, _sentiment);
                // add newly estimated z_i to count variables
                trnModel.nwl[w][topic][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nal[a][topic][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nwlsum[topic][_sentiment] += 1 ;
                //* trnModel.word_weights[w][m];
                trnModel.nalsum[a][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
              
                return topic;
        }
        
      
        public void computeTheta(){
                for (int m = 0; m < trnModel.A; m++){
                	for( int s =0; s< trnModel.S;s++)
                	{
                		 for (int k = 0; k < trnModel.K; k++){
                             trnModel.theta[m][k][s] = (trnModel.nal[m][k][s] + trnModel.alpha) / (trnModel.nalsum[m][s] + trnModel.K * trnModel.alpha);
                		
                            
                		}
                	}   
                }
            	
        }
        
        public void computePhi(){
                for (int k = 0; k < trnModel.K; k++){
                	for(int s=0; s<trnModel.S; s++)
                	{
                		for (int w = 0; w < trnModel.V; w++){
                            trnModel.phi[k][w][s] = (trnModel.nwl[w][k][s] + trnModel.beta) / (trnModel.nwlsum[k][s] + trnModel.V * trnModel.beta);
                		}
                	}
                        
                }
        }
        public void computePi(){
            for (int m = 0; m < trnModel.A; m++){
            	for(int s=0; s<trnModel.S; s++)
            	{
                   trnModel.pi[m][s] = (trnModel.nalsum[m][s] + trnModel.gamma) / (trnModel.nasum[m] + trnModel.S * trnModel.gamma);
            		
            	}
                    
            }
    }
}