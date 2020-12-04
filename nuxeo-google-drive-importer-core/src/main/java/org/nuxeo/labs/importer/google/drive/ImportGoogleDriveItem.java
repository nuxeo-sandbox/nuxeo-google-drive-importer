package org.nuxeo.labs.importer.google.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProvider;
import org.nuxeo.ecm.platform.oauth2.providers.OAuth2ServiceProviderRegistry;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 *
 */
@Operation(
        id=ImportGoogleDriveItem.ID,
        category=Constants.CAT_DOCUMENT,
        label="Google Drive Item Importer",
        description="Import a Google Drive Item by ID in Nuxeo")
public class ImportGoogleDriveItem {

    public static final String ID = "Document.ImportGoogleDriveItem";

    @Context
    protected CoreSession session;

    @Param(name = "itemId", required = true)
    protected String itemId;

    @Param(name = "providerId", required = false)
    protected String providerId = "googledrive-importer";

    @OperationMethod
    public DocumentModel run(DocumentModel input) throws IOException {
        OAuth2ServiceProvider oAuth2ServiceProvider =
                Framework.getService(OAuth2ServiceProviderRegistry.class).getProvider(providerId);
        String username = session.getPrincipal().getName();
        Credential credential = oAuth2ServiceProvider.loadCredential(username);
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
        Drive drive =  new Drive.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Nuxeo")
                .build();

        File file = drive.files().get(itemId).execute();
        InputStream in = drive.files().get(itemId).executeAsInputStream();

        Blob blob = new FileBlob(in,file.getMimeType());
        blob.setFilename(file.getTitle());

        DocumentModel doc = session.createDocumentModel(input.getPathAsString(),file.getTitle(),"File");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);

        return input;
    }
}
