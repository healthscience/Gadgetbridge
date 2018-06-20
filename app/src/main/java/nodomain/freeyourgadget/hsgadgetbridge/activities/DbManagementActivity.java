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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.SqlUtils;
import de.greenrobot.dao.internal.DaoConfig;

import nodomain.freeyourgadget.hsgadgetbridge.GBApplication;
import nodomain.freeyourgadget.hsgadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.hsgadgetbridge.R;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.hsgadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.hsgadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.hsgadgetbridge.entities.User;
import nodomain.freeyourgadget.hsgadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.hsgadgetbridge.util.GB;
import nodomain.freeyourgadget.hsgadgetbridge.util.ImportExportSharedPreferences;

import static nodomain.freeyourgadget.hsgadgetbridge.GBApplication.getContext;


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

    JSONArray arraysync = new JSONArray();

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
                syncDB();
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

    private void syncDB() {

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            exportShared();
            DBHelper helper = new DBHelper(this);
            GB.toast(this, "getting ready for sync", Toast.LENGTH_LONG, GB.INFO);

            // get token from app
            EditText editToken = (EditText)findViewById(R.id.tokenText);
            String editTokenStr = editToken.getText().toString();
            Toast.makeText(getApplicationContext(),"token in :" + editTokenStr, Toast.LENGTH_LONG).show();
            // create variable to hold data

            // setup sqllite connection manual method ie not DAO
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();

            // form the query
            // Define a projection that specifies which columns from the database
            // you will actually use after this query.
            String[] projection = {
                    "TIMESTAMP",
                    "STEPS",
                    "HEART_RATE"
            };

            // Filter results WHERE "title" = 'My Title'
            String selection = "DEVICE_ID" + " = ?";
            String[] selectionArgs = { "1" };

            // How you want the results sorted in the resulting Cursor
            String sortOrder =
                    "TIMESTAMP" + " DESC";

            Cursor cursor = db.query(
                    "MI_BAND_ACTIVITY_SAMPLE",   // The table to query
                    projection,             // The array of columns to return (pass null to get all)
                    selection,              // The columns for the WHERE clause
                    selectionArgs,          // The values for the WHERE clause
                    null,                   // don't group the rows
                    null,                   // don't filter by row groups
                    sortOrder               // The sort order
            );

            // first query database to get new data since last sync date
            String message = null;

            // iterate of data in cursor form/ pass data on to volley
            List itemIds = new ArrayList<>();
            while(cursor.moveToNext()) {

                JSONObject item = new JSONObject();

                long itemId = cursor.getLong(
                        cursor.getColumnIndexOrThrow("TIMESTAMP"));
                long stepsIN = cursor.getLong(
                        cursor.getColumnIndexOrThrow("STEPS"));
                long heartrateIN = cursor.getLong(
                        cursor.getColumnIndexOrThrow("HEART_RATE"));

                itemIds.add(itemId);
                JSONObject json = new JSONObject();
                // form an object and add to array list
                item.put("timestamp", itemId);
                item.put("steps", stepsIN);
                item.put("heartrate", heartrateIN);
                arraysync.put(item);

            }

            message = arraysync.get(0).toString();
            Toast.makeText(getApplicationContext(),"Query ARRAY :" + itemIds.get(0).toString(), Toast.LENGTH_LONG).show();
            Toast.makeText(getApplicationContext(),"Query JSON ARRAY :" + message, Toast.LENGTH_LONG).show();
            cursor.close();

            // form token URL
            urltoken = urlsave + editTokenStr;
            Toast.makeText(getApplicationContext(),"token URL:" + urltoken, Toast.LENGTH_LONG).show();//di

            // make a get call to server
            //sendAndRequestResponse();

            // make a put ie save to HS network
            PostandRequestResponse();


        } catch (Exception ex) {
            GB.toast(this, "error with sync", Toast.LENGTH_LONG, GB.ERROR, ex);
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


    private void PostandRequestResponse() {

/*
        //Create json array for filter
        JSONArray array = new JSONArray();

        //Create json objects for two filter Ids
        JSONObject jsonParam = new JSONObject();
        JSONObject jsonParam1 = new JSONObject();
test array JSON for post request
        try {
            //Add string params
            jsonParam.put("NAME", "XXXXXXXXXXXXXX");
            jsonParam.put("USERNAME", "XXXXXXXXXXXXXX");
            jsonParam.put("PASSWORD", "XXXXXXXXXXXX");
            jsonParam1.put("NAME", "XXXXXXXXXXXXXX");
            jsonParam1.put("USERNAME", "XXXXXXXXXXXXXX");
            jsonParam1.put("PASSWORD", "XXXXXXXXXXXX");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        array.put(jsonParam);
        array.put(jsonParam1);
*/
        Toast.makeText(getApplicationContext(),"within POST:" + urltoken, Toast.LENGTH_LONG).show();//di
        // JsonObjectRequest JsonArrayRequest
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.POST, urltoken,arraysync,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        //serverResp.setText("String Response : "+ response.toString());
                        Toast.makeText(getApplicationContext(),"String Response : "+ response.toString(), Toast.LENGTH_LONG).show();
                        // need if check save good then clear array object
                        arraysync = new JSONArray();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getApplicationContext(),"save sync error "+ error.toString(), Toast.LENGTH_LONG).show();//display the response on screen
                Log.i(null,"Error :" + error.toString());
            }
        });

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);

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
