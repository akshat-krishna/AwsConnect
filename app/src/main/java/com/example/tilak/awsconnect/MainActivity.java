package com.example.tilak.awsconnect;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;



import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    Button subConnect,subDisconnect,subSub;
    EditText ipinputsubscribe;
    TextView message,tvStatus;


    private static final String CUSTOMER_SPECIFIC_ENDPOINT ="a75t5z3ve6d72.iot.ap-southeast-2.amazonaws.com";



    String clientId;


    CognitoCachingCredentialsProvider credentialsProvider;
    AWSIotMqttManager mqttManager;
    AWSIotClient mIotAndroidClient;

    String LOG_TAG="AWS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        subConnect=(Button)findViewById(R.id.subConnect);
        subSub=(Button)findViewById(R.id.subsub);
        subDisconnect=(Button)findViewById(R.id.subDisconnect);
        ipinputsubscribe=(EditText)findViewById(R.id.ipinputsubscribe);
        message=(TextView)findViewById(R.id.message);
        tvStatus=findViewById(R.id.tvStatus);
        clientId = UUID.randomUUID().toString();




        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-southeast-2:b85261db-a616-40a0-b8af-40871a56e948", // Identity pool ID
                Regions.AP_SOUTHEAST_2 // Region
        );

        Region region = Region.getRegion(Regions.AP_SOUTHEAST_2 );

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, "a75t5z3ve6d72.iot.ap-southeast-2.amazonaws.com");

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);





        subConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("LOG_TAG", "clientId = " + clientId);
                try {
                    mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                        @Override
                        public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                    final Throwable throwable) {
                            Log.d("LOG_TAG", "Status = " + String.valueOf(status));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (status == AWSIotMqttClientStatus.Connecting) {
                                        tvStatus.setText("Connecting...");

                                    } else if (status == AWSIotMqttClientStatus.Connected) {
                                        tvStatus.setText("Connected");
                                        subSub.setVisibility(View.VISIBLE);
                                        subDisconnect.setVisibility(View.VISIBLE);
                                        subConnect.setVisibility(View.INVISIBLE);

                                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                        if (throwable != null) {
                                            Log.e("LOG_TAG", "Connection error.", throwable);
                                        }
                                        tvStatus.setText("Reconnecting");
                                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                        if (throwable != null) {
                                            Log.e("LOG_TAG", "Connection error.", throwable);
                                            throwable.printStackTrace();
                                        }
                                        tvStatus.setText("Disconnected");
                                    } else {
                                        tvStatus.setText("Disconnected");

                                    }
                                }
                            });
                        }
                    });

                } catch (final Exception e) {
                    Log.e("LOG_TAG", "Connection error.", e);
                    tvStatus.setText("Error! " + e.getMessage());
                }

            }

            });

        subDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mqttManager.disconnect();
                    subSub.setVisibility(View.INVISIBLE);
                    subDisconnect.setVisibility(View.INVISIBLE);
                    subConnect.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Disconnect error.", e);
                }

            }
        });

        subSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String topic = ipinputsubscribe.getText().toString();

                Log.d(LOG_TAG, "topic = " + topic);

                try {
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String topic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                String messages = new String(data, "UTF-8");
                                                Log.d(LOG_TAG, "Message arrived:");
                                                Log.d(LOG_TAG, "   Topic: " + topic);
                                                Log.d(LOG_TAG, " Message: " + message);

                                                message.setText(messages);

                                            } catch (UnsupportedEncodingException e) {
                                                Log.e(LOG_TAG, "Message encoding error.", e);
                                            }
                                        }
                                    });
                                }
                            });
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Subscription error.", e);
                }


            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}