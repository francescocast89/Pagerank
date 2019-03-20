package it.cnr.isti.pad;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.LineIterator;
import java.util.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;

public class Pagerank
{
	//------------------------------------------Parser-------------------------------------------------
	public static class Parser extends Mapper<Text, Text, Text, Text>
	{
		private Text map_key = new Text();
		private Text map_value = new Text();
		private char delimiter = '\t';
		public static String NODES_NUMBER = "pagerank.nodes_number";
		private int nodes_number;

		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{  
			map_key.set(key.toString().split("\\:")[0]);
			map_value.set(Double.toString(1.0/nodes_number)+delimiter+key.toString().split("\\:")[1]);
			context.write(map_key,map_value);
		}
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			nodes_number = context.getConfiguration().getInt(NODES_NUMBER, 0);
		}
	}
	//-------------------------------------------Sorter------------------------------------------------
	public static class Sorter extends Mapper<Text, Text, DoubleWritable, Text>
	{
		private DoubleWritable map_key = new DoubleWritable();
		private Text map_value = new Text();

		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{  
			map_key.set(Double.parseDouble(value.toString().split("\\t")[0]));
			context.write(map_key,key);
		}
	}
	//------------------------------------------Calc PR-----------------------------------------------
	public static class Map extends Mapper<Text, Text, Text, Text>
	{

		private char delimiter = '\t';
		private Text map_key = new Text();
		private Text map_value = new Text();
		private double pagerank=0;
		private String[] adj_nodes;
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			map_key.set(key.toString());
			map_value.set(value.toString().split("\\t")[0]+delimiter+value.toString().split("\\t")[1]);
			context.write(map_key,map_value);
			pagerank= Double.parseDouble(value.toString().split("\\t")[0]);
			adj_nodes=(value.toString().split("\\t")[1]).split("[ ]");
			for (int i=1; i<adj_nodes.length;i++){
				map_key.set(adj_nodes[i]);
				map_value.set(Double.toString(pagerank/(adj_nodes.length-1)));
				context.write(map_key,map_value);
			}
		}

	}
	public static class Red extends Reducer<Text, Text, Text, Text>
	{
		public static String NODES_NUMBER = "pagerank.nodes_number";
		private int nodes_number;
		private double lambda = 0.8;
		private Text red_key = new Text();
		private Text red_value = new Text();
		private char delimiter = '\t';

		public static enum Counter {
			CONV
		}

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
		{
			Node node = new Node(key.toString());
			double sum=0;
			double finalpagerank=0;
			for (Text val:values){
				String entry[] = val.toString().split("\\t");
				if (entry.length > 1){
					node.setAdj_list(entry[1]);
					node.setPagerank(Double.parseDouble(entry[0]));
				}else{
					sum+=Double.parseDouble(entry[0]);
				}
			}
			finalpagerank=((1-lambda)/nodes_number)+(lambda*(sum));
			context.getCounter(Counter.CONV).increment((int)(Math.abs(finalpagerank-node.getPagerank())*(nodes_number*1000)));
			node.setPagerank(finalpagerank);
			red_value.set(Double.toString(finalpagerank)+delimiter+node.getAdj_list());
			red_key.set(key.toString());
			context.write(red_key ,red_value);
		}
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			nodes_number = context.getConfiguration().getInt(NODES_NUMBER, 0);
		}
	}
//----------------------------------------------------------------------------------------------------------------------
	public static int getNumNodes(Path input_file) throws IOException {
		Configuration conf = new Configuration();
		FileSystem fs = input_file.getFileSystem(conf);
		LineIterator lineIterator = IOUtils.lineIterator(fs.open(input_file), "UTF8");
		int lines=0;
		while ( lineIterator.hasNext() ) {
			lines++;
			lineIterator.nextLine();
		}
		return lines;
	}

	public static void main(String[] args) throws Exception
	{	
		Configuration conf = new Configuration();
		Path inputPath = new Path(args[0]);
		Path outputPath = new Path(args[1]);
		outputPath.getFileSystem(conf).delete(outputPath, true);
		int nodes_number = getNumNodes(inputPath);
		double convergence = 0;
		double req_convergence = 0.01;
		if(args.length == 3 && args[2].equals("-parse")){
			Path parserOutputPath = new Path(outputPath,"parsed_"+inputPath);
			conf.setInt(Parser.NODES_NUMBER,nodes_number);
			Job job_parser = new Job(conf, "Parser_job");
			job_parser.setJarByClass(Pagerank.class);
			job_parser.setOutputKeyClass(Text.class);
			job_parser.setOutputValueClass(Text.class);
			job_parser.setMapperClass(Parser.class);
			job_parser.setNumReduceTasks(0);
			job_parser.setInputFormatClass(KeyValueTextInputFormat.class);
			FileInputFormat.setInputPaths(job_parser, inputPath);
			FileOutputFormat.setOutputPath(job_parser, parserOutputPath);
			job_parser.waitForCompletion(true);
			inputPath=parserOutputPath;
		}
		int iter=0;
		while (true) {
			Path jobOutputPath = new Path(outputPath, String.valueOf(iter));
			Job job = Job.getInstance(conf, "PageRankJob_"+iter);
			conf.setInt(Red.NODES_NUMBER,nodes_number);
			job.setJarByClass(Pagerank.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			job.setMapperClass(Map.class);
			job.setReducerClass(Red.class);
			job.setInputFormatClass(KeyValueTextInputFormat.class);
			FileInputFormat.setInputPaths(job, inputPath);
			FileOutputFormat.setOutputPath(job, jobOutputPath);
			job.waitForCompletion(true);
			inputPath.getFileSystem(conf).delete(inputPath, true);
			convergence = ((double)(job.getCounters().findCounter(Red.Counter.CONV).getValue()))/((nodes_number*1000)*nodes_number);
			inputPath = jobOutputPath;
	      	iter++;
			if(convergence<req_convergence){
				break;
			}
	    }

		Job job_sorter = new Job(conf, "Sorter_job");
		job_sorter.setJarByClass(Pagerank.class);
		job_sorter.setOutputKeyClass(DoubleWritable.class);
		job_sorter.setOutputValueClass(Text.class);
		job_sorter.setMapperClass(Sorter.class);
		job_sorter.setInputFormatClass(KeyValueTextInputFormat.class);
		job_sorter.setSortComparatorClass(DescendingKeyComparator.class);
		FileInputFormat.setInputPaths(job_sorter, inputPath);
		FileOutputFormat.setOutputPath(job_sorter, new Path(outputPath,"sorted_"+inputPath));
		job_sorter.waitForCompletion(true);
	}
}

