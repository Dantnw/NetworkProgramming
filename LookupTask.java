import java.net.*;
import java.util.concurrent.Callable;

public class LookupTask implements Callable<String>{
	
	private String line;
	private int option;
	
	public LookupTask(String line, int option){
		this.line = line;
		this.option = option;
	}
	
	@Override
	public String call(){
		try{
		
			if(option == 1){
				int index = line.indexOf(" ");
				String address = line.substring(0, index);
				String hostname = InetAddress.getByName(address).getHostName();
				return hostname;
			}
			
			if(option == 2){
				int index = line.lastIndexOf(" ") + 1;
				String bytes = line.substring(index, line.length());
				return bytes;
			}
			
			if(option == 3){
				int index = line.indexOf(" ");
				String address = line.substring(0, index);
				String hostname = InetAddress.getByName(address).getHostName();
				return hostname;
			}
	
			else{
				int index = line.indexOf(' ');
				String address = line.substring(0, index);
				String theRest = line.substring(index);
				String hostname = InetAddress.getByName(address).getHostName();
				return hostname + " " + theRest;
			}
		}catch(Exception ex){
			return line.substring(0, (line.indexOf(" ")));
		}
	}
}
