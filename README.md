## Description

A plugin to import content stored in Google Drive. Unlike nuxeo-liveconnect, the content is duplicated in Nuxeo.

## How to build
```
git clone https://github.com/nuxeo-sandbox/nuxeo-google-drive-importer
cd nuxeo-google-drive-importer
mvn clean install -DskipTests
```

## How to run tests

First you'll need an oauth2 authentication token. The easiest way to get one is to use [Google's oauth playground](https://developers.google.com/oauthplayground)

- Using Google's playground, generate a token with the https://www.googleapis.com/auth/drive.readonly scope
- Next you need the ID of a file to import. The ID can be found in the google drive file's URL. 

```
mvn test -Dgoogle-drive-accesstoken=MY_TOKEN -Dgoogle-drive-file-id=MY_FILE_ID -Dgoogle-drive-folder-id=MY_FOLDER_ID
```

## How to run

- install this package as well as nuxeo-liveconnect on your nuxeo environment
- as an administrator, [configure liveconnect for google drive](https://doc.nuxeo.com/nxdoc/nuxeo-live-connect/#setting-up-live-connect-for-google-drive)
- in webui, connect your Google Drive account to the application (user setting / Cloud Services Accounts / Connect to Google Drive)
- Use the Automation API to import content

```
curl --location --request POST 'https://MY_SERVER/nuxeo/api/v1/automation/Document.ImportGoogleDriveItem' \
--header 'Authorization: Basic ...' \
--header 'Content-Type: application/json' \
--data-raw '{
  "input": "doc:/default-domain/workspaces/myworkspace"
  "params": {
    "itemId": "ITEM_ID"
  },
  "context": {}
}'
```

## Known limitations
This plugin is a work in progress.

## About Nuxeo
[Nuxeo](www.nuxeo.com), developer of the leading Content Services Platform, is reinventing enterprise content management (ECM) and digital asset management (DAM). Nuxeo is fundamentally changing how people work with data and content to realize new value from digital information. Its cloud-native platform has been deployed by large enterprises, mid-sized businesses and government agencies worldwide. Customers like Verizon, Electronic Arts, ABN Amro, and the Department of Defense have used Nuxeo's technology to transform the way they do business. Founded in 2008, the company is based in New York with offices across the United States, Europe, and Asia.

Learn more at www.nuxeo.com.
