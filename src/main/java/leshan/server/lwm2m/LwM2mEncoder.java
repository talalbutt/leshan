package leshan.server.lwm2m;

import java.io.UnsupportedEncodingException;

import leshan.server.lwm2m.message.server.CreatedResponse;
import leshan.server.lwm2m.message.server.MessageEncoder;

import org.apache.mina.coap.CoapCode;
import org.apache.mina.coap.CoapMessage;
import org.apache.mina.coap.CoapOption;
import org.apache.mina.coap.CoapOptionType;
import org.apache.mina.coap.MessageType;

public class LwM2mEncoder implements MessageEncoder {

    @Override
    public CoapMessage encode(CreatedResponse message) {

        CoapOption[] options = new CoapOption[message.getNewLocation().length];
        for (int i = 0; i < message.getNewLocation().length; i++) {
            try {
                options[i] = new CoapOption(CoapOptionType.LOCATION_PATH, message.getNewLocation()[i].getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return new CoapMessage(1, MessageType.ACK, CoapCode.CREATED.getCode(), message.getId(), null, options, null);
    }

}
