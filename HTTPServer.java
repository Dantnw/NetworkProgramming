/*
Program name: HTTPServer
File: HTTPServer.java

Statement of originality (copied from assignment)
I declare that this assignment is my own work and that all material previously 
written or published in any source by any other person has been duly 
acknowledged in the assignment. I have not submitted this work, or a 
significant part thereof, previously as part of any academic program. 
In submitting this assignment I give permission to copy it for assessment 
purposes only.

Dan Rose
2437236
COMP348, Java Network Programming
Assignment 2 

@author Dan Rose, 2437236, based on Java Network Programming PooledWebLog.java
@version 1.0

This program is based on Elliote Rusty Harold, "NonblockingSingleFileHTTPSErver" 
from Java Network Programming, page 373. The modifications I have made involved
two major changes to meet the assigment requests:

1) The program now listens for an HTTP request and serves the requisite file.
2) The program now logs requests and their results in "access_log"

These changes were accomplished generally by moving the portions of the single-
file server that generated a byte array for the channel into the section
of the program that reads the request header from the client. This made it
possible to have the program respond directly to the request headers, so
the server could generate a header based on the requested file and serve it
to the client through the ByteBuffer channel. The logging does not use the
java logger class; I felt it would be simpler to implement by constructing
a String with the values I was already generating for and reading from the
header files. 

Purpose: To serve HTML and multimedia files on a selected port

Compilation: With the mingw java environment, navigate to the files and use:
	javac HTTPServer.java
	
To run: Compile, then navigate to the .class file. Ensure that the log file
to process is in the same folder as the .class file. Use the following
command options:

	java HTTPServer <port selection - if no port is selected the default port
						80 will be used>


Sample output:


C:\Users\JEETR\mingw\JNP\Assignment 2>java HTTPServer 45555
Listening for connections on port 45555

C:\Users\JEETR\mingw\JNP\Assignment 2>java HTTPServer
Listening for connections on port 80

The program by default loads a file called "new.html" first. If you wish to
change the server's main file, make alterations to "new.html".

Known issues:
Sometimes the server crashes randomly. The console reports a buffer underrun
exception when this happens, but the server appears to be idling so I'm not
sure why this happens. It is characteristically several seconds after the 
media has been served to an open connection.

Test plan:

Testing the program followed sequences of four steps. 

	Step 1: compile. If successful, then:
	
	Step 2: Run server. Attempt to connect to server using Chrome and typing in
	my own IP address and port number in the URL bar. If successful, then:
	
	Step 3: View site to verify all HTML and embedded content is correctly
	displayed. This step took some time.
	
	Step 4: Check log - ensure that the log lines are identically formatted
	to the log lines in the access_log example from assignment 1. 
	
	The main issues that emerged and were resolved through testing:
	
	1) The server was not passing the html document to Chrome.
		-I set up the server to pass multimedia files but left the original 
		constructor intact. This was a major design issue and solving the 
		problem required treating the html document as one of the possible 
		documents that could be requested. 
	2)	The server crashed when trying to pass multimedia files to the client
		-the multimedia files were being sent with improper headers and the
		buffer was not flipped correcting these issues resolved the problem
	
	This was a fun assignment! I really feel like it stretched my understanding
	of the topic. 
*/
//long list of imports for this one
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import java.util.logging.*;

//beginning of class HTTPServer
public class HTTPServer{
	
	//three member variables - this was the most
	//convenient place to put them so they could be
	//accessed by the run method
	private ByteBuffer contentBuffer;
	private int port = 80;
	BufferedWriter writer;
	
	/*
	Constructor for HTTPServer objects
	int port: defaults to 80, the port for the server to reside on
	writer - a bufferedWriter for the access_log
	*/
	public HTTPServer(String encoding, int port) throws IOException{			
			this.port = port;
			this.writer = new BufferedWriter(new FileWriter("access_log"));
		}
		
		/*
		Run method for the server - this server is not multithreaded but uses
		non-blocking IO to allow good balance in attention. For a server of
		this magnitute it's not a big deal either way.
		*/
		public void run() throws IOException{
			//create a serversocketchannel and a serversocket
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			ServerSocket serverSocket = serverChannel.socket();
			//create a selector
			Selector selector = Selector.open();
			//create an inetsocketaddress for the server and bind it
			InetSocketAddress  localPort = new InetSocketAddress(port);
			serverSocket.bind(localPort);
			//set blocking to false
			serverChannel.configureBlocking(false);
			//set the selector to Accept mode
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			//Now the server is running
			while(true){
				//while there are keys in the list of keys, check their
				//status
				selector.select();
				Iterator<SelectionKey> keys 
					= selector.selectedKeys().iterator();
				while(keys.hasNext()){
					SelectionKey key = keys.next();
					keys.remove();
					
					//if the key is Acceptable, connect the sockets
					try{
						if(key.isAcceptable()){
							ServerSocketChannel server 
								= (ServerSocketChannel) key.channel();
							SocketChannel channel = server.accept();
							channel.configureBlocking(false);
							channel.register(selector, SelectionKey.OP_READ);
						}
					
					//if the key reads "writeable", write the buffer
					//"contentBuffer" to the channel
						else if(key.isWritable()){
							SocketChannel channel = (SocketChannel) key.channel();
							ByteBuffer buffer = (ByteBuffer) key.attachment();
							if(buffer.hasRemaining()){
								channel.write(buffer);
							}else{
								channel.close();
							}
					
					//if the key reads "readable", the magic begins!
						}else if(key.isReadable()){
							SocketChannel channel 
								= (SocketChannel) key.channel();
							
							//create a bytebuffer and read the channel to it
							ByteBuffer buffer = ByteBuffer.allocate(4096);
							channel.read(buffer);
							
							//stringbuilder "request" reads the request from
							//the buffer and saves it as a string
							StringBuilder request = new StringBuilder();
							buffer.flip();
							while(true){
								int c = buffer.get();
								if(c == '\n' || c == '\r' || c == -1) break;
								request.append((char) c);
							
							}
							
							//pull the requested file from the HTTP request
							String fileName = request.substring(5, 
								request.indexOf(" ", 5));
							
							//in one condition, the request does not have a 
							//specific filename - in this case, submit new.html
							//In this if-loop, a header is generated (this code
							//owes dearly to Harold's code on which this program
							//is based).
							if(fileName.equals("")){
								//get type for new.html
								String MIMEType 
								= URLConnection.getFileNameMap().getContentTypeFor("new.html");
								
								//get path for new.html
								Path file 
								= FileSystems.getDefault().getPath("new.html");
								
								//read the file new.html
								byte[] data = Files.readAllBytes(file);
								
								//generate the header
								String header = "HTTP/1.0 200 OK\r\n"
									+ "Server: HTTPServer\r\n"
									+ "Content-length: " + data.length + "\r\n"
									+ "Content-type: " + MIMEType + "\r\n\r\n";
								
								//store the header in a byte array
								byte[] headerData
								= header.getBytes(Charset.forName("US-ASCII"));
								
								//create a bytebuffer called toSend
								ByteBuffer toSend 
								= ByteBuffer.allocate(headerData.length
								+ data.length);
								
								//pack all the data in the buffer
								toSend.put(headerData);
								toSend.put(data);
								
								//flip the buffer
								toSend.flip();
								
								//assign contentBuffer to it
								contentBuffer = toSend;
								
								//This section generates the access_log
								try{
									//get date/time
									Date now = new Date();
									
									//get the address for the client connection
									InetSocketAddress client 
									= (InetSocketAddress)channel.getRemoteAddress();
									
									//get the actual address for the client
									InetAddress clientAddress = client.getAddress();
									String protocol 
									= request.substring(0, request.indexOf(" "));
									
									//generate the log line
									String logLine 
									= (clientAddress + " - - [" + now + "] \""
									+ protocol + " " + file.toString() 
									+ " " + "200" + " " + data.length + "\n");
									
									//trim the "/" off the beginning
									logLine 
									= logLine.substring(1, logLine.length());
									
									//write it to access_log
									writer.write(logLine);
									
									//flush the pipe!
									writer.flush();
								
								}catch(IOException ex){//if the log fails, we will never know!
								}
							}else{
								//So if the filename has an actual value:
								//get its type
								String MIMEType = 
								URLConnection.getFileNameMap().getContentTypeFor(fileName);
								
								//find its path
								Path file 
								= FileSystems.getDefault().getPath(fileName);
								
								//read its data
								byte[] data = Files.readAllBytes(file);
								
								//generate the HTTP header
								String header = "HTTP/1.0 200 OK\r\n"
									+ "Server: HTTPServer\r\n"
									+ "Content-length: " + data.length + "\r\n"
									+ "Content-typte: " + MIMEType + "\r\n\r\n";
								
								//get the header data
								byte[] headerData
								= header.getBytes(Charset.forName("US-ASCII"));
								
								//create a bytebuffer to pack it all in
								//size determined by the size of the two byte
								//arrays we're packing
								ByteBuffer toSend
								= ByteBuffer.allocate(headerData.length
								+ data.length);
								
								//pack it
								toSend.put(headerData);
								toSend.put(data);
								
								//flip it
								toSend.flip();
								
								//assign it to contentBuffer
								contentBuffer = toSend;
								
								//Generating the log line for this file
								try{
									//make date/time
									Date now = new Date();
									//get the socketAddress to get the IP
									//client address
									InetSocketAddress client 
									= (InetSocketAddress)channel.getRemoteAddress();
									
									InetAddress clientAddress = client.getAddress();
									
									//get the protocol (hint: it's going to 
									//be GET)
									String protocol = request.substring(0, request.indexOf(" "));
									
									//generate the log line
									String logLine = (clientAddress + " - - [" + now + "] \""
									+ protocol + " " + file.toString() + " " + "200" + " " 
									+ data.length + "\n");
									
									//trim the slash off the front
									logLine = logLine.substring(1, logLine.length());
									
									//write it to the log
									writer.write(logLine);
									
									//flush the pipe
									writer.flush();
								
								}catch(IOException ex){//if the log fails, we will never know!
								}
							}			
							//switch to write-only mode
							//these two lines are lifted directly from Harold
							//and they set the key to Write and attach a dupe
							//of the content buffer to it
							key.interestOps(SelectionKey.OP_WRITE);
							key.attach(contentBuffer.duplicate());
						}
					//and if that doesn't work, scrap the key
					}catch(IOException ex){
						key.cancel();
						try{
						//and close the channel
							key.channel().close();
						}catch(IOException cex){}
					}
				}
			}
		}
		
		
	//main method - sets up the server, generates it, and calls its run method	
	public static void main(String[] args){
		
		try{
			//set listening port
			int port;
			try{
				port = Integer.parseInt(args[0]);
				if (port < 1 || port > 65535) port = 80;
			}catch(RuntimeException ex){
				port = 80;
			}
			//This is kind of unneccessary, but it's here and eveyrthing
			//works so i'm not moving it
			String encoding = "UTF-8";
			
			//Just so you know it's not hanging
			System.out.println("Listening for connections on port " + port);
			
			//create new HTTPServer object
			HTTPServer server = new HTTPServer(encoding, port);
			
			//call its Run method
			server.run();
			
		}catch(IOException ex){
			System.err.println(ex);
		}
	}
}
				
				
				
				
				
				
				
				
					
						