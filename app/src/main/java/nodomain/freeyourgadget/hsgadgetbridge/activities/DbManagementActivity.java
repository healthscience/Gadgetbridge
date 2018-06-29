/*  Copyright (C) 2016-2018 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.hsgadgetbridge.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.hsgadgetbridge.GBApplication;
import nodomain.freeyourgadget.hsgadgetbridge.R;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.hsgadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.hsgadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.hsgadgetbridge.util.GB;
import nodomain.freeyourgadget.hsgadgetbridge.util.ImportExportSharedPreferences;


public class DbManagementActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DbManagementActivity.class);
    private static SharedPreferences sharedPrefs;
    private ImportExportSharedPreferences shared_file = new ImportExportSharedPreferences();

    private Button exportDBButton;
    private Button importDBButton;
    private Button deleteOldActivityDBButton;
    private Button deleteDBButton;
    private Button syncDBButton;
    private TextView dbPath;

    public static final String DATABASE_NAME = "Gadgetbridge";

    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    private String url = "http://188.166.138.93:8882/";
    private String urlsave = "http://188.166.138.93:8882/datamsave/";
    private String urltoken;
    private String editTokenStr;
    // holds sync data for POST call
    ArrayList batchdata;
    JSONArray arraysync = new JSONArray();
    private String nowDate;
    private String lastsyncDate;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_management);

        dbPath = findViewById(R.id.activity_db_management_path);
        dbPath.setText(getExternalPath());

        exportDBButton = findViewById(R.id.exportDBButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();
            }
        });
        importDBButton = findViewById(R.id.importDBButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });

        syncDBButton = findViewById(R.id.syncDBButton);
        syncDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // first check there is interenet connection
                boolean InternetOn = isNetworkConnected();
                if(InternetOn == true) {

                    syncDB();
                }
                else {
                    Toast.makeText(getApplicationContext(),"NO INTERNET CONNECTION", Toast.LENGTH_LONG).show();
                }

            }
        });

        int oldDBVisibility = hasOldActivityDatabase() ? View.VISIBLE : View.GONE;

        deleteOldActivityDBButton = findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setVisibility(oldDBVisibility);
        deleteOldActivityDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOldActivityDbFile();
            }
        });

        deleteDBButton = findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private boolean hasOldActivityDatabase() {
        return new DBHelper(this).existsDB("ActivityDatabase");
    }

    private String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
            LOG.warn("Unable to get external files dir", ex);
        }
        return getString(R.string.dbmanagementactivvity_cannot_access_export_path);
    }

    private void exportShared() {
        // BEGIN EXAMPLE
        File myPath = null;
        try {
            myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.exportToFile(sharedPrefs,myFile,null);
        } catch (IOException ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_shared, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void importShared() {
        // BEGIN EXAMPLE
        File myPath = null;
        try {
            myPath = FileUtils.getExternalFilesDir();
            File myFile = new File(myPath, "Export_preference");
            ImportExportSharedPreferences.importFromFile(sharedPrefs,myFile );
        } catch (Exception ex) {
            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            GB.toast(this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    // batching utility method
    public static <T> List<List<T>> getBatches(List collection, int batchSize){
        int i = 0;
        List<List<T>> batches = new ArrayList<List<T>>();
        while(i<collection.size()){
            int nextInc = Math.min(collection.size()-i,batchSize);
            List<T> batch = collection.subList(i,i+nextInc);
            batches.add(batch);
            i = i + nextInc;
        }

        return batches;
    }

    // prepare data to sync to peer to peer network
    private void syncDB() {

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();

            GB.toast(this, "getting ready for sync", Toast.LENGTH_LONG, GB.INFO);

            // get token from app
            EditText editToken = (EditText)findViewById(R.id.tokenText);
            String editTokenStr = editToken.getText().toString();
            //Toast.makeText(getApplicationContext(),"token in :" + editTokenStr, Toast.LENGTH_LONG).show();

            // form token URL
            urltoken = urlsave + editTokenStr;
            //Toast.makeText(getApplicationContext(),"token URL:" + urltoken, Toast.LENGTH_LONG).show();//di

            // drop table
            //db.execSQL("DROP TABLE NETWORK_SYNC_TIMESTAMP");
            //Toast.makeText(this, "Database Dropped NETWORK_SYNC_TIMESTAMP", Toast.LENGTH_LONG).show();

            // need to create a new table to save sync data to ptop work
            boolean tableExists = false;
            /* get cursor on it */
            try
            {
                db.query("NETWORK_SYNC_TIMESTAMP", null,
                    null, null, null, null, null);
                tableExists = true;
            }
            catch (Exception e) {
                /* no table set it up */
                db.execSQL("CREATE TABLE IF NOT EXISTS NETWORK_SYNC_TIMESTAMP(SYNCSTAMP INTEGER PRIMARY KEY NOT NULL)");
                Toast.makeText(this, "Database Created SYNC", Toast.LENGTH_LONG).show();

                // insert first sync date
                db.execSQL("INSERT INTO NETWORK_SYNC_TIMESTAMP(SYNCSTAMP) VALUES (0000000001)");
                Toast.makeText(this, "insert base syncstamp", Toast.LENGTH_LONG).show();


            }

            // form query to get last sync date
            Date liveDate;
            Long longmillseconds;
            Long millseconds;
            liveDate = DateTimeUtils.todayUTC();
            longmillseconds = liveDate.getTime();
            millseconds = longmillseconds/1000;
            //GB.toast(this, "date right now:" + millseconds, Toast.LENGTH_LONG, GB.INFO);

            // query last sync table to get last sync date
            String[] projectiond = {
                    "SYNCSTAMP"
            };

            // Filter results
            String selectiond = "";
            String[] selectionArgsd = { };

            // How you want the results sorted in the resulting Cursor
            String sortOrder =
                    "SYNCSTAMP" + " DESC";

            Cursor lastsync = db.query(
                    "NETWORK_SYNC_TIMESTAMP",   // The table to query
                    projectiond,             // The array of columns to return (pass null to get all)
                    selectiond,              // The columns for the WHERE clause
                    selectionArgsd,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrder               // The sort order
            );

            lastsync.moveToFirst();
            String lastsyncqdate = lastsync.getString(lastsync.getColumnIndex("SYNCSTAMP"));

            TextView tv=(TextView)findViewById(R.id.syncText);
            tv.setText("Last sync date: " + lastsyncqdate);
            Toast.makeText(this, "SYNCING STARTED", Toast.LENGTH_LONG).show();

            // display the last success data point
            TextView stv=(TextView)findViewById(R.id.syncDate);
            stv.setText("Success sync date: " + lastsyncqdate);

            lastsyncDate = lastsyncqdate;//"1526033220";//lastsyncqdate;
            nowDate = millseconds.toString();//"1529666370";//millseconds.toString();

            // display now date
            TextView ntv=(TextView)findViewById(R.id.nowDate);
            ntv.setText("NOW date: " + nowDate);

            // form the query for data
            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    "TIMESTAMP",
                    "STEPS",
                    "HEART_RATE"
            };

            // Filter results
            String selection = "DEVICE_ID" + " = ? AND TIMESTAMP BETWEEN ? AND ?";
            String[] selectionArgs = { "1", lastsyncDate, nowDate };

            // How you want the results sorted in the resulting Cursor
            String sortOrderd =
                    "TIMESTAMP" + " ASC";

            Cursor cursor = db.query(
                    "MI_BAND_ACTIVITY_SAMPLE",   // The table to query
                    projection,             // The array of columns to return (pass null to get all)
                    selection,              // The columns for the WHERE clause
                    selectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrderd               // The sort order
            );

            String message = null;

            // iterate of data in cursor form/ pass data on to volley
            // find size of data
            Integer syncLength = cursor.getCount();
            //Toast.makeText(getApplicationContext(),"cursor length" + syncLength, Toast.LENGTH_LONG).show();

            // first prepare normal list array from Cursor to allow sublit operation to batch
            List all = new ArrayList<>();
            while(cursor.moveToNext()) {

                //List itemIds = new ArrayList<>();
                JSONObject item = new JSONObject();

                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("TIMESTAMP"));
                long stepsIN = cursor.getLong(
                        cursor.getColumnIndexOrThrow("STEPS"));
                long heartrateIN = cursor.getLong(
                        cursor.getColumnIndexOrThrow("HEART_RATE"));

                //all.add(itemId);
                //JSONObject json = new JSONObject();
                // form an object and add to array list
                item.put("timestamp", itemId);
                item.put("steps", stepsIN);
                item.put("heartrate", heartrateIN);
                //itemIds.add(item);
                all.add(item);

            }
            cursor.close();
            // find size of new LIST
            Integer allLength = all.size();
            Toast.makeText(getApplicationContext(),"New LIST length" + allLength, Toast.LENGTH_LONG).show();

            //prepare batches
            List batched = getBatches(all,1000);
            Integer batchLength = batched.size();
            Toast.makeText(getApplicationContext(),"Batched length" + batchLength, Toast.LENGTH_LONG).show();

            if(syncLength <= 1000 && syncLength != 0) {
                // make a put ie save to HS network
                //TextView btv=(TextView)findViewById(R.id.syncText);
                //btv.setText("RAW Batched chunk: " + batched.get(0).toString());
                //Toast.makeText(getApplicationContext(),"Length under 1000", Toast.LENGTH_LONG).show();
                prepareSyncJSON((List) batched.get(0));

            }
            else {
                // save every
                //Toast.makeText(getApplicationContext(),"Length OVER 1000 or Zero", Toast.LENGTH_LONG).show();
                //if zero nothing to update
                //else batch and sync
                if(syncLength == 0) {
                    Toast.makeText(getApplicationContext(),"Nothing to Sync", Toast.LENGTH_LONG).show();

                }
                else {
                    Toast.makeText(getApplicationContext(), "Preparing Batches for Sync", Toast.LENGTH_LONG).show();
                    // itterate batch listed
                    int j = 0;
                    while (batched.size() > j) {
                        j++;
                        if(batchLength == j) {

                            Toast.makeText(getApplicationContext(), "Data sync COMPLETE", Toast.LENGTH_SHORT).show();
                            TextView tvcomplete=(TextView)findViewById(R.id.syncText);
                            tvcomplete.setText("SYNC COMPLETED");
                        }
                        // batch limit reached - form JSON and send
                        //Toast.makeText(getApplicationContext(), "Batch" + j, Toast.LENGTH_SHORT).show();
                        // extract/pass on data to
                        prepareSyncJSON((List) batched.get(j));
                        //arraysync = new JSONArray();

                    }
                }
            }

        } catch (Exception ex) {
            //GB.toast(this, "error with PRE sync", Toast.LENGTH_LONG, GB.ERROR, ex);
        }

    }

    private void sendAndRequestResponse() {

        //RequestQueue initialized
        mRequestQueue = Volley.newRequestQueue(this);

        //String Request initialized
        mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Toast.makeText(getApplicationContext(),"Response :" + response.toString(), Toast.LENGTH_LONG).show();//display the response on screen

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.i(null,"Error :" + error.toString());
            }
        });

        mRequestQueue.add(mStringRequest);
    }

    private void prepareSyncJSON (Object batchIN) {
        // form JSONarray
        //JSONArray mJSONArray = new JSONArray(Arrays.asList(batchIN));
        Object json = null;
        JSONArray jsonArray = null;
        try {
            json = new JSONTokener(batchIN.toString()).nextValue();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json instanceof JSONArray) {
            jsonArray = (JSONArray) json;
        }

        arraysync = jsonArray;
        PostandRequestResponse();

    }

    private void PostandRequestResponse() {

        //Toast.makeText(getApplicationContext(),"token URL:" + urltoken, Toast.LENGTH_LONG).show();
        //TextView stv=(TextView)findViewById(R.id.syncDate);
        //stv.setText("Godo JSON: " + arraysync.toString());

            //Toast.makeText(getApplicationContext(),"within POST:" + urltoken, Toast.LENGTH_LONG).show();//di
            // JsonObjectRequest JsonArrayRequest types of data to post
            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.POST, urltoken,arraysync,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            //serverResp.setText("String Response : "+ response.toString());
                            //Toast.makeText(getApplicationContext(),"String Response : "+ response.toString(), Toast.LENGTH_LONG).show();

                            // need if check save good then clear array object
                            JSONObject tot_obj = null;
                            try {
                                tot_obj = arraysync.getJSONObject(arraysync.length()-1);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            String lastsyncitem = tot_obj.optString("timestamp");
                            syncTimestampDB(lastsyncitem);

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //serverResp.setText("Error getting response");
                    Toast.makeText(getApplicationContext(),"save volley sync error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                    Log.i(null,"Error :" + error.toString());
                }
            });

            VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);


    }

    // save last sync timestamp to sqlite
    private void syncTimestampDB(String syncdataIN) {

        //Toast.makeText(getApplicationContext(), "before add one", Toast.LENGTH_LONG).show();
        Integer addone =  Integer.parseInt(syncdataIN);
        Integer addtwo = addone + 1;
        //Toast.makeText(getApplicationContext(), "after one add", Toast.LENGTH_LONG).show();

        String localdata = addtwo.toString();

        //TextView tv=(TextView)findViewById(R.id.syncDate);
        //tv.setText("start of update SYNSTAMP: " + syncdataIN);
        //GB.toast(this, "new sync data====" + localdata, Toast.LENGTH_LONG, GB.INFO);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
            // update sqlite SYNC table
            db.execSQL("INSERT INTO NETWORK_SYNC_TIMESTAMP(SYNCSTAMP) VALUES ( " + localdata + " )");
            //TextView stv=(TextView)findViewById(R.id.syncDate);
            //stv.setText("Success sync date: " + localdata);
            Toast.makeText(getApplicationContext(), "updated SYNC success", Toast.LENGTH_LONG).show();


        } catch (Exception e) {
            e.printStackTrace();
            //tv.setText("error SYNSTAMP: " + e.toString());

            //GB.toast(this, "error syncstamp" + e.toString(), Toast.LENGTH_LONG, GB.ERROR, e);
        }

     }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_import_data_title)
                .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                .setPositiveButton(R.string.dbmanagementactivity_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            importShared();
                            DBHelper helper = new DBHelper(DbManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_import_successful), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
                .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_database_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this).setCancelable(true);
        new AlertDialog.Builder(this).setTitle(R.string.dbmanagementactivity_delete_old_activity_db);
        new AlertDialog.Builder(this).setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation);
        new AlertDialog.Builder(this).setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (GBApplication.deleteOldActivityDatabase(DbManagementActivity.this)) {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
        new AlertDialog.Builder(this).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        new AlertDialog.Builder(this).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
