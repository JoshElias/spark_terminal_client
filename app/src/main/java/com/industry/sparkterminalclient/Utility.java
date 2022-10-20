package com.industry.sparkterminalclient;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import bolts.Task;

/**
 * Created by Two on 9/22/2014.
 */
public class Utility {
    public static String TAG = "Utility";


    static public final String CHAR_ENCODING = "UTF-8";

    /**
     *  MEMORY
     */

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));

    }


    /**
     *  IO
     */
/*
    public static byte[] getFileInBytes(File file) throws FileNotFoundException, IOException {

        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IO Error");
            e.printStackTrace();
        }

        return bytes;
    }
*/
    public static String getFileBase64Checksum(File file, String digestAlg) throws Exception {
        RandomAccessFile rFile = null;
        FileChannel fileChannel = null;

        try {
            // Get MappedByteBuffer
            rFile = new RandomAccessFile(file, "r");
            MessageDigest digest = MessageDigest.getInstance(digestAlg);
            fileChannel = new RandomAccessFile(file, "r").getChannel();
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            buffer.load();

            // Get File Checksum
            digest.update(buffer);
            byte[] rawChecksum = digest.digest();

            // Clear Buffer
            buffer.clear();

            return new String(Base64.encode(rawChecksum));

        } finally {
            if(fileChannel != null) fileChannel.close();
            if(fileChannel != null) rFile.close();
        }
        /*
        String fileString = new String(Base64.encode(Utility.getFileInBytes(file)));
        MessageDigest hash = MessageDigest.getInstance("SHA-256");
        hash.
        byte[] rawChecksum = hash.digest(fileString.getBytes("UTF-8"));
        String checksum = new String(Base64.encode(rawChecksum));
        */
    }

    public static File createTempDirectory() throws IOException {
        final File temp;

        temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

        if(!(temp.delete()))
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if(!(temp.mkdir()))
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }

    /**
     *  NETWORK
     */

    public static boolean isDownloadManagerAvailable(Context context) {
        try {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                Log.e(TAG, "SDK isn't high enough for Device Manager");
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
            List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;
        } catch(Exception e) {
            return false;
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
    }

    public static <T> void finishTask(Task<T>.TaskCompletionSource task) {
        finishTask(task, null);
    }

    public static <T> void finishTask(Task<T>.TaskCompletionSource task, Throwable t) {
        if(task == null || task.getTask().isCompleted())
            return;

        if(t == null) {
            task.setResult(null);
        } else {
            t.printStackTrace();
            task.setError(new Exception(t));
        }
    }

    public static void recursivelyGetFiles(File rootDir, ArrayList<File> fileList) {
        File[] apkFileList = rootDir.listFiles();
        File currFile = null;
        for (int i = 0; i < apkFileList.length; i++) {
            currFile = apkFileList[i];
            if (currFile.isDirectory()) {
                recursivelyGetFiles(currFile, fileList);
            } else if (currFile.isFile()) {
                fileList.add(currFile);
            }
        }
    }

    public static ArrayList<String> getAllPackageNamesInFile(PackageManager packageManager, File _file) {
        ArrayList<File> fileList = new ArrayList<File>();
        Utility.recursivelyGetFiles(_file, fileList);
        ArrayList<String> packageNames = new ArrayList<String>();

        for(File file : fileList) {
            PackageInfo info = packageManager.getPackageArchiveInfo(file.getPath(), 0);
            packageNames.add(info.packageName);
        }

        return packageNames;
    }

    public static boolean recursiveDelete(File file) {
        boolean success = true;
        ArrayList<File> foldersToDelete = new ArrayList<File>();
        emptyFolders(file, foldersToDelete);
        for(File folder : foldersToDelete) {
           if(!folder.delete()) {
               Log.e(TAG, "Unable to delete folder: "+folder.getPath());
               success = false;
           }
        }
        return success;
    }
    private static void emptyFolders(File file, ArrayList<File> foldersToDelete) {
        if(file.isDirectory()) {
            foldersToDelete.add(file);
            for(File subFile : file.listFiles()) {
                recursiveDelete(subFile);
            }
        } else {
             if(!file.delete()) {
                 Log.e(TAG, "Unable to delete file: "+file.getPath());
             }
        }
    }

    public static void unzip(File zipFile, File outputFolder) {
        byte[] buffer = new byte[(int)zipFile.length()];
        FileInputStream is = null;
        ZipInputStream zis = null;

        try{

            // create output directory if not exists
            if(!outputFolder.exists()){
                outputFolder.mkdirs();
            }

            //get the zip file content
            is = new FileInputStream(zipFile);
            zis = new ZipInputStream(is);

            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while(ze!=null){

                String fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println("Done");

        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(is != null)
                    is.close();
                if(zis != null) {
                    zis.closeEntry();
                    zis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            } else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.getDefault());
    }

    /**
     * Converts a Hex-encoded data string to the original byte data.
     *
     * @param hexData
     *            hex-encoded data to decode.
     * @return decoded data from the hex string.
     */
    public static byte[] fromHex(String hexData) {
        byte[] result = new byte[(hexData.length() + 1) / 2];
        String hexNumber = null;
        int stringOffset = 0;
        int byteOffset = 0;
        while (stringOffset < hexData.length()) {
            hexNumber = hexData.substring(stringOffset, stringOffset + 2);
            stringOffset += 2;
            result[byteOffset++] = (byte) Integer.parseInt(hexNumber, 16);
        }
        return result;
    }


    public static byte[] fromBase64(String base64Data) {
        return Base64.decode(base64Data);
    }

    public static String toBase64(byte[] data) throws UnsupportedEncodingException {
        return new String(Base64.encode(data),"UTF-8");
    }

    public static List<String> getSortedKeyList( JSONObject jsonObject ) {
        if( jsonObject == null ) return null;
        List<String> keyList = new ArrayList<String>();
        Iterator<String> keys = jsonObject.keys();
        while( keys.hasNext() ) {
            keyList.add(keys.next());
        }
        Collections.sort(keyList);
        return keyList;
    }

    public static JSONObject parseJSON( String json ) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        return jsonObject;
    }

    public static JSONArray parseJSONArray( String json ) throws JSONException {
        JSONArray jsonArray = new JSONArray(json);
        return jsonArray;
    }


    public static String stringifyJSON( JSONObject jsonObject ) throws JSONException {
        return jsonObject.toString();
    }

    public static byte[] stringifyJSONToBytes( JSONObject jsonObject ) throws JSONException, UnsupportedEncodingException {
        return jsonObject.toString().getBytes(CHAR_ENCODING);
    }

    public static JSONObject toJSONObject( JSONArray jsonArray ) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObject.put( Integer.toString(i), jsonArray.get(i) );
        }
        return jsonObject;
    }

    public static String getUniquePsuedoID()
    {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their phone or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their phone, there will be a duplicate entry
        String serial = null;
        try
        {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();
            // Go ahead and return the serial for api => 9

            Log.d("TestApplication", "serial: " + serial);


            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        }
        catch (Exception e)
        {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        Log.d("TestApplication", "serial: " + serial);

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
}
