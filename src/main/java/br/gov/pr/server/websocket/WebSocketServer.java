package br.gov.pr.server.websocket;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import br.gov.pr.server.module.print.Base64Print;

public class WebSocketServer{

	private final static Logger LOGGER = Logger.getLogger(WebSocketServer.class.getName());
	private static final int SERVER_PORT = 8480;

    public static void main(String[] args) {
    	    
    	LOGGER.info("Starting server["+SERVER_PORT+"]...");
    	
    	try (ServerSocket server = new ServerSocket(SERVER_PORT)){    		
    		
    		while(true) {
    			
    			try (Socket client = server.accept()) {
    				
    				try (InputStream inputStream = client.getInputStream();
    						OutputStream outputStream = client.getOutputStream()) {
	    			
    					doHandShakeToInitializeWebSocketConnection(inputStream, outputStream);
	    				
	    				printInputStream(inputStream);
    				
    				} catch (Exception e) {
    					LOGGER.log(Level.SEVERE, e.getMessage(), e.getCause());
    				}
    				
    			} catch (Exception e) {
    				LOGGER.log(Level.SEVERE, e.getMessage(), e.getCause());
    			} 
    			
    		}    		
    		
    	} catch (Exception e) {
    		LOGGER.log(Level.SEVERE, e.getMessage(), e.getCause());
    	}

    }

    //Source for encoding and decoding:
    //https://stackoverflow.com/questions/8125507/how-can-i-send-and-receive-websocket-messages-on-the-server-side


    private static void printInputStream(InputStream inputStream) throws IOException {
        int len = 0;            
        byte[] b = new byte[1024];
        //rawIn is a Socket.getInputStream();
        while(true){
            len = inputStream.read(b);
            if(len!=-1){

                byte rLength = 0;
                int rMaskIndex = 2;
                int rDataStart = 0;
                //b[0] is always text in my case so no need to check;
                byte data = b[1];
                byte op = (byte) 127;
                rLength = (byte) (data & op);

                if(rLength==(byte)126) rMaskIndex=4;
                if(rLength==(byte)127) rMaskIndex=10;

                byte[] masks = new byte[4];

                int j=0;
                int i=0;
                for(i=rMaskIndex;i<(rMaskIndex+4);i++){
                    masks[j] = b[i];
                    j++;
                }

                rDataStart = rMaskIndex + 4;

                int messLen = len - rDataStart;

                byte[] message = new byte[messLen];

                for(i=rDataStart, j=0; i<len; i++, j++){
                    message[j] = (byte) (b[i] ^ masks[j % 4]);
                }
                	
                try {
                	Base64Print.print(new String(message));
                } catch (Exception e) {
					// TODO: handle exception
				}

                b = new byte[1024];

            }
        }
    }


    public static byte[] encode(String mess) throws IOException{
        byte[] rawData = mess.getBytes();

        int frameCount  = 0;
        byte[] frame = new byte[10];

        frame[0] = (byte) 129;

        if(rawData.length <= 125){
            frame[1] = (byte) rawData.length;
            frameCount = 2;
        }else if(rawData.length >= 126 && rawData.length <= 65535){
            frame[1] = (byte) 126;
            int len = rawData.length;
            frame[2] = (byte)((len >> 8 ) & (byte)255);
            frame[3] = (byte)(len & (byte)255); 
            frameCount = 4;
        }else{
            frame[1] = (byte) 127;
            int len = rawData.length;
            frame[2] = (byte)((len >> 56 ) & (byte)255);
            frame[3] = (byte)((len >> 48 ) & (byte)255);
            frame[4] = (byte)((len >> 40 ) & (byte)255);
            frame[5] = (byte)((len >> 32 ) & (byte)255);
            frame[6] = (byte)((len >> 24 ) & (byte)255);
            frame[7] = (byte)((len >> 16 ) & (byte)255);
            frame[8] = (byte)((len >> 8 ) & (byte)255);
            frame[9] = (byte)(len & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;

        byte[] reply = new byte[bLength];

        int bLim = 0;
        for(int i=0; i<frameCount;i++){
            reply[bLim] = frame[i];
            bLim++;
        }
        for(int i=0; i<rawData.length;i++){
            reply[bLim] = rawData[i];
            bLim++;
        }

        return reply;
    }

    private static void doHandShakeToInitializeWebSocketConnection(InputStream inputStream, OutputStream outputStream) throws UnsupportedEncodingException {
    	
    	LOGGER.info("Starting handshake...");
    	
		try (Scanner scan = new Scanner(new FilterInputStream(inputStream) {
	    	    public void close() throws IOException {}
	    	}, "UTF-8")) {
		
	    	String data = scan.useDelimiter("\\r\\n\\r\\n").next();
	    	
	    	Matcher get = Pattern.compile("^GET").matcher(data);
	
	        if (get.find()) {
	            
	        	Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
	            match.find();                 
	
	            byte[] response = null;
	            try {
	                response = ("HTTP/1.1 101 Switching Protocols\r\n"
	                        + "Connection: Upgrade\r\n"
	                        + "Upgrade: websocket\r\n"
	                        + "Sec-WebSocket-Accept: "
	                        + DatatypeConverter.printBase64Binary(
	                                MessageDigest
	                                .getInstance("SHA-1")
	                                .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
	                                        .getBytes("UTF-8")))
	                        + "\r\n\r\n")
	                        .getBytes("UTF-8");
	            } catch (NoSuchAlgorithmException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	
	            try {
	                outputStream.write(response, 0, response.length);
	            } catch (IOException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        } else {
	
	        }        
		}
    }
}
