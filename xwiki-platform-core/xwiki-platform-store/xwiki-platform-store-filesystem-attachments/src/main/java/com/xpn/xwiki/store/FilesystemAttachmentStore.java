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
package com.xpn.xwiki.store;

import java.io.File;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.FilesystemAttachmentContent;
import com.xpn.xwiki.doc.ListAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import javax.inject.Inject;
import javax.inject.Named;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;
import org.xwiki.store.FileSaveTransactionRunnable;
import org.xwiki.store.FileDeleteTransactionRunnable;
import org.xwiki.store.StreamProvider;
import org.xwiki.store.TransactionRunnable;
import org.xwiki.store.XWikiTransactionProvider;
import org.xwiki.store.XWikiTransaction;
import org.xwiki.store.StartableTransactionRunnable;


/**
 * Filesystem based implementation of XWikiAttachmentStoreInterface.
 * 
 * @version $Id$
 * @since 3.0M2
 */
@Component("file")
public class FilesystemAttachmentStore implements XWikiAttachmentStoreInterface
{
    /**
     * Tools for getting files to store given content in.
     */
    @Inject
    private FilesystemStoreTools fileTools;

    /**
     * A means of getting a transaction to run the attachment save operation in.
     */
    @Named("configured")
    @Inject
    private XWikiTransactionProvider provider;

    /**
     * Testing Constructor.
     *
     * @param fileTools tools for getting files to store given content in and locks.
     * @param provider the means to get a transaction to run the store operation in.
     */
    public FilesystemAttachmentStore(final FilesystemStoreTools fileTools,
                                     final XWikiTransactionProvider provider)
    {
        this.fileTools = fileTools;
        this.provider = provider;
    }

    /** Constructor for component manager. */
    public FilesystemAttachmentStore()
    {
    }

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter
     * whether bTransaction is true or false.
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentContent(
     *          XWikiAttachment, XWikiContext, boolean)
     */
    public void saveAttachmentContent(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        this.saveAttachmentContent(attachment, true, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter
     * whether bTransaction is true or false.
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentContent(
     *          XWikiAttachment, boolean, XWikiContext, boolean)
     */
    public void saveAttachmentContent(final XWikiAttachment attachment,
                                      final boolean updateDocument,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        final StartableTransactionRunnable transaction = this.provider.get();
        this.getAttachmentContentSaveRunnable(attachment, updateDocument, context).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            throw new RuntimeException("Exception while saving attachment.", e);
        }
    }

    /**
     * Get a TransactionRunnable for saving the attachment content.
     * If {@link XWikiAttachment#getAttachment_content()} yields null, this runnable will do nothing.
     *
     * @param attachment the XWikiAttachment whose content should be saved.
     * @param updateDocument whether or not to update the document at the same time.
     * @param context the XWikiContext for the request.
     * @return a TransactionRunnable for saving the attachment content in an XWikiTransaction.
     * @throws XWikiException if thrown by AttachmentSaveTransactionRunnable()
     */
    private TransactionRunnable<XWikiTransaction> getAttachmentContentSaveRunnable(
        final XWikiAttachment attachment,
        final boolean updateDocument,
        final XWikiContext context)
        throws XWikiException
    {
        final XWikiAttachmentContent content = attachment.getAttachment_content();

        if (content == null) {
            // If content does not exist we should not blank the attachment.
            return new TransactionRunnable<XWikiTransaction>();
        }

        // This is the permanent location where the attachment content will go.
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment).getAttachmentContentFile();

        return new AttachmentSaveTransactionRunnable(attachment,
                                                     updateDocument,
                                                     context,
                                                     attachFile,
                                                     this.fileTools.getTempFile(attachFile),
                                                     this.fileTools.getBackupFile(attachFile),
                                                     this.fileTools.getLockForFile(attachFile));
    }

    /**
     * {@inheritDoc}
     * This implementation cannot operate in a larger transaction so it starts a new transaction no matter
     * whether bTransaction is true or false.
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#saveAttachmentsContent(
     *          List, XWikiDocument, boolean, XWikiContext, boolean)
     */
    public void saveAttachmentsContent(final List<XWikiAttachment> attachments,
                                       final XWikiDocument doc,
                                       final boolean updateDocument,
                                       final XWikiContext context,
                                       final boolean bTransaction) throws XWikiException
    {
        if (attachments == null || attachments.size() == 0) {
            return;
        }

        try {
            final StartableTransactionRunnable transaction = this.provider.get();

            for (XWikiAttachment attach : attachments) {
                this.getAttachmentContentSaveRunnable(attach, false, context).runIn(transaction);
            }

            // Save the parent document only once.
            if (updateDocument) {
                new TransactionRunnable<XWikiTransaction>() {
                    protected void onRun() throws Exception
                    {
                        context.getWiki().getStore().saveXWikiDoc(doc, context, false);
                    }
                } .runIn(transaction);
            }

            transaction.start();

        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                                     "Exception while saving attachments", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#loadAttachmentContent(
     *          XWikiAttachment, XWikiContext, boolean)
     */
    public void loadAttachmentContent(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment).getAttachmentContentFile();

        if (attachFile.exists()) {
            attachment.setAttachment_content(
                new FilesystemAttachmentContent(attachFile,
                                                attachment,
                                                this.fileTools.getLockForFile(attachFile)));
            return;
        }

        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                 XWikiException.ERROR_XWIKI_STORE_FILENOTFOUND,
                                 "The attachment could not be found in the filesystem attachment store.\n"
                                 + "This can happen if attachment storage is switched from database to "
                                 + "filesystem without first moving all of the database attachments over "
                                 + "to the filesystem using a script.");
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#deleteXWikiAttachment(
     *          XWikiAttachment, XWikiContext, boolean)
     */
    public void deleteXWikiAttachment(final XWikiAttachment attachment,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        this.deleteXWikiAttachment(attachment, true, context, bTransaction);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#deleteXWikiAttachment(
     *          XWikiAttachment, boolean, XWikiContext, boolean)
     */
    public void deleteXWikiAttachment(final XWikiAttachment attachment,
                                      final boolean parentUpdate,
                                      final XWikiContext context,
                                      final boolean bTransaction)
        throws XWikiException
    {
        final StartableTransactionRunnable<XWikiTransaction> transaction = this.provider.get();
        this.getAttachmentDeleteRunnable(attachment, parentUpdate, context).runIn(transaction);
        try {
            transaction.start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                                     XWikiException.ERROR_XWIKI_UNKNOWN,
                                     "Exception while deleting attachment.", e);
        }
    }

    /**
     * Get a TransactionRunnable for deleting an attachment.
     *
     * @param attachment the XWikiAttachment to delete.
     * @param updateDocument whether or not to update the document at the same time.
     * @param context the XWikiContext for the request.
     * @return a TransactionRunnable for deleting the attachment which must be run inside of an
     *                               XWikiTransaction
     * @throws XWikiException if unable to load the attachment archive to delete.
     */
    private TransactionRunnable<XWikiTransaction> getAttachmentDeleteRunnable(
        final XWikiAttachment attachment,
        final boolean updateDocument,
        final XWikiContext context)
        throws XWikiException
    {
        final File attachFile =
            this.fileTools.getAttachmentFileProvider(attachment).getAttachmentContentFile();

        return new AttachmentDeleteTransactionRunnable(attachment,
                                                       updateDocument,
                                                       context,
                                                       attachFile,
                                                       this.fileTools.getBackupFile(attachFile),
                                                       this.fileTools.getLockForFile(attachFile));
    }


    /**
     * {@inheritDoc}
     * This implementation does nothing.
     *
     * @see com.xpn.xwiki.store.XWikiAttachmentStoreInterface#cleanUp(XWikiContext)
     */
    public void cleanUp(XWikiContext context)
    {
        // Do nothing.
    }

    /* ---------------------------- Nested Classes. ---------------------------- */

    /**
     * A TransactionRunnable for saving an attachment.
     */
    private static class AttachmentSaveTransactionRunnable extends TransactionRunnable<XWikiTransaction>
    {
        /**
         * Construct a TransactionRunnable for saving the attachment content.
         *
         * @param attachment the XWikiAttachment whose content should be saved.
         * @param updateDocument whether or not to update the document at the same time.
         * @param context the XWikiContext for the request.
         * @param attachFile the File to store the attachment in.
         * @param tempFile the File to put the attachment content in until the transaction is complete.
         * @param backupFile the File to backup the content of the existing attachment in.
         * @param lock this Lock will be locked while the attachment file is being written to.
         * @throws XWikiException if thrown by {@link XWikiAttachment#updateContentArchive(XWikiContext)}
         *         or {@link FilesystemAttachmentVersioningStore#
         *                getArchiveSaveRunnable(XWikiAttachmentArchive, XWikiContext)
         */
        public AttachmentSaveTransactionRunnable(final XWikiAttachment attachment,
                                                 final boolean updateDocument,
                                                 final XWikiContext context,
                                                 final File attachFile,
                                                 final File tempFile,
                                                 final File backupFile,
                                                 final ReadWriteLock lock)
            throws XWikiException
        {
            final StreamProvider provider = new AttachmentContentStreamProvider(attachment, context);
            new FileSaveTransactionRunnable(attachFile, tempFile, backupFile, lock, provider).runIn(this);

            // If the versioning store supports TransactionRunnable then use it, otherwise don't.
            final AttachmentVersioningStore avs = context.getWiki().getAttachmentVersioningStore();
            final XWikiAttachmentArchive archive = attachment.getAttachment_archive();
            if (avs instanceof FilesystemAttachmentVersioningStore) {
                final FilesystemAttachmentVersioningStore favs = (FilesystemAttachmentVersioningStore) avs;

                // If first save then create a new archive.
                if (archive == null) {
                    favs.getArchiveSaveRunnable(new ListAttachmentArchive(attachment), context).runIn(this);
                } else {
                    favs.getArchiveSaveRunnable(archive, context).runIn(this);
                }
            } else {
                new TransactionRunnable<XWikiTransaction>() {
                    protected void onRun() throws XWikiException
                    {
                        avs.saveArchive(archive, context, false);
                    }
                } .runIn(this);
            }

            // If updating of the parent document is required then add a TransactionRunnable to do that.
            if (updateDocument) {
                final XWikiStoreInterface store = context.getWiki().getStore();
                final XWikiDocument doc = attachment.getDoc();
                new TransactionRunnable<XWikiTransaction>() {
                    protected void onRun() throws XWikiException
                    {
                        store.saveXWikiDoc(doc, context, false);
                    }
                } .runIn(this);
            }
        }
    }

    /**
     * A TransactionRunnable for deleting an attachment.
     */
    private static class AttachmentDeleteTransactionRunnable extends TransactionRunnable<XWikiTransaction>
    {
        /**
         * Construct a TransactionRunnable for deleting the attachment.
         *
         * @param attachment the XWikiAttachment to delete
         * @param updateDocument whether or not to update the document at the same time.
         * @param context the XWikiContext for the request.
         * @param attachFile the file to where the attachment content is stored.
         * @param tempFile the file to to move the attachment content to temporarily.
         * @param lock this Lock will be locked while the attachment file is being written to.
         * @throws XWikiException if unable to load the archive for the attachment to delete.
         */
        public AttachmentDeleteTransactionRunnable(final XWikiAttachment attachment,
                                                   final boolean updateDocument,
                                                   final XWikiContext context,
                                                   final File attachFile,
                                                   final File tempFile,
                                                   final ReadWriteLock lock)
            throws XWikiException
        {
            new FileDeleteTransactionRunnable(attachFile, tempFile, lock).runIn(this);

            // If the store supports deleting in the same transaction then do it.
            final AttachmentVersioningStore avs = context.getWiki().getAttachmentVersioningStore();

            if (avs instanceof FilesystemAttachmentVersioningStore) {
                final FilesystemAttachmentVersioningStore favs = (FilesystemAttachmentVersioningStore) avs;
                favs.getArchiveDeleteRunnable(attachment.loadArchive(context)).runIn(this);
            } else {
                new TransactionRunnable<XWikiTransaction>() {
                    protected void onRun() throws XWikiException
                    {
                        avs.deleteArchive(attachment, context, false);
                    }
                } .runIn(this);
            }

            // Update the document if required.
            if (updateDocument) {
                final String filename = attachment.getFilename();
                final List<XWikiAttachment> list = attachment.getDoc().getAttachmentList();
                new TransactionRunnable<XWikiTransaction>() {
                    protected void onRun() throws XWikiException
                    {
                        for (int i = 0; i < list.size(); i++) {
                            if (filename.equals(list.get(i).getFilename())) {
                                list.remove(i);
                                break;
                            }
                        }
                        context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(),
                                                                  context,
                                                                  false);
                    }
                } .runIn(this);
            }
        }
    }
}
