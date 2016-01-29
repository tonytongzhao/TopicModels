package TEM;

public class GBPair {
	private int generalCount; 
	private int bestCount;
	
	public GBPair(){
		this.generalCount = 0;
		this.bestCount = 0;
	}
	
	public GBPair(int g, int b){
		this.generalCount = g;
		this.bestCount = b;
	}

	public void gIncrement(){
		this.generalCount++;
	}
	
	public void bIncrement(){
		this.bestCount++;
	}
	
	public int getGeneralCount(){
		return this.generalCount;
	}
	
	public int getBestCount(){
		return this.bestCount;
	}
}
