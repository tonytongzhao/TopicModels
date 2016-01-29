package jGibbATM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class LDADataset {
        //---------------------------------------------------------------
        // Instance Variables
        //---------------------------------------------------------------
        
        public Dictionary localDict;                    // local dictionary     
        public Document [] docs;                // a list of documents  
        public int M;                                   // number of documents
        public int A;                                   // number of authors
        public int V;                                   // number of words
        public double [][] freqs;               // freq of each word in each doc, size V x M
        public double [] numDocsPresent;// number of docs a word appears in, size V
        // map from local coordinates (id) to global ones 
        // null if the global dictionary is not set
        public Map<Integer, Integer> lid2gid; 
        public Map<Integer, Integer>  la2ga;
        public Map<String, Integer> name2aid;
        public Map<String, Integer> globalauthormap;

        //link to a global dictionary (optional), null for train data, not null for test data
        public Dictionary globalDict;                   
        
        //--------------------------------------------------------------
        // Constructor
        //--------------------------------------------------------------
        public LDADataset(){
                localDict = new Dictionary();
                M = 0;
                V = 0;
                A = 0;
                docs = null;
             
                freqs = null;
                numDocsPresent = null;
                
                globalDict = null;
                lid2gid = null;
                name2aid = null;
        }
        
        public LDADataset(int M){
                localDict = new Dictionary();
                this.M = M;
                this.V = 0;
                this.A = 0;
                docs = new Document[M]; 

                freqs = null;
                numDocsPresent = null;
                
                globalDict = null;
                lid2gid = null;
                name2aid = new HashMap<String,Integer>();
        }
        public LDADataset(int M, Dictionary globalDict){
            localDict = new Dictionary();   
            this.M = M;
            this.V = 0;
            docs = new Document[M]; 
           
            this.globalDict = globalDict;
            lid2gid = new HashMap<Integer, Integer>();
    }
        public LDADataset(int M, Dictionary globalDict, Map<String, Integer> authormap){
                localDict = new Dictionary();   
                this.M = M;
                this.V = 0;
                docs = new Document[M]; 
                this.name2aid=new HashMap<String, Integer>();
                this.A=0;        
                this.globalDict = globalDict;
                this.globalauthormap=authormap;
                lid2gid = new HashMap<Integer, Integer>();
                this.la2ga=new HashMap<Integer, Integer>();
        }
        
        //-------------------------------------------------------------
        //Public Instance Methods
        //-------------------------------------------------------------
        /**
         * set the document at the index idx if idx is greater than 0 and less than M
         * @param doc document to be set
         * @param idx index in the document array
         */     
        public void setDoc(Document doc, int idx){
                if (0 <= idx && idx < M){
                        docs[idx] = doc;
                }
        }
        /**
         * set the document at the index idx if idx is greater than 0 and less than M
         * @param str string contains doc
         * @param idx index in the document array
         */
        public void setDoc(String str, int idx, String dir){
                if (0 <= idx && idx < M){
                        
                        // parse out the authors of the doc and give them id numbers
                        
                        String author_set = str.substring(0,str.indexOf(";"));
                        String [] authors = author_set.split(",");
                        
                        Vector<Integer> aids = new Vector<Integer>();
                        for (String author : authors)
                        {
                                if (name2aid.containsKey(author))
                                        aids.add(name2aid.get(author));
                                else
                                {	      		
                                        aids.add(A);
                                        name2aid.put(author, A++);
                                }
                                if(globalauthormap!=null)
                                {
                                	int a1=name2aid.get(author);
                                	Integer a2=globalauthormap.get(author);
                                	if(a2!=null)
                                	{
                                		la2ga.put(a1, a2);
                                	}
                                }
                        }
                        if(dir!=null)
                        {
                        	 str = str.split(";")[1];
                             try {
     							BufferedReader br=new BufferedReader(new FileReader(dir+File.separator+str));
     							String tmp="";
     							str="";
     							while((tmp=br.readLine())!=null)
     							{
     								str+=tmp+" ";
     							}
     							br.close();
     						} catch (FileNotFoundException e) {
     							// TODO Auto-generated catch block
     							e.printStackTrace();
     						} catch (Exception e) {
     							// TODO Auto-generated catch block
     							e.printStackTrace();
     						}
                            // System.out.println(str);
                             String [] words = str.split(" ");
                             
                             // parse out the words and give unseen words ids
                             
                             Vector<Integer> ids = new Vector<Integer>();
                             for (String word : words){
                                     int _id = localDict.word2id.size();
                                     
                                     if (localDict.contains(word))           
                                             _id = localDict.getID(word);
                                                                     
                                     if (globalDict != null){
                                             //get the global id                                     
                                             Integer id = globalDict.getID(word);
                                             //System.out.println(id);
                                             
                                             if (id != null){
                                                     localDict.addWord(word);
                                                     
                                                     lid2gid.put(_id, id);
                                                     ids.add(_id);
                                             }
                                             else { //not in global dictionary
                                                     //do nothing currently
                                             }
                                     }
                                     else {
                                             localDict.addWord(word);
                                             ids.add(_id);
                                     }
                             }
                             Document doc = new Document(ids, aids, str);
                             docs[idx] = doc;
                             V = localDict.word2id.size();
                        }
                        else
                        {
                        	str = str.substring(str.indexOf(";")+1);
                        	String [] words = str.split("[ \\t\\n]"); 
                             Vector<Integer> ids = new Vector<Integer>();
                             for (String word : words){
                                     int _id = localDict.word2id.size();
                                     
                                     if (localDict.contains(word))           
                                             _id = localDict.getID(word);
                                                                     
                                     if (globalDict != null){
                                             //get the global id                                     
                                             Integer id = globalDict.getID(word);
                                             //System.out.println(id);
                                             
                                             if (id != null){
                                                     localDict.addWord(word);
                                                     
                                                     lid2gid.put(_id, id);
                                                     ids.add(_id);
                                             }
                                             else { //not in global dictionary
                                                     //do nothing currently
                                             }
                                     }
                                     else {
                                             localDict.addWord(word);
                                             ids.add(_id);
                                     }
                             }
                             Document doc = new Document(ids, aids, str);
                             docs[idx] = doc;
                             V = localDict.word2id.size();
                             System.out.println("Document "+idx+" done!\n");
                        }
                        
                        
                        
                }
        }
        //---------------------------------------------------------------
        // I/O methods
        //---------------------------------------------------------------
        
        /**
         *  read a dataset from a stream, create new dictionary
         *  @return dataset if success and null otherwise
         */
        public static LDADataset readDataSet(String filename,LDACmdOption option){
                try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(filename), "UTF-8"));
                        
                        LDADataset data = readDataSet(reader,option.dir);
                        System.out.println(data.A);
                        System.out.println(data.M);
                        System.out.println(data.V);
                        System.out.println(data.docs.length);
                        reader.close();
                        return data;
                }
                catch (Exception e){
                        System.out.println("Read Dataset Error: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }
        
        /**
         * read a dataset from a file with a preknown vocabulary
         * @param filename file from which we read dataset
         * @param dict the dictionary
         * @return dataset if success and null otherwise
         */
        public static LDADataset readDataSet(String filename, Dictionary dict, HashMap<String, Integer> authormap, String dir){
                try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(filename), "UTF-8"));
                        LDADataset data = readDataSetA(reader, dict,authormap,dir);
                        
                        reader.close();
                        return data;
                }
                catch (Exception e){
                        System.out.println("Read Dataset Error: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }
        
        /**
         *  read a dataset from a stream, create new dictionary
         *  @return dataset if success and null otherwise
         */
        public static LDADataset readDataSet(BufferedReader reader,String dir){
                try {
                        //read number of document
                        String line;
                        line = reader.readLine();
                        int M = Integer.parseInt(line);
                        
                        LDADataset data = new LDADataset(M);
                        for (int i = 0; i < M; ++i){
                                line = reader.readLine();
                                if (line != null)
                                        data.setDoc(line, i,dir);
                        }
//                        
//                    
                        
                        return data;
                }
                catch (Exception e){
                        System.out.println("Read Dataset Error: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        }
        
        /**
         * read a dataset from a stream with respect to a specified dictionary
         * @param reader stream from which we read dataset
         * @param dict the dictionary
         * @return dataset if success and null otherwise
         */
        public static LDADataset readDataSetA(BufferedReader reader, Dictionary dict, HashMap<String, Integer> authormap, String dir){
                try {
                        //read number of document
                        String line;
                        line = reader.readLine();
                        int M = Integer.parseInt(line);
                        System.out.println("NewM:" + M);
                        
                        LDADataset data = new LDADataset(M, dict,authormap);
                        for (int i = 0; i < M; ++i){
                                line = reader.readLine();                  
                                data.setDoc(line, i,dir);
                        }
                        
                        return data;
                }
                catch (Exception e){
                        System.out.println("Read Dataset Error: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                }
        } 
        
        
        /**
         * read a dataset from a string, create new dictionary
         * @param str String from which we get the dataset, documents are seperated by newline character 
         * @return dataset if success and null otherwise
         */
        public static LDADataset readDataSet(String [] strs){
                LDADataset data = new LDADataset(strs.length);
                
                for (int i = 0 ; i < strs.length; ++i){
                        data.setDoc(strs[i], i,null);
                }
                return data;
        }
        
        /**
         * read a dataset from a string with respect to a specified dictionary
         * @param str String from which we get the dataset, documents are seperated by newline character        
         * @param dict the dictionary
         * @return dataset if success and null otherwise
         */
        public static LDADataset readDataSet(String [] strs, Dictionary dict){
                //System.out.println("readDataset...");
                LDADataset data = new LDADataset(strs.length, dict);
                
                for (int i = 0 ; i < strs.length; ++i){
                        //System.out.println("set doc " + i);
                        data.setDoc(strs[i], i,null);
                }
                return data;
        }
        
        public void writeAuthorMap(String name) 
        {
        	BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(name));
			
        	 Iterator<String> it = name2aid.keySet().iterator();
             while (it.hasNext()){
                    String key = it.next();
                    Integer value = name2aid.get(key); 
					bw.write(value + ";" + key + "\n");
					
			}
             bw.close();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
        public 	HashMap readAuthorMap(String dir)
        {
        	HashMap t=new HashMap<String, Integer>();
        	try {
				BufferedReader br =new BufferedReader(new FileReader(new File(dir+File.separator+"Author_map.txt")));
				int i=0;
				String line;
				while((line=br.readLine())!=null)
				{
					i++;
					t.put(line.split(";")[1], Integer.parseInt(line.split(";")[0]));
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return t;
        }
}
