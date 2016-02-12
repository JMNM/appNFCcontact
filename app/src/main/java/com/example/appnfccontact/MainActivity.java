package com.example.appnfccontact;

import android.annotation.TargetApi;
import android.graphics.Color;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
    TextView textInfo;
    EditText textOut;
    Button guardar_numero;
    Button agregar_contacto;
    TextView num_guardado;
    String numero;
    TextView num_rec;

    NfcAdapter nfcAdapter;

    private final String SHARED_PREFS_FILE = "HMPrefs";
    private final String KEY_NUMBER = "number";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textInfo = (TextView)findViewById(R.id.textView);
        textOut = (EditText)findViewById(R.id.textout);
        num_guardado = (TextView)findViewById(R.id.numero_guardado);
        guardar_numero = (Button)findViewById(R.id.button);
        agregar_contacto = (Button)findViewById(R.id.button2);
        num_rec = (TextView)findViewById(R.id.num_rec);
        leerNumeroGuardado();
        guardar_numero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarNumero();
            }

        });
        agregar_contacto.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent contactIntent = new Intent(Intent.ACTION_INSERT);

                contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE,num_rec.getText());

                startActivity(contactIntent);
            }

        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(nfcAdapter==null){
            textInfo.setText("NFC No Existe");
            textInfo.setTextColor(Color.RED);
        }else if(!nfcAdapter.isEnabled()){
            textInfo.setText("¡NFC DESACTIVADO!");
            textInfo.setTextColor(Color.RED);
        }else {
            textInfo.setText("NFC Activo");
            textInfo.setTextColor(Color.GREEN);
            textInfo.setTextColor(3);
            nfcAdapter.setNdefPushMessageCallback(this, this);
            nfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }

    }
    private static boolean isNumeric(String cadena){
        try {
            Integer.parseInt(cadena);
            return true;
        } catch (NumberFormatException nfe){
            return false;
        }
    }
    private void guardarNumero(){
        String str = textOut.getText().toString();
        if(str.length()!=9) Toast.makeText(getBaseContext(), "El número no es correcto!!", Toast.LENGTH_SHORT).show();
        else if(!isNumeric(str)) Toast.makeText(getBaseContext(), "El número no es correcto!!", Toast.LENGTH_SHORT).show();
        else {
            try {
                FileOutputStream fos = openFileOutput("appNFCcontactNumber.txt", MODE_PRIVATE);
                OutputStreamWriter osw = new OutputStreamWriter(fos);

                // Escribimos el String en el archivo
                osw.write(str);
                osw.flush();
                osw.close();

                Toast.makeText(getBaseContext(), "Guardado", Toast.LENGTH_SHORT).show();

                numero=textOut.getText().toString();
                num_guardado.setText(numero);
                textOut.setText("");

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    private void leerNumeroGuardado(){
        try{
            FileInputStream fis = openFileInput("appNFCcontactNumber.txt");
            InputStreamReader isr = new InputStreamReader(fis);

            char[] inputBuffer = new char[20];
            String s = "";

            int charRead;
            while((charRead = isr.read(inputBuffer)) > 0){
                // Convertimos los char a String
                String readString = String.copyValueOf(inputBuffer, 0, charRead);
                s += readString;

                inputBuffer = new char[20];
            }

            // Establecemos en el EditText el texto que hemos leido
            num_guardado.setText(s);
            numero=s;

            // Mostramos un Toast con el proceso completado
            Toast.makeText(getBaseContext(), "Cargado", Toast.LENGTH_SHORT).show();

            isr.close();
        }catch (IOException ex){
            Toast.makeText(getBaseContext(), "No hay número guardado, guardelo para poder enviarlo", Toast.LENGTH_SHORT).show();
        }


    }
    @Override
    protected void onResume() {
        super.onResume();
        if(!nfcAdapter.isEnabled()){
            textInfo.setText("¡NFC DESACTIVADO!");
            textInfo.setTextColor(Color.RED);
        }else {
            textInfo.setText("NFC Activo");
            textInfo.setTextColor(Color.GREEN);
        }
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
            Parcelable[] parcelables =
                    intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage inNdefMessage = (NdefMessage)parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord NdefRecord_0 = inNdefRecords[0];
            String inMsg = new String(NdefRecord_0.getPayload());
            num_rec.setText(inMsg);

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public NdefMessage createNdefMessage(NfcEvent event) {

        byte[] bytesOut = numero.getBytes();

        NdefRecord ndefRecordOut = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA,
                "text/plain".getBytes(),
                new byte[] {},
                bytesOut);

        NdefMessage ndefMessageout = new NdefMessage(ndefRecordOut);

        return ndefMessageout;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {

        final String eventString = "onNdefPushComplete\n" + event.toString();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),
                        eventString,
                        Toast.LENGTH_LONG).show();
            }
        });

    }
}
