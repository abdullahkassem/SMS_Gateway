package com.example.smsgateway;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

//This AsyncTask should call getSMS() API and send the sms every 5 seconds


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    boolean Start = false;    //This attribute will represent which button is pressed
    int counter = 0;
    private static final int SMS_permission_code = 100;
    TextView counter_view ;
    String Empty_table_str = "Table empty";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        counter = 0;
        Start = false;
        counter_view = (TextView) findViewById(R.id.counter_txt);
        Button start_button = (Button) findViewById(R.id.Start_button);
        start_button.setOnClickListener(this);
        Button stop_button = (Button) findViewById(R.id.Stop_button);
        stop_button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.Start_button) {
            if(Start == false)
            {
                Start = true;
                Log.i("TAG", "Start Button pressed ");
                new MyTask().execute("http://localhost:3000/getSMS");
            }

        } else if (id == R.id.Stop_button) {
            Log.i("TAG", "Stop Button pressed ");
            Start = false;
        }
    }

    // Borrowed and modified from https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Log.i("TAG", "checkPermission: Permission already granted");

        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == SMS_permission_code) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("TAG", "onRequestPermissionsResult: SMS permission granted");
            }
            else {
                Log.e("TAG", "onRequestPermissionsResult: SMS permission Denied");
                //checkPermission(Manifest.permission.SEND_SMS,SMS_permission_code);
            }
        }

    }



// I used a skeleton I found for AsyncTask https://stackoverflow.com/questions/25647881/android-asynctask-example-and-explanation/25647882#25647882
    private class MyTask extends AsyncTask<String, Integer, String> {

        String server_response;

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Do something like display a progress bar
        }

        // This is run in a background thread
        // doInBackground and readstream were borrowed from https://stackoverflow.com/a/38313386
        @Override
        protected String doInBackground(String... params) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL("http:///10.0.2.2:3000/getSMS");
                urlConnection = (HttpURLConnection) url.openConnection();

                int responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    server_response = readStream(urlConnection.getInputStream());
                    Log.v("CatalogClient", server_response);
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.i("TAG", "doInBackground:"+server_response+".");
            if(Objects.equals(server_response, Empty_table_str))
            {
                SystemClock.sleep(5000);
                Log.i("TAG", "Will return now table is empty");
                return server_response;
            }else
            {
                //send SMS and update counter.
                checkPermission(Manifest.permission.SEND_SMS,SMS_permission_code);

                String phoneNo,message;
                try {
                    JSONObject reader = new JSONObject(server_response);
                    phoneNo = reader.getString("phone_no");
                    message = reader.getString("Body");
                    Log.i("SMS Info", "phoneNo =  "+phoneNo+" message = "+message);
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                } catch (JSONException e) {
                    Log.e("JSON", "unexpected JSON exception", e);
                    e.printStackTrace();
                }catch (Exception ex){
                    Log.e("SMS", "doInBackground: Failed to send sms" );
                    ex.printStackTrace();
                }



            }


            SystemClock.sleep(5000);
            return server_response;
        }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

        // This is called from background thread but runs in UI
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            // Do things like update the progress bar
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("Main thread", "Background Task finished ");
            Toast.makeText(MainActivity.this, "5 seconds passed",Toast.LENGTH_SHORT).show();

            if(Objects.equals(server_response, Empty_table_str))
            {
                Log.i("NoSMS", "onPostExecute: table empty no sms sent");
            }else
            {
                Log.i("SMS_Sent", "SMS sent , server response is "+s);
                counter = counter +1;
                counter_view.setText(String.valueOf(counter));

            }

            if(Start == true)
                new MyTask().execute("http://localhost:3000/getSMS");
        }
}
}
