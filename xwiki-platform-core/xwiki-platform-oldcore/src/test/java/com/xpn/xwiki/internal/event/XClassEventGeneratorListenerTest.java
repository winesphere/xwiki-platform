package com.xpn.xwiki.internal.event;

import java.util.Arrays;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link XObjectEventGeneratorListener}.
 * 
 * @version $Id$
 */
public class XClassEventGeneratorListenerTest extends AbstractBridgedComponentTestCase
{
    private ObservationManager observation;

    private XWiki xwiki;

    private XWikiDocument document;

    private XWikiDocument documentOrigin;
    
    private EventListener listener;
    
    private BaseClass xclass;
    
    private BaseClass xclassOrigin;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.observation = getComponentManager().lookup(ObservationManager.class);

        // Remove wiki macro listener which is useless and try to load documents from database
        this.observation.removeListener("wikimacrolistener");

        this.xwiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.xwiki);
        
        this.listener = getMockery().mock(EventListener.class);

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.documentOrigin = new XWikiDocument(this.document.getDocumentReference());
        this.document.setOriginalDocument(this.documentOrigin);
        
        this.xclass = this.document.getXClass();
        this.xclassOrigin = this.documentOrigin.getXClass();
        
        getMockery().checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            //allowing(xwiki).getXClass(xclass.getDocumentReference(), getContext()); will(returnValue(xclass));
        }});
    }

    @Test
    public void testAddDocument()
    {
        this.xclass.addTextField("property", "Property", 30);

        final Event event = new XClassPropertyAddedEvent(this.xclass.getField("property").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentCreatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testDeleteDocument()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);

        final Event event = new XClassPropertyDeletedEvent(this.xclassOrigin.getField("property").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentDeletedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXClassPropertyAdded()
    {
        this.xclass.addTextField("property", "Property", 30);

        final Event event = new XClassPropertyAddedEvent(this.xclass.getField("property").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);

        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }

    @Test
    public void testModifiedDocumentXClassPropertyDeleted()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);

        final Event event = new XClassPropertyDeletedEvent(this.xclassOrigin.getField("property").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }
    
    @Test
    public void testModifiedDocumentXClassPropertyModified()
    {
        this.xclassOrigin.addTextField("property", "Property", 30);
        this.xclass.addTextField("property", "New Property", 30);

        final Event event = new XClassPropertyUpdatedEvent(this.xclassOrigin.getField("property").getReference());

        getMockery().checking(new Expectations() {{
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(with(equal(event)), with(same(document)), with(same(getContext())));
        }});
        this.observation.addListener(this.listener);
 
        this.observation.notify(new DocumentUpdatedEvent(this.document.getDocumentReference()), this.document,
            getContext());
    }
}
