package com.android.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

// En chattklient som tar emot och skickar meddelanden mellan klienter via en server. Asynctask används för att hantera nätverksaktivitet och skickar informationen vidare till metoder i UI tråden som uppdaterar textfält som visar informationen grafiskt för användaren.

public class Client extends AppCompatActivity {

    private TextView refChatText;
    private EditText refChatEdit;
    private Button refSendButton;
    private PrintWriter writer;
    private ServerListener serverListener;
    private Socket clientSocket;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationChannel notificationChannel;
    private NotificationManager notificationManager;
    private NotificationManagerCompat notificationManagerCompat;
    private String username;
    private Intent loginCredentialsIntent;
    private RecyclerView rRecyclerView;
    private static RecyclerView.Adapter rAdapter;
    private RecyclerView.LayoutManager rLayoutManager;
    private static ArrayList<String> messageArray;

    // Skapar referenser till textfält i gränssnittet och hämtar intent från tidigare aktivitet samt skapar en notifikationskanal som kommer användas för att visa en notifikation varje gång ett nytt meddelande tas emot i klienten.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);
        this.refChatText = findViewById(R.id.chat_text);
        this.refChatText.setMovementMethod(new ScrollingMovementMethod());
        this.refChatEdit = findViewById(R.id.chat_edit);
        this.refSendButton = findViewById(R.id.send_button);
        this.refSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(v);
            }
        });
        this.serverListener = new ServerListener();
        this.serverListener.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
        this.loginCredentialsIntent = getIntent();
        this.username = loginCredentialsIntent.getStringExtra("USERNAME");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.notificationChannel = new NotificationChannel("Message", "Message notification", NotificationManager.IMPORTANCE_DEFAULT);
            this.notificationChannel.setDescription("New message received");
            this.notificationManager = getSystemService(NotificationManager.class);
            this.notificationManager.createNotificationChannel(notificationChannel);
            this.notificationBuilder = new NotificationCompat.Builder(this, "Message");
            this.notificationBuilder.setSmallIcon(R.drawable.remote_message_bg);
            this.notificationBuilder.setContentTitle("New message");
            this.notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }
        messageArray = new ArrayList<>();
        this.rRecyclerView = findViewById(R.id.recyclerView);
        this.rLayoutManager = new LinearLayoutManager(this);
        this.rRecyclerView.setLayoutManager(rLayoutManager);
        this.rAdapter = new MessageAdapter(messageArray);
        this.rRecyclerView.setAdapter(rAdapter);
    }

    // Behandlar ett mottaget meddelande genom att antingen logga ut och återgå till inloggningsskärmen eller ta emot en notifikation och lägg till en textbubbla innehållandes meddelandet genom att lägga till det i listan messageArray och meddela RecyclerView adaptern rAdapter att litan har uppdaterats.
    // rRecyclerView.scrollToPosition gör så att skärmen alltid scrollar till det nyaste meddelandet automatiskt och runOnUiThread säkerställer att allt utförs i UI-tråden.
    public void receiveMessage(String message) {
        if(message.equals("/shutdown")) {
            logOff();
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !message.startsWith(username)) {
                this.notificationBuilder.setContentText(message);
                this.notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(00001, notificationBuilder.build());
            }
            messageArray.add(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rAdapter.notifyDataSetChanged();
                    rRecyclerView.scrollToPosition(rAdapter.getItemCount()-1);
                }
            });
        }
    }

    // Startar en ny Asynctask i serie med THREAD_POOL_EXECUTOR som skickar meddelandet till servern och återställer textfältet.
    public void sendMessage(View view) {
        new ServerSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, refChatEdit.getText().toString(), null, null);
        refChatEdit.setText("");
    }

    // Loggar ut från servern genom att först skapa en Asynctask i serie med THREAD_POOL_EXECUTOR och sedan ge en notis till användaren genom en vibration från metoden alert().
    public void logOff() {
        new ServerSender().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "/logout", null, null);
        if(!serverListener.isCancelled()) {
            serverListener.cancel(true);
        }
        alert();
        finish();
    }

    // Ger användaren feedback att något har skett genom att skicka en vibration.
    public void alert() {
        Vibrator vibrator = (Vibrator)getSystemService(this.VIBRATOR_SERVICE);
        vibrator.vibrate(300);
    }

    // Skapar en dialogruta med två knappar, YES respektive NO, när användaren trycker på Back-knappen på sin android telefon där den förstnämnda avslutar sessionen och loggar ut från servern och den andra stänger dialogen och ignorerar knapptrycket.
    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Server exit");
        dialogBuilder.setMessage("Are you sure you want to disconnect from the server?");
        dialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logOff();
            }
        });
        dialogBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
    }


    /* 	Ansluter till en server i bakgrunden - utanför UI tråden - med Socket klassen tillsammans med parametrarna HOST och PORT från loginCredentialsIntent, samt skapar ett PrintWriter objekt och skickar ett inledande meddelande till servern via USERNAME och PASSWORD från samma intent.
    	Ett BufferedReader objekt skapas sedan som lyssnar efter meddelanden från servern och aktiverar utifrån dessa meddelanden metoder i Client-klassen som kan uppdatera textfält i UI tråden.
    	BufferedReader fortsätter läsa tills meddelandet är null eller AsyncTask har avbrytits, varpå användaren loggas ut om hen är inloggad och Socket stängs. */
    private class ServerListener extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {


            try {
                clientSocket = new Socket(loginCredentialsIntent.getStringExtra("HOST"), loginCredentialsIntent.getIntExtra("PORT", 0));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println("/updateuserlist" + loginCredentialsIntent.getStringExtra("USERNAME") + "/" + loginCredentialsIntent.getStringExtra("PASSWORD"));

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Socket error: Could not resolve connection to client");
                logOff();
            }

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String message;

                while (true && !isCancelled()) {
                    message = reader.readLine();

                    if (message == null) {
                        break;
                    }

                    if (message.startsWith("/updateuserlist")) {
                    } else if (message.equals("/kick " + loginCredentialsIntent.getStringExtra("USERNAME")) || message.equals("/deny")) {
                        break;
                    } else {
                        receiveMessage(message);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("InputStream error: Could not resolve connection to client InputStream or Asynctask was closed prematurely");
                logOff();
            }

            if(!isCancelled()) {
                logOff();
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Client socket not closed properly");
            }
            return null;
        }
    }

    // Skapar en ny AsyncTask som skickar meddelanden till servern med PrintWriter objektet writer och avbryter AsyncTask när meddelandet är skickat för att motverka onödig resursanvändning i bakgrunden.
    private class ServerSender extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... message) {
            writer.println(message[0]);
            this.cancel(true);
            return null;
        }
    }
}

