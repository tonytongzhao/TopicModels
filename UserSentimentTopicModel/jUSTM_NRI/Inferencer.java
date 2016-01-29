package jUSTM_NRI;

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
               // computeTrnTheta();
               // computeTrnPhi();
                
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
                int author=newModel.za[m].get(n);
                int _w = newModel.data.docs[m].words[n];
                int w = newModel.data.lid2gid.get(_w);
                int senti=newModel.zl[m].get(n);
                newModel.nwl[_w][topic][senti] -= 1;
                newModel.nal[author][topic][senti] -= 1;
                newModel.nwlsum[topic][senti] -= 1;
                newModel.nalsum[author][senti] -= 1;
                newModel.nasum[author] -= 1;
                int num_authors=newModel.da[m].size();
                double Vbeta = trnModel.V * newModel.beta;
                double Kalpha = trnModel.K * newModel.alpha;                    
                double Sgamma = trnModel.S * newModel.gamma;
                double [] ps = new double[num_authors*newModel.K*newModel.S];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
              
                for (int b = 0; b < num_authors; ++b)
                {
                	int a = newModel.da[m].get(b);
                	for(int s = 0; s<newModel.S; s++)
                	{         
                        for (int k = 0; k < newModel.K; k++){
                        	
                        		ps[current_index] = (trnModel.nwl[w][k][s] +newModel.nwl[_w][k][s] + trnModel.beta)/(trnModel.nwlsum[k][s] +newModel.nwlsum[k][s]+ Vbeta) * (newModel.nal[a][k][s] + trnModel.alpha)/(newModel.nalsum[a][s] + Kalpha)*(newModel.nalsum[a][s]+newModel.gamma)/(newModel.nasum[a]+Sgamma);
                                total_prob += ps[current_index++];
                        	}              
                        }
                }
                // cummulate multinomial parameters
                for (int k = 1; k < newModel.K; k++){
                        newModel.p[k] += newModel.p[k - 1];
                }
                
                // scaled sample because of unnormalized p[]
                int _topic = 0;
                int _author = 0;
                int _sentiment=0;
     
                double u = Math.random() * total_prob;
                
                for (int i = 0; i < ps.length; ++i){
                        if (ps[i] > u)
                        {
                                _topic = i % trnModel.K;
                                _sentiment = (i / trnModel.K)%trnModel.S;
                                _author = i /(trnModel.K*trnModel.S);
                                break;
                        }
                }  
                int a = newModel.da[m].get(_author);
                topic = _topic;  
                //set the author for the word (the topic is set in evaluate())
                newModel.za[m].set(n, a);
                newModel.zl[m].set(n, _sentiment);
                // add newly estimated z_i to count variables
                newModel.nwl[w][topic][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nal[a][topic][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nwlsum[topic][_sentiment] += 1 ;
                //* trnModel.word_weights[w][m];
                newModel.nalsum[a][_sentiment] += 1;
                //* trnModel.word_weights[w][m];
              
                return topic;
        }
        
        protected void computeNewTheta(){
                for (int m = 0; m < newModel.A; m++){
                	for(int n=0;n<newModel.S;n++)
                	{
                        for (int k = 0; k < newModel.K; k++){
                                newModel.theta[m][k][n] = (newModel.nal[m][k][n] + newModel.alpha) / (newModel.nalsum[m][n] + newModel.K * newModel.alpha);
                        }
                    }//end foreach topic
                }//end foreach new document
       }
        
        protected void computeNewPhi(){
        	for(int s=0;s<newModel.S;s++)
        	{
                for (int k = 0; k < newModel.K; k++){
                        for (int _w = 0; _w < newModel.V; _w++){
                                Integer id = newModel.data.lid2gid.get(_w);
                                
                                if (id != null){
                                        newModel.phi[k][_w][s] = (trnModel.nwl[id][k][s] + newModel.nwl[_w][k][s] + newModel.beta) / (trnModel.nwlsum[k][s] + newModel.nwlsum[k][s] + trnModel.V * newModel.beta);
                                }
                        }//end foreach word
             
                }// end foreach topic
        	}
       }
        
        /*protected void computeTrnTheta(){
                for (int m = 0; m < trnModel.M; m++){
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][k] = (trnModel.na[m][k] + trnModel.alpha) / (trnModel.nasum[m] + trnModel.K * trnModel.alpha);
                        }
                }
        }
        
        protected void computeTrnPhi(){
                for (int k = 0; k < trnModel.K; k++){
                        for (int w = 0; w < trnModel.V; w++){
                                trnModel.phi[k][w] = (trnModel.nw[w][k] + trnModel.beta) / (trnModel.nwsum[k] + trnModel.V * trnModel.beta);
                        }
                }
        }*/
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
    				int senti=newModel.zl[i].get(j);
    				
    					for(int k=0;k<newModel.K;k++)
    					{
    						per1=per1+newModel.theta[author][k][senti]*newModel.phi[k][word][senti];
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