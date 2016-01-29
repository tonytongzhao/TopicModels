package TEM;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


public class Dictionary {
        public Map<String,Integer> word2id;
        public Map<Integer, String> id2word;
                
        //--------------------------------------------------
        // constructors
        //--------------------------------------------------
        
        public Dictionary(){
                word2id = new HashMap<String, Integer>();
                id2word = new HashMap<Integer, String>();
        }
        
        //---------------------------------------------------
        // get/set methods
        //---------------------------------------------------
        
        public String getWord(int id){
                return id2word.get(id);
        }
        
        public Integer getID (String word){
                return word2id.get(word);
        }
        
        //----------------------------------------------------
        // checking methods
        //----------------------------------------------------
        /**
         * check if this dictionary contains a specified word
         */
        public boolean contains(String word){
                return word2id.containsKey(word);
        }
        
        public boolean contains(int id){
                return id2word.containsKey(id);
        }
        //---------------------------------------------------
        // manupulating methods
        //---------------------------------------------------
        /**
         * add a word into this dictionary
         * return the corresponding id
         */
        public int addWord(String word){
                if (!contains(word)){
                        int id = word2id.size();
                        
                        word2id.put(word, id);
                        id2word.put(id,word);
                        
                        return id;
                }
                else return getID(word);                
        }
        
        //---------------------------------------------------
        // I/O methods
        //---------------------------------------------------
        /**
         * read dictionary from file
         */
        public boolean readWordMap(String wordMapFile){         
            try{
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(wordMapFile), "UTF-8"));
                    String line;
                    
                    //read the number of words
                    line = reader.readLine();                       
                    int nwords = Integer.parseInt(line);
                    
                    //read map
                    for (int i = 0; i < nwords; ++i){
                            line = reader.readLine();
                            
                            String[] tmp=line.split(" ");                      
                            String word = tmp[0];
                            String id = tmp[1];
                            int intID = Integer.parseInt(id);
                            
                            id2word.put(intID, word);
                            word2id.put(word, intID);
                    }
                    
                    reader.close();
                    return true;
            }
            catch (Exception e){
                    System.out.println("Error while reading dictionary:" + e.getMessage());
                    e.printStackTrace();
                    return false;
            }               
    }
    
    public boolean writeWordMap(String wordMapFile){
            try{
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                                    new FileOutputStream(wordMapFile), "UTF-8"));
                    
                    //write number of words
                    writer.write(word2id.size() + "\n");
                	List<Pair> words = new ArrayList<Pair>(); 
     
                    //write word to id
                	Iterator<String> it = word2id.keySet().iterator();
        			while (it.hasNext()){
        				String key = it.next();
        				Integer value = word2id.get(key);
        				
        				writer.write(key + " " + value + "\n");
        			}
        			
        			writer.close();
                    return true;
            }
            catch (Exception e){
                    System.out.println("Error while writing word map " + e.getMessage());
                    e.printStackTrace();
                    return false;
            }
            
            
    }
}
