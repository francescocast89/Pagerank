#!/bin/bash
declare -A arr
declare -A result_arr
i=0
while IFS=$'\t' read -r -a line ; do
	arr[${line[1]}]=${line[0]}
done < "test/g_standard"

stop-all.sh 
rm -rf /tmp/hadoop-hadoop
mvn clean package
hadoop namenode -format
start-all.sh 
hadoop fs -put test/simple-graph.txt simple-graph.txt
hadoop jar target/Pagerank-1.0-SNAPSHOT.jar it.cnr.isti.pad.Pagerank simple-graph.txt pagerank_output

while IFS=$'\t' read -r -a line ; do
    result_arr[${line[1]}]=${line[0]}
done <<< "$(hadoop fs -cat pagerank_output/pagerank_results/part-*)"
printf '%s\n' "---------------------------------------------------------------" 
printf '%s\t%s\t%s\t%s\t%s\n' " " "G standard" "PR Results" "changed"
for k in "${!arr[@]}"
do
    if [ ${arr["$k"]} != ${result_arr["$k"]} ]; then
    	printf '%s\t%s\t%s\t%s\n' "$k" "${arr["$k"]::9}" "${result_arr["$k"]::9}" "Y"
    else
    	printf '%s\t%s\t%s\t%s\n' "$k" "${arr["$k"]::9}" "${result_arr["$k"]::9}" "N"
    fi
done | sort -n -k1
printf '%s\n' "---------------------------------------------------------------" 

