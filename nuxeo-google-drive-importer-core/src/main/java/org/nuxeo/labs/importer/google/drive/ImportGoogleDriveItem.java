package org.nuxeo.labs.importer.google.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
@Operation(
        id = ImportGoogleDriveItem.ID,
        category = Constants.CAT_DOCUMENT,
        label = "Google Drive Item Importer",
        description = "Import a Google Drive Item by ID in Nuxeo")
public class ImportGoogleDriveItem {

    public static final String ID = "Document.ImportGoogleDriveItem";

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Param(name = "itemId", required = true)
    protected String itemId;

    @Param(name = "providerId", required = false)
    protected String providerId = "googledrive-importer";

    @Param(name = "batchSize", required = false)
    protected long batchSize = 10;

    protected int counter=0;

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {
        GoogleOAuth2ServiceProvider oAuth2ServiceProvider =
                (GoogleOAuth2ServiceProvider) Framework.getService(OAuth2ServiceProviderRegistry.class).getProvider(providerId);
        String username = session.getPrincipal().getName();
        String serviceUsername = oAuth2ServiceProvider.getServiceUser(username);
        Credential credential = oAuth2ServiceProvider.loadCredential(serviceUsername);
        if (credential == null) {
            String message = "No credentials found for user " + username + " and service " +
                    providerId;
            throw new NuxeoException(message);
        }
        Long expiresInSeconds = credential.getExpiresInSeconds();
        if (expiresInSeconds != null && expiresInSeconds.longValue() <= 0) {
            credential.refreshToken();
        }

        HttpTransport httpTransport = credential.getTransport();
        JsonFactory jsonFactory = credential.getJsonFactory();
        Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Nuxeo")
                .build();

        File file = drive.files().get(itemId).execute();
        importFile(drive, file, input);
        return input;
    }

    public void importFolder(Drive drive, File file, DocumentModel root) throws IOException {
        DocumentModel folder = fileManager.createFolder(
                session,file.getTitle(),root.getPathAsString(),true);
        incrementDocumentCounter();
        FileList children = drive.files().list().setQ(String.format("'%s' in parents",file.getId())).execute();
        for(File child: children.getItems()) {
            importFile(drive,child,folder);
        }
    }

    public void importFile(Drive drive, File file, DocumentModel root) throws IOException {
        if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
            importFolder(drive, file, root);
            return;
        }

        String mimetype = file.getMimeType();
        if (mimetype.startsWith("application/vnd.google-apps.")) {
            //todo upgrade to V3 API with more recent SDK (current one is from 2015)
            return;
        }

        InputStream in = drive.files().get(file.getId()).executeMediaAsInputStream();


        Blob blob = new FileBlob(in, file.getMimeType());
        blob.setFilename(file.getTitle());

        FileImporterContext context = FileImporterContext.builder(session,
                blob, root.getPathAsString())
                .overwrite(true)
                .build();
        fileManager.createOrUpdateDocument(context);

        incrementDocumentCounter();
    }

    protected void incrementDocumentCounter() {
        counter++;
        if (counter % batchSize == 0) {
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }
}
