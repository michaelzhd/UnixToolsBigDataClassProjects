package drugbankstat;

/**
 * 
 * DrugBank drug-drug graph and statistics generation. 
 *  
 * 
 * @Author: Zhenghong Dong (Michael)
 * @Date:   May 6, 2014
 * 
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;




public class DrugBankStat extends Configured implements Tool {

	//Used to collect nodes.
	protected static Set<String> nodeSet = new HashSet<String>();
	//Used to count edge number.
	protected static long edgeNum = 0;
	
	//Record line number that cause exception.
	enum Counter{
		LINESKIP;
	}
	
	
	//Mapper for first mapreduce job.
	public static class DrugBankStepOneMapper extends Mapper<LongWritable,Text,Text,Text>{
		
		private final Text drugText = new Text();
		private final Text enzymeText = new Text();
		private final List<String> drugPair = new ArrayList<String>();
		
		@Override
		protected void map(LongWritable key, Text value, Context context) {
			
			//Fetch a line of data and break into fields and store into a list.
			StringTokenizer tokenizer = new StringTokenizer(value.toString());
			while(tokenizer.hasMoreTokens()){
				drugPair.add(tokenizer.nextToken());
			}
			
			drugText.set(drugPair.get(0));
			enzymeText.set(drugPair.get(1));
			//clear the list for next round usage.
			drugPair.clear();
			
			try {
				context.write(enzymeText, drugText);
			} catch (IOException e) {
				context.getCounter(Counter.LINESKIP).increment(1);
				System.out.println("Error encountered while trying to write mapping result.");
				return;
			} catch (InterruptedException e) {
				context.getCounter(Counter.LINESKIP).increment(1);
				System.out.println("Interrupted while trying to write mapping result.");
				return;
			}
			
		}
		
	}
	
	
	//Reducer for the first mapreduce job.
	public static class DrugBankStepOneReducer extends Reducer<Text, Text, Text, Text>{
		
		private List<String> drugList = new ArrayList<String>();
		
		
		@Override
		protected void reduce(Text enzyme, Iterable<Text> drugs, Context context) throws IOException, InterruptedException{

			for(Text drug:drugs){
				drugList.add(drug.toString());
			}
			
			//Sort the drug in current list so that the generated drug pairs are in order.
			//Thus,each pair of drugs has only 1 direction in all data.
			Collections.sort(drugList);
			
			int drugSize =drugList.size();
			
			//Filter out those enzymes with only 1 drug.
			if(2 <= drugSize){
				
				//list all possible couple combinations in all drugs related to current enzyme.
				for(int i=0;i<drugSize-1;i++){
					nodeSet.add(drugList.get(i));
					for(int j=i+1;j<drugSize;j++){
							context.write(new Text(drugList.get(i)),new Text(drugList.get(j)));
					}
				}
				nodeSet.add(drugList.get(drugSize-1));
				
			}
			
			//Clear the list for next round usage.
			drugList.clear();
			
		}
		
	}
	
	//Mapper for the second mapreduce job.
	public static class DrugBankStepTwoMapper extends Mapper<LongWritable,Text,Text,IntWritable>{
		
		private final static IntWritable One = new IntWritable(1);

		
		@Override
		protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
			
			context.write(value, One);
			
		}
		
	}
	
	
	//Reducer for the second mapreduce job.
	public static class DrugBankStepTwoReducer extends Reducer<Text, IntWritable, Text, IntWritable>{
		
		
		
		@Override
		protected void reduce(Text drugPair, Iterable<IntWritable> edges, Context context) throws IOException, InterruptedException{

			int weight = 0;
			for(IntWritable edge:edges){
				weight += edge.get();
				
			}
			
			edgeNum += 1;
			context.write(drugPair, new IntWritable(weight));
			
		}
		
	}
	
	
	
	public int run(String[] args) throws IOException, InterruptedException, ClassNotFoundException{
		
		String[] otherArgs =new GenericOptionsParser(getConf(),args).getRemainingArgs();
		if(otherArgs.length != 2){
			System.err.println("Usage: DrugBankStat <in> <out>");
			System.exit(2);
			
		}
		
		Path inputDir = new Path(otherArgs[0]);
		Path intermediateDir = new Path("hdfs://hd:9000/drugbank/intermediateDir");
		Path outputDir = new Path(otherArgs[1]);
		
		
		
		//Set up first mapreduce job.
		Job FirstStepJob = Job.getInstance(getConf(),"DrugBank Statistics step 1");
		FirstStepJob.setJarByClass(getClass());
		
		TextInputFormat.addInputPath(FirstStepJob,inputDir);
		TextOutputFormat.setOutputPath(FirstStepJob,intermediateDir);
		
		FirstStepJob.setInputFormatClass(TextInputFormat.class);
		FirstStepJob.setMapOutputKeyClass(Text.class);
		FirstStepJob.setMapOutputValueClass(Text.class);
		FirstStepJob.setMapperClass(DrugBankStepOneMapper.class);
		FirstStepJob.setReducerClass(DrugBankStepOneReducer.class);
		
		FirstStepJob.setOutputFormatClass(TextOutputFormat.class);
		FirstStepJob.setOutputKeyClass(Text.class);
		FirstStepJob.setOutputValueClass(Text.class);
		FirstStepJob.waitForCompletion(true);//Make sure second job only runs after the first job finished./
	
		
		//Set up second mapreduce job.
		Job SecondStepJob = Job.getInstance(getConf(),"DrugBank Statistics step 2");
		SecondStepJob.setJarByClass(getClass());
		
		TextInputFormat.addInputPath(SecondStepJob,intermediateDir);
		TextOutputFormat.setOutputPath(SecondStepJob,outputDir);
		
		SecondStepJob.setInputFormatClass(TextInputFormat.class);
		SecondStepJob.setMapOutputKeyClass(Text.class);
		SecondStepJob.setMapOutputValueClass(IntWritable.class);
		SecondStepJob.setMapperClass(DrugBankStepTwoMapper.class);
		SecondStepJob.setCombinerClass(DrugBankStepTwoReducer.class);
		SecondStepJob.setReducerClass(DrugBankStepTwoReducer.class);
		
		SecondStepJob.setOutputFormatClass(TextOutputFormat.class);
		SecondStepJob.setOutputKeyClass(Text.class);
		SecondStepJob.setOutputValueClass(IntWritable.class);
		return SecondStepJob.waitForCompletion(true)?0:1;
	}
	
	
	public static void main(String[] args) {
		int exitCode = 0;
		try {
			exitCode = ToolRunner.run(new DrugBankStat(),args);
		} catch (Exception e) {
			System.out.println("Exception occured while trying to start ToolRunner.run method.");
		}
		System.out.println("Total node number is: " + nodeSet.size() + ".");
		//edgeNum has been counted in both combiner and reducer of the second job, thus real edge number is edgeNum/2.
		System.out.println("Total egde number is: " + edgeNum/2 +".");
		
		System.exit(exitCode);
		
	}

}
