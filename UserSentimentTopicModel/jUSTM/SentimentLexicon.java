package jUSTM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SentimentLexicon {
	public HashMap <Integer, Integer> SentiWord; //sentiment
	public HashMap <String , Integer> SentiMap;
		//0: strong negative; 1:weak negative; 2: neutral; 3: weak positive; 4: strong positive
	public static String PositiveMap0="sentiment"+File.separator+"正面评价词语中文.txt"; 
	public static String PositiveMap1="sentiment"+File.separator+"正面情感词语中文.txt";
	public static String NegativeMap0="sentiment"+File.separator+"负面评价词语中文.txt";
	public static String NegativeMap1="sentiment"+File.separator+"负面情感词语中文.txt";
	public SentimentLexicon()
	{
		SentiWord=new HashMap<Integer, Integer>();
		SentiMap=new HashMap<String, Integer>();
	}
	public int getSenti(int wordId)
	{
		return SentiWord.get(wordId);
	}

	
	public HashMap setSentiLexicon(Dictionary wordDict)
	{
		try {
			BufferedReader br ;
			br=new BufferedReader( new FileReader(PositiveMap0));
			String line;			
			line=br.readLine();
			line=br.readLine();
			while((line=br.readLine())!=null)
			{
				SentiMap.put(line, 2);
			}
			
			br=new BufferedReader(new FileReader(PositiveMap1));
			line=br.readLine();
			line=br.readLine();
			while((line=br.readLine())!=null)
			{
				SentiMap.put(line, 2);
			}
			br=new BufferedReader( new FileReader(NegativeMap0));
			
			line=br.readLine();
			line=br.readLine();
			while((line=br.readLine())!=null)
			{
				SentiMap.put(line, 0);
			}
			
			br=new BufferedReader(new FileReader(NegativeMap1));
			line=br.readLine();
			line=br.readLine();
			while((line=br.readLine())!=null)
			{
				SentiMap.put(line, 0);
			}
			br.close();
			Iterator<String> it=wordDict.word2id.keySet().iterator();
			while(it.hasNext())
			{
				String key =it.next();
				if(SentiMap.containsKey(key))
				{
					SentiWord.put(wordDict.word2id.get(key), SentiMap.get(key));
				}
				else
				{
					SentiWord.put(wordDict.word2id.get(key), (int)(Math.random()*3));
				}
					
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return SentiWord;
	}
}
