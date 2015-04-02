SETUP:

On machine 1 : 

- javac *.java
- java Node 1
- view 

Following will appear on screen:
*****************************************************
IP : 129.21.30.38
Port : 42052
Node ID : 1
Known nodes: 
Enter following command in other node's terminal to add this node: 
node 129.21.30.38 42052 1
*****************************************************


Copy last line which indicates command. 


On machine 2: 

- java Node 2
- paste the copied command
- join 

On machine 3: 

- java Node 3
- paste the copied command
- join 

On machine 4: 

- java Node 4
- paste the copied command
- join 

On machine 5: 

- java Node 5
- paste the copied command
- join 


On machine 1: 
- Enter “ie” (initiates election)


- Enter “view” on all machines to verify current leader
- Enter “sf” or simply exit the leader Node i.e. Node 5.



SCENARIO:

- Initiate election from node 1 , by entering command "ie"
- Node 5 should be elected as leader. 
- Confirm this by entering "view" command on all nodes , it will indicate who current leader is. 
- Terminate Node 5, ether by terminating program or by entering "sf" command
- You should see re elections happening and new leader being chosen. 
- Confirm this by entering "view" command.




