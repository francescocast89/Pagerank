package it.cnr.isti.pad;
import org.apache.hadoop.io.*;
import org.apache.hadoop.io.DoubleWritable;

class DescendingKeyComparator extends WritableComparator {
    protected DescendingKeyComparator() {
        super(DoubleWritable.class, true);
    }

    @Override
    public int compare(WritableComparable w1, WritableComparable w2) {
        DoubleWritable key1 = (DoubleWritable) w1;
        DoubleWritable key2 = (DoubleWritable) w2;          
        return -1 * key1.compareTo(key2);
    }
}