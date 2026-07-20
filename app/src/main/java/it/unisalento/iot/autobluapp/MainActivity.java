package it.unisalento.iot.autobluapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Per costruire questo capolavoro, ho consultato (non in ordine):
 *  - https://www.geeksforgeeks.org/android/a-complete-guide-to-learn-xml-for-android-app-development/
 *  - https://www.geeksforgeeks.org/android/implicit-and-explicit-intents-in-android-with-examples/
 *  - https://www.geeksforgeeks.org/android/button-in-kotlin/
 *  - https://www.geeksforgeeks.org/android/android-ui-layouts/
 *
 *  W gli indiani
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private OkHttpClient client;

    private EditText editboxIpAddr;
    private EditText editboxUsername;
    private EditText editboxPassword;
    private TextView textError;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.root),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );

        this.editboxIpAddr   = findViewById(R.id.editbox_ip);
        this.editboxUsername = findViewById(R.id.editbox_username);
        this.editboxPassword = findViewById(R.id.editbox_password);
        this.textError       = findViewById(R.id.text_error);

        Button loginButton = findViewById(R.id.button_login);
        loginButton.setOnClickListener(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        this.client = new OkHttpClient();

    }


    @Override
    public void onClick(View v) {

        /*
         * https://stackoverflow.com/a/49234215
         * https://github.com/square/okhttp/blob/master/okhttp/src/jvmTest/kotlin/okhttp3/FormBodyTest.kt
         * https://stackoverflow.com/a/62477085
         *
         * E per finire in bellezza,
         * https://stackoverflow.com/a/9289190
         *
         * Non ho alcun rimorso
         */

        try {

            String jwt = this.login();
            User me = this.fetchUserData(jwt);
            ArrayList<Beacon> beacons = this.fetchUserBeacons(jwt, me);

            Bundle bundle = new Bundle();

            ArrayList<String> addresses = new ArrayList<>();
            ArrayList<String> uuids = new ArrayList<>();
            for (Beacon b : beacons) {
                addresses.add(b.getAddress());
                uuids.add(b.getSelfId());
            }

            bundle.putStringArrayList("addresses", addresses);
            bundle.putStringArrayList("uuids", uuids);
            bundle.putString("ip", this.editboxIpAddr.getText().toString().strip());

            Intent i = new Intent(getApplicationContext(), BleScanActivity.class);
            i.putExtras(bundle);
            startActivity(i);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    private String login() throws IOException {

        String ip       = this.editboxIpAddr.getText().toString().strip();
        String username = this.editboxUsername.getText().toString().strip();
        String password = this.editboxPassword.getText().toString().strip();

        RequestBody requestBody = new FormBody.Builder()
                .add("client_id", "autoblu-be")
                .add("username", username)
                .add("password", password)
                .add("grant_type", "password")
                .build();

        Request request = new Request.Builder()
                .url("http://" + ip + ":8080/realms/unisalento-iot/protocol/openid-connect/token")
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("ERRORE!! Codice " + response);
        }

        String jsonBody = response.body().string();

        ObjectMapper mapper = new ObjectMapper();
        KeycloakLoginResponse kcr = mapper.readValue(jsonBody, KeycloakLoginResponse.class);
        return kcr.getJwt();

    }


    private User fetchUserData(String jwt) throws IOException {

        String ip = this.editboxIpAddr.getText().toString().strip();

        Request request = new Request.Builder()
                .url("http://" + ip + ":18080/autoblu/api/v1/user/me")
                .addHeader("Authorization", "Bearer " + jwt)
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("ERRORE!! Codice " + response);
        }

        String jsonBody = response.body().string();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonBody, User.class);

    }


    private ArrayList<Beacon> fetchUserBeacons(String jwt, User user) throws IOException {

        String ip = this.editboxIpAddr.getText().toString().strip();

        ObjectMapper mapper = new ObjectMapper();
        ArrayList<Beacon> beacons = new ArrayList<>();

        for (String id : user.getBeacons()) {

            Request request = new Request.Builder()
                    .url("http://" + ip + ":18080/autoblu/api/v1/beacon/" + id)
                    .addHeader("Authorization", "Bearer " + jwt)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("ERRORE!! Codice " + response);
            }

            String jsonBody = response.body().string();
            beacons.add(mapper.readValue(jsonBody, Beacon.class));

        }

        return beacons;

    }

    
}