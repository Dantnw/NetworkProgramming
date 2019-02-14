/*
Program name: PooledWeblog1
File: PooledWeblog1.java

Dan Rose
2437236
COMP348, Java Network Programming
Assignment 1 

@author Dan Rose, 2437236, based on Java Network Programming PooledWebLog.java
@version 1.0
@since April 29, 2018


Purpose: To process logfiles in a multithreaded environment using three
different scenarios: Either count accesses by remotehost, count total bytes
transferred, or count bytes by remotehost.

Compilation: With the mingw java environment, navigate to the files and use:
	javac LookupTask.java
	javac PooledWebLog1.java
	
To run: Compile, then navigate to the .class file. Ensure that the log file
to process is in the same folder as the .class file. Use the following
command options:

	java PooledWebLog1 <log name> <0, 1, 2, or 3>

If option 1 is selected, the hosts and number of accesses for each host will be
listed. 
If option 2 is selected, the total number of bytes will be listed. 
If option 3 is selected, each host and the total number of bytes transferred to 
each host will be listed. 

For options 1 and 3:
Expected output: 
Address: xxxxxx
Accesses: xxxxxx
...

For option 2:
Expected output:


Sample output:


**Sample option 1
C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_log 1
Option 1 selected


Address: 64.242.88.10
Accesses: 47


Address: lordgun.org
Accesses: 1


Address: lj1036.inktomisearch.com
Accesses: 1


Address: lj1090.inktomisearch.com
Accesses: 1



**Sample option 2
C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_log 2
Option 2 selected
Total bytes Transmitted: 512540



**Sample option 3
C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_log 3
Option 3 selected


Address: 64.242.88.10
Total bytes: 505743


Address: lordgun.org
Total bytes: 2869


Address: lj1036.inktomisearch.com
Total bytes: 68


Address: lj1090.inktomisearch.com
Total bytes: 3860

Error conditions:

1) The program throws an error if the log file is invalid - either
not properly formatted, missing, or containing the wrong information

Sample: 

C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_larrrg 1
java.io.FileNotFoundException: access_larrrg (The system cannot find the file sp
ecified)

2) The program behaves unpredictably if invalid input occurs after the access_log name

Sample:
C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_log 153
Option = 0
Option 0 selected
...

Sample 2:
C:\Users\JEETR\mingw\JNP\Assignment 1>java PooledWeblog1 access_log abv
Exception in thread "main" java.lang.NumberFormatException: For input string: "a
bv"
        at java.lang.NumberFormatException.forInputString(Unknown Source)
        at java.lang.Integer.parseInt(Unknown Source)
        at java.lang.Integer.valueOf(Unknown Source)
        at PooledWeblog1.main(PooledWebLog1.java:61)

*/

//Import java IO, util, and concurrent classes
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/*
beginning of public class PooledWeblog1
*/
public class PooledWeblog1{
	
	//NUM_THREADS = the number of threads allocated for the executor
	//option = the numerical value input determining which path the program will follow
	private final static int NUM_THREADS = 4;
	public static int option;
	
	/*
	Main method:
	-Reads the logfile and the option value input to the console by the user
	-Chooses a path based on which option number is input
	*/
	public static void main(String[] args) throws IOException{
		
		//create executor
		ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
		//create a linkedlist to hold log entries in case the option number is
		//"0", which is the original path and the program default
		Queue<LogEntry> results = new LinkedList<LogEntry>();
		
		//store the second cmd-line argument passed as an option
		//0 = default behavior
		//1 = count accesses by each remote host
		//2 = count total bytes transmitted
		//3 = count total bytes by remote host
		if(args.length > 1){
			option = Integer.valueOf(args[1]);
		}

		if(option > 3){
			option = 0;
			System.out.println("Option = " + option);
		}
			
		
		//Create a buffered - inputstream - fileinputstream chain with the
		//first cmd-line argument representing the filename
		try(BufferedReader in = new BufferedReader(
			new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));){

			/*
			For each line in the log, a lookup-task tries to resolve
			the IP address to a host name. This is the default behavior
			of the program from Java Network Programming
			*/
			if(option == 0){			
				
				System.out.println("Option 0 selected");
				
				for(String entry = in.readLine(); entry != null; entry = 
					in.readLine()){
					
					LookupTask task = new LookupTask(entry, option);
					Future<String> future = executor.submit(task);
					LogEntry result = new LogEntry(entry, future);
					results.add(result);
				}
				
				/*
				Print the results of the parsing of the access log
				*/
				for(LogEntry result : results){
					try{
						System.out.println(result.future.get());
					} catch (InterruptedException | ExecutionException ex){
					System.out.println(result.original);
					}
				}
			}
			
			/*
			If option 1 is selected, the pooled weblog iterates through
			the access_log file and passes each line to LookupTask along
			with the option selected. LookupTask returns the trimmed ip
			address, which is passed to a new LogCount object. The LogCount
			object is compared to the other logcount objects in the list
			"addresses" and if it is the same as another, the counter
			associated with that address is incremented and the second logcount
			object is discarded. Otherwise, the logcount object is added to
			"addresses"		
			*/
			if(option == 1){
				
				System.out.println("Option 1 selected");
				
				//use a synchronizedList to store LogCount objects since
				//standard linkedlists are not synchronized
				List<LogCount> addresses = Collections.synchronizedList(
					new LinkedList<LogCount>());
				
				//iterate through the logfile
				for(String entry = in.readLine(); entry != null; entry =
				in.readLine()){
					
					//trim the address off the beginning of the log entry
					String address = entry.substring(0, entry.indexOf(" "));
					//pass the option value and the trimmed address to a new
					//lookup task
					LookupTask task = new LookupTask(entry, option);
					//create a future string to hold the hostname associated
					//with the address
					Future<String> future = executor.submit(task);
					
					//Create a new logcount object to hold the address and
					//future hostname
					LogCount lc = new LogCount(address, future);
					
					//need a value to test whether the address is unique
					boolean check = false;
						
					try{
						
						//loop through the list of LogCounts and test whether
						//the associated address is new or not
						for(LogCount log : addresses){
							if(log.address.equals(lc.address)){
								check = true;
								//if it is not unique, flip the boolean and
								//increment the counter on the logCount object
								//in the list
								log.increment();
							}
						}
					//catch errors
					}catch(Exception ex){
						System.err.print(ex);
					}
					//if the address isn't already in the list, add it
					if(check == false){
						addresses.add(lc);
					}
				}
				
				//iterate through the list of unique addresses
				for(LogCount lc : addresses){
					System.out.println("\n");
					try{
						//try printing by remoteHost name
						System.out.println("Address: " + lc.remoteHost.get() + 
						"\n" + "Accesses: " + lc.numberOfAccesses);
					}catch(InterruptedException | ExecutionException ex){
						//if the remoteHost name hasn't resolve, print by
						//address
						System.out.println("Could not resolve hostname");
						System.out.println("Address: " + lc.address + "\n" +
						"Accesses: " + lc.numberOfAccesses);
					}	
				}
			}
			
			/*
			Option two passes each line to a lookupTask and the lookuptask
			returns the number of bytes as a string in that line. This section
			of code casts the string as an int and adds it to the long TotalBytes
			*/
		
			if(option == 2){
				long totalBytes = 0;
				
				try{
					
					System.out.println("Option 2 selected");
					
					//iterate through the logfile
					for(String entry = in.readLine(); entry != null; entry = in.readLine()){
						
						//pass each line to the LookupTask
						LookupTask task = new LookupTask(entry, option);
						
						//LookupTask returns a string representing the last
						//item in the line, which is the byte count
						Future<String> future = executor.submit(task);
						
						//cast the string as an int and add it to the long;
						long bytes = Integer.valueOf(future.get());
						totalBytes += bytes;
					}
				}catch(ExecutionException ex){
					System.err.print(ex);
				}catch(InterruptedException ex){
					System.err.print(ex);
				}
			//print the result
			System.out.println("Total bytes Transmitted: " + totalBytes);
			}
					
			/*
			Option three follows these steps:
			-A synchronized linked list is created
			-the logfile is iterated through
			-the address is trimmed off each line and submitted to the
				lookuptask
			-a new LogCount object is created which stores the address, the
				future hostname, and the number of bytes for the line in the
				logfile
			-Similar to option 1 - the address is compared to addresses already
				stored in the linkedlist. If the addresses match, the bytes 
				are added. If the addresses do not match, the logcount object
				is added to the list.
				*/
			if(option == 3){
				
				System.out.println("Option 3 selected");
				
				//create a synchronized linked list
				List<LogCount> addresses = Collections.synchronizedList(
					new LinkedList<LogCount>());
				
				//iterate through the entries in the logfile
				for(String entry = in.readLine(); entry != null; 
				entry = in.readLine()){
					
					//trim and store the address and the number of bytes from 
					//each line
					int index = entry.lastIndexOf(" ") + 1;
					int bytes = Integer.valueOf(entry.substring(index, 
					entry.length()));
					String address = entry.substring(0, entry.indexOf(" "));
					
					//hand the entry and the option value to a new LookupTask
					LookupTask task = new LookupTask(entry, option);
					
					Future<String> future = executor.submit(task);
					
					boolean check = false;
					
					//create a new logcount object that stores the address,
					//the future hostname, and the number of bytes transferred
					//for each line in the logfile
					LogCount lc = new LogCount(address, future, bytes);
					
					try{
						//iterate through the logcounts
						for(LogCount log : addresses){
							//if addresses match, add the bytes to the logcount
							//object in the linkedlist
							if(log.address.equals(lc.address)){
								log.addBytes(bytes);
								log.increment();
								check = true;
							}
						}
						
					}catch(Exception ex){
						System.err.print(ex);
					}
					
					//if the address isn't already in the list, add the
					//logcount object to the linkedlist
					if(check == false){
						addresses.add(lc);
					}
				}
				
				//for each logcount object in the addresses list
				for(LogCount log : addresses){
					try{
						//try to print by hostname
						System.out.println("\n");
						System.out.println("Address: " + 
						log.remoteHost.get() + "\n" + "Total bytes: " 
						+ log.totalBytes);
						
					}catch(InterruptedException | ExecutionException ex){
						//if the hostnames don't resolve, print by address from
						//the actual logfile
						System.out.println("\n");
						System.out.println("Address: " + log.address + "\n"
						+ "Total bytes: " + log.totalBytes);
					}
					
				}
			}
			
		}catch(IOException ex){
			System.err.print(ex);
		}
		
		//shut down the executor
		executor.shutdown();
	}
	
	/*
	Static class LogCount
	@Params
	address: the original address from the logfile
	remoteHost: the future<String> intended to hold the resolved host name
	numberOfAccesses: the number of accesses each unique host has
	totalBytes: the number of total bytes each host has received
	
	@methods
	LogCount() default constructer (no args)
	LogCount(String address, Future<String> remoteHost) - constructor
		stores address and future<string> representing hostname
	LogCount(String address, Future<String> remoteHost, long totalBytes)
		-constructor stores address, future<string> representing remotehost,
		and long total bytes transferred
	addBytes(int bytes) - add value of "bytes" to member variable totalBytes
	increment() - add one to member variable numberOfAccesses
	
	*/
	private static class LogCount{
		String address;
		Future<String> remoteHost;
		private int numberOfAccesses = 1;
		private long totalBytes;
		
		public LogCount(){
		}
		
		public LogCount(String address, Future<String> remoteHost){
			this.address = address;
			this.remoteHost = remoteHost;
		}
		
		public LogCount(String address, Future<String> remoteHost, long totalBytes){
			this.address = address;
			this.remoteHost = remoteHost;
			this.totalBytes = totalBytes;
		}
		
		public void addBytes(int bytes){
			this.totalBytes += bytes;
		}
		
		public void increment(){
			this.numberOfAccesses++;
		}
	}
	
	/*
	Nested static class LogEntry
	From original example PooledWebLog from Java Network Programming
	
	@Member variables:
		String original - the original address
		Future<String> future - the resolved hostname
		
	@Constructor:
		LogEntry(String original, Future<String> future)
		stores original as member variable original, future as member
		variable future
	*/
	private static class LogEntry{
		String original;
		Future<String> future;
		
		LogEntry(String original, Future<String> future){
			this.original = original;
			this.future = future;
		}
	}
}