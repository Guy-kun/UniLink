package io.moe.unilink;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class Main extends ListActivity implements ActionBar.OnNavigationListener, PullToRefreshAttacher.OnRefreshListener {
    Object imageLock = new Object();
    Object userInfoLock = new Object();

    Twitter twitter;
    ArrayList<User> users;
    UserAdapter adapter;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    private PullToRefreshAttacher mPullToRefreshAttacher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        users = new ArrayList<User>();


        ArrayList<User> initialUsers = new ArrayList<User>();

        User u = new User();
        u.id=2157036348l;
        User v = new User();
        v.id=102014181l;
        initialUsers.add(u);
        initialUsers.add(v);
        initialUsers.add(v);
        initialUsers.add(v);
        initialUsers.add(v);
        initialUsers.add(v);
        initialUsers.add(v);

        adapter=new UserAdapter(this, R.layout.user_card, users);
        setListAdapter(adapter);

        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayList<String> itemList = new ArrayList<String>();
        itemList.add("My University");
        itemList.add("My Module");
        itemList.add("My Year");
        ArrayAdapter<String> aAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, itemList);
        actionBar.setListNavigationCallbacks(aAdpt, this);
        actionBar.setSelectedNavigationItem(1);


        // Create a PullToRefreshAttacher instance
        mPullToRefreshAttacher = PullToRefreshAttacher.get(this);

        // Add the Refreshable View and provide the refresh listener
        mPullToRefreshAttacher.addRefreshableView(getListView(), this);



        StartupTask t = new StartupTask();
        t.execute(initialUsers);
    }

    @Override
    protected void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.this);
        TextView vv = (TextView)findViewById(R.id.emptytext);
        if (prefs.getString("institution","").length()==0)
        {
            vv.setText("No University configured :(\n\nTap to set up");
            vv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
                   View dialog = getLayoutInflater().inflate(R.layout.setupdialog,null);


                    // set the custom dialog components - text, image and button
                    final EditText inst = (EditText)dialog.findViewById(R.id.setup_inst);
                    final EditText module = (EditText)dialog.findViewById(R.id.setup_module);
                    final EditText year = (EditText)dialog.findViewById(R.id.setup_year);
                    builder.setView(dialog).setTitle("Setup")
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.this);
                                    SharedPreferences.Editor e = prefs.edit();
                                    e.putString("institution",inst.getText().toString());
                                    e.putString("module",module.getText().toString());
                                    e.putString("finishyear", year.getText().toString());
                                    e.commit();
                                    dialog.dismiss();

                                    //Twitter setup
                                    String url = "http://www.twitter.com/intent/tweet?text=@unilinkreg "+inst.getText().toString()
                                            +" "+ module.getText().toString() + " " +year.getText().toString();
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(url));
                                    startActivity(i);
                                }
                            }).create().show();
                }
            });
        }
        else{
            vv.setText("No-one here yet!");
            vv.setOnClickListener(null);

        }
        super.onResume();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        onRefreshStarted(getListView());
        return true;
    }


    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.action_settings){
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        }
        else if (item.getItemId()==R.id.action_refresh){
            onRefreshStarted(getListView());
        }
        return super.onOptionsItemSelected(item);
    }

    class UserAdapter extends ArrayAdapter<User> {

        Context context;
        int layoutResourceId;
        ArrayList<User> users = null;

        public UserAdapter(Context mContext, int layoutResourceId,  ArrayList<User> users) {
            super(mContext,layoutResourceId,R.id.card_username,users);
            this.layoutResourceId = layoutResourceId;
            this.users = users;
            this.context= mContext;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                // inflate the layout
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId,null);
            }


            synchronized (userInfoLock)
            {
            // object item based on the position
            final User user = users.get(position);

            TextView username = (TextView) convertView.findViewById(R.id.card_username);
            TextView name = (TextView) convertView.findViewById(R.id.card_name);
            TextView tagline = (TextView) convertView.findViewById(R.id.card_tagline);
            username.setText("@"+user.username);
            name.setText(user.name);
            tagline.setText(user.tagline);

                ImageView avatar = (ImageView) convertView.findViewById(R.id.card_avatar);
                avatar.setImageBitmap(user.avatar);
                avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Twitter setup
                        String url = "https://twitter.com/intent/user?screen_name="+user.username;
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });
            }



            return convertView;
        }

    }

    class StartupTask extends AsyncTask<Object,Void,Void>{
        Object userlist;

        @Override
        protected Void doInBackground(Object... objects) {
            userlist=objects[0];
            ConfigurationBuilder cb = new ConfigurationBuilder();
            //cb.setApplicationOnlyAuthEnabled(true);
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey("dWaJ5ZaaGVVvxQIu0Tsw")
                    .setOAuthConsumerSecret("EeVAXbSNQFS4q7WxLXpIuKeCL4QIs4sDynLlt3Gaiw")
                    .setOAuthAccessToken("2157036348-0nQc6dFkGRuPYeWBKTFGxGX0CGq5AXblDKX9GRC")
                    .setOAuthAccessTokenSecret("CGR50c88AJCuIH7a5E5iy0GYsG8tf7rLA7OqXwJqv6mWN");
            try{
            TwitterFactory tf = new TwitterFactory(cb.build());
            twitter = tf.getInstance();
            String s = twitter.getScreenName();
            }
            catch (Exception ex){
                //Toast.makeText(Main.this,"Twitter app auth failed",Toast.LENGTH_SHORT).show();
                twitter=null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (twitter!=null){
            onRefreshStarted(getListView());
            }
            super.onPostExecute(aVoid);
        }
    }

    class LoadAvatarTask extends AsyncTask<Object,Void,Void>{
        User user;

        @Override
        protected Void doInBackground(Object... objects) {
            try{
                user = (User)objects[0];
                URL url = new URL(user.avatarurl);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                synchronized (userInfoLock)
                {
                    user.avatar=bmp;
                }

            }
            catch (Exception ex){
                //As if we care
                //twitter=null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            synchronized (userInfoLock)
            {
            ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
            }
            super.onPostExecute(aVoid);
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                mPullToRefreshAttacher.setRefreshing(true);
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    synchronized (userInfoLock){
                        users.clear();
                    }

                    ArrayList<User> initialUsers = new ArrayList<User>();
                    int filterType = getActionBar().getSelectedNavigationIndex();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Main.this);

                    String uni = prefs.getString("institution","");
                    String module = prefs.getString("module","");
                    String year = prefs.getString("finishyear","");

                    if (uni.length()==0)
                    {
                        return null;
                    }

                    if (filterType==0){
                        module="n";
                        year ="n";
                    }
                    if (filterType==1){
                        year ="n";
                    }
                    module = module.length()==0?"n":module;
                    year = year.length()==0?"n":year;

                    try{
                        HttpURLConnection connection;
                        OutputStreamWriter request = null;

                        String url = "https://unilink-server-c9-southrop.c9.io/api?institute="+uni+"&course="+module+"&year="+year+"&filter=n";

                        try
                        {
                            HttpGet httpGet = new HttpGet(url);
                            HttpParams httpParameters = new BasicHttpParams();
                            int timeoutConnection = 3000;
                            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
                            int timeoutSocket = 5000;
                            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

                            DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
                            HttpResponse httpResponse = httpClient.execute(httpGet);
                            HttpEntity httpEntity = httpResponse.getEntity();
                            String response = EntityUtils.toString(httpEntity);

                            JSONArray a = new JSONArray(response);
                                for (int i = 0; i< a.length();i++){
                                    User u = new User();
                                    u.id = a.getJSONObject(i).getLong("id");
                                    initialUsers.add(u);
                                }

                        }
                        catch(IOException e)
                        {
                            Log.d("UniLink","Server GET blew up");

                        }

                    }
                    catch (Exception ex){

                    }

                    if (initialUsers.size()>0)
                    {

                    long[] ids = new long[initialUsers.size()];
                    for (int i = 0; i < initialUsers.size();i++){
                        ids[i]= initialUsers.get(i).id;
                    }

                    ResponseList<twitter4j.User> responseUsers = twitter.lookupUsers(ids);
                    synchronized (userInfoLock)
                    {
                        for (int i = 0;i<responseUsers.size();i++){
                            twitter4j.User twitteruser = responseUsers.get(i);

                            if (twitteruser!=null){
                                User u = new User();
                                u.name = twitteruser.getName();
                                u.username = twitteruser.getScreenName();
                                u.avatarurl = twitteruser.getOriginalProfileImageURL();
                                u.tagline=twitteruser.getDescription();
                                users.add(u);

                                LoadAvatarTask t = new LoadAvatarTask();
                                t.execute(u);
                            }
                        }
                    }
                    }
                    else
                    {
                        Log.d("UniLink","server gave no user IDs");

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("UniLink","Twitter threw an exception fetching users");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                synchronized (userInfoLock)
                {
                    ((BaseAdapter)getListAdapter()).notifyDataSetChanged();
                }
                // Notify PullToRefreshAttacher that the refresh has finished
                mPullToRefreshAttacher.setRefreshComplete();
            }
        }.execute();
    }
    
}
