package validate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Validate {
	String [] sentilabel;
	String [] author;
	String [] Docs;
	ArrayList<String> authors;
	Map author2id;
	String [] authorpi;
	int [] authorpos;
	int [] authorneg;
	int M,MP,MN;
	int indp,indn;
	public Validate()
	{
	}
	public void readDocs()
	{
		int pos=0;
		int neg=0;
		File f=new File("validation3");
		try {
			BufferedWriter br= new BufferedWriter(new FileWriter("validation3"+File.separator+"user_post7.txt"));
			for(int i=0;i<f.list().length;i++)
			{
				if(f.list()[i].contains("pos"))
				{
					pos++;
				}
				else
				{
					neg++;
				}
				br.write((int)(Math.random()*100)+";"+f.list()[i]+"\n");
			}
		br.close();
	System.out.println(f.list().length);
	System.out.println(pos);
	System.out.println(neg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void evaluate() throws Exception
	{
		String tmp;
		String a=null;
		sentilabel=null;
		int FN=0,FP=0,IN=0,IP=0;
		int SUMFN=0,SUMFP=0,SUMIN=0,SUMIP=0;
		author2id =new HashMap<String, Integer>();
		File r=new File("validation3"+File.separator+"user_post7.txt");
		File fn=new File("Author_Doc_Negative.txt");
		File fp=new File("Author_Doc_Positive.txt");
		File fnt=new File("Topic_Author_Negative.txt");
		File fpt=new File("Topic_Author_Positive.txt");
		File fa=new File("validation3"+File.separator+"Author_map.txt");
		File fpi=new File("validation3"+File.separator+"model-final.pi");
		authors=new ArrayList<String>();
		int i=0;
		BufferedReader ba=new BufferedReader(new FileReader(fa));
		while((tmp=ba.readLine())!=null)
		{
			authors.add(tmp);
			author2id.put(tmp,i);
			i++;
		}
	
		ba.close();
		
		BufferedReader br =new BufferedReader(new  FileReader(r));		
		tmp=br.readLine();
		sentilabel =new String[Integer.parseInt(tmp)];
		author     =new String[Integer.parseInt(tmp)]; 
		Docs       =new String[Integer.parseInt(tmp)]; 
		authorneg=new int[authors.size()];
		authorpos=new int[authors.size()];
		authorpi=new String[authors.size()];
		for(int l=0;l<authors.size();l++)
		{
			authorneg[l]=authorpos[l]=0;
		}
		//System.out.println(sentilabel.length);
		 i=0;
		while((tmp=br.readLine())!=null)
		{
			author[i]=tmp.split(";")[0];
			sentilabel[i]=tmp.split(";")[1].split("_")[1].substring(0, 3);
			Docs[i]=tmp.split(";")[1].split("_")[0];
			
			if(sentilabel[i].equals("pos"))
			{
				authorpos[Integer.parseInt(author2id.get(author[i]).toString())]++;
			}
			else
			{
				
					authorneg[Integer.parseInt(author2id.get(author[i]).toString())]++;
				
			}
			i++;
		}
		br.close();
		M=i;
		
		BufferedWriter wr1=new BufferedWriter(new FileWriter(new File("Author_Negative_Precise.txt")));
		BufferedReader br1= new BufferedReader(new FileReader(fn));
		while((tmp=br1.readLine())!=null)
		{
			if(tmp.contains("Author"))
			{
				if(FN!=0)
				{
					wr1.write("Precise: "+(double)IN/FN+"\n");
					if(FN<3)
					{
						SUMFN=SUMFN-FN;
						SUMIN=SUMIN-IN;
					}
				}
				FN=0;
				IN=0;
		
				wr1.newLine();
				wr1.newLine();
				a=tmp.split(" ")[1];
				
				wr1.write("Author "+authors.get(Integer.parseInt(a))+" ");
				wr1.newLine();
				
				
			}
			else if(tmp.contains("Topic"))
			{
				wr1.write(tmp);
				wr1.newLine();
			}
			else
			{		
				if(author[Integer.parseInt(tmp)].equals(authors.get(Integer.parseInt(a))))
				{
					SUMFN++;
					FN++;
					wr1.write(Docs[Integer.parseInt(tmp)]);
					if(sentilabel[Integer.parseInt(tmp)].equals("neg"))
					{
						IN++;
						SUMIN++;
						wr1.write(" Y");
						wr1.newLine();
					}
					else
					{
						wr1.write(" N");
						wr1.newLine();
					}
				}
			}
			
		}

		wr1.write(""+(double)IN/FN);
		wr1.newLine();
		wr1.write("Sum Precise: "+(double)SUMIN/SUMFN);
		br1.close();
		wr1.close();
		
		BufferedWriter wr2=new BufferedWriter(new FileWriter(new File("Author_Positive_Precise.txt")));
		BufferedReader br2= new BufferedReader(new FileReader(fp));
		while((tmp=br2.readLine())!=null)
		{
			if(tmp.contains("Author"))
			{
				if(FP!=0)
				{

					wr2.write("Precise: "+(double)IP/FP+"\n");
					if(FP<3)
					{
						SUMFP=SUMFP-FP;
						SUMIP=SUMIP-IP;
					}
				}
				FP=0;
				IP=0;
			
				wr2.newLine();
				wr2.newLine();
				a=tmp.split(" ")[1];
				System.out.println(authors.get(Integer.parseInt(a)));
				wr2.write("Author "+authors.get(Integer.parseInt(a))+" ");
				wr2.newLine();
				
				
			}
			else if(tmp.contains("Topic"))
			{
				wr2.write(tmp);
				wr2.newLine();
			}
			else
			{		
				if(author[Integer.parseInt(tmp)].equals(authors.get(Integer.parseInt(a))))
				{
					SUMFP++;
					FP++;
					wr2.write(Docs[Integer.parseInt(tmp)]);
					if(sentilabel[Integer.parseInt(tmp)].equals("pos"))
					{
						SUMIP++;
						IP++;
						wr2.write(" Y");
						wr2.newLine();
					}
					else
					{
						wr2.write(" N");
						wr2.newLine();
					}
				}
			}
			
		}
	
		wr2.write(""+(double)IP/FP);
		wr2.newLine();
		wr2.write("Sum Precise: "+(double)SUMIP/SUMFP);
		br2.close();
		wr2.close();
		
		BufferedReader brpi=new BufferedReader(new FileReader(fpi));
		i=0;
		while((tmp=brpi.readLine())!=null)
		{
			if(Double.valueOf(tmp.split(" ")[0])>Double.valueOf(tmp.split(" ")[1]))
			{
				authorpi[i]="neg";
			}
			else
			{
				authorpi[i]="pos";
			}
			i++;
		}
		brpi.close();
		int ansum=0,anprecise=0,apsum=0,apprecise=0;
		BufferedWriter bwpi=new BufferedWriter(new FileWriter(new File("Author Precise.txt")));
		for(i=0;i<authors.size();i++)
		{
			
			bwpi.write("Author "+authors.get(i)+" neg:"+authorneg[i]+" pos:"+authorpos[i]+" pi:"+authorpi[i]);
			
			if(authorpi[i].equals("pos"))
			{
				apsum++;
				if(authorpos[i]>authorneg[i])
				{
					apprecise++;	
					bwpi.write(" Y");
					bwpi.newLine();
				}
				else
				{
					bwpi.write(" N");
					bwpi.newLine();
				}
			}
			if(authorpi[i].equals("neg"))
			{
				ansum++;
				if(authorpos[i]<authorneg[i])
				{
					anprecise++;	
					bwpi.write(" Y");
					bwpi.newLine();
				}
				else
				{
					bwpi.write(" N");
					bwpi.newLine();
				}
			}
		}
		bwpi.newLine();
		bwpi.write("positive precise: "+(double)apprecise/apsum+" negative precise: "+(double)anprecise/ansum+" sum precise:"+(double)(apprecise+anprecise)/(ansum+apsum));
		bwpi.close();
		String tt=null;
		FP=0;
		IP=0;
		SUMIP=0;
		SUMFP=0;
		 wr2=new BufferedWriter(new FileWriter(new File("Topic_Author_Positive_Precise.txt")));
		 br2= new BufferedReader(new FileReader(fpt));
		while((tmp=br2.readLine())!=null)
		{
			if(tmp.contains("Topic"))
			{
				
				tt=tmp.split(" ")[1];
				
				wr2.write("Topic "+tt+" ");
				wr2.newLine();
				
				
			}
			else if(tmp.contains("Author"))
			{
				if(FP!=0)
				{
					wr2.write("Precise: "+(double)IP/FP+"\n");
					if(FP<3)
					{
						SUMFP=SUMFP-FP;
						SUMIP=SUMIP-IP;
					}
				}
				FP=0;
				IP=0;
			
				wr2.newLine();
				wr2.newLine();
				a=authors.get(Integer.parseInt(tmp.split(" ")[1]));
				wr2.write("Author "+authors.get(Integer.parseInt(tmp.split(" ")[1])));
				wr2.newLine();
			}
			else
			{		
				if(author[Integer.parseInt(tmp)].equals(authors.get(Integer.parseInt(a))))
				{
					SUMFP++;
					FP++;
					wr2.write(Docs[Integer.parseInt(tmp)]);
					if(sentilabel[Integer.parseInt(tmp)].equals("pos"))
					{
						SUMIP++;
						IP++;
						wr2.write(" Y");
						wr2.newLine();
					}
					else
					{
						wr2.write(" N");
						wr2.newLine();
					}
				}
			}
			
		}

		wr2.write(""+(double)IP/FP);
		wr2.newLine();
		wr2.write("Sum Precise: "+(double)SUMIP/SUMFP);
		br2.close();
		wr2.close();
	
		SUMFN=0;
		SUMIN=0;
		FN=0;
		IN=0;
		 wr2=new BufferedWriter(new FileWriter(new File("Topic_Author_Negative_Precise.txt")));
		 br2= new BufferedReader(new FileReader(fnt));
		while((tmp=br2.readLine())!=null)
		{
			if(tmp.contains("Topic"))
			{
			
				tt=tmp.split(" ")[1];
				wr2.write("Topic "+tt+" ");
				wr2.newLine();
				
				
				
			}
			else if(tmp.contains("Author"))
			{	if(FN!=0)
				{
				
				wr2.write("Precise: "+(double)IN/FN+"\n");
				
				if(FN<3)
				{
					SUMFN=SUMFN-FN;
					SUMIN=SUMIN-IN;
				}
			}
			FN=0;
			IN=0;
		
			wr2.newLine();
			wr2.newLine();
			System.out.println("asd"+tmp.split(" ")[1]);
				a=authors.get(Integer.parseInt(tmp.split(" ")[1]));
				
				wr2.write("Author "+a);
				wr2.newLine();
			}
			else
			{		
				if(author[Integer.parseInt(tmp)].equals(a))
				{
					SUMFN++;
					FN++;
					wr2.write(Docs[Integer.parseInt(tmp)]);
					if(sentilabel[Integer.parseInt(tmp)].equals("neg"))
					{
						SUMIN++;
						IN++;
						wr2.write(" Y");
						wr2.newLine();
					}
					else
					{
						wr2.write(" N");
						wr2.newLine();
					}
				}
			}
			
		}

		wr2.write(""+(double)IN/FN);
		wr2.newLine();
		wr2.write("Sum Precise: "+(double)SUMIN/SUMFN);
		br2.close();
		wr2.close();
		
		
	}
	public static void main(String[] arg) throws Exception
	{
		Validate v =	new Validate();
		v.evaluate();
		
	}
}
