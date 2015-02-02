package edu.zhd.unixtools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * This a class extends the DefaultHandler and uses SAX to parse 
 * xml file. It contains a constructor that accept a String for 
 * output file location.
 * 
 * It scans the xml file for <drug> tags with property of "small molecule",
 * then finds the drug-id. It also search for <enzymes> tag and records the
 * enzyme ID within the tag. 
 * 
 * To avoid recording irrelevant IDs, a few flag switches were set to filter
 * through.
 * 
 * 
 * 
 * @author Michael (Zhenghong) Dong
 * @since March 14,2014
 * 
 */


public class DrugBankHandler extends DefaultHandler {
	
	//a flag switch used to filter drugs that are not small molecule.
	private boolean smallMoleculeDrugFlag = false;
	
	//a flag switch used to get tags and values inside <enzymes> node.
	private boolean enzymeFlag = false;
	
	//a flag switch used to filter out <drug> tags that are inside
	//<drug-interactions>, which are not our targets.
	private boolean drugInteractionFlag =false;
	
	//a flag switch used to filter out <id> tags inside <target> nodes.
	private boolean targetFlag = false;
	
	private String drugID = null;
	private String preTag = null;
	private String enzymeID = null;
	private List<String> enzymeIDs = new ArrayList<String>();
//	private String output="/Users/Michaelzhd/result.txt";
	private String output=null;
	
	
	
	BipartGraph drugGraph =new BipartGraph(null); 
	
	
	
	
	// constructor accepts output parameter.
	public DrugBankHandler(String output) {
		super();
		this.output = output;
	}

	//Remember the current io stream.
	PrintStream out = System.out;
	
	
	//The beginning of processing a xml file.
	@Override
	public void startDocument() throws SAXException {
		System.out.println("Start processing data, please wait...");
		
		//Try to redirect the io stream to a designated file on disk.
		try {
			File outputfile =new File(this.output);
			PrintStream resultFile =new PrintStream(outputfile);
			System.setOut(resultFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		
		preTag = qName;
		
		//find out <drug> nodes with "small molecule" attributes.
		if("drug".equalsIgnoreCase(preTag)){
			for(int i=0; i<attributes.getLength(); i++){
				if("type".equalsIgnoreCase(attributes.getQName(i))&&"small molecule".equals(attributes.getValue(i))){
					smallMoleculeDrugFlag = true;
					break;
					
				}
			}
		}
		if(true==smallMoleculeDrugFlag&&"enzymes".equalsIgnoreCase(preTag)){
			enzymeFlag = true;
		}
		
		//filter <drug> nodes inside <drug-interactions> nodes.
		if("drug-interactions".equalsIgnoreCase(preTag)){
			drugInteractionFlag = true;
			
		}
		
		//filter out ID nodes inside <target> nodes.
		if("targets".equalsIgnoreCase(preTag)){
			targetFlag = true;
			
		}
		
		
	}
	
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		
		if(false==drugInteractionFlag&&false==targetFlag){
			
			if(true==smallMoleculeDrugFlag&&null!=preTag){
				
				if("drugbank-id".equalsIgnoreCase(preTag)){
					
					// store the drug id into a variant
					drugID = new String(ch,start,length);
					
					// store the enzyme id into a variant and add to an enzyme id arraylist.
				}else if((true==enzymeFlag)&&"id".equalsIgnoreCase(preTag)){
					enzymeID = new String(ch,start,length);
					
					if(null!=enzymeID){
						enzymeIDs.add(enzymeID);
						
					}
				}
				
				
			}
			
		}
		
		
	}
	
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		preTag = null;
		
		
		
		if(false==drugInteractionFlag&&false==targetFlag){
			
			if("drug".equalsIgnoreCase(qName)){

				//add the collected drug id and enzyme id into a bipartite graph.
				if(0!=enzymeIDs.size()&&null!=drugID){
					for(String enID:enzymeIDs){
						drugGraph.addVertex(drugID);
						drugGraph.drugSetAdd(drugID);
						
						drugGraph.addVertex(enID);
						drugGraph.enzymeSetAdd(enID);
						
						drugGraph.addEdge(drugID, enID);
					}
					
				}
				
				//clear the enzyme id arraylist for next round of looping.
				enzymeIDs.clear();
				smallMoleculeDrugFlag = false;
			}else if("enzymes".equalsIgnoreCase(qName)){
				enzymeFlag = false;
				
			}
		}
		
		if("drug-interactions".equalsIgnoreCase(qName)){
			drugInteractionFlag = false;
			
		}
		if("targets".equalsIgnoreCase(qName)){
			targetFlag = false;
			
		}
		
		
	}
	
	//Scanning of the xml file is now ended. 
	//Print some simple reports and notifications.
	@Override
	public void endDocument() throws SAXException {
		
		drugGraph.printGraph();
		
		System.setOut(out);
		
		System.out.println("total number of small molecule drugs with enzymes is "+drugGraph.getDrugSetSize());
		System.out.println("total drug:enzyme pair number is "+drugGraph.getEdgeNumber());
		System.out.println("Data processing has finished, please check result file \"DrugBankResult.txt\" or your designated file.");
		System.out.println("Visualization is in process, please wait a few seconds...");
		
	}
	
	//A getter function that delivers the recorded bipartite graph.
	public BipartGraph getParsedBipartGraph(){
		
			return this.drugGraph;
		
	}
	
	
}
