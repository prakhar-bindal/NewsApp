package com.jigsaw.prakhar.newsapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();

    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase newsDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listview = (ListView) findViewById(R.id.listview);

        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);

        listview.setAdapter(arrayAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(),NewsActivity.class);
                intent.putExtra("content",content.get(i));

                startActivity(intent);
            }
        });

        newsDB = this.openOrCreateDatabase("NEWS",MODE_PRIVATE,null);

        newsDB.execSQL("CREATE TABLE IF NOT EXISTS news ( id INTEGER PRIMARY KEY, articleid INTEGER, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();

        task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
    }

    public void updateListView(){

        Cursor c = newsDB.rawQuery("SELECT * FROM news",null);

        int contentIndex = c.getColumnIndex("content");

        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){

            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }

    public class  DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while(data!=-1)
                {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray array = new JSONArray(result);

                int numofitems = 20;

                if(array.length()<20){
                    numofitems = array.length();
                }

                newsDB.execSQL("DELETE FROM news");

                for(int i = 0;i<numofitems;i++){
                    String id = array.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+id+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleinfo = "";

                    while(data!=-1)
                    {
                        char current = (char) data;
                        articleinfo += current;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleinfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        in = urlConnection.getInputStream();

                        reader = new InputStreamReader(in);

                        data = reader.read();

                        String articleContent = "";

                        while(data!=-1)
                        {
                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }

                        String sql = "INSERT INTO news (articleid,title,content) VALUES (?, ?, ?)";

                        SQLiteStatement statement = newsDB.compileStatement(sql);

                        statement.bindString(1,id);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);

                        statement.execute();
                    }

                }

            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        protected void onPostExecute(String s){
            super.onPostExecute(s);

            updateListView();
        }
    }
}
