package com.aeolusbot.app.aeolusbotunitfunctesttool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;

public class MapTouchActivity extends AppCompatActivity {

    WebSocketClient webSocketClient;
    EditText addressTextView;
    ImageView mapImageView;
    ImageView targetImageView;
    TextView logTextView;
    Bitmap mapBitmap;
    Bitmap targetBitmap;
    Bitmap backupTargetBitmap;
    Bitmap overlayBitmap;
    URI uri;
    int mapWidth = 0;
    int mapHeight = 0;
    double mapResolution = 0;
    double mapOrigin_x = 0;
    double mapOrigin_y = 0;
    private static final String TAG = "MapTouch";
    String topicMap = "/map";
    String topicFootprint = "/move_base/global_costmap/footprint";
    Queue mapQueue = new LinkedList();
    Queue footprintQueue = new LinkedList();
    Thread mapThread;
    Button disConnectButton;
    Button connectButton;
    int targetX = 0;
    int targetY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_touch);

        addressTextView = (EditText) findViewById(R.id.addressEditText);
        logTextView = (TextView) findViewById(R.id.logTextView);
        mapImageView = (ImageView) findViewById(R.id.imageView);
        disConnectButton = (Button) findViewById(R.id.disconnectButton);
        connectButton = (Button) findViewById(R.id.connectButton);

        mapImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float eventX = event.getX();
                float eventY = event.getY();
                float[] eventXY = new float[] {eventX, eventY};

                Matrix invertMatrix = new Matrix();
                ((ImageView)v).getImageMatrix().invert(invertMatrix);

                invertMatrix.mapPoints(eventXY);
                int x = Integer.valueOf((int)eventXY[0]);
                int y = Integer.valueOf((int)eventXY[1]);

                if((x>0 && x<mapWidth) && (y>0 && y<mapHeight))
                {
                    double result_x = cellTometer(x, mapOrigin_x, mapResolution);
                    double result_y = cellTometer(((mapHeight - y)-1), mapOrigin_y, mapResolution);
                    Toast.makeText(MapTouchActivity.this, "("+result_x+":"+result_y+") has tapped!!", Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });

        //Handle the map info.
        mapThread = new Thread(new Runnable() {
            public void run() {
                // loop until the thread is interrupted
                while (!Thread.currentThread().isInterrupted()) {
                    // do something in the loop

                    if (!mapQueue.isEmpty()) {
                        //Log.d(TAG, "run: Queue has element");
                        String message = (String) mapQueue.poll();
                        onProcessMap(message);
                    }

                    if(!footprintQueue.isEmpty() && mapBitmap != null){
                        String message = (String) footprintQueue.poll();
                        onProcessFootprint(message);
                    }

                    if(mapBitmap != null && targetBitmap != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //Log.d(TAG, msg.toString());
                                overlayBitmap = overlay(mapBitmap,targetBitmap);
                                mapImageView.setImageBitmap(overlayBitmap);
                            }
                        });
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected  void  onStop()
    {
        super.onStop();
        String subDetailName = "/map";
        String subDetailNameTwo = "/move_base/global_costmap/footprint";

        if(mapThread != null) mapThread.interrupt();
    }


    public void onDisConnect(View view) {
        String subDetailName = "/map";
        String subDetailNameTwo = "/move_base/global_costmap/footprint";

            webSocketClient.send("{\"op\":\"unsubscribe\",\"topic\":\"" + subDetailName + "\"}");
            webSocketClient.send("{\"op\":\"unsubscribe\",\"topic\":\"" + subDetailNameTwo + "\"}");
            webSocketClient.close();


        disConnectButton.setVisibility(View.INVISIBLE);
        connectButton.setVisibility(View.VISIBLE);
    }

    public void onProcessMap(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            final JSONObject msgJsonObject = new JSONObject(jsonObject.getString("msg"));
            JSONObject infoJsonObject = new JSONObject(msgJsonObject.getString("info"));
            JSONObject originJsonObject = new JSONObject(infoJsonObject.getString("origin"));
            JSONObject positionJsonObject = new JSONObject(originJsonObject.getString("position"));
            final JSONArray dataArray = new JSONArray(msgJsonObject.getString("data"));

            mapWidth = infoJsonObject.getInt("width");
            mapHeight = infoJsonObject.getInt("height");
            mapResolution = infoJsonObject.getDouble("resolution");
            mapOrigin_x = positionJsonObject.getDouble("x");
            mapOrigin_y = positionJsonObject.getDouble("y");

            if(mapBitmap == null) {
                mapBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.ARGB_8888);
            }

            for (int y = 0; y < mapHeight; y++) {
                for (int x = 0; x < mapWidth; x++) {
                    int index = (((mapHeight - 1) - y) * mapWidth) + x;
                    int value = dataArray.getInt(index);
                    if (value == -1) {
                        mapBitmap.setPixel(x, y, Color.GRAY); //UNKNOWN
                    }
                    else if (value == 100){
                        mapBitmap.setPixel(x, y, Color.BLACK); //WALL
                    }
                    else if (value == 0) {
                        mapBitmap.setPixel(x, y, Color.WHITE); //FREE SPACE
                    }
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

//------Overlay Bitmap----------------------//

    public Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp2.getWidth(), bmp2.getHeight(), bmp1.getConfig());
        float left =(bmp2.getWidth() - (bmp1.getWidth()*((float)bmp2.getHeight()/(float)bmp1.getHeight())))/(float)2.0;
        float bmp1newW = bmp1.getWidth()*((float)bmp2.getHeight()/(float)bmp1.getHeight());
        Bitmap bmp1new = getResizedBitmap(bmp1, bmp2.getHeight(), (int)bmp1newW);
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1new, left ,0 , null);
        //canvas.drawBitmap(bmp2, new Matrix(), null);

        Bitmap marker = BitmapFactory.decodeResource(getResources(),
                R.drawable.marker);

        canvas.drawBitmap(marker, targetX - (marker.getWidth() / 2), targetY - (marker.getHeight() / 2), null);
        return bmOverlay;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

//-----------------------------------------//

    public void onConnect(View view) {
            try {
                uri = new URI(addressTextView.getText().toString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    String subDetailName1 = "/map";
                    String subDetailName2 = "/move_base/global_costmap/footprint";
                    webSocketClient.send("{\"op\":\"subscribe\",\"topic\":\"" + subDetailName1 + "\"}");
                    webSocketClient.send("{\"op\":\"subscribe\",\"topic\":\"" + subDetailName2 + "\"}");

                    mapThread.start();
                }

                @Override
                public void onMessage(final String message) {

                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(message);
                        String topic = "";
                        topic = jsonObject.getString("topic");
                        if (topic.equals(topicMap)) // Topic = "\map"
                        {
                            //onProcessMap(message);
                            mapQueue.offer(message);
                        }
                        else if(topic.equals(topicFootprint))
                        {
                            //onProcessFootprint(message);
                            footprintQueue.offer(message);
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

    private void onProcessFootprint(String message) {

        //mapBitmap = backupMapBitmap.copy(backupMapBitmap.getConfig(), true);

        if(targetBitmap == null && mapBitmap != null) {
            targetBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.ARGB_8888);
            backupTargetBitmap = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.ARGB_8888);
        }
        else{
            targetBitmap = backupTargetBitmap.copy(backupTargetBitmap.getConfig(), true);
        }

        try {
            JSONObject jsonobject = new JSONObject(message);
            JSONObject msgJsonObject = new JSONObject(jsonobject.getString("msg"));
            JSONObject polygonJsonObject = new JSONObject(msgJsonObject.getString("polygon"));
            JSONArray pointsJsonArray = polygonJsonObject.getJSONArray("points");

            int countX = 0;
            int countY = 0;

            for(int i=0; i<pointsJsonArray.length(); i++)
            {
                JSONObject item = pointsJsonArray.getJSONObject(i);
                MapPoint mapPoint = new MapPoint();
                double x = Double.parseDouble(item.getString("x"));
                double y = Double.parseDouble(item.getString("y"));
                double z = Double.parseDouble(item.getString("z"));
                mapPoint.setX(x);
                mapPoint.setY(y);
                mapPoint.setZ(z);
                int point_X  = meterToCell(mapPoint.getX(), mapOrigin_x, mapResolution);
                int point_Y = meterToCell(mapPoint.getY(), mapOrigin_y, mapResolution);
                countX = countX + point_X;
                countY = countY + ((mapHeight - point_Y) - 1);
                //targetBitmap.setPixel(point_X, ((mapHeight - point_Y) - 1), Color.RED);
            }

            targetX = countX / pointsJsonArray.length();
            targetY = countY / pointsJsonArray.length();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private int meterToCell(double meter, double origin, double resolution)
    {
        int result = (int)((meter - origin) / resolution);
        return result;
    }

    private double cellTometer(int cell, double origin, double resolution)
    {
        double result = (((double)cell) * resolution) + origin;
        return  result;
    }
}
