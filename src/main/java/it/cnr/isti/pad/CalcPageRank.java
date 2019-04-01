package it.cnr.isti.pad;

import java.io.IOException;
import java.io.OutputStream;
//import org.apache.commons.io.LineIterator;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.SequenceFile;
//import org.apache.hadoop.io.LongWritable;
//import org.apache.hadoop.io.IntWritable;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
///import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

//import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;

public class CalcPageRank
{
	public static class Dangling extends Mapper<Text, PairWritable, Text, PairWritable>
	{
		private int scale_factor;
		static enum Counter{
			SINK
 		}

		public void map(Text key, PairWritable value, Context context) throws IOException, InterruptedException
		{

			if(value.getAdj_list().toString().isEmpty())
			{
				//System.out.println(value.getPagerank().get());
				context.getCounter(Counter.SINK).increment((int)(value.getPagerank().get()*scale_factor));
			}
		}
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			this.scale_factor = context.getConfiguration().getInt("SCALE", 10000);
		}
	}

	public static class Map extends Mapper<Text, PairWritable, Text, PairWritable>
	{
		public void map(Text key, PairWritable value, Context context) throws IOException, InterruptedException
		{
			PairWritable map_value = new PairWritable();
			Text map_key = new Text();
			context.write(key,value);
			//System.out.println(key+"	"+value.getPagerank().get()+"	"+value.getAdj_list().toString()+"	"+value.isNode().get());
			if(!value.getAdj_list().toString().isEmpty())
			{
				//map_value.set(value.getPagerank().get(),value.getAdj_list().toString());
				String[] adj_list = value.getAdj_list().toString().split("[ ]");
				for (String s: adj_list)
				{
					map_key.set(s);
					map_value.set(value.getPagerank().get()/adj_list.length,"");
					map_value.setNode(false);
					context.write(map_key,map_value);
					//System.out.println(map_key.toString()+"	"+map_value.getPagerank().get()+"	"+map_value.getAdj_list().toString()+"	"+map_value.isNode().get());
				}
			}
		}

	}
	public static class Red extends Reducer<Text, PairWritable, Text, PairWritable>
	{
		private int nodes_number;
		private double lambda;
		private int scale_factor;
		private int sink;
		private double dangling;
		public static enum Counter {
			CONV,
			SUM
		}

		public void reduce(Text key, Iterable<PairWritable> values, Context context) throws IOException, InterruptedException
		{
			Text red_key = new Text();
			PairWritable red_value = new PairWritable();
			double sum = 0;
			double pagerank = 0;
			String adj_list="";
			for (PairWritable val:values){
				if(!val.isNode().get()){
					sum+=val.getPagerank().get();
				}else{
					adj_list=val.getAdj_list().toString();
					pagerank=val.getPagerank().get();
				}
			}
			sum = ((1-lambda)/nodes_number)+((lambda)*((dangling/nodes_number)+((1-lambda)/nodes_number)+((lambda)*(sum))));
			context.getCounter(Counter.CONV).increment((int)(Math.abs(sum-pagerank)*scale_factor));
			context.getCounter(Counter.SUM).increment((int)(sum*scale_factor));
			red_value.set(sum,adj_list);
			red_value.setNode(true);
			context.write(key ,red_value);
			//System.out.println(key.toString()+"	"+red_value.getPagerank().get()+"	"+red_value.getAdj_list().toString()+"	"+red_value.isNode().get());
		}
		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			this.nodes_number = context.getConfiguration().getInt("NODES_NUMBER", 0);
			this.lambda = Double.parseDouble(context.getConfiguration().get("LAMBDA", "0.8"));
			this.scale_factor = context.getConfiguration().getInt("SCALE", 10000);
			this.dangling = Double.parseDouble(context.getConfiguration().get("DANGLING", "0"));
		}
	}
	
	public static class Sorter extends Mapper<Text, PairWritable, DoubleWritable, Text>
	{
		public void map(Text key, PairWritable value, Context context) throws IOException, InterruptedException
		{  	
			context.write(value.getPagerank(),key);
		}
	}
}