package com.think123.code.log;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpHost;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * 实际封装还可以考虑异步收集，批量收集、失败重试等功能完善
 */
@Plugin(name = "EsAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class EsAppender extends AbstractAppender {

    private static String esHost;
    private static int port;
    private static String indexName;
    private static String projectName;
    private static String projectType;


    public EsAppender(String name, Filter filter, Layout<? extends Serializable> layout,
            boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @PluginFactory
    public static EsAppender createAppender(@PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") Filter filter,
            @PluginAttribute("esHost") String esHost,
            @PluginAttribute(value = "port", defaultInt = 9002) int port,
            @PluginAttribute("indexName") String indexName,
            @PluginAttribute(value = "projectName", defaultString = "") String projectName,
            @PluginAttribute(value = "projectType", defaultString = "") String projectType) {

        if (name == null) {
            LOGGER.error("No name provided for ESAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        EsAppender.esHost = esHost;
        EsAppender.port = port;
        EsAppender.indexName = indexName;
        EsAppender.projectName = projectName;
        EsAppender.projectType = projectType;

        return new EsAppender(name, filter, layout, true);

    }

    @Override
    public void append(LogEvent event) {

        Map<String, Object> map = new HashMap<>();

        map.put("projectName", projectName);
        map.put("projectType", projectType);
        map.put("className", event.getLoggerName());
        map.put("methodName", event.getSource().getMethodName());
        map.put("message", event.getMessage().getFormattedMessage());

        try {
            map.put("ip", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ThrowableProxy thrownProxy = event.getThrownProxy();

        map.put("logLevel", event.getLevel().name());
        map.put("logThread", event.getThreadName());
        if (null != thrownProxy) {
            map.put("errorMsg", thrownProxy.getMessage());
            map.put("exception", thrownProxy.getName());
            map.put("stackTrace",
                    parseException(thrownProxy.getStackTrace()));
        }
        // 这里面会放置MDC中的值，比如requestId
        Map contextData = event.getContextData().toMap();
        map.putAll(contextData);

        RestHighLevelClient esClient = getEsRestClient();

        IndexRequest request = new IndexRequest(indexName);
        request.source(map, XContentType.JSON);

        try {
            esClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            LOGGER.error(e);
        }


    }

    public String parseException(StackTraceElement[] stackTrace) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        Arrays.stream(stackTrace).forEach(
                (e) -> sb.append(e.getClassName()).append(".").append(e.getMethodName()).append("(")
                        .append(e.getFileName()).append(":").append(e.getLineNumber()).append(")")
                        .append("\n")
        );
        return sb.toString();
    }

    public RestHighLevelClient getEsRestClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(esHost, port))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectTimeout(3000)
                        .setSocketTimeout(5000)
                        .setConnectionRequestTimeout(1000));
        return new RestHighLevelClient(builder);
    }


}


