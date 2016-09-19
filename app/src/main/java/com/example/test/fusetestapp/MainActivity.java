package com.example.test.fusetestapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    final String d_path = "fusion-universal.com/apiv1/company.json";
    final OkHttpClient d_client = new OkHttpClient();

    EditText d_company_text;
    ImageView d_logo_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        d_company_text = (EditText) findViewById(R.id.companyEditText);
        d_logo_image = (ImageView) findViewById(R.id.logoImageView);

        d_company_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    reset();
                    handleCompanyNameEntered();
                }
                return false;
            }
        });

    }

    private void reset() {
        d_company_text.setBackgroundColor(Color.WHITE);
        if (d_logo_image.getDrawable() != null) {
            d_logo_image.setImageDrawable(null);
        }

    }

    private void setErrorBackgroundColor() {
        d_company_text.setBackgroundColor(Color.RED);
    }

    private void setSuccessBackgroundColor() {
        d_company_text.setBackgroundColor(Color.GREEN);
    }

    private void handleJsonResponse(String url, String body) {

        if (url.contains(d_path)) {

            JSONObject json = null;
            String name = null;
            String logo_path = null;

            try {
                json = new JSONObject(body);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            try {
                name = json.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }

            try {
                logo_path = json.getString("logo");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            d_company_text.setText(name);
            doGetRequest(logo_path);
        }
    }

    private void handleCompanyNameEntered() {
        //get string and remove white space
        String company = d_company_text.getText().toString().replaceAll("\\s", "");

        if (company.length() < 2) {
            setErrorBackgroundColor();
            return;
        }

        doGetRequest("https://" + company + "." + d_path);
    }

    private void doGetRequest(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        d_client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException exception) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setErrorBackgroundColor();
                            }
                        });
                    }

                    @Override
                    public void onResponse(final Response response) throws IOException {

                        final String content_type = response.body().contentType().toString();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (response.code() != 200) {
                                    setErrorBackgroundColor();
                                    return;
                                }

                                //we can set a success as the url is valid
                                //however this does not indicate that we have received the expected data
                                setSuccessBackgroundColor();

                                if (content_type.equals("application/json")) {
                                    try {
                                        handleJsonResponse(response.networkResponse().request().url().toString(),
                                                response.body().string());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else if (content_type.equals("image/png")) {
                                    InputStream input_stream = response.body().byteStream();
                                    BitmapDecoder decoder = new BitmapDecoder();
                                    decoder.execute(input_stream);
                                } else {
                                    Log.d("Header", "Received unknown content-type: " + content_type);
                                }
                            }
                        });
                    }
                });
    }

    private class BitmapDecoder extends AsyncTask<InputStream, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(InputStream... params) {
            InputStream is = params[0];
            Bitmap bmp = BitmapFactory.decodeStream(is);
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bmp) {
            super.onPostExecute(bmp);
            if (bmp != null) {
                d_logo_image.setImageBitmap(bmp);
            }
        }
    }
}