package htmlParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;




public class ProcessTag {

	/**
	 * Method main
	 *
	 *
	 * @param args
	 * @throws Exception 
	 *
	 */
	public static void main(String[] args) throws Exception {
	  
	  OpenUrl ou=new OpenUrl();
	  File f =new File("movie");
	  File a =new File("a2d.txt");
	  BufferedWriter wr1=new BufferedWriter(new FileWriter(a));

	  System.out.println(f.list().length);
	 
	  
	  for(int i=0;i<f.list().length;i++)
	  {  
		  File f1= new File(f+File.separator+f.list()[i]);
		 System.out.println(f.list()[i]);
		 String content = ou.getContent(f1);
		  //System.out.println(content);
		  HtmlParser tp = new HtmlParser();
		  tp.doParser(content);
		  System.out.println(tp.author);
		  wr1.write(tp.author+";"+f.list()[i]);
		  wr1.newLine();
	  }
	  wr1.close();
	}
}
class OpenUrl
{
 public  String getContent(File f)
 // һ��public�����������ַ������򷵻�"error open url"
 {
  try{

   
   BufferedReader br=new BufferedReader(new FileReader(f));
   String s="";
   StringBuffer sb=new StringBuffer("");
   while((s=br.readLine())!=null)
   {
    sb.append(s);
   }
   br.close();
   return sb.toString();
  }
  catch(Exception e){
   return "error open url" + f;

  }
 }
 }