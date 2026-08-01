package gr.larryngton.demise.utils.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import okhttp3.HttpUrl;

import java.io.IOException;

@Getter
@ToString
@AllArgsConstructor
public final class HttpException extends IOException {
    private final String method;
    private final HttpUrl url;
    private final int code;
    private final String body;
}
