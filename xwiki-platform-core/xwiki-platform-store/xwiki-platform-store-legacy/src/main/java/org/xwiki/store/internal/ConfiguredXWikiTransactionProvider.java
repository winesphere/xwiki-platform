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

import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import javax.inject.Inject;
import org.xwiki.context.Execution;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.store.XWikiTransaction;
import org.xwiki.store.XWikiTransactionProvider;
import org.xwiki.store.StartableTransactionRunnable;


/**
 * XWikiTransactionProvider which provides the transaction type given by the xwiki.cfg config file.
 * 
 * @version $Id$
 * @since 3.2M1
 */
@Component("configured")
public class ConfiguredXWikiTransactionProvider implements XWikiTransactionProvider, Initializable
{
    /** A means to get the xwiki.cfg configured hint. */
    @Inject
    private Execution exec;

    /** A map of all providers. */
    @Inject
    private Map<String, XWikiTransactionProvider> providers;

    /** The provider which will be wrapped, chosen by examining configuration. */
    private XWikiTransactionProvider provider;

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        final XWikiContext context = (XWikiContext) this.exec.getContext().getProperty("xwiki-context");
        final String hint = context.getWiki().Param("xwiki.store.main.hint");
        final XWikiTransactionProvider prov = providers.get(hint);
        if (prov == null) {
            throw new InitializationException("Could not find provider specified in xwiki.cfg "
                                              + "xwiki.store.main.hint, check that there is actually a "
                                              + "transaction provider by that name.");
        }
        if (prov == this) {
            throw new InitializationException("The provider specified in xwiki.cfg "
                                              + "xwiki.store.main.hint, is the same hint as the provider "
                                              + "which defers to the hint. This would cause an "
                                              + "infinite loop.");
        }
        this.provider = prov;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.store.XWikiTransactionProvider#get()
     */
    public StartableTransactionRunnable<XWikiTransaction> get()
    {
        return this.provider.get();
    }
}
