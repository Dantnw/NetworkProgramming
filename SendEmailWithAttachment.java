/*
Program name: SendEmail
File: SendEmail.java

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

@author Dan Rose, 2437236, based on SendEmail from the Java TutorialsPoint
JavaMail API tutorial at 
https://www.tutorialspoint.com/javamail_api/javamail_api_sending_simple_email.htm
and on 
https://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/

The purpose of this program is to send an email from a properly formatted text
document through an email server. THIS VERSION OF THE PROGRAM ALSO ACCEPTS A 
command line argument corresponding with a file to be sent as an attachment.
The file extension must be included in the command line argument.

to compile: 
javac -cp javax.mail.jar email/SendEmail.java

to run:
java -cp javax.mail.jar;. email/SendEmail <email file>


*/


package email;

import java.io.*;
import java.util.*;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;


public class SendEmailWithAttachment{
	
	//main method
	public static void main(String[] args){
		
		File email = new File("email.txt");
		File attach;
		
		//get the file from the arguments, or use the
		//default email.txt file
		try{
			email = new File(args[0]);
		}catch(ArrayIndexOutOfBoundsException ex){
			System.out.println("no email file specified");
			System.exit(0);
		}
		
		//get the file attachment
		try{
			attach = new File(args[1]);
		}catch(ArrayIndexOutOfBoundsException ex){
			System.out.println("No attachment file specified.");
			System.out.println("Use SendEmail if no attachments are to"
			+ " be sent.");
			System.exit(0);
		}
		
		
		//necessary variables
		String line;
		String server = "";
		final String user;
		final String password;
		String to = "";
		LinkedList<String> cc = new LinkedList<String>();
		LinkedList<String> bcc = new LinkedList<String>();
		String subject = "";
		String body = "";
		String temp;
	
		//this try/catch statement encapsulates the portion of the program
		//that reads the necessary fields from the email file
		try{
			//this scanner earns it
			Scanner scanner = new Scanner(email);
			
			//get server
			server = scanner.nextLine();
			server = server.substring(8, (server.length()));
			//System.out.println(server);
			
			//get user
			temp = scanner.nextLine();
			user = temp.substring(6, (temp.length()));
			//System.out.println("User: " + user);
			
			//get password
			temp = scanner.nextLine();
			password = temp.substring(10, (temp.length()));
			//System.out.println("Password: " + password);
			
			//get recipient
			to = scanner.nextLine();
			to = to.substring(4, (to.length()));
			//System.out.println("To: " + to);
			
			//get cc list
			line = scanner.nextLine();
			line = line.substring(4, line.length());
			//a little convoluted, but it trims an email address,
			//removes it from the line, and catches the exceptions
			//if the line is empty. It's guaranteed to throw exceptions
			//each time but I haven't seen any issues with that so far.
			while(line.length() > 2){
				try{
					temp = line.substring(0, (line.indexOf(",")));
				}catch(StringIndexOutOfBoundsException SIOOBE){
					temp = line.substring(0, line.length());
				}
				try{
					line = line.substring((temp.length() + 1), line.length());
				}catch(StringIndexOutOfBoundsException ex){
					line = "";
				}
				cc.add(temp);
			}
			//System.out.println("CCs: ");
			//for(String s : cc){
			//	System.out.println(s);
			//}
			
			//get bcc list - carbon copy of the cc list algorithm
			line = scanner.nextLine();
			line = line.substring(5, line.length());
			while(line.length() > 2){
				try{
					temp = line.substring(0, (line.indexOf(",")));
				}catch(StringIndexOutOfBoundsException SIOOBE){
					temp = line.substring(0, line.length());
				}
				try{
					line = line.substring((temp.length() + 1), line.length());
				}catch(StringIndexOutOfBoundsException ex){
					line = "";
				}
				bcc.add(temp);
			}
			//System.out.println("BCCs: ");
			//for(String s : bcc){
			//	System.out.println(s);
			//}			
			
			//get subject
			subject = scanner.nextLine();
			subject = subject.substring(8, subject.length());
			//System.out.println("Subject: " + subject);
			
			//the rest is Body
			while(scanner.hasNextLine()){
				body = body + scanner.nextLine();
				body += "\n";
			}
			
			//System.out.println(body);
			

		
//https://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
		//I need to send a thank-you email to mykong for so perfectly
		//demonstrating how to build the correct properties to work
		//with gmail. IF THERE ARE ACTUAL REGIONAL DIFFERENCES FOR
		//WHICH GMAIL SERVERS YOU CAN CONNECT TO then it is possible
		//that this code will not execute properly. If that's the case,
		//change the Server in the email.txt document to a gmail server
		//you are certain you can connect to. It's not a perfect system.
		Properties props = new Properties();
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.host", server);	//server field used here
		props.put("mail.smtp.socketFactory.class", 
			"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		
		//create the session
		Session session = Session.getDefaultInstance(props, 
			new javax.mail.Authenticator(){//gotta authenticate!
			protected PasswordAuthentication getPasswordAuthentication(){
			return new PasswordAuthentication(user, password);
			}
			});
		
		//assemble the message
		try{		
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(user));
				
			//set the "to"
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			
			//set ccs
			for(String s : cc){
				message.addRecipient(Message.RecipientType.CC, new InternetAddress(s));
			}
			
			//set bccs
			for(String s : bcc){
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(s));
			}
			
			//set subject
			message.setSubject(subject);
			
			//this will be handy when we make it accept attachments
			Multipart multipart = new MimeMultipart();
			
			//make a body part, pass it the body, and tack it on
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body);
			multipart.addBodyPart(messageBodyPart);
			
			//make a body part for the attachment
			DataSource source = new FileDataSource(args[1]);
			BodyPart attachment = new MimeBodyPart();
			attachment.setDataHandler(new DataHandler(source));
			attachment.setFileName(args[1]);
			multipart.addBodyPart(attachment);
			
			//tack the multipart onto the message
			message.setContent(multipart);
			
			//send it!
			Transport.send(message);
			
			//confirmation!
			System.out.println("Go check your email!");
		}catch(MessagingException e){
			throw new RuntimeException(e);
		}//if something goes wrong with assembling the message
		
		}catch(IOException ex){
			System.out.println("Something went wrong - check formatting"
			+ " of email document");
			System.exit(0);
		}//if something goes wrong with the IO operation - the whole program
		//is within this try/catch block so the second half has access to
		//the variables pulled out of the text file in the IO operation
	}
}