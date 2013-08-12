package com.maestros.mdwmapasbd;


import java.util.ArrayList;
import java.util.HashMap;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.maestrosmdwmapas.data.DBAdapterPlace;
import com.maestrosmdwmapas.data.DatabaseHelperClass;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;
import com.maestros.mdwmapasbd.api.*;

public class MainActivityMapas extends Activity {

	
	static final LatLng RESTAURANT1 = new LatLng(53.558, 9.927);
	static final LatLng RESTAURANT2 = new LatLng(53.551, 9.993);
	public static final String APPTAG = "MDWRestaurantes";
	private GoogleMap map;
	Location mCurrentLocation;
	private HashMap<Marker, Lugares> restauranteMarkerMap = new HashMap<Marker, Lugares>();
	private static APIConexionHelper api = new APIConexionHelper();
	private DBAdapterPlace db;
	private MainActivityMapas appstate;
	
	public static final int NOTIFICATION_ID = 189; 
    public static final int WITH_SOUND=0;
    public static final int WITH_VIBRATION=1;
    public static final int WITH_LIGHT=2; 
    
    private static boolean modified=false;

    private NotificationCompat.Builder mdwMapasBuilder;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDB();
        setContentView(R.layout.activity_main_activity_mapas); 
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        if (map!=null){
        	/* Hay Distintos tipos de Mapas: MAP_TYPE_NORMAL, MAP_TYPE_HYBRID, MAP_TYPE_SATELLITE, MAP_NONE, MAP_TERRAIN*/
        	 map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        	 new APIAsyncTask().execute();
             moveMapToMyLocation();
          } 
        map.setInfoWindowAdapter(new CustomInfoWindow(getLayoutInflater()));
    	appstate = this;
    	mdwMapasBuilder = new NotificationCompat.Builder(this); 
    	mdwMapasBuilder.setAutoCancel(true)
    	.setTicker("Mdw Restaurantes")
    	.setSmallIcon(R.drawable.logomdw);
    }

    public void setDB() {
		if (db == null) {
			db = new DBAdapterPlace(getApplicationContext());	
		}
	}
    
    public DBAdapterPlace getDb(){
		return this.db;
	}
    
    private void sendNotification(String title ,String text,int num,int sound){
    	Intent startActivityIntent=new 
    	Intent(this,MainActivityMapas.class);
    	PendingIntent launchIntent =PendingIntent.getActivity(this, 0, startActivityIntent, 0);
    	mdwMapasBuilder.setContentIntent(launchIntent).setContentTitle(title).setContentText(text).setNumber(num).setAutoCancel(true);
    	NotificationManager notificationManager = 
    	(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	switch(sound){
    	case WITH_SOUND:
    		Uri ringURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    		mdwMapasBuilder.setSound(ringURI);
    		break;
    	case WITH_VIBRATION:
    		double vibrateLength = 1000;
    		long[] vibrate = new long[] {100, 100, (long)vibrateLength }; 
    		mdwMapasBuilder.setVibrate(vibrate);
    		break;
    	case WITH_LIGHT:
    		double vibrateLength2 = 1000;
    		mdwMapasBuilder.setLights( Color.RED,(int)vibrateLength2, (int)vibrateLength2);
    		break;
    }
    	notificationManager.notify(NOTIFICATION_ID,mdwMapasBuilder.build());
    	}
    
    private void moveMapToMyLocation() {
    	LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    	Criteria crit = new Criteria();
    	Location loc = locationManager.getLastKnownLocation(locationManager.getBestProvider(crit, false));
    	map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(),loc.getLongitude()), 35)); 
        map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
        Marker myLocation = map.addMarker(new MarkerOptions().position(new LatLng(loc.getLatitude(),loc.getLongitude())).title("Mi Posicion").icon(BitmapDescriptorFactory
                .fromResource(R.drawable.logomdw)).draggable(true));
        Lugares actual = new Lugares(new LatLng(loc.getLatitude(),loc.getLongitude()),"Yo","Mi posicion actual","LI","direccion Actual");
        restauranteMarkerMap.put(myLocation,actual);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; Esto agrega los elementos al action bar si estan presentes
        getMenuInflater().inflate(R.menu.main_activity_mapas, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_licencia:
            	Log.d("menu"," menu-licencia");
            	Intent nextScreen2 = new Intent(MainActivityMapas.this, Contactenos.class);
                startActivityForResult(nextScreen2, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
   
    

    class APIAsyncTask extends AsyncTask<Void, Void, ArrayList<Lugares>> {
		private ProgressDialog dialog = new ProgressDialog(MainActivityMapas.this);

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Actualizando...");
			dialog.show();
		}  

		@Override
		protected ArrayList<Lugares> doInBackground(Void... params) {
			Log.d("ApiRestaurantes",APIConexionHelper.URL);
			return api.parseJSONArray(api.readJSONArrayFromURL(APIConexionHelper.URL));			
		}

		@Override
		protected void onPostExecute(ArrayList<Lugares> result){
			dialog.dismiss();
			boolean updated = true;
			DBAdapterPlace db = appstate.getDb();
			db.open();
			for (Lugares p : result) {
				Lugares in_database = db.getPlaceById(p.getId());
				if (in_database != null) {
					updated = updated && db.updatePlace(p);					
				} else {
					updated = updated && (db.insertPlace(p) != -1);
				}
			}
			
			db.close();
			if(modified==false){
				sendNotification("MdwMapas" ,"Se han agregado "+ result.size() +" restaurantes ",+result.size(),2);
			}
			modified=true;
			Cursor all_places_in_database=db.getAllPlaces();
			if(result.size()>0){
			  for (Lugares temp : result) {
	    		  Marker marker_res = map.addMarker(new MarkerOptions()
	              	.position(temp.getLatLng())
	              	.title(temp.getTitle())
	              	.snippet(temp.getDesc()+temp.getCode())
	              	.icon(BitmapDescriptorFactory
	                .fromResource(R.drawable.logomdw)));
	    		  
	    		  CircleOptions circleOptions = new CircleOptions()
	    		    	.center(temp.getLatLng())
	    		    	.radius(1000).fillColor(Color.BLUE);
	    		  Circle circle = map.addCircle(circleOptions);
	    		  restauranteMarkerMap.put(marker_res, temp);
	    	  }
			  map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

	              @Override
	              public void onInfoWindowClick(Marker marker) {
	                  Log.d("markerdetail", marker.getTitle()); 
	                  Lugares lugar_presionado=restauranteMarkerMap.get(marker);
	                  Intent nextScreen = new Intent(MainActivityMapas.this, DetailActivity.class);
	                  nextScreen.putExtra("lugar_title",lugar_presionado.getTitle());
	                  nextScreen.putExtra("lugar_desc",lugar_presionado.getDesc());
	                  nextScreen.putExtra("lugar_address",lugar_presionado.getAddress());
	                  nextScreen.putExtra("lugar_code",lugar_presionado.getCode());
	                  startActivityForResult(nextScreen, 0);
	              }
	          });
			}
			
			
		}
	}
    
    
    
    
}
