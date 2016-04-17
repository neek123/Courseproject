package com.ntnu.kristian.courseproject;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

//
//YOUTUBE API KEY: AIzaSyCzuvfmoET-A0tHJwE-f8nTYWdBFtWVHgA
//
public class DetailActivityFragment extends Fragment {
    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private AndroidFlavor poster;
    private TextView tv_overView;
    private TextView tv_release;
    private TextView tv_title;

    private YouTubePlayer YPlayer;

    final public String YOUTUBE_API_KEY = "AIzaSyCzuvfmoET-A0tHJwE-f8nTYWdBFtWVHgA";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        tv_overView = (TextView) rootView.findViewById(R.id.detail_overViewTV);
        tv_release = (TextView) rootView.findViewById(R.id.detail_release);
        tv_title = (TextView) rootView.findViewById(R.id.detail_titleTV);
        Intent intent = getActivity().getIntent();
        if (intent != null){
            // receives the poster object from intent
            poster = intent.getParcelableExtra("movieTag");
            // initializes imageview from fragment_detail
            ImageView imgView = (ImageView) rootView.findViewById(R.id.detail_posterIV);
            // base url, common for all movieposters
            // w780 size, bigger is always better! (assuming you have fast internet)
            String baseUrl = "http://image.tmdb.org/t/p/w342";
            // Uses picasso library to load image from url to imageview

            tv_title.setText(poster.versionName);
            tv_overView.setText(poster.overView);
            tv_release.setText(poster.releaseDate);

            Picasso.with(getContext()).load(baseUrl + poster.posterNumber).into(imgView);

            GetTrailer();
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(poster.versionName);
    }

    public void LoadVideo(String link){
        YouTubePlayerSupportFragment youTubePlayerFragment = YouTubePlayerSupportFragment.newInstance();
        final String youtubeLink = link;
        youTubePlayerFragment.initialize(YOUTUBE_API_KEY, new OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {

                if (!wasRestored) {
                    YPlayer = player;
                    //YPlayer.setFullscreen(true);
                    YPlayer.setShowFullscreenButton(false);
                    YPlayer.loadVideo(youtubeLink);
                    YPlayer.pause();

                    //YPlayer.play();
                }
            }
            @Override
            public void onInitializationFailure(YouTubePlayer.Provider arg0, YouTubeInitializationResult arg1) {
                // TODO Auto-generated method stub

            }
        });
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.youtube_fragment, youTubePlayerFragment).commit();
    }

    public void GetTrailer(){
        TrailerReceive trailer = new TrailerReceive();
        trailer.execute();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser) {
            Activity a = getActivity();
            if(a != null) a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public class TrailerReceive extends AsyncTask<String, Void, String> {
        private final String LOG_TAG = TrailerReceive.class.getSimpleName();

        @Override
        protected String doInBackground(String... params){
            // JsonList
            // http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key=5aa5bc75c39f6d200fa6bd741896baaa

            Log.d(LOG_TAG, "doInBackground");
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String API_KEY = "5aa5bc75c39f6d200fa6bd741896baaa";
            String posterJsonStr = null;

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("http://api.themoviedb.org/3/movie/" + poster.id + "/videos");
            stringBuilder.append("?api_key=" + API_KEY);

            try {
                URL url = new URL(stringBuilder.toString());

                // Create the request to theMovieDataBase, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine()) != null){
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                posterJsonStr = buffer.toString();

            } catch (IOException e){
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("MainActivityFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getPosterDataFromJson(posterJsonStr);
            } catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return null;
        }

        private String getPosterDataFromJson(String json)
                throws JSONException {
            //
            // Used a jsonformatter to look at how the json is arranged, to know what arrays and objects I need

            Log.d(LOG_TAG, "getPosterData ");
            final String OWM_RESULTS = "results";
            final String OWM_KEY = "key";

            JSONObject posterJson = new JSONObject(json);
            JSONArray posterArray = posterJson.getJSONArray(OWM_RESULTS);
            if(posterArray.length() != 0)
                return posterArray.getJSONObject(0).getString(OWM_KEY);
            return "";
        }

        @Override
        protected void onPostExecute(String result){
            if(result != null){
                Log.d(LOG_TAG, "onPostExecute");
                // result = youtube id for trailer, e.g: youtube.com/watch?v=<result>

                if(result != "")
                    LoadVideo(result);
            }
        }
    }
}