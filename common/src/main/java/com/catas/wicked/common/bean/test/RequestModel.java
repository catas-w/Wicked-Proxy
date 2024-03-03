package com.catas.wicked.common.bean.test;

import com.catas.wicked.common.bean.StrPair;
import com.catas.wicked.common.constant.GeneralContentType;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Data;

import java.util.List;

/**
 * Request data for mocking
 */
@Data
public class RequestModel {

    private String protocol;

    private String url;

    private HttpMethod method;

    private List<StrPair> queryParams;

    private List<StrPair> headers;

    private GeneralContentType contentType;

    private JsonNode content;

    private ExpectModel expect;
}
