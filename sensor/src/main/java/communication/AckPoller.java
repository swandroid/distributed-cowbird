package communication;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;

public class AckPoller extends Thread {

    private BufferedReader inputStream;

    public AckPoller(BufferedReader inputStream) {
        this.inputStream = inputStream;
    }

    public void run() {
        System.out.println("Communication Latency:");
        while(!isInterrupted()) {
            try {
                String message = inputStream.readLine();
                JSONObject object = new JSONObject(message);

                int id = object.getInt("id");
                LatencyController.sharedInstance().setIncomingId(id);
                LatencyController.sharedInstance().setIncomingTimestamp(System.currentTimeMillis());
                System.out.println(""+ManagementFactory.getRuntimeMXBean().getName()+ "\t" + LatencyController.sharedInstance().getLatency()+"\n");
            } catch (IOException exception) {
                System.out.println(exception.getLocalizedMessage());
            }
        }
    }
}
