package it.cnr.isti.pad;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.BooleanWritable;

public class PairWritable implements Writable{
 private DoubleWritable pagerank;
 private Text adj_list;
 private BooleanWritable isNode;


 PairWritable() { 
  this.pagerank = new DoubleWritable();
  this.adj_list = new Text();
  this.isNode = new BooleanWritable(false);
 }

 public void set(Double pagerank, String values){
  this.pagerank.set(pagerank);
  this.adj_list.set(values);
}

 public void setPagerank(Double pagerank){
  this.pagerank.set(pagerank);
}
 public void setAdj_lsit(String values){
  this.adj_list.set(values);
}

public DoubleWritable getPagerank(){
  return pagerank;
}
public Text getAdj_list(){
  return adj_list;
}

public void setNode(Boolean isnode){
  this.isNode.set(isnode);
}

public BooleanWritable isNode(){
  return isNode;
}

public void readFields(DataInput in) throws IOException {
  pagerank.readFields(in);
  adj_list.readFields(in);
  isNode.readFields(in);
}

@Override
public void write(DataOutput out) throws IOException {
  pagerank.write(out);
  adj_list.write(out);
  isNode.write(out);
}
}

