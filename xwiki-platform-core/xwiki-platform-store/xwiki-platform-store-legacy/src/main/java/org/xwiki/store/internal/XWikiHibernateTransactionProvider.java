/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.store.internal;

import com.xpn.xwiki.XWikiContext;
import javax.inject.Inject;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.store.XWikiTransactionProvider;
import org.xwiki.store.XWikiTransaction;
import org.xwiki.store.StartableTransactionRunnable;

/**
 * A provider for acquiring transaction based on XWikiHibernateStore.
 * This is the default provider because XWikiHibernateStore is the default storage component.
 *
 * @version $Id$
 * @since 3.2M1
 */
@Component
public class XWikiHibernateTransactionProvider implements XWikiTransactionProvider
{
    /** The means of getting the XWikiContext. */
    @Inject
    private Execution exec;

    /**
     * {@inheritDoc}
     *
     * @see XWikiTransactionProvider#get()
     */
    public StartableTransactionRunnable<XWikiTransaction> get()
    {
        final XWikiContext context = (XWikiContext) this.exec.getContext().getProperty("xwiki-context");
        return new XWikiHibernateTransaction(context);
    }
}
