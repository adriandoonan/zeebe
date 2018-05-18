package io.zeebe.client.impl.data;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class MsgpackPayloadModule extends SimpleModule
{
    private static final long serialVersionUID = 1L;

    public MsgpackPayloadModule(MsgPackConverter msgPackConverter)
    {
        addSerializer(PayloadField.class, new MsgpackPayloadSerializer());
        addDeserializer(PayloadField.class, new MsgpackPayloadDeserializer(msgPackConverter));
    }

    class MsgpackPayloadSerializer extends StdSerializer<PayloadField>
    {

        private static final long serialVersionUID = 1L;

        protected MsgpackPayloadSerializer()
        {
            this(null);
        }

        protected MsgpackPayloadSerializer(Class<PayloadField> t)
        {
            super(t);
        }

        @Override
        public void serialize(PayloadField value, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeBinary(value.getMsgPack());
        }
    }

    class MsgpackPayloadDeserializer extends StdDeserializer<PayloadField>
    {
        private static final long serialVersionUID = 1L;

        private MsgPackConverter msgPackConverter;

        protected MsgpackPayloadDeserializer(MsgPackConverter msgPackConverter)
        {
            this((Class<?>) null);
            this.msgPackConverter = msgPackConverter;
        }

        protected MsgpackPayloadDeserializer(Class<?> vc)
        {
            super(vc);
        }

        @Override
        public PayloadField deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException
        {
            final byte[] msgpackPayload = p.getBinaryValue();

            final PayloadField payload = new PayloadField(msgPackConverter);
            payload.setMsgPack(msgpackPayload);

            return payload;
        }
    }

}
