package it.cnr.isti.pad;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.LineIterator;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import it.cnr.isti.pad.Parser.*;
import it.cnr.isti.pad.CalcPageRank.*;

public class Pagerank
{
static double req_convergence = 0.01;
static double convergence;
static double lambda = 0.85;
static int nodes_number;
static Path pagelink_inputPath;
static Path outputPath;
static int max_iter=10;

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
		if(args.length==2){
			parse_text_job(conf,args[0],args[1]);

		}else{
			System.out.println("Error");
			System.exit(0);
		}
		long scale_factor = (long) (100*(Math.pow(10,Math.floor(Math.log10(nodes_number) + 1))));
		int iter=1;
		while (true) {
			convergence=calc_pagerank_job(conf, iter, scale_factor);
			iter++;
			if((convergence<req_convergence)||(iter>max_iter)){
				sort_job(conf);
				break;
			}
	    }
	}

    public static void parse_text_job(Configuration conf, String pagelink, String out) throws IOException, ClassNotFoundException, InterruptedException {
        
        	pagelink_inputPath = new Path(pagelink);
			nodes_number = getNumNodes(pagelink_inputPath);
			outputPath = new Path(out);

			outputPath.getFileSystem(conf).delete(outputPath, true);
			
			Job job_parser = Job.getInstance(conf, "Parse Text");
			job_parser.getConfiguration().setInt("NODES_NUMBER", nodes_number);
			job_parser.setJarByClass(Parser.class);
			job_parser.setOutputKeyClass(Text.class);
			job_parser.setOutputValueClass(PairWritable.class);
			job_parser.setMapperClass(Simple_Parser.class);
			job_parser.setNumReduceTasks(0);
			job_parser.setInputFormatClass(KeyValueTextInputFormat.class);
			job_parser.setOutputFormatClass(SequenceFileOutputFormat.class);
			FileInputFormat.setInputPaths(job_parser, pagelink_inputPath);
			Path parsedOutputPath = new Path(outputPath,"parsed_"+out.split("\\.")[0]);
			FileOutputFormat.setOutputPath(job_parser, parsedOutputPath);
			pagelink_inputPath=parsedOutputPath;
			job_parser.waitForCompletion(true); 
    }

    public static double calc_pagerank_job(Configuration conf, int iter, long scale_factor ) throws IOException, ClassNotFoundException, InterruptedException {
        	
        	Job job =new Job(conf, "PageRank Dangling Calc "+iter);
			job.getConfiguration().setLong("SCALE", scale_factor);
			job.setJarByClass(CalcPageRank.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(PairWritable.class);
			job.setMapperClass(Dangling.class);
			job.setNumReduceTasks(0);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(NullOutputFormat.class);
			FileInputFormat.setInputPaths(job, pagelink_inputPath);
			job.waitForCompletion(true);

			double dangling = (double)(job.getCounters().findCounter(Dangling.Counter.SINK).getValue())/(scale_factor);
			
			job = Job.getInstance(conf, "PageRank Calc "+iter);
			job.getConfiguration().setInt("NODES_NUMBER", nodes_number);
			job.getConfiguration().set("DANGLING", Double.toString(dangling));
			job.getConfiguration().set("LAMBDA", Double.toString(lambda));
			job.getConfiguration().setLong("SCALE", scale_factor); 
			job.setJarByClass(CalcPageRank.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(PairWritable.class);
			job.setMapperClass(Map.class);
			job.setReducerClass(Red.class);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			FileInputFormat.setInputPaths(job, pagelink_inputPath);
			Path jobOutputPath = new Path(outputPath, String.valueOf(iter));
			FileOutputFormat.setOutputPath(job, jobOutputPath);
			job.waitForCompletion(true);
			pagelink_inputPath.getFileSystem(conf).delete(pagelink_inputPath, true);
			convergence = ((double)(job.getCounters().findCounter(Red.Counter.CONV).getValue()))/((nodes_number*scale_factor));
			double sum = (double)(job.getCounters().findCounter(Red.Counter.SUM).getValue())/(scale_factor);	

			pagelink_inputPath = jobOutputPath;

			System.out.println("-----------------------------------------");
			System.out.println("iteration:	"+iter);
	    	System.out.println("nodes number:	"+nodes_number);
	    	System.out.println("scale factor:	"+scale_factor);
	    	System.out.println("sum:	"+sum);
	    	System.out.println("convergence:	"+convergence+" / "+req_convergence);
	    	System.out.println("dangling:		"+dangling);
	    	System.out.println("------------------------------------------");

        return convergence;
    }
    public static void sort_job(Configuration conf) throws IOException, ClassNotFoundException, InterruptedException {
    	Job job_sorter = new Job(conf, "Sorter_job");
		job_sorter.setJarByClass(CalcPageRank.class);
		job_sorter.setOutputKeyClass(DoubleWritable.class);
		job_sorter.setOutputValueClass(Text.class);
		job_sorter.setMapperClass(Sorter.class);
		job_sorter.setInputFormatClass(SequenceFileInputFormat.class);
		job_sorter.setSortComparatorClass(DescendingKeyComparator.class);
		FileInputFormat.setInputPaths(job_sorter, pagelink_inputPath);
		FileOutputFormat.setOutputPath(job_sorter, new Path(outputPath,"pagerank_results"));
        job_sorter.waitForCompletion(true);
        pagelink_inputPath.getFileSystem(conf).delete(pagelink_inputPath, true);
    }

}

