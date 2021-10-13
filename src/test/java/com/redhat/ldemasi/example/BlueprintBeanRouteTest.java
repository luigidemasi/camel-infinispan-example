package com.redhat.ldemasi.example;

import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Dictionary;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BlueprintBeanRouteTest extends CamelBlueprintTestSupport {
	
    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint-bean.xml";
    }
    protected static String keyOne;
    protected static String keyTwo;
    protected static String keyThree;
    protected static String valueOne;
    protected static String valueTwo;
    protected static String valueThree;
    protected BasicCacheContainer basicCacheContainer;



    protected String getCamelContextId() {
        return "blueprint-bean-context";
    }

    public BasicCache<Object, Object> currentCache() {
        if (getCacheName() != null) {
            return basicCacheContainer.getCache(getCacheName());
        }
        return basicCacheContainer.getCache();
    }

    private String getCacheName() {
        return null;
    }

    @Before
    public void setupTestValues() {
        keyOne = getRandomString("keyOne");
        keyTwo = getRandomString("keyTwo");
        keyThree = getRandomString("keyThree");
        valueOne = getRandomString("valueOne");
        valueTwo = getRandomString("valueTwo");
        valueThree = getRandomString("valueThree");
        basicCacheContainer = (BasicCacheContainer) context.getRegistry().lookupByName("cacheManager");
    }

    @After
    public void clearCacheContainer() {
        currentCache().clear();
    }

    protected String getRandomString(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString();
    }


    @Test
    public void consumerReceivedEntryRemoveEventNotifications() throws Exception {
        currentCache().put(keyOne, valueOne);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        final MockEndpoint mock = prepareMockAssertions("remove", "CACHE_ENTRY_REMOVED");

        currentCache().remove(keyOne);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        mock.assertIsSatisfied();
    }

    @Test
    public void consumerReceivedEntryCreatedEventNotifications() throws Exception {
        final MockEndpoint mock = prepareMockAssertions("create", "CACHE_ENTRY_CREATED");

        currentCache().put(keyOne, valueOne);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        mock.assertIsSatisfied();
    }

    @Test
    public void consumerReceivedEntryUpdateEventNotifications() throws Exception {
        currentCache().put(keyOne, valueOne);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));

        final MockEndpoint mock = prepareMockAssertions("update", "CACHE_ENTRY_MODIFIED");

        currentCache().replace(keyOne, valueTwo);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        mock.assertIsSatisfied();
    }

    @Test
    public void consumerReceivedEntryVisitedEventNotifications() throws Exception {
        currentCache().put(keyOne, valueOne);

        final MockEndpoint mock = prepareMockAssertions("visit", "CACHE_ENTRY_VISITED");

        currentCache().get(keyOne);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        mock.assertIsSatisfied();
    }

    protected MockEndpoint prepareMockAssertions(String mockName, String eventType) {
        final MockEndpoint mock = context.getEndpoint("mock:" + mockName, MockEndpoint.class);
        mock.reset();
        mock.expectedMessageCount(2);
        setMockAssertion(mock, 0, eventType, true);
        setMockAssertion(mock, 1, eventType, false);
        return mock;
    }
    protected void setMockAssertion(MockEndpoint mock, int msgIndex, String eventType, boolean isPre) {
        mock.message(msgIndex).outHeader(InfinispanConstants.EVENT_TYPE).isEqualTo(eventType);
        mock.message(msgIndex).outHeader(InfinispanConstants.IS_PRE).isEqualTo(isPre);
        mock.message(msgIndex).outHeader(InfinispanConstants.CACHE_NAME).isNotNull();
        mock.message(msgIndex).outHeader(InfinispanConstants.KEY).isEqualTo(keyOne);
    }


}
