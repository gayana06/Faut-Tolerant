start java -jar DemoApplication.jar conf/process.conf
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 0 -qos consensus -del 
sleep 1
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 1 -qos consensus -del 
sleep 1
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 2 -qos consensus -del 
sleep 1
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 3 -qos consensus -del 
sleep 1
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 4 -qos consensus -del 
sleep 1
start java -jar RandomizedConsensus.jar -f conf/process.conf -n 5 -qos consensus -del 