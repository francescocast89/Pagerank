package it.cnr.isti.pad;

import java.io.IOException;
import java.util.*;
import org.apache.hadoop.io.Text;

public class Node
{
	private String node;
	private double pagerank;	
	private String adj_list;

	public Node(){
	}

	public Node(String node){
		this.node=node;
	}

	public Node(String node, double pagerank, String adj_list){
		this.node = node;
		this.pagerank = pagerank;
		this.adj_list=adj_list;
	}

	public String getNode(){
		return node;
	}
	public double getPagerank(){
		return pagerank;
	}
	public String getAdj_list(){
		return adj_list;
	}
	public void setAdj_list(String adj_list){
		this.adj_list=adj_list;
	}
	public void setPagerank(double pagerank){
		this.pagerank=pagerank;
	}
}