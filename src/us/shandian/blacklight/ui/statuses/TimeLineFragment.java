/* 
 * Copyright (C) 2014 Peter Cai
 *
 * This file is part of BlackLight
 *
 * BlackLight is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlackLight is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlackLight.  If not, see <http://www.gnu.org/licenses/>.
 */

package us.shandian.blacklight.ui.statuses;

import android.app.Fragment;
import android.app.Service;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Build;
import android.os.Vibrator;

import android.support.v4.widget.SwipeRefreshLayout;

import java.util.ConcurrentModificationException;

import us.shandian.blacklight.R;
import us.shandian.blacklight.cache.statuses.HomeTimeLineApiCache;
import us.shandian.blacklight.support.AsyncTask;
import us.shandian.blacklight.support.Settings;
import us.shandian.blacklight.support.Utility;
import us.shandian.blacklight.support.adapter.WeiboAdapter;
import us.shandian.blacklight.ui.common.SwipeUpAndDownRefreshLayout;
import us.shandian.blacklight.ui.main.MainActivity;

import static us.shandian.blacklight.support.Utility.hasSmartBar;

public class TimeLineFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener,
													View.OnTouchListener, View.OnLongClickListener
{
	
	protected ListView mList;
	protected View mNew, mRefresh;
	private WeiboAdapter mAdapter;
	private HomeTimeLineApiCache mCache;
	
	// Pull To Refresh
	private SwipeUpAndDownRefreshLayout mSwipeRefresh;
	
	private boolean mRefreshing = false;
	private boolean mNewHidden = false;
	
	protected boolean mBindOrig = true;
	protected boolean mShowCommentStatus = true;
	
	private int mLastCount = 0;
	private float mLastY = -1.0f;

	private Runnable mCallback;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		initTitle();

		View v = inflater.inflate(R.layout.home_timeline, null);
		mList = (ListView) v.findViewById(R.id.home_timeline);
		mCache = bindApiCache();
		mCache.loadFromCache();
		
		mList.setDrawingCacheEnabled(true);
		mList.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		mList.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE | ViewGroup.PERSISTENT_SCROLLING_CACHE);
		
		// Swipe To Refresh
		bindSwipeToRefresh((ViewGroup) v);
		
		if (mCache.mMessages.getSize() == 0) {
			new Refresher().execute(new Boolean[]{true});
		}

		// Content Margin
		if (getActivity() instanceof MainActivity) {
			View header = new View(getActivity());
			LayoutParams p = new LayoutParams(LayoutParams.MATCH_PARENT,
					Utility.getActionBarHeight(getActivity()));

			if (Build.VERSION.SDK_INT >= 19) {
				p.height += Utility.getStatusBarHeight(getActivity());
			}

			header.setLayoutParams(p);
			mList.addHeaderView(header);
			mSwipeRefresh.setTopMargin(p.height);
		}

		// Adapter
		mAdapter = new WeiboAdapter(getActivity(), mList, mCache.mMessages, mBindOrig, mShowCommentStatus);
		mList.setAdapter(mAdapter);
		
		// Floating "New" and "Refresh" button
		bindNewButton(v);

		mList.setOnTouchListener(this);
		
		return v;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		try {
			mCache.cache();
		} catch (ConcurrentModificationException e) {
			
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		
		if (!hidden) {
			initTitle();
			resume();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		resume();
	}
	
	public void resume() {
		Settings settings = Settings.getInstance(getActivity());
		
		boolean fs = settings.getBoolean(Settings.FAST_SCROLL, false);
		mList.setFastScrollEnabled(fs);
		
		if (fs) {
			// Scroller
			new Thread(new Runnable() {
				@Override
				public void run() {
					while(!Utility.changeFastScrollColor(mList, getResources().getColor(R.color.gray)));
				}
			}).start();
		}
	}
	
	@Override
	public void onRefresh() {
		if (!mRefreshing) {
			new Refresher().execute(new Boolean[]{!mSwipeRefresh.isDown()});
		}
	}
	
	@Override
	public void onClick(View v) {
		if (v == mNew) {
			newPost();
		} else if (v == mRefresh) {
			mSwipeRefresh.setIsDown(false);

			if (mList.getFirstVisiblePosition() <= 30) {
				mList.smoothScrollToPosition(0);
			} else {
				mList.setSelection(0);
			}
			mList.post(new Runnable() {
				@Override
				public void run() {
					onRefresh();
				}
			});
		}
	}

	@Override
	public boolean onLongClick(View v) {
		Vibrator vibrator = (Vibrator) getActivity().getApplication().getSystemService(Service.VIBRATOR_SERVICE);
		vibrator.vibrate(50);
		Toast.makeText(getActivity().getApplicationContext(), getString(v == mNew ? R.string.new_post : R.string.refresh), Toast.LENGTH_SHORT).show();
		return true;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent ev) {
		
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				mLastY = ev.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				if (mLastY == -1.0f) break;
				
				float y = ev.getY();
				
				if (!mNewHidden && y < mLastY) {
					if (mNew != null && mRefresh != null) {
						mNew.removeCallbacks(mCallback);
						mNew.clearAnimation();
						mRefresh.clearAnimation();
					
						TranslateAnimation anim = new TranslateAnimation(0, 0, 0, mList.getHeight() - mNew.getTop());
						anim.setFillAfter(true);
						anim.setDuration(400);
					
						mNew.setAnimation(anim);
						mRefresh.setAnimation(anim);
						anim.startNow();

						// Hide after animation
						mNew.postDelayed((mCallback = new Runnable() {
							@Override
							public void run() {
								mNew.clearAnimation();
								mRefresh.clearAnimation();

								mNew.setVisibility(View.GONE);
								mRefresh.setVisibility(View.GONE);
							}
						}), 400);
					}

					mNewHidden = true;

					if (getActivity() instanceof MainActivity) {
						getActivity().getActionBar().hide();
					}
				} else if (mNewHidden && y > mLastY) {
					if (mNew != null && mRefresh != null) {
						// Show them first
						mNew.removeCallbacks(mCallback);
						mNew.setVisibility(View.VISIBLE);
						mRefresh.setVisibility(View.VISIBLE);

						mNew.clearAnimation();
						mRefresh.clearAnimation();

						TranslateAnimation anim = new TranslateAnimation(0, 0, mList.getHeight() - mNew.getTop(), 0);
						anim.setFillAfter(true);
						anim.setDuration(400);

						mNew.setAnimation(anim);
						mRefresh.setAnimation(anim);
						anim.startNow();
					}

					mNewHidden = false;

					if (getActivity() instanceof MainActivity) {
						getActivity().getActionBar().show();
					}
				}
				
				mLastY = y;
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				mLastY = -1.0f;
				break;
		}
		
		return false;
	}
	
	protected HomeTimeLineApiCache bindApiCache() {
		return new HomeTimeLineApiCache(getActivity());
	}
	
	protected void initTitle() {
		getActivity().getActionBar().setTitle(R.string.timeline);
	}
	
	protected void bindSwipeToRefresh(ViewGroup v) {
		mSwipeRefresh = new SwipeUpAndDownRefreshLayout(getActivity());
		
		// Move child to SwipeRefreshLayout, and add SwipeRefreshLayout to root view
		v.removeViewInLayout(mList);
		v.addView(mSwipeRefresh, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mSwipeRefresh.addView(mList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		
		mSwipeRefresh.setOnRefreshListener(this);
		mSwipeRefresh.setColorScheme(R.color.ptr_green, R.color.ptr_orange, R.color.ptr_red, R.color.ptr_blue);
	}
	
	protected void bindNewButton(View v) {
		mNew = v.findViewById(R.id.home_timeline_new);
		mRefresh = v.findViewById(R.id.home_timeline_refresh);
		if (!hasSmartBar()) {
			mNew.setVisibility(View.VISIBLE);
			mNew.bringToFront();
			mNew.setOnClickListener(this);
			mNew.setOnLongClickListener(this);
			mRefresh.setVisibility(View.VISIBLE);
			mRefresh.bringToFront();
			mRefresh.setOnClickListener(this);
			mRefresh.setOnLongClickListener(this);
		} else {
			mNew.setVisibility(View.INVISIBLE);
			mRefresh.setVisibility(View.INVISIBLE);
		}
	}
	
	protected void newPost() {
		Intent i = new Intent();
		i.setAction(Intent.ACTION_MAIN);
		i.setClass(getActivity(), NewPostActivity.class);
		startActivity(i);
	}
	
	private class Refresher extends AsyncTask<Boolean, Void, Boolean>
	{

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLastCount = mCache.mMessages.getSize();
			mRefreshing = true;
			if (mSwipeRefresh != null) {
				mSwipeRefresh.setRefreshing(true);
			}
		}
		
		@Override
		protected Boolean doInBackground(Boolean... params) {
			mCache.load(params[0]);
			return params[0];
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			mList.setSelection(0);
			if (!result) {
				mAdapter.notifyDataSetChanged();
			} else {
				mAdapter.notifyDataSetChangedAndClear();
			}
			mRefreshing = false;
			if (mSwipeRefresh != null) {
				mSwipeRefresh.setRefreshing(false);
			}
		}

		
	}
}
