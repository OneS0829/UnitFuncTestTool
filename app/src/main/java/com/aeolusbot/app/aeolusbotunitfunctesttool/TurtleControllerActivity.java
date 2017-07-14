package com.aeolusbot.app.aeolusbotunitfunctesttool;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;

public class TurtleControllerActivity extends AppCompatActivity {

    WebView myWebView;
    public String myURL;
    WebSocketClient webSocketClient;
    EditText addressTextView;
    Button disConnectButton;
    Button connectButton;
    Queue turtleQueue = new LinkedList();
    private static final String TAG = "TurtleCtrl";
    String topicTurtle = "/turtle1/pose";
    Thread turtleThread;
    URI uri;
    public Button upButton, downButton, leftButton, rightButton;
    public boolean isUpOnLongClick, isDownOnLongClick, isLeftOnLongClick, isRightOnLongClick;
    GridLayout ctrlGridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turtle_controller);

        myWebView = (WebView) findViewById(R.id.myWebView);
        addressTextView = (EditText) findViewById(R.id.addressEditText);
        disConnectButton = (Button) findViewById(R.id.disconnectButton);
        connectButton = (Button) findViewById(R.id.connectButton);
        upButton = (Button) findViewById(R.id.upButton);
        downButton = (Button) findViewById(R.id.downButton);
        rightButton = (Button) findViewById(R.id.rightButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        ctrlGridLayout = (GridLayout) findViewById(R.id.controllerGridLayout);

        isUpOnLongClick = false;
        isDownOnLongClick = false;
        isLeftOnLongClick = false;
        isRightOnLongClick = false;


        ButtonListener buttonListener = new ButtonListener();

        //Setting OnTouch
        upButton.setOnTouchListener(buttonListener);
        downButton.setOnTouchListener(buttonListener);
        rightButton.setOnTouchListener(buttonListener);
        leftButton.setOnTouchListener(buttonListener);


        //Setting OnClick
        upButton.setOnClickListener(buttonListener);
        downButton.setOnClickListener(buttonListener);
        rightButton.setOnClickListener(buttonListener);
        leftButton.setOnClickListener(buttonListener);


        //Handle the map info.
        turtleThread = new Thread(new Runnable() {
            public void run() {
                // loop until the thread is interrupted
                while (!Thread.currentThread().isInterrupted()) {
                    // do something in the loop

                    if (!turtleQueue.isEmpty()) {
                        //Log.d(TAG, "run: Queue has element");
                        String message = (String) turtleQueue.poll();
                        onProcessTurtle(message);
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


    }

    public void onProcessTurtle(String message) {
        try {
            final JSONObject jsonobject = new JSONObject(message);
            final String msg = jsonobject.getString("msg");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: "+msg);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void onConnect(View view) {
        try {
            uri = new URI(addressTextView.getText().toString());

            myURL = "http://"+uri.getHost()+":8080/stream?topic=/camera/image_raw";

            WebSettings websettings = myWebView.getSettings();
            websettings.setSupportZoom(true);
            websettings.setBuiltInZoomControls(true);
            websettings.setJavaScriptEnabled(true);
            websettings.setDomStorageEnabled(true);

            //Webview 適應性大小
            websettings.setUseWideViewPort(true);
            websettings.setLoadWithOverviewMode(true);

            myWebView.loadUrl(myURL);

            //Log.d(TAG, "onConnect: "+uri.getHost());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                String subDetailName = "/turtle1/pose";
                webSocketClient.send("{\"op\":\"subscribe\",\"topic\":\"" + subDetailName + "\"}");

                ctrlGridLayout.setVisibility(View.VISIBLE);

                turtleThread.start();
            }

            @Override
            public void onMessage(final String message) {

                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(message);
                    String topic = "";
                    topic = jsonObject.getString("topic");

                    if (topic.equals(topicTurtle)) // Topic = "\map"
                    {
                        //onProcessMap(message);
                        turtleQueue.offer(message);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };

        webSocketClient.connect();
        disConnectButton.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.INVISIBLE);

    }

    public void onDisConnect(View view) {
        String subDetailName = "/turtle1/pose";

        webSocketClient.send("{\"op\":\"unsubscribe\",\"topic\":\"" + subDetailName + "\"}");
        webSocketClient.close();


        disConnectButton.setVisibility(View.INVISIBLE);
        connectButton.setVisibility(View.VISIBLE);
        ctrlGridLayout.setVisibility(View.INVISIBLE);
    }


    @Override
    protected  void  onStop()
    {
        super.onStop();
        String subDetailName = "/turtle1/pose";


        if(turtleThread != null) turtleThread.interrupt();
    }

    public class ButtonListener implements View.OnClickListener, View.OnTouchListener {

        public void onClick(View v) {
            /*
            if(v.getId() == R.id.upButton){
                    Log.i("Move", "Up");
            }
            else if(v.getId() == R.id.downButton) {
                    Log.i("Move", "Down");
            }
            else if(v.getId() == R.id.leftButton) {
                    Log.i("Move", "Left");
            }
            else if(v.getId() == R.id.rightButton) {
                    Log.i("Move", "Right");
            }
            */
        }

        public boolean onTouch(View v, MotionEvent event) {



            if(v.getId() == R.id.upButton){
                if(event.getAction() == MotionEvent.ACTION_UP){
                    Log.i("Move", "Up release");
                    isUpOnLongClick = false;
                }
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    //Log.i("Move", "Up");
                    MoveUpThread moveUpThread = new MoveUpThread();
                    isUpOnLongClick = true;
                    moveUpThread.start();
                }
            }
            else if(v.getId() == R.id.downButton) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.i("Move", "Down release");
                    isDownOnLongClick = false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Log.i("Move", "Down");
                    MoveDownThread moveDownThread = new MoveDownThread();
                    isDownOnLongClick = true;
                    moveDownThread.start();
                }
            }
            else if(v.getId() == R.id.leftButton) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.i("Move", "Left release");
                    isLeftOnLongClick = false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Log.i("Move", "Left");
                    MoveLeftThread moveLeftThread = new MoveLeftThread();
                    isLeftOnLongClick = true;
                    moveLeftThread.start();
                }
            }
            else if(v.getId() == R.id.rightButton) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.i("Move", "Right release");
                    isRightOnLongClick = false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //Log.i("Move", "Right");
                    MoveRightThread moveRightThread = new MoveRightThread();
                    isRightOnLongClick = true;
                    moveRightThread.start();
                }
            }
            return false;
        }

    }

    public void onReset(View view)
    {
        String msg = "";
        String detailName = "/reset";
        msg = "{\"op\":\"call_service\",\"service\":\"" + detailName + "\"}";
        webSocketClient.send(msg);
    }

    public void onClear(View view)
    {
        String msg = "";
        String detailName = "/clear";
        msg = "{\"op\":\"call_service\",\"service\":\"" + detailName + "\"}";
        webSocketClient.send(msg);
    }

    public class MoveUpThread extends Thread{
        @Override
        public void run() {
            while (isUpOnLongClick) {
                try {
                    String msg = "";

                    Log.i("Move", "Up");
                    String detailName = "/turtle1/cmd_vel";
                    msg = "{\"op\":\"publish\",\"topic\":\"" + detailName + "\",\"msg\":{\"linear\":{\"x\":1.0,\"y\":0.0,\"z\":0.0}, \"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}}";
                    Log.i("Msg",msg);
                    webSocketClient.send(msg);
                    Thread.sleep(500);
                    //myHandler.sendEmptyMessage(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }
    }

    public class MoveDownThread extends Thread{
        @Override
        public void run() {
            while (isDownOnLongClick) {
                try {
                    Log.i("Move", "Down");
                    String msg = "";
                    String detailName = "/turtle1/cmd_vel";
                    msg = "{\"op\":\"publish\",\"topic\":\"" + detailName + "\",\"msg\":{\"linear\":{\"x\":-1.0,\"y\":0.0,\"z\":0.0}, \"angular\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}}}";
                    Log.i("Msg",msg);
                    webSocketClient.send(msg);
                    Thread.sleep(500);
                    //myHandler.sendEmptyMessage(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }
    }

    public class MoveLeftThread extends Thread{
        @Override
        public void run() {
            while (isLeftOnLongClick) {
                try {

                    //myHandler.sendEmptyMessage(2);
                    Log.i("Move", "Left");
                    String msg = "";
                    String detailName = "/turtle1/cmd_vel";
                    msg = "{\"op\":\"publish\",\"topic\":\"" + detailName + "\",\"msg\":{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}, \"angular\":{\"x\":0.0,\"y\":0.0,\"z\":1.0}}}";
                    Log.i("Msg",msg);
                    webSocketClient.send(msg);
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }
    }

    public class MoveRightThread extends Thread{
        @Override
        public void run() {
            while (isRightOnLongClick) {
                try {
                    //myHandler.sendEmptyMessage(2);
                    Log.i("Move", "Right");
                    String msg = "";
                    String detailName = "/turtle1/cmd_vel";
                    msg = "{\"op\":\"publish\",\"topic\":\"" + detailName + "\",\"msg\":{\"linear\":{\"x\":0.0,\"y\":0.0,\"z\":0.0}, \"angular\":{\"x\":0.0,\"y\":0.0,\"z\":-1.0}}}";
                    Log.i("Msg",msg);
                    webSocketClient.send(msg);
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }
    }



}
