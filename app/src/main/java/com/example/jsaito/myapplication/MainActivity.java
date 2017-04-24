package com.example.jsaito.myapplication;

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public static Uri currentPhotoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called when the user taps the Send button
     */
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
                Log.v("JTS", "cannot create file: " + ex.getMessage());
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
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                String fileName = getFileName(currentPhotoURI);
                String uploadFilePath =
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();

                Log.v("JTS", "start scaling: ");
                ImageScaler imageScaler = new ImageScaler(uploadFilePath, fileName);
                String scaledFilePath = imageScaler.scale();
                Log.v("JTS", "imageScaler output: " + scaledFilePath);

                uploadFile(scaledFilePath);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;

        try {
            image = File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        } catch (Exception e) {
            Log.v("JTS", e.toString());
        }

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
        String url = getResources().getString(R.string.post_photo_url); //"http://192.168.101.89:8000/upload"; TODO prod url
        // String url = "http://192.168.101.89:8000/upload";

        MultipartRequest multipartRequest = new MultipartRequest(Request.Method.POST, url,
                uploadFileName, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                String resultResponse = new String(response.data);
                try {
                    Log.v("JTS", "onResponse(): status code" + response.statusCode);
                    Log.v("JTS", "onResponse(): response.data" + resultResponse);

                    if (response.statusCode == 200) {
                        JSONObject result = new JSONObject(resultResponse);
                        String resultContent = result.getString("Result");
                        Log.v("JTS", "result content: " + resultContent);
                    } else {
                        Log.v("JTS", "Unexpected response");
                    }
                } catch (JSONException e) {
                    Log.v("JTS", "onResponse(): error, " + e);
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

                    if (networkResponse.statusCode == 404) {
                        errorMessage = "Resource not found";
                    } else if (networkResponse.statusCode == 401) {
                        errorMessage = result + " Please login again";
                    } else if (networkResponse.statusCode == 400) {
                        errorMessage = result + " Check your inputs";
                    } else if (networkResponse.statusCode == 500) {
                        errorMessage = result + " Something is getting wrong";
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

                try {
                    File file = new File(this.mUploadFileName);
                    //File file = new File("/storage/emulated/0/Android/data/com.example.jsaito.myapplication/files/Pictures/small.jpg");
                    byte[] bytes = getFileBytes(file);

                    Log.v("JTS", "finished collecting upload bytes for file: " + this.mUploadFileName);

                    params.put("upload", new DataPart("file_avatar.jpg", bytes, "image/jpeg"));
                } catch (IOException e) {
                    Log.v("JTS", "error: issues");
                    e.printStackTrace();
                }

                return params;
            }
        };

        VolleySingleton queue = VolleySingleton.getInstance(getBaseContext());
        queue.clear();
        multipartRequest.setShouldCache(false);  // TODO remove in production?
        queue.addToRequestQueue(multipartRequest);
    }

    // file as array of byte
    protected static byte[] getFileBytes(File file) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        byte[] buf = new byte[(int) file.length()];
        int numRead = in.read(buf);
        return buf;
    }
}