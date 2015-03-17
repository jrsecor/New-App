package pcStream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.video.VideoQuality;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import streamsession.*;

public class PCStreamingServer implements Session.Callback{
    public ServerSocket sSocket;
    public ServiceListener listener;

    public final String TAG = "CAMERASTREAM";

    private boolean connected;

    private String clientIP;

    private String sessionDescription;

    //media session from libstreaming
    public Session mSession;

    private List<ServiceHandler> clients;

    private VideoQuality vidQuality;

    private ArrayList<Integer> portList;

    private Integer currentPort;

    private Context mContext;

    private SurfaceView mSurfaceView;

    public PCStreamingServer(){
        clients = new ArrayList<ServiceHandler>();
        mSession = null;
        mContext = null;
        mSurfaceView = null;

        //TODO: set portlist in config file in the apk
        portList = new ArrayList<Integer>();
        for(int portNum = 40000; portNum < 40200; portNum++){
            portList.add(portNum);
        }

        connected = false;
    }

    public void buildSession(){
        if(mContext != null && mSurfaceView != null && mSession == null){
            vidQuality = new VideoQuality(640,480,15,500000);
            mSession = SessionBuilder.getInstance()
                    .setCallback(this)
                    .setSurfaceView(mSurfaceView)
                    .setPreviewOrientation(90)
                    .setContext(mContext)
                    .setAudioEncoder(SessionBuilder.AUDIO_AAC)
                    .setVideoEncoder(SessionBuilder.VIDEO_H264)
                    .setVideoQuality(vidQuality)
                    .build();
        } else {
            Log.e(TAG, "You must set the app context and video preview surface view using setContext and setPreviewView");
        }
    }

    public void createListener(){
        try {
            listener = new ServiceListener(portList);
            currentPort = listener.getPort();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void destroy(){
        mSession.stop();
        mSession.release();
        listener.interrupt();
        for(ServiceHandler client : clients){
            removeClient(client);
        }
    }


    /**
     * Once the stream is configured, you can get a SDP formated session description
     * that you can send to the receiver of the stream.
     * For example, to receive the stream in VLC, store the session description in a .sdp file
     * and open it with VLC while streaming.
     */
    @Override
    public void onSessionConfigured() {
        setSDP(mSession.getSessionDescription());

        mSession.start();
    }

    /**
     * Once the session is started we can send out the session description
     */
    @Override
    public void onSessionStarted() {
        Log.i(TAG, "SESSION STARTED");

        ServiceHandler client;
        if((client = getWaitingClient()) != null){
            //not waiting for session description so it will fire now
            client.setWaiting(false);
        }

        Log.i(TAG, "SESSION DESCRIPTION" + mSession.getSessionDescription());
    }

    @Override
    public void onSessionStopped() {
        Log.i(TAG, "SESSION STOPPED");
        try {
            sSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        Log.i(TAG, "BITRATE: " + bitrate);
    }

    @Override
    public void onSessionError(int reason, int streamType, Exception e) {
        Log.e(TAG, "ERROR: " + reason);
    }

    public void startListening(){
        if(!listener.isListening()){
            listener.start();
        }
    }
    public void stopListening(){
        listener.interrupt();
    }

    public ServiceHandler addClient(ServiceHandler client){
        clients.add(client);
        return client;
    }

    public void removeClient(ServiceHandler client){
        clients.remove(client);
    }

    public ServiceHandler getWaitingClient(){
        for(ServiceHandler c : clients){
            if(c.isWaiting()){
                return c;
            }
        }
        return null;
    }

    public boolean hasWaitingClient(){
        if(getWaitingClient() != null){
            return true;
        }
        return false;
    }

    public void startPreview(){
        mSession.startPreview();
    }

    public boolean hasSession(){
        return !(mSession == null);
    }

    public void setSDP(String sdp){
        sessionDescription = sdp;
    }

    public void clearSDP(){
        sessionDescription = "";
    }
    public String getSDP(){
        return sessionDescription;
    }

    public void setPreviewView(SurfaceView sv){
        mSurfaceView = sv;
    }
    public void setContext(Context ct){
        mContext = ct;
    }
    public Session getSession() {
        return mSession;
    }

    /*************************************************************************/
    /************ HELPER CLASSES *********************************************/
    /*************************************************************************/
    private class ServiceListener extends Thread implements Runnable{
        private ServerSocket sSocket;
        private boolean portReady = false;
        private int portIndex = 0;
        private boolean listening = false;

        private int sPort;

        //creates server socket and starts the thread
        public ServiceListener(ArrayList<Integer> ports) throws Exception{
            super();

            while(!portReady){
                //TODO: make sure exception is fine
                if(portIndex > ports.size()){
                    throw new Exception("No port found.");
                }

                try {
                    sSocket = new ServerSocket(ports.get(portIndex));
                    sPort = sSocket.getLocalPort();
                    Log.i(TAG, Integer.valueOf(sPort).toString());
                    portReady = true;
                } catch (IOException e) {
                    Log.e(TAG,"Port already in use !");
                    Log.e(TAG, e.getMessage());
                    portIndex++;
                }
            }
        }

        @Override
        public void run() {
            while(!Thread.interrupted()){
                try {
                    addClient(new ServiceHandler(sSocket.accept())).start();
                    Log.i(TAG, "Client connected.");
                } catch (IOException e){
                    Log.e(TAG, "Connection error");
                }
            }
            try {
                sSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing the stream failed");
            }
        }

        public int getPort(){
            return sPort;
        }
        public boolean isListening(){
            return listening;
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    private class ServiceHandler extends Thread implements Runnable{
        private Socket cSocket;

        private BufferedReader in;
        private PrintWriter out;

        private String handlerIP;

        private boolean waitingForResponse;

        public ServiceHandler(Socket sock){
            cSocket = sock;
            waitingForResponse = false;
            handlerIP = sock.getInetAddress().getHostAddress();
            try {
                in = new BufferedReader(new InputStreamReader(cSocket.getInputStream()));
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(cSocket.getOutputStream())), true);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            //parseRequest();
            while(!Thread.interrupted()){
                try {
                    StreamRequest request = StreamRequest.parseRequest(in);
                    if(request.getCommand().equals(StreamRequest.CMD_CONNECT)) {
                        //start up stream session
                        if (!waitingForResponse && !connected) {
                            waitingForResponse = true;
                            setupSession();

                            //spin every 5 seconds until we get the session description
                            final int loopDelay = 5000;

                            while (waitingForResponse) {
                                Log.i(TAG, "Waiting for session setup.");
                                try {
                                    Thread.sleep(loopDelay);
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Session creation timed out");
                                    e.printStackTrace();
                                }
                            }

                            //gotta have headers
                            if (!sessionDescription.contains("\n")) {
                                Log.e(TAG, "No headers");
                                break;
                            }

                            List<String> headers = Arrays.asList(sessionDescription.split("\n"));
                            StreamResponse response = new StreamResponse(StreamResponse.RESPONSE_OK, headers);
                            response.sendResponse(out);
                            connected = true;
                            clientIP = handlerIP;
                        }
                        //reject connection refer them to session holder
                        else {
                            Log.i(TAG, "Connection refused. Referred to: " + handlerIP);

                            List<String> headers = new ArrayList<String>();
                            headers.add("handlerIP");

                            StreamResponse response = new StreamResponse(StreamResponse.RESPONSE_NO, headers);
                            response.sendResponse(out);

                            this.interrupt();
                        }
                    } else if (request.getCommand().equals(StreamRequest.CMD_DISCONNECT)) {
                        if(connected){
                            handleDisconnect();
                            this.interrupt();
                        }
                        StreamResponse response = new StreamResponse(StreamResponse.RESPONSE_DONE, new ArrayList<String>());
                        response.sendResponse(out);
                    } else {
                        System.out.println("BAD REQUEST");
                    }

                } catch (Exception e){
                    Log.e(TAG, "ERROR YO");
                    handleDisconnect();
                    this.interrupt();
                }
            }

            //at end of main service loop.
            dispose();

        }

        private void dispose(){
            try {
                in.close();
                out.close();
                cSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleDisconnect(){
            if(clientIP == handlerIP){
                connected = false;
                clientIP = "";
                clearSDP();
                mSession.stopPreview();
                mSession.stop();
                //mSession.release();
            }
        }

        public boolean isWaiting(){
            return waitingForResponse;
        }
        public void setWaiting(boolean waiting){
            waitingForResponse = waiting;
        }

        public void setupSession(){
            String localIP = cSocket.getInetAddress().getHostAddress();
            mSession.setDestination(localIP);
            if (!mSession.isStreaming()) {
                mSession.configure();
            } else {
                mSession.stop();
            }
        }

    }
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onPreviewStarted() {
        // TODO Auto-generated method stub

    }

}
