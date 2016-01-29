package AATM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class Inferencer {       
        // Train model
        public Model trnModel;
        public Dictionary globalDict;
        private LDACmdOption option;
        double [] ps;
        private Model newModel;
        public int niters = 100;
        
        //-----------------------------------------------------
        // Init method
        //-----------------------------------------------------
        public boolean init(LDACmdOption option){
                this.option = option;
                trnModel = new Model();
                
                if (!trnModel.initEstimatedModel(option))
                        return false;           
                
                globalDict = trnModel.data.localDict;
                computeTrnTheta();
                computeTrnPhi();
                
                return true;
        }
        
        //inference new model ~ getting data from a specified dataset
        public Model inference( LDADataset newData){
                System.out.println("init new model");
                Model newModel = new Model();           
                
                newModel.initNewModel(option, newData, trnModel);               
                this.newModel = newModel;               
                
                System.out.println("Sampling " + niters + " iteration for inference!");         
                for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
                        //System.out.println("Iteration " + newModel.liter + " ...");
                        
                        // for all newz_i
                        for (int m = 0; m < newModel.M; ++m){
                                for (int n = 0; n < newModel.data.docs[m].length; n++){
                                        // (newz_i = newz[m][n]
                                        // sample from p(z_i|z_-1,w)
                                        int topic = infSampling(m, n);
                                        newModel.z[m].set(n, topic);
                                }
                        }//end foreach new doc
                        computeNewTheta();
                        computeNewPhi();
                        System.out.println(newModel.liter);
                        perplexitycal();
                }// end iterations
                
                System.out.println("Gibbs sampling for inference completed!");
                
                computeNewTheta();
                computeNewPhi();
                newModel.liter--;
                return this.newModel;
        }
        
        public Model inference(String [] strs){
                //System.out.println("inference");
                Model newModel = new Model();
                
                //System.out.println("read dataset");
                LDADataset dataset = LDADataset.readDataSet(strs, globalDict);
                
                return inference(dataset);
        }
        
        //inference new model ~ getting dataset from file specified in option
        public Model inference(){       
                //System.out.println("inference");
                
                newModel = new Model();
                if (!newModel.initNewModel(option, trnModel)) return null;
                
                System.out.println("Sampling " + niters + " iteration for inference!");
                
                for (newModel.liter = 1; newModel.liter <= niters; newModel.liter++){
                        //System.out.println("Iteration " + newModel.liter + " ...");
                        
                        // for all newz_i
                        for (int m = 0; m < newModel.M; ++m){
                                for (int n = 0; n < newModel.data.docs[m].length; n++){
                                        // (newz_i = newz[m][n]
                                        // sample from p(z_i|z_-1,w)
                                        int topic = infSampling(m, n);
                                        newModel.z[m].set(n, topic);
                                }
                        }//end foreach new doc
                        computeNewPhi();
        				computeNewTheta();
        				System.out.println(newModel.liter);
        				perplexitycal();
                }// end iterations
                
                System.out.println("Gibbs sampling for inference completed!");          
                System.out.println("Saving the inference outputs!");
                
                computeNewTheta();
                computeNewPhi();
                newModel.liter--;
                newModel.saveModel(newModel.dfile + "." + newModel.modelName);          
                
                return newModel;
        }
        
        /**
         * do sampling for inference
         * m: document number
         * n: word number?
         */
        protected int infSampling(int m, int n){
                // remove z_i from the count variables
                int topic = newModel.z[m].get(n);
                int _w = newModel.data.docs[m].words[n];
                int w = newModel.data.lid2gid.get(_w);
                int _asker = newModel.za[m].get(n);
                int asker = newModel.data.la2ga.get(_asker);
                int _replier= newModel.zr[m].get(n);
                int replier = newModel.data.la2ga.get(_replier);
                newModel.nw[_w][topic] -= 1;
                newModel.naat[_asker][_replier][topic] -= 1;
                newModel.naa[_asker][_replier]-=1;
                newModel.nwsum[topic] -= 1;
                newModel.naatsum[_asker][_replier]-=1;
                newModel.naasum[_asker] -= 1;
                int num_authors = newModel.dr[m].size();
                double Vbeta = trnModel.V * newModel.beta;
                double Kalpha = trnModel.K * newModel.alpha;
                
                // do multinomial sampling via cummulative method               
                ps= new double[num_authors*trnModel.K];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
                for (int b = 0; b < num_authors; ++b)
                {
                        int a = newModel.dr[m].get(b);
                        for (int k = 0; k < trnModel.K; k++){
                        	
                                ps[current_index] = (trnModel.nw[w][k]+newModel.nw[_w][k] + newModel.beta)/(trnModel.nwsum[k] +newModel.nwsum[k]+ Vbeta) * (newModel.naat[asker][a][k] + newModel.alpha)/(newModel.naatsum[asker][a] + Kalpha)*(newModel.naa[asker][a] + newModel.gammaforusers[asker][a])/(newModel.naasum[asker] + newModel.A*newModel.gammaforusers[asker][a]);
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
                
                int a = newModel.dr[m].get(_author);
                topic = _topic;
                
                //set the author for the word (the topic is set in evaluate())
                
                newModel.zr[m].set(n,a);
                // add newly estimated z_i to count variables
                newModel.nw[w][topic] += 1;
                //* trnModel.word_weights[w][m];
                newModel.naa[asker][a] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nwsum[topic] += 1 ;
                //* trnModel.word_weights[w][m];
                newModel.naasum[asker] += 1;
                //* trnModel.word_weights[w][m];
                newModel.naat[asker][a][topic] += 1;
        		
                newModel.naatsum[asker][a] += 1;
              
                return topic;
        }
        
        protected void computeNewTheta(){
                for (int m = 0; m < newModel.A; m++){
                	for (int n = 0; n < newModel.A; n++){
                        for (int k = 0; k < newModel.K; k++){
                                newModel.theta[m][n][k] = (newModel.naat[m][n][k] + newModel.alpha) / (newModel.naatsum[m][n] + newModel.K * newModel.alpha);
                        }//end foreach topic
                	}
                }//end foreach new document
        }
        
        protected void computeNewPhi(){
                for (int k = 0; k < newModel.K; k++){
                        for (int _w = 0; _w < newModel.V; _w++){
                                Integer id = newModel.data.lid2gid.get(_w);
                                
                                if (id != null){
                                        newModel.phi[k][_w] = (trnModel.nw[id][k] + newModel.nw[_w][k] + newModel.beta) / (trnModel.nwsum[k] + newModel.nwsum[k] + trnModel.V * newModel.beta);
                                }
                        }//end foreach word
                }// end foreach topic
        }
        
        protected void computeTrnTheta(){
                for (int m = 0; m < trnModel.A; m++){
                	 for (int n = 0; n < trnModel.A; n++){
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][n][k] = (trnModel.naat[m][n][k] + trnModel.alpha) / (trnModel.naatsum[m][n] + trnModel.K * trnModel.alpha);
                        }
                	 }
                }
        }
        
        protected void computeTrnPhi(){
                for (int k = 0; k < trnModel.K; k++){
                        for (int w = 0; w < trnModel.V; w++){
                                trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + trnModel.V * trnModel.beta);
                        }
                }
        }
        
        public void computeNewPi(){
            for (int a = 0; a < newModel.A; a++){
                    for (int b = 0; b < newModel.A; b++){
                    	newModel.pi[a][b] = (newModel.naa[a][b] + newModel.gammaforusers[a][b]) / (newModel.naasum[a] + newModel.A * newModel.gammaforusers[a][b]);
                    }
            }
        }
      
        
        public void perplexitycal()
    	{
    		double per=0;
    		int tokens=0;
    		
    		double per1=0;
    		for(int i=0;i<newModel.data.M;i++)
    		{
    			
    			for(int j=0;j<newModel.data.docs[i].length;j++)
    			{
    				int word=newModel.data.docs[i].words[j];
    				int author=newModel.za[i].get(j);
    				int replier=newModel.zr[i].get(j);
    					for(int k=0;k<newModel.K;k++)
    					{
    						per1=per1+newModel.theta[author][replier][k]*newModel.phi[k][word];
    					}
    					per=per+Math.log(per1);
    					per1=0;
    			}
    			
    			tokens+=newModel.data.docs[i].length;
    		}
    		
    		double perplexity=Math.exp(-per/tokens);
    		System.out.println("Perplexity : "+perplexity);
    	}
      
}