package klDivergence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class KLDivergence {
	int m,n;
	double matrix1[][];
	double matrix2[][];
	public KLDivergence()
	{
		matrix1=null;
		matrix2=null;
	}
	public void calculate_KL(File f) throws Exception
	{		
		matrix1=null;
		m=0;
		ArrayList<String> lines =new ArrayList<String>();
		String tmp;
		BufferedReader br =new BufferedReader(new FileReader(f));
		while((tmp=br.readLine())!=null)
		{
			m++;
			lines.add(tmp);
		}
		br.close();
		n=lines.get(0).split(" ").length;
		System.out.println(m+" "+n);
		matrix1=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f));
		String[] t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix1[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}
		
		double sum=0;
		for(int i=0;i<m-1;i++)
		{
			for(int j=i+1;j<m;j++)
			{
				for(int k=0;k<n;k++)
				{
					sum+=matrix1[i][k]* Math.log((matrix1[i][k]/matrix1[j][k]));
				}
			}
		}
		double avgKL=sum*2/(m*(m-1));
		System.out.println(avgKL);
		
	}
	public void calculate_KL(File f1,File f2) throws Exception
	{		
		matrix1=null;
		matrix2=null;
		m=0;
		ArrayList<String> lines =new ArrayList<String>();
		String tmp;
		BufferedReader br =new BufferedReader(new FileReader(f1));
		while((tmp=br.readLine())!=null)
		{
			m++;
			lines.add(tmp);
		}
		br.close();
		n=lines.get(0).split(" ").length;
		matrix1=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f1));
		String[] t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix1[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}

		br =new BufferedReader(new FileReader(f2));
		while((tmp=br.readLine())!=null)
		{
			lines.add(tmp);
		}
		br.close();
		matrix2=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f2));
		t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix2[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}
		double sum=0;
		for(int i=0;i<m;i++)
		{
			for(int j=0;j<m;j++)
			{
				for(int k=0;k<n;k++)
				{
					sum+=matrix1[i][k]* Math.log((matrix1[i][k]/matrix2[j][k]));
				}
			}
		}
		double avgKL=sum/(m*m);
		System.out.println(avgKL);
		
	}
	public void calculate_JS(File f) throws Exception
	{		
		matrix1=null;
		m=0;
		ArrayList<String> lines =new ArrayList<String>();
		String tmp;
		BufferedReader br =new BufferedReader(new FileReader(f));
		while((tmp=br.readLine())!=null)
		{
			m++;
			lines.add(tmp);
		}
		br.close();
		n=lines.get(0).split(" ").length;
		System.out.println(m+" "+n);
		matrix1=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f));
		String[] t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix1[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}
		double []tk=new double[n];
		double sum=0;
		for(int i=0;i<m-1;i++)
		{
			for(int j=i+1;j<m;j++)
			{
				
				for(int k=0;k<n;k++)
				{
					tk[k]=(matrix1[i][k]+matrix1[j][k])/2;
					sum+=(matrix1[i][k]* Math.log((matrix1[i][k]/tk[k]))+matrix1[j][k]* Math.log((matrix1[j][k]/tk[k])))/2;
				}
			}
		}
		double avgKL=sum*2/(m*(m-1));
		System.out.println(avgKL);
		
	}
	public void calculate_JS(File f1,File f2) throws Exception
	{		
		matrix1=null;
		matrix2=null;
		m=0;
		ArrayList<String> lines =new ArrayList<String>();
		String tmp;
		BufferedReader br =new BufferedReader(new FileReader(f1));
		while((tmp=br.readLine())!=null)
		{
			m++;
			lines.add(tmp);
		}
		br.close();
		n=lines.get(0).split(" ").length;
		matrix1=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f1));
		String[] t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix1[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}

		br =new BufferedReader(new FileReader(f2));
		while((tmp=br.readLine())!=null)
		{
			lines.add(tmp);
		}
		br.close();
		matrix2=new double[m][n];
		lines.clear();
		m=0;
		br=new BufferedReader(new  FileReader(f2));
		t=null;
		while((tmp=br.readLine())!=null)
		{
			t=tmp.split(" ");
			for(int i=0;i<t.length;i++)
			{
				matrix2[m][i]=Double.parseDouble(t[i]);
			}
			++m;
		}
		double tk[]=new double[n];
		double sum=0;
		for(int i=0;i<m;i++)
		{
			for(int j=0;j<m;j++)
			{
				for(int k=0;k<n;k++)
				{
					tk[k]=(matrix1[i][k]+matrix2[j][k])/2;
					sum+=(matrix1[i][k]* Math.log((matrix1[i][k]/tk[k]))+matrix1[j][k]* Math.log((matrix1[j][k]/tk[k])))/2;
				}
			}
		}
		double avgKL=sum/(m*m);
		System.out.println(avgKL);
		
	}
	public static void main(String[] args) throws Exception
	{
		File f= new File("model-final.phi_AT");
		File f1=new File("model-final.phi_LDA");
		File f2=new File("KLresult/model-final.theta_negative");
		KLDivergence kl=new KLDivergence();
		kl.calculate_JS(f);
		kl.calculate_JS(f1);
		//kl.calculate_KL(f1);
		//kl.calculate_KL(f2,f1);
	}
}
