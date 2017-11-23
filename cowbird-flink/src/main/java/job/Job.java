package job;

import cowbird.flink.common.messages.control.ComplexCompareControlMessage;
import cowbird.flink.common.messages.result.ResultMessage;
import cowbird.flink.common.config.Topics;

import cowbird.flink.common.messages.control.ConstantCompareControlMessage;
import cowbird.flink.common.messages.control.ControlMessage;
import cowbird.flink.common.messages.sensor.SensorMessage;

import cowbird.flink.common.util.Utils;
import org.apache.flink.api.java.utils.ParameterTool;

import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;

import org.apache.kafka.common.serialization.StringSerializer;
import processing.core.ComplexCompareProcessFunction;
import processing.core.ConstantCompareFlatMap;
import processing.core.CoreProcessFunction;

import org.apache.flink.api.common.functions.MapFunction;

import org.apache.flink.api.java.tuple.Tuple2;

import org.apache.flink.streaming.api.TimeCharacteristic;

import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStream;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import processing.light.StreamingProcessFlatMap;


import java.util.Properties;

/**
 * Created by gdibernardo on 17/07/2017.
 */

public class Job {

    private static final String LIGHT_PARAMETER = "light-mode";

    private static final String JOB_NAME = "Cowbird-Flink";

    //private static final String KAFKA_BROKER_CONFIG = "kafka-gdiberna-0.sda.surf-hosted.nl:9092";
    private static final String KAFKA_BROKER_CONFIG = "localhost:9092";
    /*  This can be (is) a path on HDFS. */
    // private static final String ROCKSDB_STATE_PATH = "hdfs://hathi-surfsara/user/gdiberna/cowbird_state";

    private static final String FS_STATE_PATH = "hdfs://hathi-surfsara/user/gdiberna/cowbird_fs_state";

    private static final String ROCKSDB_STATE_PATH = "file:///Users/gdibernardo/Documents/cowbird/cowbird_state";
    private static final long CHECKPOINT_INTERVAL = 3600000; // ms

    public static final String CONSUMER_FLINK_SENSORS_VALUES_GROUP_ID = "CONSUMER_FLINK_SENSORS_VALUES_GROUP_ID";

    public static final String CONSUMER_FLINK_CONTROL_TOPIC_SVE_GROUP_ID = "CONSUMER_FLINK_CONTROL_TOPIC_SVE_GROUP_ID";
    public static final String CONSUMER_FLINK_CONTROL_TOPIC_CVE_GROUP_ID = "CONSUMER_FLINK_CONTROL_TOPIC_CVE_GROUP_ID";
    public static final String CONSUMER_FLINK_CONTROL_TOPIC_CE_GROUP_ID = "CONSUMER_FLINK_CONTROL_TOPIC_CE_GROUP_ID";

    public static final String CLIENT_ID = "CLIENT_ID";


    public static Properties defaultConsumingProperties() {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKER_CONFIG);
        return properties;
    }

    static boolean validateLightModeParameter(String lightModeArg) {
        return lightModeArg.toLowerCase().equals("on");
    }


    public static void main(String args[]) throws Exception {


        ParameterTool parameterTool = ParameterTool.fromArgs(args);

        boolean isLightModeEnabled = false;

        if(validateLightModeParameter(parameterTool.get(LIGHT_PARAMETER, new String()))) {
            System.out.println("SWAN light mode (streaming-oriented) evaluation enabled.");
            isLightModeEnabled = true;
        }

        /*  Create streaming environment.   */
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        environment.enableCheckpointing(CHECKPOINT_INTERVAL, CheckpointingMode.AT_LEAST_ONCE);
        /*  Setting RocksDB backend.    */
        environment.setStateBackend(new RocksDBStateBackend(ROCKSDB_STATE_PATH));

        /*  Setting Fs backend. */
        // environment.setStateBackend(new FsStateBackend(FS_STATE_PATH, true));

        /*  Init sensors values Kafka source.   */
        Properties sensorValuesConsumerProperties = defaultConsumingProperties();
        sensorValuesConsumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_FLINK_SENSORS_VALUES_GROUP_ID);
        FlinkKafkaConsumer010<String> sensorsValuesConsumer = new FlinkKafkaConsumer010<String>(Topics.SENSORS_VALUES_TOPIC,
                new SimpleStringSchema(),
                sensorValuesConsumerProperties);

        /*  Init control sensor value expressions Kafka source. */
        Properties controlsConsumerSVEProperties = defaultConsumingProperties();
        controlsConsumerSVEProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_FLINK_CONTROL_TOPIC_SVE_GROUP_ID);
        FlinkKafkaConsumer010<String> controlsConsumerSVE = new FlinkKafkaConsumer010<String>(Topics.CONTROL_TOPIC_SVE, new SimpleStringSchema(), controlsConsumerSVEProperties);

        /*  Init control comparison constant value expression Kafka source. */
        Properties controlsConsumerCVEProperties = defaultConsumingProperties();
        controlsConsumerCVEProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_FLINK_CONTROL_TOPIC_CVE_GROUP_ID);
        FlinkKafkaConsumer010<String> controlsConsumerCVE = new FlinkKafkaConsumer010<String>(Topics.CONTROL_TOPIC_CVE, new SimpleStringSchema(), controlsConsumerCVEProperties);

        /* Init control complex expressions Kafka source.   */
        Properties controlsConsumerCEProperties = defaultConsumingProperties();
        controlsConsumerCEProperties.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_FLINK_CONTROL_TOPIC_CE_GROUP_ID);
        FlinkKafkaConsumer010<String> controlsConsumerCE = new FlinkKafkaConsumer010<String>(Topics.CONTROL_TOPIC_CE, new SimpleStringSchema(), controlsConsumerCEProperties);

        FlinkKafkaProducer010<String> kafkaProducer = new FlinkKafkaProducer010(KAFKA_BROKER_CONFIG, Topics.RESULT_TOPIC, new SimpleStringSchema());


        DataStream<Tuple2<String,ControlMessage>> controlStreamSVE = environment.addSource(controlsConsumerSVE)
                .rebalance()
                .map(new MapFunction<String, Tuple2<String, ControlMessage>>() {
                    @Override
                    public Tuple2<String, ControlMessage> map(String json) throws Exception {
                        ControlMessage controlMessage = new ControlMessage();
                        controlMessage.initFromJSON(json);

                        return new Tuple2<>(controlMessage.getExpressionId(), controlMessage);
                    }
                });

        DataStream<Tuple2<String,SensorMessage>> sensorsValuesStream = environment.addSource(sensorsValuesConsumer)
                .rebalance()
                .map(new MapFunction<String, Tuple2<String,SensorMessage>>() {
                    @Override
                    public Tuple2<String,SensorMessage> map(String json) throws Exception {
                        SensorMessage sensorMessage = new SensorMessage();
                        sensorMessage.initFromJSON(json);

                        return new Tuple2<>(Utils.getParentForExpression(sensorMessage.getExpressionId()), sensorMessage);
                    }
                });

        DataStream<Tuple2<String, ConstantCompareControlMessage>> controlStreamCVE = environment.addSource(controlsConsumerCVE)
                .rebalance()
                .map(new MapFunction<String, Tuple2<String, ConstantCompareControlMessage>>() {
                    @Override
                    public Tuple2<String, ConstantCompareControlMessage> map(String json) throws Exception {
                        ConstantCompareControlMessage constantValueCompareMessage = new ConstantCompareControlMessage();
                        constantValueCompareMessage.initFromJSON(json);

                        return new Tuple2<>(constantValueCompareMessage.getExpressionId(), constantValueCompareMessage);
                    }
                });


        DataStream<Tuple2<String, ComplexCompareControlMessage>> controlStreamCE = environment.addSource(controlsConsumerCE)
                .rebalance()
                .map(new MapFunction<String, Tuple2<String, ComplexCompareControlMessage>>() {
                    @Override
                    public Tuple2<String, ComplexCompareControlMessage> map(String json) throws Exception {
                        ComplexCompareControlMessage complexCompareControlMessage = new ComplexCompareControlMessage();
                        complexCompareControlMessage.initFromJSON(json);

                        return new Tuple2<>(complexCompareControlMessage.getExpressionId(), complexCompareControlMessage);
                    }
                });

        ConnectedStreams<Tuple2<String, ControlMessage>, Tuple2<String, SensorMessage>> connectedStreamsSVE = controlStreamSVE
                .connect(sensorsValuesStream)
                .keyBy(0,0);

        if(isLightModeEnabled) {
            /*  Running light topology. */
            connectedStreamsSVE.flatMap(new StreamingProcessFlatMap())
                    .rebalance()
                    .map(new MapFunction<ResultMessage, String>() {
                        @Override
                        public String map(ResultMessage message) throws Exception {
                            return message.toJSON();
                        }
                    })
                    //.addSink(kafkaProducer);
                    .addSink( new FlinkKafkaProducer010(KAFKA_BROKER_CONFIG, Topics.RESULT_TOPIC, new SimpleStringSchema()));
        } else {
            connectedStreamsSVE.process(new CoreProcessFunction())
                    .rebalance()
                    .map(new MapFunction<ResultMessage, String>() {
                        @Override
                        public String map(ResultMessage message) throws Exception {
                            return message.toJSON();
                        }
                    })
                    .addSink(kafkaProducer);
        }

        ConnectedStreams<Tuple2<String, ConstantCompareControlMessage>, Tuple2<String, SensorMessage>> connectedStreamsCVE = controlStreamCVE
                .connect(sensorsValuesStream)
                .keyBy(0,0);

        connectedStreamsCVE.flatMap(new ConstantCompareFlatMap())
                .rebalance()
                .map(new MapFunction<ResultMessage, String>() {
                    @Override
                    public String map(ResultMessage message) throws Exception {
                        return message.toJSON();
                    }
                })
                .addSink(kafkaProducer);

        ConnectedStreams<Tuple2<String, ComplexCompareControlMessage>, Tuple2<String, SensorMessage>> connectedStreamCE = controlStreamCE
                .connect(sensorsValuesStream)
                .keyBy(0,0);

        connectedStreamCE.process(new ComplexCompareProcessFunction())
                .rebalance()
                .map(new MapFunction<ResultMessage, String>() {
                    @Override
                    public String map(ResultMessage message) throws Exception {
                        return message.toJSON();
                    }
                })
                .addSink(new FlinkKafkaProducer010(KAFKA_BROKER_CONFIG, Topics.RESULT_TOPIC, new SimpleStringSchema()));

        environment.execute(JOB_NAME);
    }
}
