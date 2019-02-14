/*

Program name: GetEmail
File: GetEmail.java

Statement of originality:

Statement of originality (copied from assignment)
I declare that this assignment is my own work and that all material previously 
written or published in any source by any other person has been duly 
acknowledged in the assignment. I have not submitted this work, or a 
significant part thereof, previously as part of any academic program. 
In submitting this assignment I give permission to copy it for assessment 
purposes only.

...that said, it would be wrong to say that this program was purely a product
of my own imagination. There have been countless internet sources and tutorials
that made this possible. Please see the associated word documents for my 
tutorial journey and the test plan for this program.


Dan Rose
2437236
COMP348, Java Network Programming
Assignment 4

@author Dan Rose, 2437236, based on FetchEmail from the Java TutorialsPoint
JavaMail API tutorial at 
https://www.tutorialspoint.com/javamail_api/javamail_api_sending_simple_email.htm
and on 
https://stackoverflow.com/questions/11240368/how-to-read-text-inside-body-of-mail-using-javax-mail
						

The purpose of this program is twofold:
1) To view the number, subject line, and sender of various emails in a gmail account
2) To select an email to have its body displayed in the console

to compile: 
javac -cp javax.mail.jar email/GetMail.java

to run:
java -cp javax.mail.jar;. email/GetMail <server> <username> <password> 
	<OPTIONAL: int email selection>

For test plan, see Assignment4StudyJournal.docx
	

*/


package email;

//imports
import java.io.*;
import java.util.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;


//start of class GetMail
public class GetMail{
	
	//main method
	public static void main(String[] args){

		try{
			
			//get the arguments representing the choices
			final String server = args[0];
			final String user = args[1];
			final String password = args[2];
			

			
			try{
				
				//we need a properties object for either path the program
				//takes
				Properties properties = new Properties();
				properties.put("mail.pop3.host", server);
				properties.put("mail.pop3.port", "995");
				properties.put("mail.pop3.starttls.enable", "true");
				
				//also a session
				Session emailSession = Session.getDefaultInstance(properties);
				
				//and a store
				Store store = emailSession.getStore("pop3s");
				
				//and a connection to said store
				store.connect(server, user, password);
				
				//and a folder(inbox)
				Folder folder = store.getFolder("INBOX");
				folder.open(Folder.READ_ONLY);
				
				//and an array of messages from that folder
				Message[] messages = folder.getMessages();
				
				
				//This is where the program forks - if a fourth argument
				//was provided, we jump to that email
				if(args.length == 4){
					int selection = Integer.parseInt(args[3]) -1;
					try{
						
						//all the messages I sent myself with the
						//other applications were MIME multipart, so
						//i want the message body reader to handle
						//those. This post saved me hours:
						//https://stackoverflow.com/questions/11240368/how-to-read-
						//text-inside-body-of-mail-using-javax-mail
						
						//pick out the email
						Message m = messages[selection];
						//print the subject line
						System.out.println(m.getSubject());
						
						//if it's plaintext, just print it already!
						if(m.isMimeType("text/plain")){
							System.out.println(m.getContent().toString());
						
						//if it's a multipart, cast its content as a multipart
						}else if(m.isMimeType("multipart/*")){
							MimeMultipart mim = 
								(MimeMultipart) m.getContent();
							//iterate through the parts and print all the
							//plain text parts
							int i = mim.getCount();
							for(int n = 0; n < i; n++){
								BodyPart bp = mim.getBodyPart(n);
								if(bp.isMimeType("text/plain")){
									System.out.println(bp.getContent());
								}
							}
						}
						//then exit - we're done
						System.exit(0);						
					//if a bad selection is made with the last argument
					}catch(ArrayIndexOutOfBoundsException ex){
						System.out.println("That email doesn't seem"
						+ " to exist!");
						System.exit(0);
					}

				}
				//assuming no fourth argument was provided, proceed as
				//normal - print the subject lines and senders for the
				//emails in the mailbox
				for(int i = 0; i < messages.length; i++){
					System.out.println();
					System.out.println((i+1) + ": " + messages[i].getSubject()
					+ " (" + messages[i].getFrom()[0] + ")");
				}
				//then we're done!
				store.close();
			
			//sloppy, but it'll catch messaging exceptions and who knows
			//what else this JavaMail API will throw!
			}catch(Exception ex){
				System.out.println(ex);
			}
		//If there aren't enough arguments, print some instructions
		}catch(ArrayIndexOutOfBoundsException ey){
			System.out.println("Usage: ");
			System.out.println("java GetMail <server> <username> "
			+ "<password> <OPTIONAL: email selection(int)>");
		}
	}
}
	
	
	
	