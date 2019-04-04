package it.cnr.isti.pad;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.LineIterator;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
///import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.SequenceFile;
//import org.apache.hadoop.io.LongWritable;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
///import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
//import org.apache.hadoop.mapreduce.Counter;
//import org.apache.hadoop.mapreduce.Counters;
//import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;

public class Parser
{
	public static class PageParser extends Mapper<Text, Text, IntWritable, Text>
	{
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			IntWritable map_key = new IntWritable();
			Text map_value = new Text();
			String[] entry=key.toString().split("\\,");
			if(entry[1].equals("0")){
				map_key.set(Integer.parseInt(entry[0]));
				map_value.set(entry[2]);
				context.write(map_key,map_value);
			}  
		}
	}

	public static class PagelinkParser_Map_2 extends Mapper<Text, Text, Text, PairWritable>
	{
		
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			Text map_key = new Text();
			PairWritable map_value = new PairWritable();
			String[] entry=key.toString().split("\\,");
				if(entry[1].equals("0")){
					map_key.set(entry[2]);
					map_value.set(0.0,entry[0]);
					context.write(map_key,map_value);
					//System.out.println(map_key+","+map_value.getPagerank()+"	"+map_value.getAdj_list().toString());
				}
		}
	}
	public static class PagelinkParser_Map_1 extends Mapper<Text, Text, Text, PairWritable>
	{
		
		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			PairWritable map_value = new PairWritable();
			map_value.set(Double.NaN,key.toString());
			context.write(value,map_value);
			//System.out.println(value+","+map_value.getPagerank()+"	"+map_value.getAdj_list().toString());
			
		}
	}

	public static class PagelinkParser_Red extends Reducer<Text, PairWritable, Text, PairWritable>
	{
		private int nodes_number;

		public void reduce(Text key, Iterable<PairWritable> values, Context context) throws IOException, InterruptedException
		{
			Text red_key = new Text();
			PairWritable red_value = new PairWritable();
			String[] entry;
			boolean isActive=false;
			double pagerank=0;
			StringBuilder sbuilder = new StringBuilder(" ");
			for (PairWritable val:values){
				if(Double.isNaN(val.getPagerank().get())){
					red_key.set(val.getAdj_list().toString());
					isActive=true;
				}else{
					sbuilder.append(val.getAdj_list().toString()).append(" ");
				}
			}
			if(isActive){
				red_value.set(1.0/nodes_number,sbuilder.toString().trim());
				red_value.setNode(true);
				context.write(red_key,red_value);
				//System.out.println(red_key.toString()+"	"+red_value.getPagerank().get()+"	"+red_value.getAdj_list().toString()+"	"+red_value.getAdj_list().toString().isEmpty());
			}
		}
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			this.nodes_number = context.getConfiguration().getInt("NODES_NUMBER", 0);
		}
	}

	public static class Simple_Parser extends Mapper<Text, Text, Text, PairWritable> //this is a simple parser 
	{																				//it parse a simple .txt file with this format node_id	adj_node1 adj_node2...
		private int nodes_number;

		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			PairWritable map_value = new PairWritable();
			map_value.set(1.0/nodes_number,value.toString());
			map_value.setNode(true);
			context.write(key,map_value);
			//System.out.println(key.toString()+"	"+map_value.getPagerank().get()+"	"+map_value.getAdj_list().toString());
		}

		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			this.nodes_number = context.getConfiguration().getInt("NODES_NUMBER", 0);
		}
	}
}

		
