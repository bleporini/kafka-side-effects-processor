package blep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

public class TryableTest {

    @Test
    public void name() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Tryable<String> test = Tryable.TryOnce.build("test");
        byte[] bytes = mapper.writeValueAsBytes(test);

        System.out.println("new String(bytes) = " + new String(bytes));

        Tryable tryable = mapper.readValue(bytes, Tryable.class);

    }
}