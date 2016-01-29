package jGibbATM;

import java.io.File;
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
                }// end iterations              
                
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
                // remove z_i from the count variables
        		// get topic and author assigned to the nth word of Doc m prior 
                int topic = trnModel.z[m].get(n);
                int current_author = trnModel.za[m].get(n);
                
                // get the authors of Doc m and word n
                int w = trnModel.data.docs[m].words[n];
                int num_authors = trnModel.da[m].size();
                
                trnModel.nw[w][topic] -= 1 ;
                ///* trnModel.word_weights[w][m];
                trnModel.na[current_author][topic] -= 1 ;
                ///* trnModel.word_weights[w][m];
                trnModel.nwsum[topic] -= 1 ;
                ///* trnModel.word_weights[w][m];
                trnModel.nasum[current_author] -= 1;
               /// * trnModel.word_weights[w][m];
        		trnModel.nd[m][topic] -= 1;
        		trnModel.ndsum[topic] -= 1;
        		
        		
                double Vbeta = trnModel.V * trnModel.beta;
                double Kalpha = trnModel.K * trnModel.alpha;
                
                double [] ps = new double[num_authors*trnModel.K];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
                for (int b = 0; b < num_authors; ++b)
                {
                        int a = trnModel.da[m].get(b);
                        for (int k = 0; k < trnModel.K; k++){
                                ps[current_index] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) * (trnModel.na[a][k] + trnModel.alpha)/(trnModel.nasum[a] + Kalpha);
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
                
                int a = trnModel.da[m].get(_author);
                topic = _topic;
                
                //set the author for the word (the topic is set in evaluate())
                trnModel.za[m].set(n, a);
                
                // add newly estimated z_i to count variables
                trnModel.nw[w][topic] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.na[a][topic] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nwsum[topic] += 1 ;
                //* trnModel.word_weights[w][m];
                trnModel.nasum[a] += 1;
                //* trnModel.word_weights[w][m];
        		trnModel.nd[m][topic] += 1;
        		
        		trnModel.ndsum[topic] += 1;
                return topic;
        }
        
        // the old, wrong update method
        
        /*
        public int sampling(int m, int n){
                // remove z_i from the count variable
                int topic = trnModel.z[m].get(n);
                int current_author = trnModel.za[m].get(n);
                int w = trnModel.data.docs[m].words[n];
                // find how many authors this document has and uniformly sample one
                int num_authors = trnModel.da[m].size();
                //int a = trnModel.da[m].get((int) (Math.random() * num_authors));
                //trnModel.za[m].set(n, a);
                
                trnModel.nw[w][topic] -= 1 * trnModel.word_weights[w][m];
                trnModel.na[current_author][topic] -= 1 * trnModel.word_weights[w][m];
                trnModel.nwsum[topic] -= 1 * trnModel.word_weights[w][m];
                trnModel.nasum[current_author] -= 1 * trnModel.word_weights[w][m];
                
                double Vbeta = trnModel.V * trnModel.beta;
                double Kalpha = trnModel.K * trnModel.alpha;
                
                //do multinomial sampling via cumulative method
                for (int k = 0; k < trnModel.K; k++){
                        trnModel.p[k] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) *
                                        (trnModel.na[a][k] + trnModel.alpha)/(trnModel.nasum[a] + Kalpha);
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
                //System.out.println("w: "+w+", topic: "+topic+", m: "+m);
                trnModel.nw[w][topic] += 1 * trnModel.word_weights[w][m];
                trnModel.na[a][topic] += 1 * trnModel.word_weights[w][m];
                trnModel.nwsum[topic] += 1 * trnModel.word_weights[w][m];
                trnModel.nasum[a] += 1 * trnModel.word_weights[w][m];
                
                return topic;
        }
        */
        public void computeTheta(){
                for (int m = 0; m < trnModel.A; m++){
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][k] = (trnModel.na[m][k] + trnModel.alpha) / (trnModel.nasum[m] + trnModel.K * trnModel.alpha);
                        }
                }
                
                for (int m = 0; m < trnModel.M; m++){
        			for (int k = 0; k < trnModel.K; k++){
        				trnModel.theta1[m][k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + trnModel.K * trnModel.alpha);
        			}
        		}
        }
        
        public void computePhi(){
                for (int k = 0; k < trnModel.K; k++){
                        for (int w = 0; w < trnModel.V; w++){
                                trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + trnModel.V * trnModel.beta);
                        }
                }
        }
}