package bot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class GoogleSheetsUtil {

    private static final String SPREADSHEET_ID = "1eoQ-4TLBcmVwL_n_48sWN2_-1GVIVmEaSxYR9gm-UgE";
    private static final String APPLICATION_NAME = "KLONDIKE-CONTEST";
    private static final String CREDENTIALS_FILE_PATH = "/secret.json";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private NetHttpTransport httpTransport;
    private Sheets sheets;

    GoogleSheetsUtil() throws GeneralSecurityException, IOException {
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            sheets = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            log.error("Error in GoogleSheetsUtil constructor: ", e);
        }
    }

    private Credential getCredentials(NetHttpTransport httpTransport) {
        try {
            InputStream inputStream = GoogleSheetsUtil.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(inputStream));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .build();
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (Exception e) {
            log.error("Error in method getCredentials(): ", e);
            return null;
        }
    }

    void saveUserInput(String username, String email, String estimation) {
        try {
            int firstEmptyRow = getFirstEmptyRow();
            String range = "A" + firstEmptyRow + ":D" + firstEmptyRow;
            ValueRange valueRange = new ValueRange();
            valueRange.setRange(range);
            List<List<Object>> value = new ArrayList<>();
            List<Object> nestedValue = new ArrayList<>();
            nestedValue.add(firstEmptyRow - 1);
            nestedValue.add(username);
            nestedValue.add(email);
            nestedValue.add(estimation);
            value.add(nestedValue);
            valueRange.setValues(value);
            sheets.spreadsheets().values().append(SPREADSHEET_ID, range, valueRange).setInsertDataOption("OVERWRITE").setValueInputOption("RAW").execute();
        } catch (Exception e) {
            log.error("Error in method saveEstimation(): ", e);
        }
    }

    private int getFirstEmptyRow() throws IOException {
        String range = "A1:A";
        ValueRange column = sheets.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
        int size = column.getValues().size() + 1;
        log.info("First empty row is: {} ", size);
        return size;
    }
}