package ART;

import java.util.Vector;

public class Document {

        //----------------------------------------------------
        //Instance Variables
        //----------------------------------------------------
        public int [] words;
        public int [] authors;
        public String rawStr;
        public int length;
        
        //----------------------------------------------------
        //Constructors
        //----------------------------------------------------
        public Document(){
                words = null;
                authors = null;
                rawStr = "";
                length = 0;
        }
        
        public Document(int length){
                this.length = length;
                rawStr = "";
                words = new int[length];
                authors = null;
        }
        
        public Document(int length, int [] words){
                this.length = length;
                rawStr = "";
                
                this.words = new int[length];
                for (int i =0 ; i < length; ++i){
                        this.words[i] = words[i];
                }
        }
        
        public Document(int length, int [] words, String rawStr){
                this.length = length;
                this.rawStr = rawStr;
                
                this.words = new int[length];
                for (int i =0 ; i < length; ++i){
                        this.words[i] = words[i];
                }
        }
        
        public Document(Vector<Integer> doc){
                this.length = doc.size();
                rawStr = "";
                this.words = new int[length];
                for (int i = 0; i < length; i++){
                        this.words[i] = doc.get(i);
                }
        }
        
        public Document(Vector<Integer> doc, Vector<Integer> auths, String rawStr){
                this.length = doc.size();
                this.rawStr = rawStr;
                int num_auths = auths.size();
                this.authors = new int[num_auths];
                for (int i = 0; i < num_auths; ++i)
                        authors[i] = auths.get(i);
                this.words = new int[length];
                for (int i = 0; i < length; ++i){
                        this.words[i] = doc.get(i);
                }
        }
}