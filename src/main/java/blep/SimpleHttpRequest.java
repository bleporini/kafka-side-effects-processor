package blep;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class SimpleHttpRequest implements WithRuntimeExceptionWrapper {
    public SimpleHttpRequest(
           @JsonProperty("url") String url,
           @JsonProperty("method") Method method,
           @JsonProperty("body") String body,
           @JsonProperty("headers") Map<String, String> headers) {
        this.url = url;
        this.method = method;
        this.body = body;
        this.headers = headers;
    }

    public enum Method {
        GET{
            @Override
            public HttpRequest.Builder buildRequest(SimpleHttpRequest httpRequest) {
                return HttpRequest.newBuilder()
                        .GET();
            }
        },POST {
            @Override
            public HttpRequest.Builder buildRequest(SimpleHttpRequest httpRequest) {
                return HttpRequest.newBuilder()
                        .POST(
                                HttpRequest.BodyPublishers.ofString(httpRequest.body)
                        );
            }
        },PUT {
            @Override
            public HttpRequest.Builder buildRequest(SimpleHttpRequest httpRequest) {
                return HttpRequest.newBuilder()
                        .PUT(
                                HttpRequest.BodyPublishers.ofString(httpRequest.body)
                        );
            }
        },DELETE {
            @Override
            public HttpRequest.Builder buildRequest(SimpleHttpRequest httpRequest) {
                return HttpRequest.newBuilder()
                        .DELETE();
            }
        };


        public abstract HttpRequest.Builder buildRequest(SimpleHttpRequest httpRequest);

    }

    private final String url;

    private final Method method;

    private final String body;

    private final Map<String, String> headers;

    private URI uri(){
        return toUnchecked(() -> new URI(url));
    }

    public HttpRequest asJavaHttpRequest() {
        return io.vavr.collection.HashMap.ofAll(headers != null ? headers : new HashMap<>())
                .foldLeft(
                        method.buildRequest(this),
                        (builder, entry) -> builder.header(entry._1, entry._2)
                ).uri(uri())
                .build();
    }

}
