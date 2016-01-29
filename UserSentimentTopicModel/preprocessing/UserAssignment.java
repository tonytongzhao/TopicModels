package preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;

public class UserAssignment {
	HashMap<String, String> f2a,a2p,a2n;
	public UserAssignment()
	{
		a2p=new HashMap<String, String>();
		a2n=new HashMap<String, String>();
		f2a=new HashMap<String, String>();
		
	}
	public void assignuser() throws Exception
	{
		int number =0;
		String tmp;
		File fp=new File("pos");
		File fn=new File("neg");
		File fauthorpost=new File("author_post.txt");
		File result=new File("user_post7.txt");
		File result1=new File("author_sentiment.txt");
		BufferedWriter bw =new BufferedWriter(new FileWriter(result));
		BufferedWriter bw1 =new BufferedWriter(new FileWriter(result1));
		System.out.println(fp.list().length);
		for(int i=0;i<fp.list().length;i++)
		{
			
			a2p.put(fp.list()[i].split("_")[1].replace(".txt", ""), fp.list()[i]);
			
		}
		System.out.println(fn.list().length);
		for(int i=0;i<fn.list().length;i++)
		{
			a2n.put(fn.list()[i].split("_")[1].replace(".txt", ""), fn.list()[i]);
		}
		BufferedReader br =new BufferedReader(new FileReader(fauthorpost));
		while((tmp=br.readLine())!=null)
		{
			f2a.put(tmp.split(";")[1].replace(".html", ""), tmp.split(";")[0]);
		}
		System.out.println(a2p.keySet().size());
		Iterator it1=a2p.keySet().iterator();
		Iterator it2=a2n.keySet().iterator();
		String fname;
		while(it1.hasNext())
		{
			fname=it1.next().toString();
			number++;
			bw.write(f2a.get(fname)+";"+ a2p.get(fname));
			bw.newLine();
			bw1.write(f2a.get(fname)+";"+ a2p.get(fname)+";pos");
			bw1.newLine();
		}
		System.out.println("number :"+number);
		while(it2.hasNext())
		{
			fname=it2.next().toString();
			number++;
			bw.write(f2a.get(fname)+";"+ a2n.get(fname));
			bw.newLine();
			bw1.write(f2a.get(fname)+";"+ a2n.get(fname)+";neg");
			bw1.newLine();
		}
		System.out.println("number :"+number);
		bw.close();
		bw1.close();
		System.out.println(number);
	}
	public static void main(String[] args) throws Exception
	{
		UserAssignment ua =new UserAssignment();
		ua.assignuser();
	}
}
