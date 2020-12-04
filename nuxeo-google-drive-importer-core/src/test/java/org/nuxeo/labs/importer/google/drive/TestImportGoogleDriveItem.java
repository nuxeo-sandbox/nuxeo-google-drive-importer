package org.nuxeo.labs.importer.google.drive;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy({"org.nuxeo.labs.nuxeo-google-drive-importer-core",
        "org.nuxeo.ecm.platform.oauth",
        "org.nuxeo.ecm.platform.filemanager",
        "org.nuxeo.ecm.platform.types",
        "org.nuxeo.ecm.platform.webapp.types"
})
public class TestImportGoogleDriveItem {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Before
    public void createToken() {
        OAuth2TokenStore store = new OAuth2TokenStore("googledrive-importer");
        NuxeoOAuth2Token token = new NuxeoOAuth2Token(
                System.getProperty("google-drive-accesstoken"),
                null,
                System.currentTimeMillis() + 300000);
        token.setNuxeoLogin("Administrator");
        token.setServiceLogin("Administrator");
        token.setClientId("Administrator");
        store.store("Administrator", token);
    }

    @Test
    public void testImportOneFile() throws OperationException {
        DocumentModel folder = session.createDocumentModel(session.getRootDocument().getPathAsString(), "Folder", "Folder");
        folder = session.createDocument(folder);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(folder);
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", System.getProperty("google-drive-file-id"));
        folder = (DocumentModel) automationService.run(ctx, ImportGoogleDriveItem.ID, params);
        DocumentModelList children = session.getChildren(folder.getRef());
        assertEquals(1, children.size());
        DocumentModel importedDoc = children.get(0);
        assertNotNull(importedDoc.getPropertyValue("file:content"));
    }

    @Test
    public void testImportFolder() throws OperationException {
        DocumentModel folder = session.createDocumentModel(session.getRootDocument().getPathAsString(), "Folder", "Folder");
        folder = session.createDocument(folder);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(folder);
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", System.getProperty("google-drive-folder-id"));
        params.put("batchSize", 3);
        folder = (DocumentModel) automationService.run(ctx, ImportGoogleDriveItem.ID, params);
        DocumentModelList children = session.getChildren(folder.getRef());
        assertEquals(1, children.size());
        DocumentModel importedFolder = children.get(0);
        DocumentModelList importedFiles = session.getChildren(importedFolder.getRef());
        assertTrue(importedFiles.size() > 0);
    }
}
