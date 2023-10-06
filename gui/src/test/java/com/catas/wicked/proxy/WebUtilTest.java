package com.catas.wicked.proxy;

import com.catas.wicked.common.util.WebUtils;
import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;

public class WebUtilTest {

    @Test
    public void testSplit() throws MalformedURLException {
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com"),
                Arrays.asList("http://google.com", "<Default>"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com/"),
                Arrays.asList("http://google.com", "<Default>"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com", false),
                Arrays.asList("http://google.com"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com/", false),
                Arrays.asList("http://google.com"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com/?page=1"),
                Arrays.asList("http://google.com", "?page=1"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://www.google.com/page/1"),
                Arrays.asList("http://www.google.com", "page", "1"));
        Assert.assertEquals(
                WebUtils.getPathSplits("http://google.com/index/1/page?name=nq&host=111"),
                Arrays.asList("http://google.com", "index", "1", "page?name=nq&host=111"));

    }

    @Test
    public void testParseQueryParam() {
        String query = "param1=value1&param2=value2&&name=jack&";
        Map<String, String> params = WebUtils.parseQueryParams(query);
        Assert.assertEquals("value1", params.get("param1"));
        Assert.assertEquals("value2", params.get("param2"));
        Assert.assertEquals("jack", params.get("name"));
    }
}
