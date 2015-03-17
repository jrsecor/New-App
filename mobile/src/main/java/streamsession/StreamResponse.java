package streamsession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class StreamResponse {
	public static final String RESPONSE_OK = "OK";
	public static final String RESPONSE_NO = "NO";
	public static final String RESPONSE_DONE = "DONE";
	public String response;
	public List<String> headers;
	
	public StreamResponse(String response, List<String> headers){
		this.response = response;
		this.headers = headers;
	}
	/**
	 * Parses the response from the input stream of a socket
	 * @param in
	 * @return NULL IF BAD RESPONSE else StreamResponse with:
	 * Response code i.e. "OK" and additional headers
	 */
	public static StreamResponse parseResponse(BufferedReader in){
		String currLine;
		String response;
		List<String> headers = new ArrayList<String>();
		
		try {
			currLine = in.readLine();
			if(currLine.trim().equals(RESPONSE_OK)){
				response = RESPONSE_OK;
			} else if (currLine.trim().equals(RESPONSE_NO)){
				response = RESPONSE_NO;
			} else {
				System.out.println("Bad Response");
				return null;
			}
			
			//get headers
			while(!(currLine = in.readLine()).equals("")){
				headers.add(currLine);
			}
			
			return new StreamResponse(response, headers);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//shouldn't make it here...
		return null;
	}
	
	public void sendResponse(PrintWriter out){
		out.println(response);
		for(String s : headers){
			out.println(s);
		}
		out.print("\n");
		out.flush();
	}
}
