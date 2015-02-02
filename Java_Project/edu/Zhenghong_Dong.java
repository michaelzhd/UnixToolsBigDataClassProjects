package edu.zhd.unixtools;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.xml.sax.SAXException;

import sun.java2d.Disposer;


public class Zhenghong_Dong extends JApplet {
	
    /**
	 * A java program that anaylses drugbank xml file, finds the drugs with enzymes,
	 * stores them in a bipartitle graph and visualizes the graph with JGraph library.
	 * 
	 * Please run this program with Java 1.6 version.
	 * To run this program, put drugbank.xml under the same directory with this jar file
	 * and type:
	 * java -jar Zhenghong_Dong.jar
	 * 
	 * Or to designate a properties file:
	 * java -jar -Dconfig="The/location/of/your/properties/file" Zhenghong_Dong.jar 
	 * 
	 * In the properties file, write like this:
	 * xmlLocation=/your/xml/file/path
	 * output=/your/desired/output/location/and/name
	 *
	 * The default properties file is:
	 * xmlLocation=drugbank.xml
	 * output=DrugBankResult.txt
	 * 
	 * 
	 * 
	 * 
	 * @author Michael (Zhenghong) Dong
	 * @since March 14,2014
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Color DEFAULT_BG_COLOR = Color.decode("#FAFBFF");
    private static final Dimension DEFAULT_SIZE = new Dimension(500, 11000);
//    private static final Dimension PANEL_SIZE = new Dimension(500, 800);
    static BipartGraph biDrugGraph =new BipartGraph(DefaultEdge.class); 
    static SimpleGraph drugGraph =null;
    

    

	public static void main(String[] args) {
		
		Zhenghong_Dong drugApplet =new Zhenghong_Dong();
		
		//define proporties instance to read storage information.
		Properties prop =new Properties();
		try {
			InputStream in =drugApplet.getClass().getResourceAsStream("xml.properties");
			prop.load(in);
			in.close();
		} catch (IOException e1) {
			System.out.println("Fail to load the default properties file.");
		}
		
		//Used for overriding default configuration by user designated ones.
		String externalIn =System.getProperty("config");
		try {
			InputStream exIn =new FileInputStream(new File(externalIn));
			prop.load(exIn);
			exIn.close();
		} catch (Exception e2) {
			System.out.println("No user designated configuration file or the configuration"
					+ " does not contain valid information."
					+ "\nProgram will run using default configuration.");
		}
		
		String xmlLocation = prop.getProperty("xmlLocation");
		String output =prop.getProperty("output");
		
		File inputFile =null;
		try {
		inputFile = new File(xmlLocation);
		} catch (Exception e3) {
			System.out.println("Failed to open xml file. Please put your drugbank.xml"
					+ " file at the same directory with this jar file or"
					+ "designate your config file which contains the information of the"
					+ "location of drugbank.xml file.");
			Runtime.getRuntime().halt(0);
			System.exit(ERROR);
		}
		
		
		
		
		//Instantiate hander instance for analysing xml file.
		DrugBankHandler dbHandler = new DrugBankHandler(output);
		SAXParserFactory spFactory =SAXParserFactory.newInstance();
		SAXParser saxParser=null;
		try {
			saxParser = spFactory.newSAXParser();
		} catch (ParserConfigurationException e4) {
			System.out.println("Fail to configure Parser.");
		} catch (SAXException e5) {
			System.out.println("SAX fail to operate. Check your xml file for integrity.");
			
		}
		try {
			saxParser.parse(inputFile, dbHandler);
		} catch (SAXException e6) {
			System.out.println("SAX fail to operate. Check your xml file for integrity.");
		} catch (IOException e7) {
			System.out.println("Failed to open xml file. Please put your drugbank.xml"
					+ " file at the same directory with this jar file or"
					+ "designate your config file which contains the information of the"
					+ "location of drugbank.xml file.");		
		}
		
		biDrugGraph=dbHandler.getParsedBipartGraph();
		drugGraph =biDrugGraph.getGraph();
		
		
		drugApplet.drawGraph();
		
		
		
		//Drawing the GUI interface for visualizing the graph.
		JFrame frame = new JFrame();
		
		drugApplet.setMinimumSize(DEFAULT_SIZE);
		drugApplet.setPreferredSize(DEFAULT_SIZE);
		
		
		JScrollPane scrlpane =new JScrollPane(drugApplet);
		
		scrlpane.setPreferredSize(DEFAULT_SIZE);
		
		frame.getContentPane().add(scrlpane);
		frame.setTitle("DrugBank view");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(true);
		
		frame.pack();
		frame.setVisible(true);
		
		
	
	}
	
	
	
	
	private JGraphModelAdapter jgAdapter;

	public void drawGraph(){
		
		
		
		jgAdapter =new JGraphModelAdapter(drugGraph);
		JGraph jgraph =new JGraph(jgAdapter);
		
		adjustDisplaySettings(jgraph);
        getContentPane().add(jgraph);
        resize(DEFAULT_SIZE);
        
        
        //Place the vertices nicely with JGraph components.
        int drugYPos =0;
		LinkedHashSet<String> drugs =biDrugGraph.getDrugSet();
		Iterator<String> iterDrug =drugs.iterator();
		while(iterDrug.hasNext()){
			drugYPos += 90;
			positionVertexAt(iterDrug.next(), 50, drugYPos);
			
			
		}
		
		int enzymeYPos =0;
		LinkedHashSet<String> enzymes =biDrugGraph.getEnzymeSet();
		Iterator<String> iterEnzyme =enzymes.iterator();
		while(iterEnzyme.hasNext()){
			
			enzymeYPos +=30;
			positionVertexAt(iterEnzyme.next(), 300, enzymeYPos);
		}
		
        
		
		
	}
	

	private void adjustDisplaySettings(JGraph jg){
		
		 jg.setPreferredSize(DEFAULT_SIZE);

	        Color c = DEFAULT_BG_COLOR;
	        String colorStr = null;

	        try {
	            colorStr = getParameter("bgcolor");
	        } catch (Exception e) {
	        }

	        if (colorStr != null) {
	            c = Color.decode(colorStr);
	        }

	        jg.setBackground(c);
		
		
	}
	
	private void positionVertexAt(Object vertex, int x, int y) {
		DefaultGraphCell cell = jgAdapter.getVertexCell(vertex);
		AttributeMap attr = cell.getAttributes();
		Rectangle2D bounds = GraphConstants.getBounds(attr);

		Rectangle2D newBounds = new Rectangle2D.Double(x, y, bounds.getWidth(),
				bounds.getHeight());

		GraphConstants.setBounds(attr, newBounds);

		AttributeMap cellAttr = new AttributeMap();
		cellAttr.put(cell, attr);
		jgAdapter.edit(cellAttr, null, null, null);
	}
	

	
	
}



