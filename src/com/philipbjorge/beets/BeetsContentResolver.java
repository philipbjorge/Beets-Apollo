package com.philipbjorge.beets;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.MediaStore;

import com.andrew.apollo.ApolloApplication;
import com.andrew.apollo.utils.PreferenceUtils;
import com.goebl.david.Webb;

public class BeetsContentResolver {

	private static final Map<String, String> uri_to_server_url;
	static {
		uri_to_server_url = new HashMap<String, String>();
		uri_to_server_url.put(
				MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.toString(),
				"/albums");
		uri_to_server_url.put(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString(),
				"/songs");
		uri_to_server_url.put(
				MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI.toString(),
				"/artists");
		uri_to_server_url.put(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI.toString(), 
				"/genres");
	}

	public static String getStreamUrl() {
		return PreferenceUtils.getInstance(ApolloApplication.getAppContext()).getBeetsBaseUrl() + "/stream?";
	}
	
	public static Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		return query(uri, projection, selection, selectionArgs, sortOrder, null);
	}

	public static Cursor query(final Uri uri, String[] projection,
			String selection, String[] selectionArgs, String sortOrder,
			CancellationSignal cancellationSignal) {
		Boolean isArtistAlbumUrl = (uri.toString().startsWith(
				"content://media/external/audio/artists/") && uri.toString()
				.endsWith("/albums"));
		Boolean isAlbumUrl = (uri.toString()
				.startsWith("content://media/external/audio/albums/"));
		Boolean isGenreSongsUrl = (uri.toString().startsWith(
				"content://media/external/audio/genres") && uri.toString().endsWith(
						"/members"));
		if (!uri_to_server_url.containsKey(uri.toString()) && !isArtistAlbumUrl
				&& !isAlbumUrl && !isGenreSongsUrl) {
			return null; // pass through
		}

		MatrixCursor c = null;

		// TODO: NASTY NASTY NASTY Hack to make the static musicutils resolvers
		// work
		// These queries are assumed to be fast, so don't excute on an
		// asynctask...
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
				.permitAll().build();
		StrictMode.setThreadPolicy(policy);

		try {
			Webb webb = Webb.create();
			webb.setBaseUri(PreferenceUtils.getInstance(ApolloApplication.getAppContext()).getBeetsBaseUrl());

			// Hack
			String postUri = "";
			if (isArtistAlbumUrl) {
				postUri = "/albums";
				if (selection == null) selection = "";
				if (!selection.isEmpty()) selection += " AND ";
				selection += " artist_id="
						+ uri.toString().replaceAll("\\D+", "");
				selection = selection.trim();
			} else if (isAlbumUrl) {
				postUri = "/albums";
				if (selection == null) selection = "";
				if (!selection.isEmpty()) selection += " AND ";
				selection += " _id=" + uri.toString().replaceAll("\\D+", "");
				selection = selection.trim();
			} else if (isGenreSongsUrl) {
				postUri = "/songs";
				if (selection == null) selection = "";
				if (!selection.isEmpty()) selection += " AND ";
				selection += " genre_id=" + uri.toString().replaceAll("\\D+", "");
				selection = selection.trim();
			} else {
				postUri = uri_to_server_url.get(uri.toString());
			}

			JSONObject holder = new JSONObject();
			holder.put("fields", new JSONArray(projection));
			if (selection != null)
				holder.put("selection", selection);
			if (selectionArgs != null)
				holder.put("selectionArgs", new JSONArray(selectionArgs));
			if (sortOrder != null)
				holder.put("sortOrder", sortOrder);

			JSONArray a = webb.post(postUri).body(holder).ensureSuccess().retry(2, false)
					.asJsonArray().getBody();

			// handles audio._id AS xyz
			for (int i = 0; i < projection.length; i++) {
				String p = projection[i];
				projection[i] = p.substring(p.lastIndexOf(" ")+1);
			}
			c = new MatrixCursor(projection);
			for (int i = 0; i < a.length(); i++) {
				JSONObject o = a.getJSONObject(i);

				MatrixCursor.RowBuilder b = c.newRow();
				for (String p : projection) {
					b.add(p, o.get(p));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return c;
	}

	public static ParcelFileDescriptor openFileDescriptor(Uri uri, String string) {
		// TODO: Consider reverting changes in ImageWorker
		// and making this fetch from the web and return ParcelFileDescriptor
		return null;
	}

}
