package jUSTM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

public class Model {    
        
        //---------------------------------------------------------------
        //      Class Variables
        //---------------------------------------------------------------
        
        public static final boolean use_tfidf = true;
        
        public static String tassignSuffix;     //suffix for topic assignment file
        public static String thetaSuffix;      //suffix for theta (topic - document distribution) file
        public static String thetadoc;
        public static String phiSuffix;//suffix for phi file (topic - word distribution) file
        public static String piSuffix;//suffix for pi file (author - sentiment distribution) file
        public static String othersSuffix;      //suffix for containing other parameters
        public static String twordsSuffix;              //suffix for file containing words-per-topics
        
        //---------------------------------------------------------------
        //      Model Parameters and Variables
        //---------------------------------------------------------------
        
        public String wordMapFile;              //file that contain word to id map
        public String trainlogFile;     //training log file     
        
        public String dir;
        public String dfile;
        public String modelName;
        public int modelStatus;                 //see Constants class for status of model
        public LDADataset data;                 // link to a dataset
        
        public int M; //dataset size (i.e., number of docs)
        public double sentiparameter=0.3;
        public int A; //number of authors
        public int S=3;  // 0 negative; 1 neutral; 2 positive;
        public int V; //vocabulary size
        public int K; //number of topics
        public double alpha, beta, gamma; //LDA  hyperparameters
        public int niters; //number of Gibbs sampling iteration
        public int liter; //the iteration at which the model was saved  
        public int savestep; //saving period
        public int twords; //print out top words per each topic
        public int withrawdata;
        
        // Estimated/Inferenced parameters
        public double [][][] theta; //theta: author - topic - sentiment  distributions, size A x K x S
        public double [][][]  theta_doc;
        public double [][][] phi; // phi: topic-word - sentiment distributions, size K x V x S
        public double [][]  pi;    //pi : author - sentiment distribution, size A x S
        // Weighting values
        public double [][] word_weights; //maps each word/doc pair to a weight, size V x M
        
        // Temp variables while sampling
        public Vector<Integer> [] z; //topic assignments for words, size M x doc.size()
        public Vector<Integer> [] za; 
        public Vector<Integer> [] zl; 
        public Vector<Integer> [] da; 
        protected double [][][] nwl; //nw[i][j][l]: number of instances of word/term i assigned to topic j and sentiment l, size V x K x S
        protected double [][][] nal; //nd[i][j][l]: number of words from author i assigned to topic j, size A x K x S
        protected double [][] nwlsum; //nwsum[j][l]: total number of words assigned to topic j and sentiment l, size K x S
        protected double [][] nalsum; //ndsum[i][l]: total number of words in author i and sentiment l, size A x S
        protected double [] nasum;  //nasum[i]: total number of words in author i;
        protected double [][][] ndl;
        protected double [][] ndlsum;
        // temp variables for sampling
        protected double [] p; 
        
        //---------------------------------------------------------------
        //      Constructors
        //---------------------------------------------------------------       

        public Model(){
                setDefaultValues();     
        }
        
        /**
         * Set default values for variables
         */
        public void setDefaultValues(){
                wordMapFile = "wordmap.txt";
                trainlogFile = "trainlog.txt";
                tassignSuffix = ".tassign";
                thetaSuffix = ".theta";
                thetadoc=".theta_doc";
                phiSuffix = ".phi";
                piSuffix = ".pi";
                othersSuffix = ".others";
                twordsSuffix = ".twords";
                
                dir = "./";
                dfile = "trndocs.dat";
                modelName = "model-final";
                modelStatus = Constants.MODEL_STATUS_UNKNOWN;           
                
                M = 0;
                V = 0;
                A = 0;
                K = 100;
                alpha = 50.0 / K;
                beta = 0.1;
                gamma= 0.1;
                niters = 2000;
                liter = 0;
                
                z = null;
                da = null;
                za = null;
                zl = null;
                nwl = null;
                nal = null;
                nwlsum = null;
                nalsum = null;
                nasum = null;
                ndl=null;
                ndlsum=null;
                theta = null;
                theta_doc=null;
                phi = null;
                pi=null;
        }
        
        //---------------------------------------------------------------
        //      I/O Methods
        //---------------------------------------------------------------
        /**
         * read other file to get parameters
         */
        
        // ***
        // DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
        // ***
        protected boolean readOthersFile(String otherFile){
                //open file <model>.others to read:
                
                try {
                        BufferedReader reader = new BufferedReader(new FileReader(otherFile));
                        String line;
                        while((line = reader.readLine()) != null){
                                StringTokenizer tknr = new StringTokenizer(line,"= \t\r\n");
                                
                                int count = tknr.countTokens();
                                if (count != 2)
                                        continue;
                                
                                String optstr = tknr.nextToken();
                                String optval = tknr.nextToken();
                                
                                if (optstr.equalsIgnoreCase("alpha")){
                                        alpha = Double.parseDouble(optval);                                     
                                }
                                else if (optstr.equalsIgnoreCase("beta")){
                                        beta = Double.parseDouble(optval);
                                }
                                else if (optstr.equalsIgnoreCase("ntopics")){
                                        K = Integer.parseInt(optval);
                                }
                                else if (optstr.equalsIgnoreCase("liter")){
                                        liter = Integer.parseInt(optval);
                                }
                                else if (optstr.equalsIgnoreCase("nwords")){
                                        V = Integer.parseInt(optval);
                                }
                                else if (optstr.equalsIgnoreCase("ndocs")){
                                        M = Integer.parseInt(optval);
                                }
                                else {
                                        // any more?
                                }
                        }
                        
                        reader.close();
                }
                catch (Exception e){
                        System.out.println("Error while reading other file:" + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        
        
        // ***
        // DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
        // ***
        
        protected boolean readTAssignFile(String tassignFile){
                try {
                        int i,j;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(tassignFile), "UTF-8"));
                        
                        String line;
                        z = new Vector[M];                      
                        data = new LDADataset(M);
                        data.V = V;                     
                        for (i = 0; i < M; i++){
                                line = reader.readLine();
                                StringTokenizer tknr = new StringTokenizer(line, " \t\r\n");
                                
                                int length = tknr.countTokens();
                                
                                Vector<Integer> words = new Vector<Integer>();
                                Vector<Integer> topics = new Vector<Integer>();
                                
                                for (j = 0; j < length; j++){
                                        String token = tknr.nextToken();
                                        
                                        StringTokenizer tknr2 = new StringTokenizer(token, ":");
                                        if (tknr2.countTokens() != 2){
                                                System.out.println("Invalid word-topic assignment line\n");
                                                return false;
                                        }
                                        
                                        words.add(Integer.parseInt(tknr2.nextToken()));
                                        topics.add(Integer.parseInt(tknr2.nextToken()));
                                }//end for each topic assignment
                                
                                //allocate and add new document to the corpus
                                Document doc = new Document(words);
                                data.setDoc(doc, i);
                                
                                //assign values for z
                                z[i] = new Vector<Integer>();
                                for (j = 0; j < topics.size(); j++){
                                        z[i].add(topics.get(j));
                                }
                                
                        }//end for each doc
                        
                        reader.close();
                }
                catch (Exception e){
                        System.out.println("Error while loading model: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        /**
         * load saved model
         */
        
        // ***
        // DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
        // ***
        
        
        public boolean loadModel(){
                if (!readOthersFile(dir + File.separator + modelName + othersSuffix))
                        return false;
                
                if (!readTAssignFile(dir + File.separator + modelName + tassignSuffix))
                        return false;
                
                // read dictionary
                Dictionary dict = new Dictionary();
                if (!dict.readWordMap(dir + File.separator + wordMapFile))
                        return false;
                        
                data.localDict = dict;
                
                return true;
        }
        
        /**
         * Save word-topic assignments for this model
         */
        
        public boolean saveModelTAssign(String filename){
                int i, j;
                
                try{
                        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                        
                        //write docs with topic assignments for words
                        for (i = 0; i < data.M; i++){
                                for (j = 0; j < data.docs[i].length; ++j){
                                        writer.write(data.docs[i].words[j] + ":" + z[i].get(j) + " ");                                  
                                }
                                writer.write("\n");
                        }
                                
                        writer.close();
                }
                catch (Exception e){
                        System.out.println("Error while saving model tassign: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        /**
         * Save theta (topic distribution) for this model
         */
        public boolean saveModelTheta(String filename){
                try{
                        BufferedWriter writer;
                        writer= new BufferedWriter(new FileWriter(filename+"_negative"));
                       
                        	  for (int i = 0; i < A; i++){
                                  for (int j = 0; j < K; j++){
                                          writer.write(theta[i][j][0] + " ");
                                  }
                                  writer.write("\n");
                          }
                        	  writer.close();
                        writer = new BufferedWriter(new FileWriter(filename+"_neutral"));
                        for (int i = 0; i < A; i++){
                            for (int j = 0; j < K; j++){
                                    writer.write(theta[i][j][1] + " ");
                            }
                            writer.write("\n");
                    }
                        writer.close();
                        writer = new BufferedWriter(new FileWriter(filename+"_positive"));
                        for (int i = 0; i < A; i++){
                            for (int j = 0; j < K; j++){
                                    writer.write(theta[i][j][2] + " ");
                            }
                            writer.write("\n");
                    }
                        writer.close();
                }
                catch (Exception e){
                        System.out.println("Error while saving topic distribution file for this model: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        public boolean saveModelThetaDoc(String filename){
            try{
            	  BufferedWriter writer;
                  writer= new BufferedWriter(new FileWriter(filename));
                 
                  	  for (int i = 0; i < M; i++){
                            for (int j = 0; j < K; j++){
                                    writer.write(theta_doc[i][j][0] + " ");
                            }
                            for (int j = 0; j < K; j++){
                                writer.write(theta_doc[i][j][1] + " ");
                        }
                            for (int j = 0; j < K; j++){
                                writer.write(theta_doc[i][j][2] + " ");
                        }
                            writer.write("\n");
                    }
              
                  writer.close();
                  writer=new BufferedWriter(new FileWriter("Doc_Positive.txt"));
                  for(int i=0;i<K;i++)
                  {
                	  writer.write("Topic "+i+"th\n");
                	  List<Pair> DocProbsList = new ArrayList<Pair>(); 
                      for (int d = 0; d < M; d++){
                              Pair p = new Pair(d, theta_doc[d][i][2], false);
                              
                              DocProbsList.add(p);
                      }
                      Collections.sort(DocProbsList);
                      for(int j=0;j<10;j++)
                      {
                    	  writer.write(DocProbsList.get(j).first+" "+DocProbsList.get(j).second+"\n");
                      }
                  }
                  writer.close();
                  writer=new BufferedWriter(new FileWriter("Doc_Negative.txt"));
                  for(int i=0;i<K;i++)
                  {
                	  writer.write("Topic "+i+"th\n");
                	  List<Pair> DocProbsList = new ArrayList<Pair>(); 
                      for (int d = 0; d < M; d++){
                              Pair p = new Pair(d, theta_doc[d][i][0], false);
                              
                              DocProbsList.add(p);
                      }
                      Collections.sort(DocProbsList);
                      for(int j=0;j<10;j++)
                      {
                    	  writer.write(DocProbsList.get(j).first+" "+DocProbsList.get(j).second+"\n");
                      }
                  }
                  writer.close();
                  writer=new BufferedWriter(new FileWriter("Doc_Neutral.txt"));
                  for(int i=0;i<K;i++)
                  {
                	  writer.write("Topic "+i+"th\n");
                	  List<Pair> DocProbsList = new ArrayList<Pair>(); 
                      for (int d = 0; d < M; d++){
                              Pair p = new Pair(d, theta_doc[d][i][1], false);
                              
                              DocProbsList.add(p);
                      }
                      Collections.sort(DocProbsList);
                      for(int j=0;j<10;j++)
                      {
                    	  writer.write(DocProbsList.get(j).first+" "+DocProbsList.get(j).second+"\n");
                      }
                  }
                  writer.close();
            }
            catch (Exception e){
                    System.out.println("Error while saving topic distribution file for this model: " + e.getMessage());
                    e.printStackTrace();
                    return false;
            }
            return true;
    }
        /**
         * Save word-topic distribution
         */
        
        public boolean saveModelPhi(String filename){
                try {
                        BufferedWriter writer; 
                        writer= new BufferedWriter(new FileWriter(filename+"_negative"));
                         for (int i = 0; i < K; i++){
                              for (int j = 0; j < V; j++){
                                          writer.write(phi[i][j][0] + " ");
                                  }
                                  writer.write("\n");
                          }
                        writer.close();
                   
                        writer= new BufferedWriter(new FileWriter(filename+"_neutral"));
                  	     for (int i = 0; i < K; i++){
                            for (int j = 0; j < V; j++){
                                    writer.write(phi[i][j][1] + " ");
                            }
                            writer.write("\n");
                       }
                        writer.close();
                  writer= new BufferedWriter(new FileWriter(filename+"_positive"));
            	  for (int i = 0; i < K; i++){
                      for (int j = 0; j < V; j++){
                              writer.write(phi[i][j][2] + " ");
                      		}
                      writer.write("\n");
            	  	}
            	  	writer.close();
                }
                catch (Exception e){
                        System.out.println("Error while saving word-topic distribution:" + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        public boolean saveModelPi(String filename){
            try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                    for(int a=0;a<A;a++)
                    {
                    	 for (int i = 0; i < S; i++){
                              writer.write(pi[a][i] + " ");    
                      }
                    	  writer.write("\n");    
                    }
                  
                    writer.close();
            }
            catch (Exception e){
                    System.out.println("Error while saving word-topic distribution:" + e.getMessage());
                    e.printStackTrace();
                    return false;
            }
            return true;
    }
        /**
         * Save other information of this model
         */
        public boolean saveModelOthers(String filename){
                try{
                        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                        
                        writer.write("alpha=" + alpha + "\n");
                        writer.write("beta=" + beta + "\n");
                        writer.write("gamma="+gamma+"\n");
                        writer.write("ntopics=" + K + "\n");
                        writer.write("ndocs=" + M + "\n");
                        writer.write("nwords=" + V + "\n");
                        writer.write("liters=" + liter + "\n");
                        
                        writer.close();
                }
                catch(Exception e){
                        System.out.println("Error while saving model others:" + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        /**
         * Save model the most likely words for each topic
         */
        public boolean saveModelTwords(String filename){
                try{
                		BufferedWriter wr=new BufferedWriter(new FileWriter(dir + File.separator+"Topic_list.txt"));
                		for(int i=0;i<K;i++)
                		{
                			wr.write("Topic "+i+"th\n" );
                		}
                		wr.close();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                                        new FileOutputStream(filename), "UTF-8"));
                        
                        if (twords > V){
                                twords = V;
                        }
                        writer.write("Negative Sentiment\n");
                        for (int k = 0; k < K; k++){
                                List<Pair> wordsProbsList = new ArrayList<Pair>(); 
                                for (int w = 0; w < V; w++){
                                        Pair p = new Pair(w, phi[k][w][0], false);
                                        
                                        wordsProbsList.add(p);
                                }//end foreach word
                                
                                //print topic                           
                                writer.write("Topic " + k + "th\n");
                                Collections.sort(wordsProbsList);
                                
                                for (int i = 0; i < twords; i++){
                                        if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
                                                String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);
                                                
                                                writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
                                        }
                                }
                        } //end foreach topic                   
                              
                        writer.write("Neutral Sentiment\n");
                        for (int k = 0; k < K; k++){
                                List<Pair> wordsProbsList = new ArrayList<Pair>(); 
                                for (int w = 0; w < V; w++){
                                        Pair p = new Pair(w, phi[k][w][1], false);
                                        
                                        wordsProbsList.add(p);
                                }//end foreach word
                                
                                //print topic                           
                                writer.write("Topic " + k + "th:\n");
                                Collections.sort(wordsProbsList);
                                
                                for (int i = 0; i < twords; i++){
                                        if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
                                                String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);
                                                
                                                writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
                                        }
                                }
                        } //end foreach topic 
                       
                        writer.write("Positive Sentiment\n");
                        for (int k = 0; k < K; k++){
                                List<Pair> wordsProbsList = new ArrayList<Pair>(); 
                                for (int w = 0; w < V; w++){
                                        Pair p = new Pair(w, phi[k][w][2], false);
                                        
                                        wordsProbsList.add(p);
                                }//end foreach word
                                
                                //print topic                           
                                writer.write("Topic " + k + "th:\n");
                                Collections.sort(wordsProbsList);
                                
                                for (int i = 0; i < twords; i++){
                                        if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
                                                String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);
                                                
                                                writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
                                        }
                                }
                        } //end foreach topic 
                        writer.close();
                }
                catch(Exception e){
                        System.out.println("Error while saving model twords: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                }
                return true;
        }
        
        /**
         * Save model
         */
        public boolean saveModel(String modelName){
                if (!saveModelTAssign(dir + File.separator + modelName + tassignSuffix)){
                        return false;
                }
                
                if (!saveModelOthers(dir + File.separator + modelName + othersSuffix)){                 
                        return false;
                }
                
                if (!saveModelTheta(dir + File.separator + modelName + thetaSuffix)){
                        return false;
                }
                if (!saveModelThetaDoc(dir + File.separator + modelName + thetadoc)){
                    return false;
            }
                
                if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)){
                        return false;
                }
                if (!saveModelPi(dir + File.separator + modelName + piSuffix)){
                    return false;
            }
                if (twords > 0){
                        if (!saveModelTwords(dir + File.separator + modelName + twordsSuffix))
                                return false;
                }
                return true;
        }
        
        //---------------------------------------------------------------
        //      Init Methods
        //---------------------------------------------------------------
        /**
         * initialize the model
         */
        protected boolean init(LDACmdOption option){            
                if (option == null)
                        return false;
                
                modelName = option.modelName;
                K = option.K;
                
                alpha = option.alpha;
                if (alpha < 0.0)
                        alpha = 50.0 / K;
                
                if (option.beta >= 0)
                        beta = option.beta;
                
                gamma = option.gamma;
                niters = option.niters;
                
                dir = option.dir;
                if (dir.endsWith(File.separator))
                        dir = dir.substring(0, dir.length() - 1);
                
                dfile = option.dfile;
                twords = option.twords;
                wordMapFile = option.wordMapFileName;
                
                return true;
        }
        
        /**
         * Init parameters for estimation
         */
        public boolean initNewModel(LDACmdOption option){
                if (!init(option))
                        return false;
                
                int m, n, w, k, s;         
                p = new double[K];              
                
                data = LDADataset.readDataSet(dir + File.separator + dfile,option);
                if (data == null){
                        System.out.println("Fail to read training data!\n");
                        return false;
                }
                
                //+ allocate memory and assign values for variables             
                M = data.M;
                V = data.V;
                A = data.A;
                dir = option.dir;
                savestep = option.savestep;
      
                nwl = new double[V][K][S];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                        	for(s=0;s<S;s++)
                        	{
                        		  nwl[w][k][s] = 0;
                        	}
                              
                        }
                }
                
                nal = new double[A][K][S];
                for (m = 0; m < A; m++){
                        for (k = 0; k < K; k++){
                        	for(s=0;s<S;s++)
                        	{
                        		   nal[m][k][s] = 0;
                        	}
                             
                        }
                }
                
                nwlsum = new double[K][S];
                for (k = 0; k < K; k++){
                	for(s=0;s<S;s++)
                	{
                		nwlsum[k][s] = 0;
                	}
                }
                
                nalsum = new double[A][S];
                for (m = 0; m < A; m++){
                    for(s=0;s<S;s++)
                    {
                    	nalsum[m][s] = 0;
                    }	
                }
                ndlsum = new double[M][S];
                for (m = 0; m <M; m++){
                   for(s=0;s<S;s++)
                   {
                	   ndlsum[m][s] = 0;
                   }             	
                }
                ndl=new double[M][K][S];
                for (m = 0; m < M; m++){
                    for(k=0;k<K;k++)
                    {
                    	for(s = 0; s<S; s++)
                    	{
                    		ndl[m][k][s] = 0;
                    	}               	
                    }	
                }
                nasum = new double[A];
                for (m = 0; m < A; m++){
                   
                    	nasum[m] = 0;
                    	
                }
                // initialize da!
                da = new Vector[M];
                for (m = 0; m < data.M; m++)
                {
                        da[m] = new Vector<Integer>();
                        for (int author : data.docs[m].authors)
                                da[m].add(author);
                }
                
                // initializing the words with topics
                z = new Vector[M];
                za = new Vector[M];
                zl = new Vector[M];
                for (m = 0; m < data.M; m++){
                        int N = data.docs[m].length;
                        z[m] = new Vector<Integer>();
                        za[m] = new Vector<Integer>();
                        zl[m] =new Vector<Integer>();
                        int num_authors = da[m].size();
                        //initialize for z
                        for (n = 0; n < N; n++){
                                w = data.docs[m].words[n];
                                int a = da[m].get((int) (Math.random() * num_authors));
                                int topic = (int)Math.floor(Math.random() * K);
                                int senti_label= data.word2senti.get(w);
                                ndl[m][topic][senti_label]++;
                                z[m].add(topic);
                                za[m].add(a);
                                nasum[a]+=1;
                                zl[m].add(senti_label);
                                // number of instances of word assigned to topic j and sentiment l
                                nwl[data.docs[m].words[n]][topic][senti_label] += 1 ;                                 
                                // number of words in document i assigned to topic j and sentiment l
                                nal[a][topic][senti_label]+= 1 ;                               
                                // total number of words assigned to topic j and sentiment l
                                nwlsum[topic][senti_label] += 1 ;                                  
                                nalsum[a][senti_label] += 1 ;                    
                                ndlsum[m][senti_label]++;
                        }
                      
                }
            
               
                
                theta = new double[A][K][S];
                theta_doc=new double[M][K][S];
                phi = new double[K][V][S];
                pi= new double [A][S];
                return true;
        }
        
        /**
         * Init parameters for inference
         * @param newData DataSet for which we do inference
         */
        public boolean initNewModel(LDACmdOption option, LDADataset newData, Model trnModel){
                if (!init(option))
                        return false;
                
              /*  int m, n, w, k, s;
                
                K = trnModel.K;
                alpha = trnModel.alpha;
                beta = trnModel.beta;           
                
                p = new double[K];
                System.out.println("K:" + K);
                
                data = newData;
                
                //+ allocate memory and assign values for variables             
                M = data.M;
                V = data.V;
                dir = option.dir;
                savestep = option.savestep;
                System.out.println("M:" + M);
                System.out.println("V:" + V);
                
                // K: from command line or default value
            // alpha, beta: from command line or default values
            // niters, savestep: from command line or default values

                nwl = new double[V][K][S];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                        	for(s=0;s<S;s++)
                        	{
                        		nwl[w][k][s] = 0;
                        	}
                        
                        }
                }
                
                nal = new double[M][K][S];
                for (m = 0; m < M; m++){
                        for (k = 0; k < K; k++){
                        	for(s=0;s<S;s++)
                        	{
                        		nal[w][k][s] = 0;
                        	}
                        }
                }
                
                nwlsum = new double[K][S];
                for (k = 0; k < K; k++){
                	for(s=0;s<S;s++)
                	{
                		nwlsum[k][s] = 0;
                	}
                }
                
                nalsum = new double[A][S];
                for (m = 0; m < A; m++){
                	for(s=0;s<S;s++)
                	{
                		nalsum[k][s] = 0;
                	}
                }
                z = new Vector[M];
                za = new Vector[M];
                zl = new Vector[M];
                for (m = 0; m < data.M; m++){
                        int N = data.docs[m].length;
                        z[m] = new Vector<Integer>();
                        za[m] = new Vector<Integer>();
                        zl[m] =new Vector<Integer>();
                        int num_authors = da[m].size();
                        //initialize for z
                        for (n = 0; n < N; n++){
                                w = data.docs[m].words[n];
                                int a = da[m].get((int) (Math.random() * num_authors));
                                int topic = (int)Math.floor(Math.random() * K);
                                int senti_label= data.word2senti.get(w);
                                z[m].add(topic);
                                za[m].add(a);
                                zl[m].add(senti_label);
                                if(senti_label==0)
                                {
                                	 // number of instances of word assigned to topic j and sentiment l
                                    nwl[data.docs[m].words[n]][topic][0] += 1 ;                                 
                                    // number of words in document i assigned to topic j and sentiment l
                                    nal[a][topic][0]+= 1 ;                               
                                    // total number of words assigned to topic j and sentiment l
                                    nwlsum[topic][0] += 1 ;                                  
                                    nalsum[a][0] += 1 ;                    
                                }
                                else if(senti_label==1)
                                {
                               	 // number of instances of word assigned to topic j and sentiment l
                                   nwl[data.docs[m].words[n]][topic][0] += 1*sentiparameter ;                                 
                                   // number of words in document i assigned to topic j and sentiment l
                                   nal[a][topic][0]+=  1*sentiparameter ;                               
                                   // total number of words assigned to topic j and sentiment l
                                   nwlsum[topic][0] +=  1*sentiparameter;                                  
                                   nalsum[a][0] +=  1*sentiparameter ;                    
                               }
                                else if(senti_label==2)
                                {
                               	 // number of instances of word assigned to topic j and sentiment l
                                   nwl[data.docs[m].words[n]][topic][1] += 1 ;                                 
                                   // number of words in document i assigned to topic j and sentiment l
                                   nal[a][topic][1]+= 1 ;                               
                                   // total number of words assigned to topic j and sentiment l
                                   nwlsum[topic][1] += 1 ;                                  
                                   nalsum[a][1] += 1 ;                    
                               }
                                else if(senti_label==3)
                                {
                               	 // number of instances of word assigned to topic j and sentiment l
                                   nwl[data.docs[m].words[n]][topic][2] += 1*sentiparameter;                                 
                                   // number of words in document i assigned to topic j and sentiment l
                                   nal[a][topic][2]+= 1*sentiparameter;                               
                                   // total number of words assigned to topic j and sentiment l
                                   nwlsum[topic][2] += 1*sentiparameter;                                  
                                   nalsum[a][2] +=  1*sentiparameter;                    
                               } 
                                else if(senti_label==4)
                               {
                                 	 // number of instances of word assigned to topic j and sentiment l
                                     nwl[data.docs[m].words[n]][topic][2] += 1;                                 
                                     // number of words in document i assigned to topic j and sentiment l
                                     nal[a][topic][2]+= 1;                               
                                     // total number of words assigned to topic j and sentiment l
                                     nwlsum[topic][2] += 1;                                  
                                     nalsum[a][2] +=  1;                    
                                 } 
                               
                        }
                }
                
                theta = new double[A][K][S];               
                phi = new double[K][V][S];*/
                
                return true;
        }
        
        /**
         * Init parameters for inference
         * reading new dataset from file
         */
        
        // ***
        // DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
        // ***
        
        public boolean initNewModel(LDACmdOption option, Model trnModel){
                if (!init(option))
                        return false;
                
                LDADataset dataset = LDADataset.readDataSet(dir + File.separator + dfile, trnModel.data.localDict);
                if (dataset == null){
                        System.out.println("Fail to read dataset!\n");
                        return false;
                }
                
                return initNewModel(option, dataset , trnModel);
        }
        
        /**
         * init parameter for continue estimating or for later inference
         */
        
        // ***
        // DON'T USE - HAVE NOT UPDATED THIS METHOD FOR ATM
        // ***
        
        public boolean initEstimatedModel(LDACmdOption option){
                if (!init(option))
                        return false;
                
                int m, n, w, k;
                
                p = new double[K];
                
                // load model, i.e., read z and trndata
                if (!loadModel()){
                        System.out.println("Fail to load word-topic assignment file of the model!\n");
                        return false;
                }
                
                System.out.println("Model loaded:");
                System.out.println("\talpha:" + alpha);
                System.out.println("\tbeta:" + beta);
                System.out.println("\tM:" + M);
                System.out.println("\tV:" + V);         
                
             /*   nwl = new double[V][K][S];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                                nwl[w][k] = 0;
                        }
                }
                
                nal = new double[M][K][S];
                for (m = 0; m < M; m++){
                        for (k = 0; k < K; k++){
                                nal[m][k] = 0;
                        }
                }
                
                nwlsum = new double[K];
            for (k = 0; k < K; k++) {
                nwlsum[k] = 0;
            }
            
            nalsum = new double[M];
            for (m = 0; m < M; m++) {
                nalsum[m] = 0;
            }
            
            for (m = 0; m < data.M; m++){
                int N = data.docs[m].length;
                
                // assign values for nw, nd, nwsum, and ndsum
                for (n = 0; n < N; n++){
                        w = data.docs[m].words[n];
                        int topic = (Integer)z[m].get(n);
                        
                        // number of instances of word i assigned to topic j
                        nw[w][topic] += 1;
                        // number of words in document i assigned to topic j
                        na[m][topic] += 1;
                        // total number of words assigned to topic j
                        nwsum[topic] += 1;                      
                }
                // total number of words in document i
                nasum[m] = N;
            }
            */
            theta = new double[A][K][S];
            phi = new double[K][V][S];
            dir = option.dir;
                savestep = option.savestep;
            
                return true;
        }
        
}