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

public class Parser
{
	public static class Simple_Parser extends Mapper<Text, Text, Text, PairWritable>
	{																				
		private int nodes_number;

		public void map(Text key, Text value, Context context) throws IOException, InterruptedException
		{
			PairWritable map_value = new PairWritable();
			map_value.set(1.0/nodes_number,value.toString());
			map_value.setNode(true);
			context.write(key,map_value);
		}

		@Override
		protected void setup(Context context) throws IOException, InterruptedException {
			this.nodes_number = context.getConfiguration().getInt("NODES_NUMBER", 0);
		}
	}
}

		