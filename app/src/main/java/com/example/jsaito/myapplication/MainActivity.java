package com.example.jsaito.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.jsaito.*;

import static android.R.attr.mimeType;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static Uri currentPhotoURI;
    public static RequestQueue TheQueue;

    private byte[] mMultipartBody;
    private final String twoHyphens = "--";
    private final String lineEnd = "\r\n";
    private final String boundary = "apiclient-" + System.currentTimeMillis();
    private final String mimeType = "multipart/form-data;boundary=" + boundary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: remove
        // Instantiate the RequestQueue.
        //TheQueue = Volley.newRequestQueue(this);
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
     /*
        TODO: remove this junk
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        */

        dispatchTakePictureIntent();
    }


    // take picture
    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                currentPhotoURI = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // handle activity results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                //Log.e(data.getExtras().);
                Log.v("JTS", "onActivityResult");
                String fileName = getFileName(currentPhotoURI);
                Log.v("JTS", fileName);
                String uploadFileName =
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString() +
                        "/" + fileName;

                Log.v("JTS", "file name onActivityReceived: " + fileName);

                uploadFile(uploadFileName);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = null;

        try {
             image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch( Exception e) {
            Log.v("JTS", e.toString());
        }

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }




    private void uploadFile(String uploadFileName) {
        String url = "http://172.16.1.32:8000/upload"; //  getResources().getString(R.string.post_photo_url); TODO prod url

        Log.v("JTS", "uploadFile()" + url);
        Log.v("JTS", "Upload url: " + url);
        Log.v("JTS", "file name: " + uploadFileName);


        MultipartRequest multipartRequest = new MultipartRequest(Request.Method.POST, url, uploadFileName, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    JSONObject result = new JSONObject(resultResponse);
                    String status = result.getString("status");
                    String message = result.getString("message");

                    Log.v("JTS", "status: "+ status);

                    /*
                    if (status == "200 OK") {
                        // tell everybody you have succed upload image and post strings
                        Log.i("Messsage", message);
                    } else {
                        Log.i("Unexpected", message);
                    }
                    */
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String errorMessage = "Unknown error";
                if (networkResponse == null) {
                    if (error.getClass().equals(TimeoutError.class)) {
                        errorMessage = "Request timeout";
                    } else if (error.getClass().equals(NoConnectionError.class)) {
                        errorMessage = "Failed to connect server";
                    }
                } else {
                    String result = new String(networkResponse.data);
                    try {
                        JSONObject response = new JSONObject(result);
                        String status = response.getString("status");
                        String message = response.getString("message");

                        Log.e("Error Status", status);
                        Log.e("Error Message", message);

                        if (networkResponse.statusCode == 404) {
                            errorMessage = "Resource not found";
                        } else if (networkResponse.statusCode == 401) {
                            errorMessage = message+" Please login again";
                        } else if (networkResponse.statusCode == 400) {
                            errorMessage = message+ " Check your inputs";
                        } else if (networkResponse.statusCode == 500) {
                            errorMessage = message+" Something is getting wrong";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.i("Error", errorMessage);
                error.printStackTrace();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("id", "test");
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos);
                byte[] multipartBody = new byte[0];

                try {
                    Log.v("JTS", "getByteData() starting to collecting upload bytes");
                    Log.v("JTS", "getByteData() file name: " + this.mUploadFileName);

                    //File file = new File(this.mUploadFileName);
                    File file = new File("/storage/emulated/0/Android/data/com.example.jsaito.myapplication/files/Pictures/small.jpg");
                    byte[] bytes = getFileBytes(file);

                    // the first file
                    buildPart(dos, bytes, "foo.png");  // TODO: use real name
                    // send multipart form data necesssary after file data
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    // pass to multipart body
                    multipartBody = bos.toByteArray();

                    Log.v("JTS", "finished collecting upload bytes");

                    params.put("upload", new DataPart("file_avatar.jpg", multipartBody, "image/jpeg"));

                    //Log.v("JTS", params.toString());
                } catch (IOException e) {
                    Log.v("JTS", "error: issues");
                    e.printStackTrace();
                }

                return params;
            }
        };


        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(multipartRequest);

        // TODO why does this give compiler warnings
        // VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);


        // TODO remove this shite
        /* Volley implementation
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        byte[] multipartBody = new byte[0];
        try {
            byte[] bytes = getFileBytes(uploadFile);

            // the first file
            buildPart(dos, bytes, "foo.png");  // TODO: use real name
            // send multipart form data necesssary after file data
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            // pass to multipart body
            multipartBody = bos.toByteArray();
        } catch (IOException e) {
            Log.v("JTS", "error: cannt file issues");
            e.printStackTrace();
        }

        Map<String, String> params = ;
        MultipartRequest multipartRequest = new MultipartRequest(url, null, params, mimeType, multipartBody, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                //Toast.makeText(context, "Upload successfully!", Toast.LENGTH_SHORT).show();
                Log.v("JTS", "received JSON response");
                Log.v("JTS", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                StackTraceElement[] stackTraceElements = error.getStackTrace();
                for(int i=0; i<stackTraceElements.length; i++) {
                    StackTraceElement element = stackTraceElements[i];
                    Log.v("JTS", "error and shit: " + element.toString());
                }

                //Toast.makeText(context, "Upload failed!\r\n" + error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        // Access the RequestQueue through your singleton class.



        // testing
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(multipartRequest);
        */
    }



    // TODO obsolete
    private void buildPart(DataOutputStream dataOutputStream, byte[] fileData, String fileName) throws IOException {
        dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
        dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\""
                + fileName + "\"" + lineEnd);
        dataOutputStream.writeBytes(lineEnd);

        ByteArrayInputStream fileInputStream = new ByteArrayInputStream(fileData);
        int bytesAvailable = fileInputStream.available();

        int maxBufferSize = 1024 * 1024;
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        // read file and write it into form...
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dataOutputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        dataOutputStream.writeBytes(lineEnd);
    }


    // low level shit for android
    public static byte[] getFileBytes(File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[(int) file.length()];
        int numRead = in.read(buf);
        return buf;
    }






/*


    // upload file
    public void uploadFile(String fileName){
        Log.v("JTS", "starting upload");
        try {
            // Set your file path here
            FileInputStream fstrm = new FileInputStream(fileName);

            // Set your server page url (and the file title/description)
            String postPhotoURL = getResources().getString(R.string.post_photo_url);
            HttpFileUpload hfu = new HttpFileUpload(postPhotoURL, "id", fileName);

            hfu.Send_Now(fstrm);

            Log.v("JTS", "sent");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


*/










}
