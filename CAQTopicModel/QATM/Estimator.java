package QATM;
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
                int asker = trnModel.za[m].get(n);
                int replier= trnModel.zr[m].get(n);
                // get the authors of Doc m and word n
                int w = trnModel.data.docs[m].words[n];
                int num_authors = trnModel.dr[m].size();
                trnModel.nqa[m][replier]-=1;
                trnModel.nw[w][topic] -= 1 ;
                trnModel.nqat[m][replier][topic] -= 1 ;
                trnModel.nwsum[topic] -= 1 ;
                trnModel.nqatsum[m][replier] -= 1;
                trnModel.nqasum[m]-=1;
      	
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
                                ps[current_index] = (trnModel.nw[w][k] + trnModel.beta)/(trnModel.nwsum[k] + Vbeta) * (trnModel.nqat[m][a][k] + trnModel.alpha)/(trnModel.nqatsum[m][a] + Kalpha)*(trnModel.nqa[m][a] + trnModel.gammaforusers[m][a])/(trnModel.nqasum[m] + trnModel.A*trnModel.gammaforusers[m][a]);
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
                
                trnModel.zr[m].set(n,a);
                // add newly estimated z_i to count variables
                trnModel.nw[w][topic] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nqa[m][a] += 1;
                //* trnModel.word_weights[w][m];
                trnModel.nwsum[topic] += 1 ;
                //* trnModel.word_weights[w][m];
                trnModel.nqasum[m] += 1;
                //* trnModel.word_weights[w][m];
        		trnModel.nqat[m][a][topic] += 1;
        		
        		trnModel.nqatsum[m][a] += 1;
                return topic;
        }
        
        
        public void computeTheta(){
                for (int m = 0; m < trnModel.M; m++){
                	 for (int n = 0; n < trnModel.A; n++)
                	{
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][n][k] = (trnModel.nqat[m][n][k] + trnModel.alpha) / (trnModel.nqatsum[m][n] + trnModel.K * trnModel.alpha);
                        }
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
        public void computePi(){
            for (int a = 0; a < trnModel.M; a++){
                    for (int b = 0; b < trnModel.A; b++){
                            trnModel.pi[a][b] = (trnModel.nqa[a][b] + trnModel.gammaforusers[a][b]) / (trnModel.nqasum[a] + trnModel.A * trnModel.gammaforusers[a][b]);
                    }
            }
    }
}