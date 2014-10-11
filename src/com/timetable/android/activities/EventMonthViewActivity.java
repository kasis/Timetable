package com.timetable.android.activities;



import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.timetable.android.R;
import com.timetable.android.TimetableLogger;

public class EventMonthViewActivity extends ActionBarActivity{
	
	
	GridView mGreedView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_month_view);
		
		mGreedView = (GridView) findViewById(R.id.month_days);
		mGreedView.setAdapter(new MonthViewAdapter(this));
	}
	
	public class MonthViewAdapter extends BaseAdapter {

		private Context mContext;
		
		int mDayViewHeight = -1;
		
		public MonthViewAdapter(Context context) {
			mContext = context;
		}
		
		private int getDayViewHeight() {
			if (mDayViewHeight <= 0) {
				mDayViewHeight = EventMonthViewActivity.this.mGreedView.getHeight() / 7;
			}

			return mDayViewHeight;
		}
		
		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup Parent) {
			TextView textView = (TextView) convertView;
			if (textView == null ) {
				textView = (TextView) getLayoutInflater().inflate(R.layout.month_day_view, null);
				textView.setHeight(getDayViewHeight());
				TimetableLogger.error(Integer.toString(getDayViewHeight()));
				
			}
			textView.setText(Integer.toString(position));
			return textView;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 49;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}