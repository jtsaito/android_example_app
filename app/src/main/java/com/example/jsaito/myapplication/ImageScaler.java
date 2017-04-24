package com.example.jsaito.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by jsaito on 23.04.17.
 *
 * This is based on: http://stackoverflow.com/questions/39361550/android-resize-image-to-upload-to-server
 *
 *
 */

class ImageScaler {

    private String mInFilePath;
    private String mOutFilePath;

    public ImageScaler(String inFileDir, String inFileName) {
        this.mInFilePath = inFileDir + "/" + inFileName;
        this.mOutFilePath = inFileDir + "/scaled_" + inFileName;
    }

    public String scale() {
        final int MAX_IMAGE_SIZE = 64 * 1024; // max final file size in kilobytes

        // First decode with inJustDecodeBounds=true to check dimensions of image
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mInFilePath, options);

        options.inSampleSize = calculateInSampleSize(options, 400, 400); // tune tese params, maybe 200 is enough

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bmpPic = BitmapFactory.decodeFile(mInFilePath, options);

        int compressQuality = 100; // quality decreasing by steps of 5
        int streamLength;
        do {
            ByteArrayOutputStream bmpStream = new ByteArrayOutputStream();
            Log.d("compressBitmap", "Quality: " + compressQuality);
            bmpPic.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream);
            byte[] bmpPicByteArray = bmpStream.toByteArray();
            streamLength = bmpPicByteArray.length;
            compressQuality -= 5;
            Log.d("compressBitmap", "Size: " + streamLength / 1024 + " kb");
        } while (streamLength >= MAX_IMAGE_SIZE);

        try {
            //save the resized and compressed file to disk cache
            FileOutputStream bmpFile = new FileOutputStream(mOutFilePath);
            bmpPic.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpFile);
            bmpFile.flush();
            bmpFile.close();
        } catch (Exception e) {
            Log.v("JTS", "Cannot save file:" + e.getMessage());
        }

        return mOutFilePath;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        String debugTag = "MemoryInformation";

        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.d(debugTag, "image height: " + height + "---image width: " + width);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        Log.d(debugTag, "inSampleSize: " + inSampleSize);
        return inSampleSize;
    }

}