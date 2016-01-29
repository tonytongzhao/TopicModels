package TEM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
                                        //newModel.z[m].set(n, topic);
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
                computeNewPi();
                newModel.liter--;
                newModel.saveModel(newModel.dfile + "." + newModel.modelName);          
                output();
                return newModel;
        }
        
        protected boolean output(){
    		try {
    			BufferedWriter writer = new BufferedWriter(new FileWriter(newModel.dir + File.separator + "topic_" + newModel.K + ".inference.txt"));
    			writer.write(newModel.M+"");
    			writer.write("\n");
    			for (int i = 0; i < newModel.M; i++){
    				int x = newModel.data.docs[i].authors[0];
    				double max = newModel.theta[x][0];
    				int maxIndex = 0;
    				for(int j = 1; j < newModel.theta[x].length; j++){
    					if(newModel.theta[x][j] > max){
    						max = newModel.theta[x][j];
    						maxIndex = j;
    					}
    				}
    				int z = maxIndex;
    				int[] responders;
    				responders = sort(newModel.pi[x][z]);
    				for (int j = 0; j < responders.length-1; j++) {
    					writer.write(responders[j] + ",");
    				}
    				writer.write(responders[responders.length-1]+";");
    				writer.write(newModel.data.docs[i].authors[1]+";");
    				for (int k = 1; k<newModel.data.docs[i].authors.length; k++)
    					writer.write(newModel.data.docs[i].authors[k]+",");
    				writer.write("\n");
    			}
    			writer.close();
    		} catch (Exception e) {
    			System.out
    					.println("Error while saving topic distribution file for this model: "
    							+ e.getMessage());
    			e.printStackTrace();
    			return false;
    		}
    		return true;
        }
        
            
        private int[] sort(double[] respondersProb){
        	double[] temp = new double[respondersProb.length];
        	int[] rank = new int[respondersProb.length];
        	for(int i = 0; i < respondersProb.length; i++){
        		temp[i] = respondersProb[i];
        		rank[i] = i;
        	}
    		double tmp;
    		int tmr;
    		for(int i=1;i<respondersProb.length;i++)
    		{
    			tmp = temp[i];
    			tmr = rank[i];
    			for(int j=0;j<i;j++)
    			{
    				if(tmp > temp[j])
    				{
    					for(int k=i;k>j;k--)
    					{
    						temp[k]=temp[k-1];
    						rank[k]=rank[k-1];
    					}
    					temp[j]=tmp;
    					rank[j]=tmr;
    					break;
    				}	
    			}
    		}
    		return rank;
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
               // int asker = newModel.data.la2ga.get(_asker);
                int _replier= newModel.zr[m].get(n);
              //  int replier = newModel.data.la2ga.get(_replier);
                newModel.nwz[_w][topic] -= 1;
                newModel.nxz[_asker][topic] -= 1;
                newModel.nxzr[_asker][topic][_replier]-=1;
                trnModel.nzsumw[topic] -= 1 ;
                trnModel.nxsumz[_asker] -= 1;
                trnModel.nxzsumr[_asker][topic]-=1;
                int num_authors = newModel.dr[m].size();
               // int num_authors=newModel.A;
                double Vbeta = trnModel.V * newModel.beta;
                double Kalpha = trnModel.K * newModel.alpha;
                
                // do multinomial sampling via cummulative method               
                ps= new double[num_authors*trnModel.K];
                int current_index = 0;
                double total_prob = 0;
                //do multinomial sampling via cumulative method
                for (int b = 0;b < num_authors; ++b)
                {
                        int a = newModel.dr[m].get(b);
                        for (int k = 0; k < trnModel.K; k++){
                        	
                                ps[current_index] = (trnModel.nwz[w][k]+newModel.nwz[_w][k] + newModel.beta)/(trnModel.nzsumw[k] +newModel.nzsumw[k]+ Vbeta) * (newModel.nxz[_asker][k] + newModel.alpha)/(newModel.nxsumz[_asker] + Kalpha)*(newModel.nxzr[_asker][k][a] + newModel.gammaforaskertopic[_asker][a][k])/(newModel.nxzsumr[_asker][k] + newModel.gammatopicsum[_asker][k]);
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
               // int a =_author;
                topic = _topic;
                
                //set the author for the word (the topic is set in evaluate())
                
                newModel.zr[m].set(n,a);
                newModel.z[m].set(n, topic);
                //newModel.za[m].set(n, _asker);

                // add newly estimated z_i to count variables
                newModel.nwz[_w][topic] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nxz[_asker][topic] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nzsumw[topic] += 1 ;
                //* trnModel.word_weights[w][m];
                newModel.nxsumz[_asker] += 1;
                //* trnModel.word_weights[w][m];
                newModel.nxzr[_asker][topic][a] += 1;
        		
                newModel.nxzsumr[_asker][topic] += 1;
              
                return topic;
        }
        
        protected void computeNewTheta(){
                for (int m = 0; m < newModel.A; m++){
                        for (int k = 0; k < newModel.K; k++){
                                newModel.theta[m][k] = (newModel.nxz[m][k] + newModel.alpha) / (newModel.nxsumz[m] + newModel.K * newModel.alpha);
                        }//end foreach topic
                }//end foreach new document
        }
        
        protected void computeNewPhi(){
                for (int k = 0; k < newModel.K; k++){
                        for (int _w = 0; _w < newModel.V; _w++){
                                Integer id = newModel.data.lid2gid.get(_w);
                                
                                if (id != null){
                                        newModel.phi[k][_w] = (trnModel.nwz[id][k] + newModel.nwz[_w][k] + newModel.beta) / (trnModel.nzsumw[k] + newModel.nzsumw[k] + trnModel.V * newModel.beta);
                                }
                        }//end foreach word
                }// end foreach topic
        }
        
        protected void computeTrnTheta(){
                for (int m = 0; m < trnModel.A; m++){
                        for (int k = 0; k < trnModel.K; k++){
                                trnModel.theta[m][k] = (trnModel.nxz[m][k] + trnModel.alpha) / (trnModel.nxsumz[m] + trnModel.K * trnModel.alpha);
                        }
                }
        }
        
        protected void computeTrnPhi(){
                for (int k = 0; k < trnModel.K; k++){
                        for (int w = 0; w < trnModel.V; w++){
                                trnModel.phi[k][w] = (trnModel.nwz[w][k] + trnModel.beta) / (trnModel.nzsumw[k] + trnModel.V * trnModel.beta);
                        }
                }
        }
        
        public void computeNewPi(){
        	for (int k = 0; k < newModel.K; k++)
	            for (int a = 0; a < newModel.A; a++){
	                    for (int b = 0; b < newModel.A; b++){
	                    	newModel.pi[a][k][b] = (newModel.nxzr[a][k][b] + newModel.gammaforaskertopic[a][b][k]) / (newModel.nxzsumr[a][k] + newModel.gammatopicsum[a][k]);
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