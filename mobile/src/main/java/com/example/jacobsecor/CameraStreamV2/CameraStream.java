package com.example.jacobsecor.CameraStreamV2;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import net.majorkernelpanic.streaming.gl.SurfaceView;

import pcStream.*;
public class CameraStream extends Activity {





    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new StreamViewFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This fragment contains our stream view.
     */
    public static class StreamViewFragment extends Fragment {
        private Button doThingsButton;
        private PCStreamingServer pcServer;
        private SurfaceView mSurfaceView;
        public StreamViewFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            pcServer = new PCStreamingServer();
            View rootView = inflater.inflate(R.layout.fragment_my, container, false);
            mSurfaceView = (SurfaceView) rootView.findViewById(R.id.surface);

            if(!pcServer.hasSession()){
                pcServer.setContext(getActivity().getApplicationContext());
                pcServer.setPreviewView(mSurfaceView);
                pcServer.buildSession();
                pcServer.createListener();
                pcServer.startListening();
                pcServer.startPreview();
            }

            doThingsButton = (Button) rootView.findViewById(R.id.takePicButton);
            doThingsButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if(v.getId() == R.id.takePicButton){
                        Log.i("TEST", "This should work");
                    }
                }
            });
            return rootView;
        }
    }
}
