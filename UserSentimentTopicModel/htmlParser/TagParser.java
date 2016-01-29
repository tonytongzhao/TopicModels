package htmlParser;

import java.util.*;

public class TagParser {
	int m=0;
	boolean end = false;
	String htmlData = null;
	int postion = 0;
	public void onGetTxt(String cont){
	//	System.out.println(cont);
	}
	public void onGetTag(String tgr){
	}

	public void jumpTo(String s){
		if(this.htmlData ==  null || s == null) return ;
		int p = this.htmlData.indexOf(s, this.postion);
        if (p == -1)
            p = this.htmlData.indexOf(s.toUpperCase(), this.postion);
        if (p != -1)
            this.postion = p + s.length();
        else
            this.postion = this.htmlData.length();
	}
	public void doParser(String content){
		
		if(content == null) return ;
		htmlData = content;
        StringBuffer strBuffer = new StringBuffer(32);
        char c = '\r';
        for(this.postion = 0; this.postion < content.length(); this.postion++){
        	if(this.end)break;
            c = content.charAt(this.postion);
            if (c == '<'){
                String cont = strBuffer.toString().trim();
               
                if (cont.length() != 0){
                   this.onGetTxt(cont);
                   strBuffer = new StringBuffer(32);
                }
            }else if (c == '>'){
                String tagstro = strBuffer.toString().trim();
                //System.out.println(tagstro);
                if (tagstro.length() != 0){
                    this.onGetTag(tagstro);
                    strBuffer = new StringBuffer(32);
                }
            } else if (c != '\n' && c != '\r'){
                    strBuffer.append(c);
            }
           
       }
	}
	public static String getAbsUrl(String url, String p){

        String absurl = p;
        if (p == null || p.startsWith("http://"))
            return absurl;
        if (p.indexOf('/') != -1){
            String host = null;
            int index = 0;
            for(int i = 0 ; i < url.length();i++){
            	char c = url.charAt(i);
                index += 1;
                if (c == '/')
                    continue;
                if (i == 3){
                	host = url.substring(0,index-1);
                    absurl = host + p;
                    break;
                }
            }
            }
        else
            absurl = url.substring(0,url.lastIndexOf('/')+1) + p;
        return absurl;
    }
    private static int TAG_ST_NAME = 1;
    private static int TAG_ST_KEY = 2;
    private static int TAG_ST_VALUE = 3;

    public static Hashtable getTag(String tag){
    	if (tag == null) return null;
    	int st = TAG_ST_NAME;
    	Hashtable ht = new Hashtable(5);
    	tag = tag.trim();
    	StringBuffer tb = new StringBuffer(32);
    	String key = null;
    	for(int i = 0; i< tag.length(); i ++){
    		char c = tag.charAt(i);
			if (st == TAG_ST_NAME && c==' '){
				st = TAG_ST_KEY;
				ht.put("tag", tb.toString());
				tb = null;
				tb = new StringBuffer(32);
				continue;
			}else if(st == TAG_ST_KEY && c == '='){
				st = TAG_ST_VALUE;
				key = tb.toString().toLowerCase();
				tb = null;
				tb = new StringBuffer(32);
				continue;
			}else if(st == TAG_ST_VALUE ) {

				if (c==' ' ||i == tag.length()-1){
					st = TAG_ST_KEY;
					if(i == tag.length()-1 && c != '\"')
						tb.append(tag.charAt(i));
					ht.put(key, tb.toString());
					key = null;
					tb = null;
					tb = new StringBuffer(32);
					continue;
				}
			}
			if(c != '\"')
				tb.append(c);
    	}
    	return ht;

    }
}
class HtmlParser extends TagParser{
	String encoding = null;
	String author=null;
	public void onGetTxt(String cont){
		
		if(m==1)
		{
			author=cont;
		}
		
		if(cont.equals("reviewed by"))
		{
			m++;
		}
		else
		{
			m=0;
		}
		
	}
	public void onGetTag(String tgr){
		char c = tgr.charAt(0);
		if(c =='!'){
			if(tgr.startsWith("!--")&& !tgr.endsWith("--")){
				this.jumpTo("--");
			}
			return;
		}else if(c == 's' || c == 'S'){
			String tl= tgr.toLowerCase();
			if(tl.startsWith("script") && !tl.endsWith("/")){
				this.jumpTo("/script");
			}else if(tl.startsWith("style") && !tl.endsWith("/")){
				this.jumpTo("/style");
			}
		}else if(c == 'm' || c == 'M'){
			String tl= tgr.toLowerCase();
			if(this.encoding == null && tl.startsWith("meta")){
				if (tl.indexOf("utf") != -1)
                     this.encoding = "utf8";
                else if (tl.indexOf("gb2312") != -1)
                     this.encoding = "gb2312";
                else
                     this.encoding = "gbk";
			}
		}

	}

}
