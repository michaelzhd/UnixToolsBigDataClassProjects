package edu.zhd.unixtools;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;


/**
 * A class designed to store and process bipartite graph.
 * Use two LinkedHashSet to store two different set of vertices.
 * 
 * 
 * 
 * 
 * @author Michael (Zhenghong) Dong
 * @since March 14,2014
 *
 */

public class BipartGraph extends SimpleGraph{
	
	


	//a construct required when extending SimpleGraph.
	public BipartGraph(Class edgeClass) {
		super(edgeClass);
	}
	

	private static final long serialVersionUID = 1L;
	
	//Two LinkedHashSet to store two sets of vertices
	private LinkedHashSet<String> drugSet =new LinkedHashSet<String>();
	private LinkedHashSet<String> enzymeSet =new LinkedHashSet<String>();
	
	private List<String> enzymeArray = new ArrayList<String>();
	private int edgeNumber = 0;

	
	private SimpleGraph bipGraph=new SimpleGraph<String,DefaultEdge>(DefaultEdge.class);
	
	
	public void addVertex(String v){
		this.bipGraph.addVertex(v);
		
	}
	
	public void addEdge(String v1, String v2){
		this.bipGraph.addEdge(v1,v2);
		
	}
	
	public void drugSetAdd(String v){
		this.drugSet.add(v);
		
	}

	public void enzymeSetAdd(String v){
		this.enzymeSet.add(v);
		
	}
	
	public int getEdgeNumber(){
		return this.edgeNumber;
	}

	public int getDrugSetSize(){
		return this.drugSet.size();
	}
	
	public LinkedHashSet<String> getDrugSet(){
		
		return this.drugSet;
		
	}
	
	public LinkedHashSet<String> getEnzymeSet(){
		
		return this.enzymeSet;
	}
	
	public SimpleGraph getGraph(){
		return this.bipGraph;
	}
	
	public void printGraph() {
		
		//Scanning through the drugset for drugs and finding their corresponding enzymes.
		for(String drug:this.drugSet){

			Iterator<DefaultEdge> iter = this.bipGraph.edgesOf(drug).iterator();
			while(iter.hasNext()){
				
				//formatting data into <drug id> <enzyme id> mode.
				String str =iter.next().toString();
				String str1 =str.substring(1,8);
				String str2 =str.substring(11,20);
				
				System.out.println(str1+" "+str2);
				this.edgeNumber++;
			}
		
		}
	}
	
	
	
	
	
}
