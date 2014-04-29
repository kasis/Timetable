package com.timetable.app;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.timetable.app.R;
/*
 * Class for working with database(inserting, updating and deleting events, etc.).
 */
public class TimetableDatabase extends SQLiteOpenHelper {
	
	private static final String DB_NAME = "TimeTable";
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	
	private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private SQLiteDatabase dbRead;
	
	private SQLiteDatabase dbWrite;
	
	public TimetableDatabase(Context context) {
		super(context, DB_NAME, null, 1);
		dbRead = this.getReadableDatabase();
		dbWrite = this.getWritableDatabase();
    }
	
	@Override
    public void onCreate(SQLiteDatabase db) {
		/*
		 * Table, containing events.
		 */
		String query = String.format("CREATE TABLE Events ("
				+ "evt_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, "
				+ "evt_name VARCHAR(%s),"
				+ "evt_place VARCHAR(%s),"
				+ "evt_start_time TIME,"
				+ "evt_end_time TIME,"
				+ "evt_start_date DATE,"
				+ "evt_date DATE,"
				+ "per_id INT,"
				+ "evt_note TEXT)", Event.MAX_NAME_LENGTH, Event.MAX_PLACE_LENGTH);
		db.execSQL(query);
		
		/*
		 * Table, containing days, on which repeating event has no occurrence.
		 */
		query = "CREATE TABLE Exceptions ("
				+ "ex_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "evt_id INTEGER NOT NULL, "
				+ "ex_date DATE)"; 
		db.execSQL(query);
		
		/*
		 * Table, containing information about event's period type, end date, interval etc.
		 */
		query = "CREATE TABLE Periods ("
				+ "per_id INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "per_type INTEGER NOT NULL, "
				+ "per_interval INTEGER, "
				+ "per_week_occurences INTEGER,"
				+ "per_end_date DATE,"
				+ "per_num_of_repeats INTEGER)";
		db.execSQL(query);
		
		/*
		 * Table, containing information about event's alarm.
		 */
		query = "CREATE TABLE Alarms (" +
				"alm_id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"alm_time DATETIME," +
				"alm_type INTEGER," +
				"evt_id INTEGER)";
		db.execSQL(query);
	}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
    
    
    private ContentValues createContentValuesFromEventAlarm(EventAlarm alarm) {
    	ContentValues values = new ContentValues();
    	values.put("alm_type", alarm.type.ordinal());
    	values.put("alm_time", dateTimeFormat.format(alarm.time));
    	values.put("evt_id", alarm.eventId);
    	TimetableLogger.log(values.toString());
    	return values;
    }
    
    private EventAlarm getEventAlarmFromCursor(Cursor cursor) {
    	if (cursor.getCount() != 1) {
    		cursor.close();
    		return null;
    	}
    	cursor.moveToFirst();
    	EventAlarm alarm = new EventAlarm();
    	alarm.id = cursor.getInt(cursor.getColumnIndex("alm_id"));
    	alarm.type = EventAlarm.Type.values()[cursor.getInt(cursor.getColumnIndex("alm_type"))];
    	alarm.eventId = cursor.getInt(cursor.getColumnIndex("evt_id"));
    	
    	try {
    		alarm.time = dateTimeFormat.parse(cursor.getString(cursor.getColumnIndex("alm_time")));
    	} catch (Exception e) {
    	}
    	cursor.close();
    	return alarm;
    }
    
    /*
     * Insert alarm into Alarms table. 
     * Return id of inserted alarm, -1 if an error occurred while inserting.
     */
    public long insertEventAlarm(EventAlarm alarm) {
    	if (!alarm.isOk()) {
    		TimetableLogger.log("alarm is not Ok");
        	return -1;
    	}
    	return dbWrite.insert("Alarms", null, createContentValuesFromEventAlarm(alarm));
    }
    
    /*
     * Update alarm.
     * Return number of updated rows.
     */
    public int updateEventAlarm(EventAlarm alarm) {
    	return dbWrite.update("Alarms", createContentValuesFromEventAlarm(alarm), "alm_id = ?", new String [] {Integer.toString(alarm.id)});
    }
    /*
     * Delete alarm.
     * Return number of deleted rows.
     */
    public int deleteEventAlarm(EventAlarm alarm) {
    	return dbWrite.delete("Alarms","alm_id = ?", new String [] { Integer.toString(alarm.id)});
    }
    
    /*
     * Delete event's alarm given event id.
     * Return number of deleted rows
     */
    public int deleteEventAlarmByEventId(Integer eventId) {
     	return dbWrite.delete("Alarms","evt_id = ?", new String [] { Integer.toString(eventId)});
    }
    
    /*
     * Search alarm given alarm id.
     */
    public EventAlarm searchEventAlarmById(int id) {
    	String query = "SELECT * FROM Alarms WHERE alm_id = ?";
    	Cursor cursor = dbRead.rawQuery(query, new String [] { Integer.toString(id) });
    	return getEventAlarmFromCursor(cursor);
    }
    
    /*
     * Search alarm given event id, to which alarm is attached.
     */
    public EventAlarm searchEventAlarmByEventId(int id) {
    	String query = "SELECT * FROM Alarms WHERE evt_id = ?";
    	Cursor cursor = dbRead.rawQuery(query, new String [] { Integer.toString(id) });
    	return getEventAlarmFromCursor(cursor);
    }
    
    private ContentValues createContentValuesFromEventPeriod(EventPeriod period) {
    	ContentValues values = new ContentValues();
    	values.put("per_type", period.type.ordinal());
    	values.put("per_interval", period.interval);
    	values.put("per_week_occurences", period.getWeekOccurrences());
    	values.put("per_end_date", period.endDate == null ? "" : dateFormat.format(period.endDate));
    	values.put("per_num_of_repeats", period.numberOfRepeats);
    	return values;
    }
    
    /*
     * Insert period into Periods table.
     * Return id of inserted period, -1 if insertion was unsuccessful
     */
    public long insertEventPeriod(EventPeriod period) {
    	if (!period.isOk()) {
    		return -1;
    	}
    	return dbWrite.insert("Periods",null, createContentValuesFromEventPeriod(period));
    	
    }
    
    /*
     * Update period.
     * Return number of updated rows.
     */
    public int updateEventPeriod(EventPeriod period) {
    	return dbWrite.update("Periods", createContentValuesFromEventPeriod(period), "per_id = ?", new String [] {Integer.toString(period.id)} );
    }
    
    /*
     * Delete period.
     * Return number of deleted rows. 
     */
    public int deleteEventPeriod(EventPeriod period) {
    	return dbWrite.delete("Periods", "per_id = ?", new String [] {Integer.toString(period.id)} );
    }
    
    /*
     * Search period given period id.
     */
    public EventPeriod searchEventPeriodById(int id) {
    	String query = "SELECT * FROM Periods WHERE per_id = ?";
    	Cursor cursor = dbRead.rawQuery(query, new String [] { Integer.toString(id) });
    	if (cursor.getCount() == 0) {
    		cursor.close();
    		return null;
    	}
    	cursor.moveToFirst();
    	EventPeriod period = new EventPeriod(id);
    	period.type = EventPeriod.Type.values()[cursor.getInt(cursor.getColumnIndex("per_type"))];
    	period.interval = cursor.getInt(cursor.getColumnIndex("per_interval"));
    	period.setWeekOccurrences(cursor.getInt(cursor.getColumnIndex("per_week_occurences")));
    	try {
    		period.endDate = dateFormat.parse(cursor.getString(cursor.getColumnIndex("per_end_date")));
    	} catch (Exception e) {
    	}
    	period.numberOfRepeats = cursor.getInt(cursor.getColumnIndex("per_num_of_repeats"));
    	cursor.close();
    	return period;
    }
    
    /*
     * Insert into Exceptions event id and date, on which this repeated event has no occurrence.
     */
    public long insertException(Event event, Date date) {
    	ContentValues values = new ContentValues();
    	values.put("evt_id", event.id);
    	values.put("ex_date", dateFormat.format(date));
    	return dbWrite.insert("Exceptions", null, values);
    	}
    
    /*
     * Delete all exceptions of given event.
     */
    public int deleteAllEventExceptions(Event event) {
    	return dbWrite.delete("Exceptions", "evt_id = ?", new String [] { Integer.toString(event.id) } );
    	}
    
    /*
     * Check if repeated event has no occurrence today.
     */
    public boolean isException(Event event, Date date) {
    	String query = "SELECT * FROM Exceptions where evt_id = ? and ex_date = ?";
    	Cursor cursor = dbRead.rawQuery(query, new String [] {Integer.toString(event.id), dateFormat.format(date)});
    	boolean isException = cursor.getCount() != 0;
    	cursor.close();
    	return isException;
    }
    
    private ContentValues createContentValuesFromEvent(Event event) {
    	ContentValues values = new ContentValues();
    	values.put("evt_name", event.name);
    	values.put("evt_place", event.place);
    	values.put("evt_start_time", timeFormat.format(event.startTime));
    	values.put("evt_end_time", (event.endTime != null ? timeFormat.format(event.endTime) : ""));
    	values.put("evt_date", dateFormat.format(event.date));
    	values.put("per_id", event.period.id);
    	values.put("evt_note", event.note);
    	
    	return values;
    }
    
    /*
     * Insert given event into database.
     * Return given event with appropriate values set to fields event.id, event.period.id, event.alarm.id.
     */
    public Event insertEvent(Event event) {
    	event.period.id = (int) insertEventPeriod(event.period);
    	if (event.period.id == -1) {
    		TimetableLogger.log("Error inserting event's period.");
    		return null;
    	}
    	event.id = (int) dbWrite.insert("Events", null, createContentValuesFromEvent(event));
    	TimetableLogger.log("Inserted id: " + Integer.toString(event.id));
    	if (event.hasAlarm()) {
    		event.alarm.eventId = event.id;
        	event.alarm.id = (int) insertEventAlarm(event.alarm);
    		if (event.alarm.id == -1) {
        		TimetableLogger.log("Error inserting event's alarm");
        		return null;
        	}
    	}
    	return event;
    }
    
    /*
     * Update event.
     * Return given event, with event.alarm.id field to appropriate value, if needed.
     */
    public Event updateEvent(Event event) {
    	if (event.hasAlarm()) {
    		if (event.alarm.id == -1) {
    			event.alarm.id = (int) insertEventAlarm(event.alarm);
    			if (event.alarm.id == -1) {
    				return null;
    			}
    		}
    		else if (updateEventAlarm(event.alarm) != 1) {
    			return null;
    		}
    	}
    	else {
    		deleteEventAlarmByEventId(event.id);
    	}
    	if (updateEventPeriod(event.period) != 1) {
    		return null;
    	}
    	if (dbWrite.update("Events", createContentValuesFromEvent(event), "evt_id = ?", new String [] {Integer.toString(event.id)}) != 1) {
    		return null;
    	}
    	return event;
    }
    
    /*
     * Delete event.
     * Return number of deleted rows. 
     */
    public int deleteEvent(Event event) {
    	deleteEventPeriod(event.period);
    	if (event.hasAlarm()) {
    		deleteEventAlarm(event.alarm);
    	}
    	return dbWrite.delete("Events", "evt_id = ?", new String [] {Integer.toString(event.id)});
    }
    
    private Event getEventFromCursor(Cursor cursor) {
    	Event event = new Event(cursor.getInt(cursor.getColumnIndex("evt_id")));
    	event.name = cursor.getString(cursor.getColumnIndex("evt_name"));
		event.place = cursor.getString(cursor.getColumnIndex("evt_place"));
		
		try {
			event.startTime = timeFormat.parse(cursor.getString(cursor.getColumnIndex("evt_start_time")));
			event.date = dateFormat.parse(cursor.getString(cursor.getColumnIndex("evt_date")));
    	} catch(Exception e) {
    		return null;
    	}
		
		try {
			event.endTime = timeFormat.parse(cursor.getString(cursor.getColumnIndex("evt_end_time")));
		} catch(Exception e) {
			//this fields are null by default
		}
		
		event.note = cursor.getString(cursor.getColumnIndex("evt_note"));
		event.period = searchEventPeriodById(cursor.getInt(cursor.getColumnIndex("per_id")));
		event.alarm = searchEventAlarmByEventId(event.id);
    	return event;
    }
    
    /*
     * Search event given event id.
     */
    public Event searchEventById(int id) {
    	String query = "SELECT * FROM Events where evt_id = ?";
    	Cursor cursor = dbRead.rawQuery(query, new String [] {Integer.toString(id)});
    	Event event = null;
    	if (cursor.getCount() != 0) {
    		cursor.moveToFirst();
    		event = getEventFromCursor(cursor);
    	}
    	cursor.close();
    	return event;
    	
    }
    
    /*
     * Return all events, that have occurrence on given date.
     */
    public Vector<Event> searchEventsByDate(Date date) {
    	String dateString = dateFormat.format(date);
    	String query = "SELECT * FROM Events order by evt_start_time ASC";
    	TimetableLogger.log(dateString);
    	Cursor cursor = dbRead.rawQuery(query, new String [] {});
    	
    	Vector<Event> events = new Vector<Event>();
    	if (cursor.getCount() == 0) {
    		cursor.close();
    		return events;
    	}
    	cursor.moveToFirst();
    	do {
    		Event event = getEventFromCursor(cursor);
    		if (event != null && event.isToday(date) && !isException(event, date)) {
    			events.add(event);
    		}
    	} while(cursor.moveToNext());
    	cursor.close();
    	return events;
    }
    
    @Override
    public void close() {
    	super.close();
    	dbRead.close();
    	dbWrite.close();
    }
}
