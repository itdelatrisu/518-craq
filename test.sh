mvn clean package
len=$(($#-1))
export CHAIN_LEN=$len
for i in `seq 0 $(($#-1))`
do
  echo $i
  java -jar target/craq.jar 0 $i $* &
  export PID$i=$!
  echo ${PID}$i
done


