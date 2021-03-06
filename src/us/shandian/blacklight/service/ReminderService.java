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

package us.shandian.blacklight.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

import us.shandian.blacklight.R;
import us.shandian.blacklight.api.remind.RemindApi;
import us.shandian.blacklight.cache.comments.CommentMentionsTimeLineApiCache;
import us.shandian.blacklight.cache.comments.CommentTimeLineApiCache;
import us.shandian.blacklight.cache.directmessages.DirectMessagesUserApiCache;
import us.shandian.blacklight.cache.login.LoginApiCache;
import us.shandian.blacklight.cache.statuses.MentionsTimeLineApiCache;
import us.shandian.blacklight.model.UnreadModel;
import us.shandian.blacklight.support.Settings;
import us.shandian.blacklight.ui.entry.EntryActivity;
import static us.shandian.blacklight.BuildConfig.DEBUG;

/*
 * A service class that fetches notifications
 * From sina's API
 * Should be started by AlaramManager
 */
public class ReminderService extends IntentService {
	private static final String TAG = ReminderService.class.getSimpleName();
	
	private static final int ID = 100000;
	private static final int ID_CMT = ID + 1;
	private static final int ID_MENTION = ID + 2;
	private static final int ID_DM = ID + 3;

	private void doFetchRemind() {
		LoginApiCache cache = new LoginApiCache(this);
		String uid = cache.getUid();

		if (!TextUtils.isEmpty(uid)) {
			UnreadModel unread = RemindApi.getUnread(uid);

			if (DEBUG) {
				Log.d(TAG, "unread got: " + (unread != null));
			}

			doUpdateNotifications(unread);
		}
	}

	private void doUpdateNotifications(UnreadModel unread) {
		if (DEBUG) {
			Log.d(TAG, "update notifications");
		}

		Context c = getApplicationContext();

		if (unread != null) {
			int defaults = parseDefaults(c);
			PendingIntent i = PendingIntent.getActivity(c, 0, new Intent(c, EntryActivity.class), 0);
			String clickToView = c.getString(R.string.click_to_view);
			NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

			if (unread.cmt > 0) {
				if (DEBUG) {
					Log.d(TAG, "New comment: " + unread.cmt);
				}

				Notification n = buildNotification(c,
						format(c, R.string.new_comment, unread.cmt),
						clickToView,
						R.drawable.ic_action_chat,
						defaults,
						i);
				nm.notify(ID_CMT, n);
			}

			if (unread.mention_status > 0 || unread.mention_cmt > 0) {
				String detail = "";
				int count = 0;
				
				if (unread.mention_status > 0) {
					detail += format(c, R.string.new_at_detail_weibo, unread.mention_status);
					count += unread.mention_status;
				}

				if (unread.mention_cmt > 0) {
					if (count > 0) {
						detail += c.getString(R.string.new_at_detail_and);
					}

					detail += format(c, R.string.new_at_detail_comment, unread.mention_cmt);
					count += unread.mention_cmt;
				}

				if (DEBUG) {
					Log.d(TAG, "New mentions: " + count);
				}

				Notification n = buildNotification(c,
						format(c, R.string.new_at, count),
						detail,
						R.drawable.ic_action_reply_all,
						defaults,
						i);
				nm.notify(ID_MENTION, n);
			}

			if (unread.dm > 0) {
				if (DEBUG) {
					Log.d(TAG, "New dm: " + unread.dm);
				}
					
				Notification n = buildNotification(c,
						format(c, R.string.new_dm, unread.dm),
						clickToView,
						R.drawable.ic_action_email,
						defaults,
						i);
				nm.notify(ID_DM, n);
			}
		}
	}

	public ReminderService() {
		super(TAG);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onHandleIntent(Intent intent) {
		if (DEBUG) {
			Log.d(TAG, "start");
		}

		doFetchRemind();

	}

	private static void updateTimeLine(Object obj) {
		try {
			Method loadFromCache = findMethod(obj, "loadFromCache");
			Method load = findMethod(obj, "load", boolean.class);
			Method cache = findMethod(obj, "cache");
			loadFromCache.setAccessible(true);
			load.setAccessible(true);
			cache.setAccessible(true);
			loadFromCache.invoke(obj);
			load.invoke(obj, true);
			cache.invoke(obj);
		} catch (Exception e) {
			if (DEBUG) {
				Log.e(TAG, "WTF?! Cannot update time line??");
				Log.e(TAG, Log.getStackTraceString(e));
			}
		}
	}

	private static Method findMethod(Object obj, String name, Class<?>... params) {
		Class<?> clazz = obj.getClass();

		while (clazz != null) {
			try {
				return clazz.getDeclaredMethod(name, params);
			} catch (Exception e) {
				clazz = clazz.getSuperclass();
			}
		}

		return null;
	}

	private static int parseDefaults(Context context) {
		Settings settings = Settings.getInstance(context);

		return (settings.getBoolean(Settings.NOTIFICATION_SOUND, true) ? Notification.DEFAULT_SOUND : 0) |
			(settings.getBoolean(Settings.NOTIFICATION_VIBRATE, true) ? Notification.DEFAULT_VIBRATE : 0) |
			Notification.DEFAULT_LIGHTS;
	}

	private static Notification buildNotification(Context context, String title, String text, int icon, int defaults, PendingIntent intent) {
		return new Notification.Builder(context)
			.setContentTitle(title)
			.setContentText(text)
			.setSmallIcon(icon)
			.setDefaults(defaults)
			.setAutoCancel(true)
			.setContentIntent(intent)
			.build();
	}

	private static String format(Context context, int resId, int data) {
		return String.format(context.getString(resId), data);
	}
}
