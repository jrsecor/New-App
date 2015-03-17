package streamsession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StreamRequest {
	public static final String CMD_CONNECT = "CONNECT";
	public static final String CMD_DISCONNECT = "DISCONNECT";
	
	private String command;
	
		private String ip;
	
	private Matcher matcher;
	private Pattern pattern;
	
	public StreamRequest(String ip, String command){
		this.ip = ip;
		this.command = command;
	}
	
	public static StreamRequest parseRequest(BufferedReader in){
		String currLine;
		String reqCommand;
		String reqIP;
		try {
            currLine = in.readLine();

            //socket is disconnected
            if (currLine == null) {
                return null;
            }

            if (currLine.equals(CMD_CONNECT)) {
                reqCommand = CMD_CONNECT;
            } else if (currLine.equals(CMD_DISCONNECT)){
                reqCommand = CMD_DISCONNECT;
            }
			else{
				return null;
			}
			
			//TODO: FIgure out if we need ip address or not... for now ya
			if((currLine = in.readLine()) == null){
				return null;
			}
			
			reqIP = currLine;
			
			return new StreamRequest(reqIP, reqCommand);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void sendRequest(PrintWriter out){
		out.println(command);
		out.println(ip);
	}
	
	
	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

}
