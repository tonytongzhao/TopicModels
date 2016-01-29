package jGibbATM;

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
                        computeNewTheta();
                        computeNewPhi();
                        System.out.println(newModel.liter);
                        perplexitycal();
                }// end iterations
                
                System.out.println("Gibbs sampling for inference completed!");          
                System.out.println("Saving the inference outputs!");
                
                computeNewTheta();
                computeNewPhi();
                System.out.println(newModel.liter);
                perplexitycal();
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
                int _a =newModel.za[m].get(n);
                int _w = newModel.data.docs[m].words[n];
                int w = newModel.data.lid2gid.get(_w);
                newModel.nw[_w][topic] -= 1;
                newModel.na[_a][topic] -= 1;
                newModel.nwsum[topic] -= 1;
                newModel.nasum[_a] -= 1;
                
                double Vbeta = trnModel.V * newModel.beta;
                double Kalpha = trnModel.K * newModel.alpha;
                
                int num_authors=newModel.da[m].size();
                // do multinomial sampling via cummulative method        
                double [] ps = new double[num_authors*newModel.K];
               
                //do multinomial sampling via cumulative method
                for (int b = 0; b < num_authors; ++b)
                {
                        int a3 = newModel.da[m].get(b);
                        
                        	for (int k = 0; k < newModel.K; k++){                   
                                ps[k] = (trnModel.nw[w][k] + newModel.nw[_w][k] + newModel.beta)/(trnModel.nwsum[k] +  newModel.nwsum[k] + Vbeta) *
                                                (newModel.na[a3][k]+ newModel.alpha)/(newModel.nasum[a3] + Kalpha);
                        	}                                
                }
                int _topic = 0;
                int _author = 0;
                
                // cummulate multinomial parameters
                int size = ps.length;
                for (int i = 1; i < size; ++i)
                        ps[i] += ps[i-1];
                
                // scaled sample because of unnormalized p[]
                double u = Math.random() * ps[size - 1];
                
                for (int i = 0; i < size; ++i){
                    if (ps[i] > u)
                    {
                            _topic = i % trnModel.K;
                            _author = i - _topic;
                            _author = _author / trnModel.K;
                            break;
                    }
            }
                _a = newModel.da[m].get(_author);
                topic = _topic;
                // add newly estimated z_i to count variables
                newModel.nw[_w][topic] += 1;
                newModel.na[_a][topic] += 1;
                newModel.nwsum[topic] += 1;
                newModel.nasum[_a] += 1;
                newModel.za[m].set(n, _a);
                return topic;
        }
        
        protected void computeNewTheta(){
                for (int m = 0; m < newModel.A; m++){
                		
                		for (int k = 0; k < newModel.K; k++){
                            newModel.theta[m][k] = (newModel.na[m][k]+ newModel.alpha) / (newModel.nasum[m] + newModel.K * newModel.alpha);
                   
                }
                        //end foreach topic
                }//end foreach new document
        }
        
        protected void computeNewPhi(){
                for (int k = 0; k < newModel.K; k++){
                        for (int _w = 0; _w < newModel.V; _w++){
                                Integer id = newModel.data.lid2gid.get(_w);
                                
                                if (id != null){
                                        newModel.phi[k][_w] = (trnModel.nw[id][k] + newModel.nw[_w][k] + newModel.beta) / (newModel.nwsum[k] + trnModel.nwsum[k] + trnModel.V * newModel.beta);
                                }
                        }//end foreach word
                }// end foreach topic
        }
        
        protected void computeTrnTheta(){
                for (int m = 0; m < trnModel.A; m++){
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
    					for(int k=0;k<newModel.K;k++)
    					{
    						per1=per1+newModel.theta[author][k]*newModel.phi[k][word];
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