package com.example.scheduler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {

	private Button btnCal= null,
					btnNew= null;
	private ListView lTask= null;
	// the date whose schedules are shown
	private int year,
		month,
		day;
	// activities invoke intent flags
	public static final String SCHEDULE_DATE="1";
	
	private static final String DATABASE_NAME = "scheduler.db";
	DatabaseHelper dbh= null;
	
	// tags for debug
	private static final String TAG_DIALOG= "TEST_DIALOG";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// set up the system current time
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d");
		String sysDate = sdf.format(date);
		year = Integer.parseInt( sysDate.split("-")[0] );
		month = Integer.parseInt( sysDate.split("-")[1] );
		day = Integer.parseInt( sysDate.split("-")[2] );
		Log.i("TEST","MAIN AC INIT:"+year+"-"+month+"-"+day);
		
		// set up datebase
		dbh= new DatabaseHelper(this, DATABASE_NAME, null, 1);  
		
		// set up widgets
		btnCal= (Button)this.findViewById(R.id.go_calendar);
		btnNew= (Button)this.findViewById(R.id.new_entry);
		lTask= (ListView)this.findViewById(R.id.task_listView);
		
		btnCal.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
			    Intent intent = new Intent(MainActivity.this, CalendarActivity.class); 
			    intent.putExtra(SCHEDULE_DATE, ""+year+"-"+month+"-"+day);
			    Log.i("TEST","MAIN AC LISTENER:"+MainActivity.this.year+"-"+MainActivity.this.month);
			    MainActivity.this.startActivityForResult(intent, 1);  
			}
			
		});
		// for NEW ENTRY button
		btnNew.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				LayoutInflater inflater = getLayoutInflater();
				
				final View layout = inflater.inflate(R.layout.new_entry_dialog,
						(ViewGroup)findViewById(R.id.dialog_title));
				AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this)
						.setTitle("New Entry")
						.setView(layout);
				builder.setPositiveButton("Add", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						ScheduleEntry en= new ScheduleEntry();
						en.setId("ID_"+(int)(Math.random()*100000)); 	// id range (0,100000)
						en.setDate("" + year + "-" + month + "-" + day);
						en.setSyncFlag(ScheduleEntry.FLAG_NEW);
						// get value entered by user
						en.setTitle(getValue(R.id.dialog_title_et));
						en.setInfo(getValue(R.id.dialog_info_et));
						en.setBeginTime(getValue(R.id.dialog_beg_et));
						en.setEndTime(getValue(R.id.dialog_end_et));
						// insert the new entry
						dbh.insertNewEntry(en);
						MainActivity.this.updateListView(year, month, day);
					}
					
					private String getValue(int _rid){
						EditText et = (EditText) layout.findViewById(_rid);
			            if(et == null) 
			            	Log.d(TAG_DIALOG, "NULL");
			            else{
			                String value = et.getText().toString();
			                Log.v(TAG_DIALOG, value);
			                return value;
			            }
			            return null;
					}
					
				});
				builder.setNegativeButton("Cancel", null);
				builder.create().show();
			}
			
		});
		// update the ListView
		updateListView(year, month, day);
		// click item to modify 
		lTask.setOnItemClickListener(new OnItemClickListener(){

			/**
			 * parent-ListView，view-Item view，position is index
			 */
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				ListView lv = (ListView)parent;
				// SimpleAdapter return a Map
		        Map<String,Object> map = (HashMap<String,Object>)lv.getItemAtPosition(arg2);
		        
		        LayoutInflater inflater = getLayoutInflater();
		        final View layout = inflater.inflate(R.layout.new_entry_dialog,
						(ViewGroup)findViewById(R.id.dialog_title));
		        // pre set the edit text items of entry
		        final String id= (String)map.get("id_schedule");
		        EditText et_title= (EditText)layout.findViewById(R.id.dialog_title_et);
		        et_title.setText((String)map.get("title"));
		        EditText et_info= (EditText)layout.findViewById(R.id.dialog_info_et);
		        et_info.setText((String)map.get("info"));
		        String time[]= ((String)map.get("time")).split(" - ");
		        EditText et_beg= (EditText)layout.findViewById(R.id.dialog_beg_et);
		        et_beg.setText(time[0]);
		        EditText et_end= (EditText)layout.findViewById(R.id.dialog_end_et);
		        et_end.setText(time[1]);
		        final int sync= (Integer)map.get("sync");
		        		
		        // show the dialog for user to modify
				AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this)
						.setTitle("Modify Entry")
						.setView(layout);
				builder.setPositiveButton("Modify", new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						ScheduleEntry en= new ScheduleEntry();
						en.setId(id); 
						en.setDate("" + year + "-" + month + "-" + day);
						en.setSyncFlag(ScheduleEntry.FLAG_MODIFIED + sync);
						// get value entered by user
						en.setTitle(getValue(R.id.dialog_title_et));
						en.setInfo(getValue(R.id.dialog_info_et));
						en.setBeginTime(getValue(R.id.dialog_beg_et));
						en.setEndTime(getValue(R.id.dialog_end_et));
						// insert the new entry
						dbh.updateEntry(en);
						MainActivity.this.updateListView(year, month, day);
					}
					
					private String getValue(int _rid){
						EditText et = (EditText) layout.findViewById(_rid);
			            if(et == null) 
			            	Log.d(TAG_DIALOG, "NULL");
			            else{
			                String value = et.getText().toString();
			                Log.v(TAG_DIALOG, value);
			                return value;
			            }
			            return null;
					}
					
				});
				builder.setNegativeButton("Cancel", null);
				builder.create().show();
			}
			
		});
		// long click item to delete
		lTask.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View arg1,
					int position, long arg3) {
				// TODO Auto-generated method stub
				ListView lv = (ListView)parent;
				// SimpleAdapter return a Map
		        Map<String,Object> map = (HashMap<String,Object>)lv.getItemAtPosition(position);
		        final String id= (String)map.get("id_schedule");
		        int flag= Integer.parseInt( (String)map.get("sync_flag") );
		        final boolean isNew= flag % 2 == 1;
		        
				new AlertDialog.Builder(MainActivity.this)
					.setTitle("Delete Entry")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							if( isNew )
								dbh.deleteEntry(id);
							dbh.markDeleteEntry(id);
					        updateListView(year, month, day);
						}
					}).setNegativeButton("Cancel", null)
					.create().show();
				return false;
			}
			
		});
	}

	/**
	 * get schedule entries of the specific day that is pointed by params from db
	 * and show them on ListView
	 * @param year2
	 * @param month2
	 * @param day2
	 */
	private void updateListView(int year2, int month2, int day2) {
		// TODO Auto-generated method stub
		// try get data from sqlite3
		
		// bind the data with adapter
		SimpleAdapter adapter = new SimpleAdapter(this, dbh.queryEntries(year2, month2, day2), 
				R.layout.schedule_item,
				new String[]{"id", "title","info","time", "sync"},
				new int[]{R.id.id_schedule, R.id.title,R.id.info,R.id.time, R.id.sync_flag});
		lTask.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.getItem(0).setTitle("Sync Now!");
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //得到Id
        switch (item.getItemId()) {
        case R.id.action_settings:
        	sync();
        	MainActivity.this.updateListView(year, month, day);
            break;
        default: ;
        }
        return super.onOptionsItemSelected(item);
    }
	
	/*
	 * sync with the PC scheduler
	 */
	private void sync() {
		// TODO Auto-generated method stub
		
		// make a socket
	    Socket socket= null;
	    InputStreamReader in= null;
	    PrintWriter out= null;
		try {
			socket = new Socket("192.168.1.104", 9555);
			if(socket.isConnected())
				Log.i("TEST","connected");
		    out = new PrintWriter(socket.getOutputStream(),true); 
		    in = new InputStreamReader(socket.getInputStream());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	    
		// send entries with flag != 0
		List<Map<String,Object>> l= dbh.querySyncEntries();
		for( Map<String, Object> m : l ){
			StringBuilder sb= new StringBuilder();
			sb.append("id:");
			sb.append((String)m.get("id"));
			sb.append(";");
			sb.append("title:");
			sb.append((String)m.get("title"));
			sb.append(";");
			sb.append("info:");
			sb.append((String)m.get("info"));
			sb.append(";");
			sb.append("date:");
			sb.append((String)m.get("date"));
			sb.append(";");
			sb.append("beginTime:");
			sb.append((String)m.get("beginTime"));
			sb.append(";");
			sb.append("endTime:");
			sb.append((String)m.get("endTime"));
			sb.append(";");
			sb.append("syncFlag:");
			sb.append((Integer)m.get("syncFlag")+"");
			sb.append(";");
			String sEntry= sb.toString();
			Log.i("TEST_SEND", sEntry);
			out.write(sEntry);
			out.flush();
			char[] resp= new char[5];
			try {
				in.read(resp);
				StringBuilder fb= new StringBuilder();
				for(char c : resp){
					if( c != '\0')
						fb.append(c);
				}
				Log.v("TEST SEND FEEDBACK", fb.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		// end the sending with a string of all '\0'
		out.write("\0");	// ending for sending data 
		out.flush();
		// clear all the entries
		dbh.deleteAllEntries();
		// read entries from PC
		final int BUF_SIZE= 1024;
		char[] buf= new char[BUF_SIZE];
		try {
			while(true){
				in.read(buf);
				if( buf[0] == '\0')
					break;
				// parse the buf into an entry
				StringBuilder bufsb= new StringBuilder();
				for(char c : buf){
					if( c != '\0')
						bufsb.append(c);
				}
				String s= bufsb.toString();
				Log.i("TEST_READ", s);
				
				ScheduleEntry e= new ScheduleEntry();
				
				int beg= 0,
					end= 0;
				String sub;
				beg= s.indexOf(':') + 1;
				end= s.indexOf(';');
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setId(sub);
				
				beg= s.indexOf(':', beg) + 1;
				end= s.indexOf(';', end+1);
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setTitle(sub);
				
				beg= s.indexOf(':', beg) + 1;
				end= s.indexOf(';', end+1);
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setInfo(sub);
				
				beg= s.indexOf(':', beg) + 1;
				end= s.indexOf(';', end+1);
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setDate(sub);
				
				beg= s.indexOf(':', beg) + 1;
				end= s.indexOf(';', end+1);
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setBeginTime(sub);
				
				beg= s.indexOf("endTime:", beg) + 8;
				end= s.indexOf(';', end+1);
				sub= s.substring(beg, end);
				Log.i("TEST", sub);
				e.setEndTime(sub);
				
				e.setSyncFlag(0);	// set flag to 0
				
				dbh.insertNewEntry(e);
				
				out.write("OK");	// ending for sending data 
				out.flush();
			}
			
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * return from Calendar activity, get selected day from calendar 
	 * and update the year/month/day of showing schedule
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK){
			Bundle map= data.getExtras();
			String date= (String)map.get(CalendarActivity.SELECTED_DATE);
			String[] aDate= date.split("-"); 
			year= Integer.parseInt(aDate[0]);
			month= Integer.parseInt(aDate[1]);
			day= Integer.parseInt(aDate[2]);
			Log.i("TEST","MAIN AC BACK:"+year+"-"+month+"-"+day);
			updateListView(year, month, day);
			// update the ListView
		} else {
			Log.e("ac-invoke", "RESULT BACK TO MAIN AC");
		}
	}

}
