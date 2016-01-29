package jGibbATM;

import jGibbATM.Pair;

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
        public static String thetaSuffix;               //suffix for theta (topic - document distribution) file
        public static String phiSuffix;         //suffix for phi file (topic - word distribution) file
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
        
        public int A; //number of authors

        public int V; //vocabulary size
        public int K; //number of topics
        public double alpha, beta; //LDA  hyperparameters
        public int niters; //number of Gibbs sampling iteration
        public int liter; //the iteration at which the model was saved  
        public int savestep; //saving period
        public int twords; //print out top words per each topic
        public int withrawdata;
        
        // Estimated/Inferenced parameters
        public double [][] theta; //theta: author - topic distributions, size A x K
        public double [][] phi; // phi: topic-word distributions, size K x V
        public double [][] theta1; //theta: document - topic distributions, size M x K
        // Weighting values
        public double [][] word_weights; //maps each word/doc pair to a weight, size V x M
        
        // Temp variables while sampling
        public Vector<Integer> [] z; //topic assignments for words, size M x doc.size()
        public Vector<Integer> [] za; //author assignments for words, size M x doc.size()
        public Vector<Integer> [] da; //author assignments for documents
        protected double [][] nw; //nw[i][j]: number of instances of word/term i assigned to topic j, size V x K
        protected double [][] na; //nd[i][j]: number of words from author i assigned to topic j, size A x K
        protected double [] nwsum; //nwsum[j]: total number of words assigned to topic j, size K
        protected double [] nasum; //ndsum[i]: total number of words in document i, size M
        protected int [] ndsum; //ndsum[i]: total number of words in document i, size M
        protected int [][] nd; //nd[i][j]: number of words in document i assigned to topic j, size M x K
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
                phiSuffix = ".phi";
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
                niters = 2000;
                liter = 0;
                
                z = null;
                da = null;
                za = null;
                nw = null;
                na = null;
                nd=null;
                ndsum=null;
                nwsum = null;
                nasum = null;
                theta = null;
                phi = null;
                theta1=null;
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
                        za=new Vector[M];
                        data = new LDADataset(M);
                        data.V = V;                     
                        for (i = 0; i < M; i++){
                                line = reader.readLine();
                                String[] tknr = line.split(" ");

            					int length = tknr.length;

            					z[i] = new Vector<Integer>();
                                za[i]=new Vector<Integer>();
            					Vector<Integer> words = new Vector<Integer>();
                             
                                for (j = 0; j < length; j++) {
        							String token = tknr[j];

        							words.add(Integer.parseInt(token.split(":")[0]));
        							String tmp = token.split(":")[1];
        							if (tmp.contains(",")) {
        								//System.out.println(tmp);
        								String tmp1[] = tmp.split(",");
        								z[i].add(Integer.parseInt(tmp1[0]));
        								za[i].add(Integer.parseInt(tmp1[1]));
        								
        							}
        						}//end for each topic assignment
                                
                                //allocate and add new document to the corpus
                                Document doc = new Document(words);
                                data.setDoc(doc, i);
                                
                                //assign values for z
                               
                                
                                
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
                data.name2aid=data.readAuthorMap(dir);
                data.localDict = dict;
                A=data.name2aid.size();
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
                                        writer.write(data.docs[i].words[j] + ":" + z[i].get(j)+","+za[i].get(j)+ " ");                                  
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
                        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                        for (int i = 0; i < A; i++){
                                for (int j = 0; j < K; j++){
                                        writer.write(theta[i][j] + " ");
                                }
                                writer.write("\n");
                        }
                        writer.close();
                        writer=new BufferedWriter(new FileWriter("Author_Topic.txt"));
                        for (int a = 0; a < A; a++){
                      	  	writer.write("Author: "+a+"\n");
                      	  //writer.write("Topic "+i+"th\n");
                      	  List<Pair> TopicProbsList = new ArrayList<Pair>(); 
                            	for(int k=0;k<K;k++)
                            	{
                            		 Pair p = new Pair(k, theta[a][k], false);                            
                                   TopicProbsList.add(p);
                            	}
                            Collections.sort(TopicProbsList);            
                            for(int i=0;i<3;i++)
                            {
                          	  writer.write("Topic "+TopicProbsList.get(i).first.toString()+"th\n");
                          	
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
                        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
                        
                        for (int i = 0; i < K; i++){
                                for (int j = 0; j < V; j++){
                                        writer.write(phi[i][j] + " ");
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
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                                        new FileOutputStream(filename), "UTF-8"));
                        
                        if (twords > V){
                                twords = V;
                        }
                        
                        for (int k = 0; k < K; k++){
                                List<Pair> wordsProbsList = new ArrayList<Pair>(); 
                                for (int w = 0; w < V; w++){
                                        Pair p = new Pair(w, phi[k][w], false);
                                        
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
                
                if (!saveModelPhi(dir + File.separator + modelName + phiSuffix)){
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
                
                int m, n, w, k;         
                p = new double[K];              
               
                data = LDADataset.readDataSet(dir+File.separator+ dfile,option);
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
                
                // K: from command line or default value
            // alpha, beta: from command line or default values
            // niters, savestep: from command line or default values

                // initializing the word weights with the tfidf
                
//                word_weights = new double[V][M];
//                for (w = 0; w < V; ++w)
//                {
//                        for (m = 0; m < M; ++m)
//                        {
//                                double tf = (double) data.freqs[w][m] / data.docs[m].length;
//                                double idf = Math.log((double) M / data.numDocsPresent[w]);
//                                word_weights[w][m] = use_tfidf ? (tf * idf) : 1;
//                        }
//                }
//                
                nw = new double[V][K];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                                nw[w][k] = 0;
                        }
                }
                
                na = new double[A][K];
                for (m = 0; m < A; m++){
                        for (k = 0; k < K; k++){
                                na[m][k] = 0;
                        }
                }
                
                nwsum = new double[K];
                for (k = 0; k < K; k++){
                        nwsum[k] = 0;
                }
                
                nasum = new double[A];
                for (m = 0; m < A; m++){
                        nasum[m] = 0;
                }
                nd = new int[M][K];
        		for (m = 0; m < M; m++){
        			for (k = 0; k < K; k++){
        				nd[m][k] = 0;
        			}
        		}
        		
        		ndsum = new int[M];
        		for (m = 0; m < M; m++){
        			ndsum[m] = 0;
        		}
                // initialize da!
                da = new Vector[M];
                for (m = 0; m < data.M; m++)
                {
                	
                	//System.out.println(m);
                	if(data.docs[m].authors==null)
                	{
                		System.out.println(m);
                		da[m] = new Vector<Integer>();
                        da[m].add((int)(Math.random()*data.A));  
                	}
                	else
                	{
                		da[m] = new Vector<Integer>();
                        da[m].add(data.docs[m].authors[0]);  
                	}
              
                }
                
                // initializing the words with topics
                z = new Vector[M];
                za = new Vector[M];
                for (m = 0; m < data.M; m++){
                        int N = data.docs[m].length;
                        z[m] = new Vector<Integer>();
                        za[m] = new Vector<Integer>();
                        int num_authors = da[m].size();
                        //initialize for z
                        for (n = 0; n < N; n++){
                                w = data.docs[m].words[n];
                                int a = da[m].get((int) (Math.random() * num_authors));
                                int topic = (int)Math.floor(Math.random() * K);
                                z[m].add(topic);
                                za[m].add(a);
                                // number of instances of word assigned to topic j
                                nw[data.docs[m].words[n]][topic] += 1 ;
                                ///* word_weights[w][m];
                                // number of words in document i assigned to topic j
                                na[a][topic] += 1 ;
                                ////* word_weights[w][m];
                                // total number of words assigned to topic j
                                nwsum[topic] += 1 ;
                                ///* word_weights[w][m];
                                nasum[a] += 1 ;
                                ///* word_weights[w][m];
                            	nd[m][topic] += 1;
                        }
//                      total number of words in document i
            			ndsum[m] = N;
                }
                
                theta = new double[A][K];               
                phi = new double[K][V];
                theta1=new double[M][K];
                return true;
        }
        
        /**
         * Init parameters for inference
         * @param newData DataSet for which we do inference
         */
        public boolean initNewModel(LDACmdOption option, LDADataset newData, Model trnModel){
                if (!init(option))
                        return false;
                
                int m, n, w, k;
                
                K = trnModel.K;
                alpha = trnModel.alpha;
                beta = trnModel.beta;           
                
               
                System.out.println("K:" + K);
                
                data = newData;
                A=data.A;
                //+ allocate memory and assign values for variables             
                M = data.M;
                V = data.V;
                dir = option.dir;
                savestep = option.savestep;
                System.out.println("M:" + M);
                System.out.println("V:" + V);
                System.out.println("A:" + A);
                
                // K: from command line or default value
            // alpha, beta: from command line or default values
            // niters, savestep: from command line or default values

                nw = new double[V][K];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                                nw[w][k] = 0;
                        }
                }
                
                na = new double[A][K];
                for (m = 0; m < A; m++){
                        for (k = 0; k < K; k++){
                                na[m][k] = 0;
                        }
                }
                
                nwsum = new double[K];
                for (k = 0; k < K; k++){
                        nwsum[k] = 0;
                }
                
                nasum = new double[A];
                for (m = 0; m < A; m++){
                        nasum[m] = 0;
                }
                da = new Vector[M];
                for (m = 0; m < data.M; m++)
                {
                        da[m] = new Vector<Integer>();
                        for (int author : data.docs[m].authors)
                                da[m].add(author);
                }
                z = new Vector[M];
                za=new Vector[M];
                for (m = 0; m < data.M; m++){
                        int N = data.docs[m].length;
                        z[m] = new Vector<Integer>();
                        za[m] = new Vector<Integer>();
                        //initialize for z
                        for (n = 0; n < N; n++){
                                int topic = (int)Math.floor(Math.random() * K);
                                int a = da[m].get((int) (Math.random() * da[m].size()));
                                z[m].add(topic);
                                za[m].add(a);
                                // number of instances of word assigned to topic j
                                nw[data.docs[m].words[n]][topic] += 1 ;
                               
                                // number of words in document i assigned to topic j
                                na[a][topic] += 1 ;
                                ////* word_weights[w][m];
                                // total number of words assigned to topic j
                                nwsum[topic] += 1 ;
                                ///* word_weights[w][m];
                                nasum[a] += 1 ;
                        }
                        // total number of words in document i
                     
                }
                
                theta = new double[A][K];               
                phi = new double[K][V];
                
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
                
                LDADataset dataset = LDADataset.readDataSet(dir + File.separator + dfile, trnModel.data.localDict,(HashMap<String, Integer>)trnModel.data.name2aid, option.dir);
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
                System.out.println("\tA:" + A); 
                nw = new double[V][K];
                for (w = 0; w < V; w++){
                        for (k = 0; k < K; k++){
                                nw[w][k] = 0;
                        }
                }
                
                na = new double[A][K];
                for (m = 0; m < A; m++){
                        for (k = 0; k < K; k++){
                                na[m][k] = 0;
                        }
                }
                
                nwsum = new double[K];
            for (k = 0; k < K; k++) {
                nwsum[k] = 0;
            }
            
            nasum = new double[A];
            for (m = 0; m < A; m++) {
                nasum[m] = 0;
            }
            
            for (m = 0; m < data.M; m++){
                int N = data.docs[m].length;
                
                // assign values for nw, nd, nwsum, and ndsum
                for (n = 0; n < N; n++){
                        w = data.docs[m].words[n];
                        int topic = (Integer)z[m].get(n);
                        int a=(Integer)za[m].get(n);
                       
                        // number of instances of word i assigned to topic j
                        nw[w][topic] += 1;
                        // number of words in document i assigned to topic j
                        na[a][topic] += 1;
                        // total number of words assigned to topic j
                        nwsum[topic] += 1;    
                        nasum[a] +=1;
                }
                // total number of words in document i
                
            }
            
            theta = new double[A][K];
            phi = new double[K][V];
            dir = option.dir;
                savestep = option.savestep;
            
                return true;
        }
        
}