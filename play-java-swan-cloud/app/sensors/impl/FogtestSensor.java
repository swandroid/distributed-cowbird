package sensors.impl;

import distributed.node.CowbirdConfiguration;

import org.json.JSONException;
import org.json.JSONObject;
import sensors.base.AbstractSwanSensor;
import sensors.base.SensorPoller;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Roshan Bharath Das on 06/03/2017.
 */
public class FogtestSensor extends AbstractSwanSensor {


    private Map<String, FogtestSensor.FogTestPoller> activeThreads = new HashMap<String, FogtestSensor.FogTestPoller>();

    public static final String VALUE = "value";


    class FogTestPoller extends SensorPoller {

        int i=0;
        ServerSocket server;
        Socket socket;
        // ObjectInputStream ois;

        BufferedReader inputBuffer;
        DataOutputStream outputStream;

        FogTestPoller(String id, String valuePath, HashMap configuration) {
            super(id, valuePath, configuration);
            try {

                int port = CowbirdConfiguration.nodeConfiguration().getFogPort();
                server = new ServerSocket(port);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private long SENSOR_DELAY = 100;

        public void run() {


            try {
                socket = server.accept();
                // ois = new ObjectInputStream(socket.getInputStream());
                inputBuffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            long receiveTime = 0;
            long lastReceivedTimestamp;
            long now;

            while (!isInterrupted()) {
                try {

                    String message = (String) inputBuffer.readLine();
                    lastReceivedTimestamp = receiveTime;
                    receiveTime = System.currentTimeMillis();
                    //System.out.println("Delay in receiving data:"+(receiveTime-lastReceivedTimestamp));


                    try {
                        JSONObject json = new JSONObject(message);

                        int id = json.getInt("id");
                        //System.out.println("process data every:" + (System.currentTimeMillis() - now));
                        now = System.currentTimeMillis();
                        //System.out.println("process data every:"+ (now-lastTimestamp));
                        updateResult(FogtestSensor.this, json.get("data"), now);

                        if (json.getInt("ack") == 1) {
                            JSONObject jsonObject = new JSONObject();
                            json.put("timestamp", now);
                            json.put("id", id);

                            String jsonMessage = json.toString();

                            outputStream.writeBytes(jsonMessage);
                            outputStream.writeBytes("\n");
                        }


                        try {
                            if ((receiveTime - lastReceivedTimestamp) < SENSOR_DELAY) {
                                Thread.sleep(SENSOR_DELAY - (receiveTime - lastReceivedTimestamp));
                            } else {
                                Thread.sleep(SENSOR_DELAY);
                            }

                        } catch (InterruptedException exception) {
                            System.out.println(exception.getLocalizedMessage());
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }


    }



    @Override
    public void register(String id, String valuePath, HashMap configuration, HashMap httpConfiguration) {

        super.register(id,valuePath,configuration,httpConfiguration);

        /*getValues().put(valuePath,
                Collections.synchronizedList(new ArrayList<TimestampedValue>()));*/
        FogtestSensor.FogTestPoller fogTestPoller = new FogtestSensor.FogTestPoller(id, valuePath,
                configuration);
        activeThreads.put(id, fogTestPoller);
        fogTestPoller.start();

    }

    @Override
    public void unregister(String id) {

        super.unregister(id);
        System.out.println("Unregister sensor called");
        activeThreads.remove(id).interrupt();

    }


    @Override
    public String[] getValuePaths()  {

        //return new String[]{ VALUE};
        String [] values = new String[10000];
        for(int index = 0; index < 10000; index++) {
            values[index] = "value"+index;
        }

        return values;
    }

    @Override
    public String getEntity() {
        return "fogtest";
    }

    @Override
    public String[] getConfiguration() {
        return new String[] {"delay"};
    }


    @Override
    public boolean isHighFrequency() {
        return false;
    }
}
