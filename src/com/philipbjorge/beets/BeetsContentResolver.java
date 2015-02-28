package com.philipbjorge.beets;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.MediaStore;

import com.goebl.david.Webb;

public class BeetsContentResolver {

	private static final Map<String, String> uri_to_server_url;
	static
	{
		uri_to_server_url = new HashMap<String, String>();
		uri_to_server_url.put(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.toString(), "/albums");
		uri_to_server_url.put(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(), "/songs");
		uri_to_server_url.put(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI.toString(), "/artists");
	}
	
	private static final String base_url = "http://192.168.0.104:8080";

    public static Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
    	return query(uri, projection, selection, selectionArgs, sortOrder, null);
    }

	public static Cursor query(final Uri uri, String[] projection,
	            String selection, String[] selectionArgs, String sortOrder,
	            CancellationSignal cancellationSignal) {
		Boolean isArtistAlbumUrl = (uri.toString().startsWith("content://media/external/audio/artists/") && uri.toString().endsWith("/albums"));
		Boolean isAlbumUrl = (uri.toString().startsWith("content://media/external/audio/albums/"));
		if (!uri_to_server_url.containsKey(uri.toString()) && !isArtistAlbumUrl && !isAlbumUrl) {
			return null; // pass through
		}
		
		MatrixCursor c = new MatrixCursor(projection);
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy); 
		
		try
		{
			Webb webb = Webb.create();
			webb.setBaseUri(base_url);
			
	        // Hack
	        String postUri = "";
	        if (isArtistAlbumUrl) {
	        	postUri = "/albums";
	        	if (selection == null) selection = "";
	        	selection += " artist_id=" + uri.toString().replaceAll("\\D+","");
	        	selection = selection.trim();
	        }
	        else if (isAlbumUrl) {
	        	postUri = "/albums";
	        	if (selection == null) selection = "";
	        	selection += " _id=" + uri.toString().replaceAll("\\D+","");
	        	selection = selection.trim();
	        }
	        else {
	        	postUri = uri_to_server_url.get(uri.toString());
	        }
			
	        JSONObject holder = new JSONObject();
	        holder.put("fields", new JSONArray(projection));
	        if (selection != null) holder.put("selection", selection);
	        if (selectionArgs != null) holder.put("selectionArgs", new JSONArray(selectionArgs));
	        if (sortOrder != null) holder.put("sortOrder", sortOrder);
	        
			JSONArray a = webb.post(postUri)
					.body(holder)
					.ensureSuccess()
					.asJsonArray().getBody();
			
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = a.getJSONObject(i);
				
				MatrixCursor.RowBuilder b = c.newRow();
				for (String p : projection)
				{
				  b.add(p, o.get(p));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return c;
	}

	public static ParcelFileDescriptor openFileDescriptor(Uri uri, String string) {
		return null;
	}

}
