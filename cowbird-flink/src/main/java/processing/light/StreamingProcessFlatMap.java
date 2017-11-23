package processing.light;

import cowbird.flink.common.messages.control.ControlMessage;
import cowbird.flink.common.messages.result.ResultMessage;
import cowbird.flink.common.messages.sensor.SensorMessage;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.util.Collector;

import stream.LightStreamState;


public class StreamingProcessFlatMap extends RichCoFlatMapFunction <Tuple2<String, ControlMessage>, Tuple2<String, SensorMessage>, ResultMessage> {

    static final String STREAM_STATE_VALUE_STATE_DESCRIPTOR_NAME = "STREAM_STATE_VALUE_STATE_DESCRIPTOR_NAME";
    static final String CONTROL_MESSAGE_VALUE_STATE_DESCRIPTOR_NAME = "CONTROL_MESSAGE_VALUE_STATE_DESCRIPTOR_NAME";
    
    private transient ValueState<LightStreamState> streamStateValueState;

    private transient ValueState<ControlMessage> controlMessageValueState;

    @Override
    public void flatMap1(Tuple2<String, ControlMessage> value, Collector<ResultMessage> out) throws Exception {
        ControlMessage message = controlMessageValueState.value();

        if (message == null) {
            /*  Initializing Control request.   */
            controlMessageValueState.update(value.f1);
        } else {
            /*  Clear the status(es).   */
            /*  Unregister operation.   */
            controlMessageValueState.clear();
            streamStateValueState.clear();
        }

    }


    @Override
    public void flatMap2(Tuple2<String, SensorMessage> value, Collector<ResultMessage> out) throws Exception {
        ControlMessage controlMessage = controlMessageValueState.value();
        LightStreamState currentState = streamStateValueState.value();

        if (controlMessage == null) {
            return;
        }

        SensorMessage sensorMessage = value.f1;

        if (currentState == null) {
            currentState = new LightStreamState(controlMessage);
        }

        currentState.add(sensorMessage);

        if (sensorMessage.getEventTime() - currentState.getFirtElementTimeStamp() >= controlMessage.getHistoryLength()) {

            Object resultValue = currentState.applyReduction();

            if (resultValue != null) {
                ResultMessage resultMessage = new ResultMessage(currentState.getExpressionId());

                resultMessage.setValue(resultValue);

                out.collect(resultMessage);
            }

            currentState.toDefaultState();
        }

        streamStateValueState.update(currentState);
    }


    @Override
    public void open(Configuration parameters) throws Exception {
        // super.open(parameters);
        ValueStateDescriptor<LightStreamState> streamStateValueStateDescriptor = new ValueStateDescriptor<LightStreamState>(STREAM_STATE_VALUE_STATE_DESCRIPTOR_NAME, LightStreamState.class);
        streamStateValueState = getRuntimeContext().getState(streamStateValueStateDescriptor);

        ValueStateDescriptor<ControlMessage> controlMessageValueStateDescriptor = new ValueStateDescriptor<ControlMessage>(CONTROL_MESSAGE_VALUE_STATE_DESCRIPTOR_NAME, ControlMessage.class);
        controlMessageValueState = getRuntimeContext().getState(controlMessageValueStateDescriptor);
    }
}
