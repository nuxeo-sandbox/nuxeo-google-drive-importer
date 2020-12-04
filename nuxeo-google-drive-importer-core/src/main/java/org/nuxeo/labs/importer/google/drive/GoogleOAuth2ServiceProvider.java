package org.nuxeo.labs.importer.google.drive;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.oauth2.providers.AbstractOAuth2UserEmailProvider;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleOAuth2ServiceProvider extends AbstractOAuth2UserEmailProvider {

    private static final Log log = LogFactory.getLog(GoogleOAuth2ServiceProvider.class);

    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v1/tokeninfo";

    private static final HttpRequestFactory requestFactory =
            HTTP_TRANSPORT.createRequestFactory(request -> request.setParser(new JsonObjectParser(JSON_FACTORY)));

    public final String getServiceUser(String username) {
        Map<String, Serializable> filter = new HashMap<>();
        filter.put("serviceName", serviceName);
        filter.put(NuxeoOAuth2Token.KEY_NUXEO_LOGIN, username);
        List<DocumentModel> entries = getCredentialDataStore().query(filter);
        if (entries == null || entries.size() == 0) {
            return null;
        }
        if (entries.size() > 1) {
            log.error(String.format("Found multiple %s accounts for %s", serviceName, username));
        }
        return (String) entries.get(0).getProperty(NuxeoOAuth2Token.SCHEMA, NuxeoOAuth2Token.KEY_SERVICE_LOGIN);
    }

    @Override
    protected String getUserEmail(String accessToken) throws IOException {
        GenericUrl url = new GenericUrl(TOKEN_INFO_URL);
        url.set("access_token", accessToken);
        HttpResponse response = requestFactory.buildGetRequest(url).execute();
        GenericJson json = response.parseAs(GenericJson.class);
        return (String) json.get("email");
    }

}
