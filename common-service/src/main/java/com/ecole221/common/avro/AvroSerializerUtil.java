package com.ecole221.common.avro;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializerUtil {

    public static <T extends SpecificRecordBase> byte[] toBytes(T avro) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            SpecificDatumWriter<T> writer = new SpecificDatumWriter<>(avro.getSchema());
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(avro, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur serialisation AVRO", e);
        }
    }

    public static <T extends SpecificRecordBase> T fromBytes(byte[] data, Class<T> clazz) {
        try {
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(clazz);
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Erreur désérialisation AVRO", e);
        }
    }
}
