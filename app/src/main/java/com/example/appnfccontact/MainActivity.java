/*
    Copyright (C) 2016  José Miguel Navarro Moreno and José Antonio Larrubia García

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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

/**
 * Clase princial de la aplicación para pasar un número de teléfono (9 digitos) por NFC
 * y poder añadirlo a la lista de contactos.
 *  @autor José Miguel Navarro Moreno
 *  @autor José Antonio Larrubia García
 *  @version 13.2.2016
 */
public class MainActivity extends AppCompatActivity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
    // donde se muestra si el NFC esta activado
    TextView textInfo;
    //Donde se introducira el número a pasar la primera vez que se inicie la aplicación o si se desea cambiar.
    EditText textOut;
    //botón para cambiar el número que se va a pasar por NFC
    Button guardar_numero;
    //botón para agregar el número recibido a contactos
    Button agregar_contacto;
    //Donde se mustra el número que se enviara por NFC
    TextView num_guardado;
    String numero;
    //Donde se muetra el número recivido.
    TextView num_rec;

    //Adaptador NFC
    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Se asigna los witget a las variables
        textInfo = (TextView)findViewById(R.id.textView);
        textOut = (EditText)findViewById(R.id.textout);
        num_guardado = (TextView)findViewById(R.id.numero_guardado);
        guardar_numero = (Button)findViewById(R.id.button);
        agregar_contacto = (Button)findViewById(R.id.button2);
        num_rec = (TextView)findViewById(R.id.num_rec);

        //Si se guardo algun número en la aplicación anteriomente se carga.
        leerNumeroGuardado();

        //Se añaden los eventos de los botones
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

        //Se inicia el adaptador NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //Se notifica en el caso de que este desactivado o el dispositivo no tenga NFC
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

    /**
     * Para comprobar si un string es unicamente un número.
     * @param cadena
     * @return
     */
    private static boolean isNumeric(String cadena){
        try {
            Integer.parseInt(cadena);
            return true;
        } catch (NumberFormatException nfe){
            return false;
        }
    }

    /**
     * Para guardar un número que después se podra enviar por NFC.
     */
    private void guardarNumero(){
        //Referencia: https://sekthdroid.wordpress.com/2013/02/08/guardar-datos-de-texto-en-memoria-interna-con-android/

        String str = textOut.getText().toString();
        //se comprueba que el número es correcto.
        if(str.length()!=9) Toast.makeText(getBaseContext(), "El número no es correcto!!", Toast.LENGTH_SHORT).show();
        else if(!isNumeric(str)) Toast.makeText(getBaseContext(), "El número no es correcto!!", Toast.LENGTH_SHORT).show();
        else {
            try {
                //Se guarda el número en un archivo para no tener que introducirlo cada vez que se inicie la aplicación
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

    /**
     * Para recuperar el número guardado en un archivo.
     */
    private void leerNumeroGuardado(){
        //Referencia: https://sekthdroid.wordpress.com/2013/02/08/guardar-datos-de-texto-en-memoria-interna-con-android/

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
            //Si no hay un número guardado.
            Toast.makeText(getBaseContext(), "No hay número guardado, guardelo para poder enviarlo", Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * En este método, si se ejecuta después de un intent con una etiqueta de NDEF útil se obtendra el número de ella.
     */
    @Override
    protected void onResume() {
        //Se indica si el NFC se a activado o desactivado.
        super.onResume();
        if(!nfcAdapter.isEnabled()){
            textInfo.setText("¡NFC DESACTIVADO!");
            textInfo.setTextColor(Color.RED);
        }else {
            textInfo.setText("NFC Activo");
            textInfo.setTextColor(Color.GREEN);
        }

        //Se comprueba si el método se ha ejecutado tras un ACTION_NDEF_DISCOVERED del NfcAdapter
        //Mas información: http://developer.android.com/intl/es/reference/android/nfc/NfcAdapter.html
        //El siguente trozo código se ha obtenido de:
        //      http://android-er.blogspot.com.es/2014/04/example-of-programming-android-nfc.html
        Intent intent = getIntent();
        String action = intent.getAction();
        if(action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)){
            //Obtenemos el array de mensajes que pueda contener la etiqeuta NFC
            //en un objeto array de tipo Parcelable que nos permitara obtener el mensajes en NdefMessage
            Parcelable[] parcelables =
                    intent.getParcelableArrayExtra(
                            NfcAdapter.EXTRA_NDEF_MESSAGES);
            //Obtenemos el mensaje.
            NdefMessage inNdefMessage = (NdefMessage)parcelables[0];
            //Obtenemos el registro NdefRecord del mensaje
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord NdefRecord_0 = inNdefRecords[0];

            //Guardamos el mensaje en un string y lo mostramos en el textView
            String inMsg = new String(NdefRecord_0.getPayload());
            num_rec.setText(inMsg);

        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    /**
     * Método que crea el mensaje que se enviara por NFC
     * @param event Evento NFC
     * @return Mensaje que se envia
     */
    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public NdefMessage createNdefMessage(NfcEvent event) {
        //Referencia: http://android-er.blogspot.com.es/2014/04/example-of-programming-android-nfc.html

        //Se pasa a una cadena de byte
        byte[] bytesOut = numero.getBytes();

        //Se crea el objeto NdefRecord para el mensaje.
        NdefRecord ndefRecordOut = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA,
                "text/plain".getBytes(),
                new byte[] {},
                bytesOut);

        //Se crea el objeto NdefMessage con el NdefRecord anterior y se devuelve.
        NdefMessage ndefMessageout = new NdefMessage(ndefRecordOut);

        return ndefMessageout;
    }

    /**
     * Método que se ejecuta cuando se envía una mensaje en un evento NFC
     * @param event
     */
    @Override
    public void onNdefPushComplete(NfcEvent event) {
        //Referencia: http://android-er.blogspot.com.es/2014/04/example-of-programming-android-nfc.html

        //Se indica qué se ha enviado mediante un Toast.
        final String eventString = "Número Enviado: \n" + event.toString();
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
