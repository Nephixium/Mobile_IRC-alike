package com.android.messaging;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.net.ConnectivityManagerCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

// En inloggningsskärm som tar emot fyra parametrar från användaren i form av textfält: Serveraddress, Port, Användarnamn och Lösenord, för att sedan använda dessa för att logga in på en server som körs på en PC i syfte att kommunicera med andra klienter.

public class MainActivity extends AppCompatActivity {

    private Switch refSwitch;
    private EditText refServerPort;
    private EditText refServerAddress;
    private EditText refUsername;
    private EditText refPassword;
    private TextView refConnectionState;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private SharedPreferences sPrefFile;

    /* 	Skapar en referens till en Switch i gränssnittet och anger att metoden saveInfo() skall aktiveras när Switchen är på, samt skapar referenser till textfält innehållandes serveraddress, port, användarnamn samt lösenord som fylls i utifrån SharedPreferences om fälten återfinns i den.
    	ConnectivityManager klassen används för att uppdatera ett textfält i nedre hörn på gränssnittet som återger anslutningsstatus (WiFi, Mobil, eller ej ansluten). */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.refSwitch = findViewById(R.id.infoSwitch);
        this.refSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    saveInfo();
                }
            }
        });
        this.refServerAddress = findViewById(R.id.host_edit1);
        this.refServerPort = findViewById(R.id.port_edit1);
        this.refUsername = findViewById(R.id.username_edit);
        this.refPassword = findViewById(R.id.password_edit);
        this.refConnectionState = findViewById(R.id.connectionState_text);
        this.sPrefFile = this.getSharedPreferences("com.android.messaging", MODE_PRIVATE);
        if(sPrefFile.contains("HOST")) {
            this.refServerAddress.setText(sPrefFile.getString("HOST", ""));
            this.refServerPort.setText(sPrefFile.getString("PORT", ""));
            this.refUsername.setText(sPrefFile.getString("USERNAME",""));
        }
        this.connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if(!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo.isConnected()) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if (ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager)) {
                        refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on mobile");
                    } else {
                        refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on WiFi");
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (connectivityManager.isActiveNetworkMetered()) {
                    refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on mobile");
                } else {
                    refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on WiFi");
                }
            }
        } else {
                refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "No connection");
            }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.networkCallback = new ConnectivityManager.NetworkCallback() {

                // Om det finns en anslutning och anslutningen mäts, uppdatera textfältet att en mobil anslutning är aktiv, alternativt om den ej mäts, återge anslutning till WiFi.
                @Override
                public void onAvailable(Network network) {
                    if(connectivityManager.isActiveNetworkMetered()) {
                        refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on Mobile");
                    } else {
                        refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "Connected on WiFi");
                    }
                }

                // Om det inte finns någon anslutning, uppdatera textfältet med denna information.
                @Override
                public void onLost(Network network) {
                    refConnectionState.setText("Android version: " + Build.VERSION.SDK_INT + "\n" + "No connection");
                }
            };

            NetworkRequest networkRequest = new NetworkRequest.Builder().build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    /* 	Kontrollerar textfältens innehåll och visar ett popup meddelande för användaren om informationen är inkorrekt eller skapar en Intent innehållandes fälten PORT, HOST, USERNAME och PASSWORD.
    	Den skapade intenten används sedan för att starta chattklienten via en ny aktivitet. */
    public void joinServer(View view) {
        if(refUsername.getText().toString().contains("/") || refUsername.getText().toString().contains("+") || refUsername.length() < 3 || refUsername.length() > 14) {
            Toast toast = Toast.makeText(this, "Username cannot contain '/' or '+' and must be 3-14 characters long", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if(refPassword.getText().toString().contains("/") || refPassword.getText().toString().contains("+") || refPassword.length() < 8 || refPassword.length() > 20) {
            Toast toast = Toast.makeText(this, "Password cannot contain '/' or '+' and must be 8-20 characters long", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Intent loginIntent = new Intent(this, Client.class);
        loginIntent.putExtra("PORT", Integer.valueOf(refServerPort.getText().toString()));
        loginIntent.putExtra("HOST", refServerAddress.getText().toString());
        loginIntent.putExtra("USERNAME", refUsername.getText().toString());
        loginIntent.putExtra("PASSWORD", refPassword.getText().toString());
        startActivity(loginIntent);
    }

    // Sparar all information i fälten i SharedPreferences förutom lösenordet som av säkerhetsskäl bör skrivas in igen, så detta kan hämtas vid nästa uppstart.
    public void saveInfo() {
        SharedPreferences.Editor sPreferencesEdit = sPrefFile.edit();
        sPreferencesEdit.putString("HOST", refServerAddress.getText().toString());
        sPreferencesEdit.putString("PORT", refServerPort.getText().toString());
        sPreferencesEdit.putString("USERNAME", refUsername.getText().toString());
        sPreferencesEdit.apply();
    }
}
