SETUP:

SETUP:

On machine 1 : 

- javac *.java
- java Node -c 1
- view 

Following will appear on screen:
*****************************************************
IP : 129.21.30.38
Port : 42052
Node ID : 1
Known nodes: 
Enter following command in other node's terminal to add this node: 
csnode 129.21.30.38 42052 1
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


On machine 5: 
- Enter “ac” (access critical section)


On machine 2: 
- Enter “ac”

On machine 4: 
- Enter “ac”


SCENARIO: 

- Enter "ac" command in the Node 5 terminal, for it to access the critical section. 
- Do the same for 4 and 2. 
- To print details about the node and its
