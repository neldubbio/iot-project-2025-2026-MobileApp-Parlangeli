package it.unisalento.iot.autobluapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;


public class BleScanActivity extends AppCompatActivity {

    private static final String TAG = "AUTOBLU";
    private static final int REQUEST_PERMISSIONS = 100;

    // Quanto aspettare prima di (provare ad) inviare un mesaggio MQTT al server
    private static final long MQTT_MESSAGE_COOLDOWN = 30000;
    private long lastMqttMessageTime = 0;

    private BluetoothLeScanner scanner;

    private String ip;
    private ArrayList<String> addresses;
    private ArrayList<String> uuids;

    private TextView textInfo;
    private TextView textStreet;

    private String currStreet = "VIA TOSCANA";
    private double currLat = 44.4482040481231;
    private double currLon = 11.3592392311116;

    private MqttAsyncClient mqttClient;
    private boolean mqttConnected = false;


    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {

            String address = result.getDevice().getAddress();

            int i = 0;
            for (String a : addresses) {
                if (a.equals(address)) {
                    Log.d(TAG, "TROVATO!! " + address + " [" + i + "]");

                    if (canSendMqttMessage()) {
                        Log.d(TAG, "########## MQTT ############");
                        try {
                            sendMqttMessage(i);
                            lastMqttMessageTime = System.currentTimeMillis();
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                i++;
            }

            Log.d(TAG, "-------------------------------------------------------");

        }
    };

    private boolean canSendMqttMessage() {
        return System.currentTimeMillis() - lastMqttMessageTime >= MQTT_MESSAGE_COOLDOWN;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_ble_scan);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.textInfo = findViewById(R.id.text_info);
        this.textStreet = findViewById(R.id.text_street);
        this.textStreet.setText("Posizione: \"Via Toscana\"\nLat: 44.4482040481231\nLon: 11.3592392311116");

        Button b1 = findViewById(R.id.btn_street1);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currStreet = "VIA TOSCANA";
                currLat = 44.4482040481231;
                currLon = 11.3592392311116;
                textStreet.setText("Posizione: \"Via Toscana\"\nLat: 44.4482040481231\nLon: 11.3592392311116");
            }
        });

        Button b2 = findViewById(R.id.btn_street2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currStreet = "VIA MAZZINI";
                currLat = 44.4897501701348;
                currLon = 11.3583559634235;
                textStreet.setText("Posizione: \"Via Mazzini\"\nLat: 44.4897501701348\nLon: 11.3583559634235");
            }
        });

        Button b3 = findViewById(R.id.btn_street3);
        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currStreet = "VIA GIOVANNA ZACCHERINI ALVISI";
                currLat = 44.4932095746169;
                currLon = 11.3605470479019;
                textStreet.setText("Posizione: \"Via Giovanna Zaccherini Alvisi\"\nLat: 44.4932095746169\nLon: 11.3605470479019");
            }
        });

        Button b4 = findViewById(R.id.btn_street4);
        b4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currStreet = "VIA S.DONATO";
                currLat = 44.503327968489;
                currLon = 11.363747753325;
                textStreet.setText("Posizione: \"Via S. Donato\"\nLat: 44.503327968489\nLon: 11.363747753325");
            }
        });

        Button b5 = findViewById(R.id.btn_street5);
        b5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currStreet = "VIA DELLA SALUTE";
                currLat = 44.5222547013235;
                currLon = 11.2729414631605;
                textStreet.setText("Posizione: \"Via della salute\"\nLat: 44.5222547013235\nLon: 11.2729414631605");
            }
        });

        Bundle bundle = getIntent().getExtras();
        this.ip = bundle.getString("ip");
        this.addresses = bundle.getStringArrayList("addresses");
        this.uuids = bundle.getStringArrayList("uuids");

        Log.i(TAG, this.addresses.toString());
        Log.i(TAG, this.uuids.toString());

        try {
            setupMqttClient();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        this.scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();

        // Sicurezza, sicurezza...
        if (!hasPermissions()) requestPermissions();
        else                   beginLeScan();

    }


    private boolean hasPermissions() {

        return (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)       == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        );

    }

    /**
     * Online leggo che per usare il BLE c'è bisogno dell'autorizzazione "BLUETOOTH_SCAN".
     *
     * Ok, bene. Non funziona. Nessun errore sulla console.
     *
     * A quanto pare serve anche quella sulla posizione.
     *
     * Ok, bene. Non funziona. Nessun errore sulla console.
     *
     * Poi scopro che il mio telefono non mi stava mostrando la UI con la conferma dei permessi.
     * Ho dovuto abilitarli manualmente nelle impostazioni dell'app.
     *
     * Ora funziona.
     *
     * IPHONE:  1
     * SAMSUNG: 0
     */
    private void requestPermissions() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                REQUEST_PERMISSIONS
        );

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginLeScan();
        } else {
            Log.e(TAG, "ERRORE: PERMESSSI NEGATI!!!");
            this.textInfo.setText("Permessi negati...");
        }
    }


    @SuppressLint("MissingPermission")
    private void beginLeScan() {

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        Log.d(TAG, "================ INIZIO SCAN ======================");
        scanner.startScan(null, settings, leScanCallback);
        this.textInfo.setText("Scan in corso!!!!!!");

    }


    private void setupMqttClient() throws MqttException {

        String url = "tcp://" + this.ip + ":1883";
        mqttClient = new MqttAsyncClient(url, MqttAsyncClient.generateClientId(), new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        try {
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "MQTT CONNESSO");
                    mqttConnected = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "!!! ERRORE MQTT !!!", exception);
                    mqttConnected = false;
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    // https://www.baeldung.com/java-generating-random-numbers-in-range#intstreamiterate
    public int randrange(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private void sendMqttMessage(int beaconIdx) throws MqttException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());

        String message = "{" +
                "\"timestamp\":\"" + timestamp + "\"" +
                ",\"beaconUUID\":\"" + this.uuids.get(beaconIdx) + "\"" +
                ",\"streetName\":\"" + this.currStreet + "\"" +
                ",\"lat\":" + this.currLat +
                ",\"lon\":" + this.currLon +
                ",\"estimatedVelocity\":" + this.randrange(0, 41) +
                "}";

        if (this.mqttConnected) {
            mqttClient.publish("user-ping", message.toString().getBytes(StandardCharsets.UTF_8), 1, false);
        }

    }

}