package sensors.impl;

import distributed.node.CowbirdConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import sensors.base.AbstractSwanSensor;
import sensors.base.SensorPoller;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Roshan Bharath Das on 06/03/2017.
 */
public class FogtestlocalSensor extends AbstractSwanSensor {


    private Map<String, FogtestlocalSensor.FogTestPoller> activeThreads = new HashMap<String, FogtestlocalSensor.FogTestPoller>();

    public static final String VALUE = "value";


    class FogTestPoller extends SensorPoller {

        float min = 0.0f;
        float max = 130.0f;

        Random rand = new Random();

        FogTestPoller(String id, String valuePath, HashMap configuration) {
            super(id, valuePath, configuration);

        }

        private long SENSOR_DELAY = 100;

        public void run() {

            long lastTimestamp = 0;


            while (!isInterrupted()) {

                long now = System.currentTimeMillis();


                // updateResult(FogtestSensor.this,json.get("data"), json.getLong("timestamp"));
                updateResult(FogtestlocalSensor.this,rand.nextFloat() * (max - min) + min, now);


                //              catch (ClassNotFoundException e) {
//                    e.printStackTrace();
//                }

                try {
                    Thread.sleep(SENSOR_DELAY);
                } catch (InterruptedException e) {
                    break;
                }

            }


        }


    }



    @Override
    public void register(String id, String valuePath, HashMap configuration, HashMap httpConfiguration) {

        super.register(id,valuePath,configuration,httpConfiguration);

        /*getValues().put(valuePath,
                Collections.synchronizedList(new ArrayList<TimestampedValue>()));*/
        FogtestlocalSensor.FogTestPoller fogTestPoller = new FogtestlocalSensor.FogTestPoller(id, valuePath,
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
        return "fogtestlocal";
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
