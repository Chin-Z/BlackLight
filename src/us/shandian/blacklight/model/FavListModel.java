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

package us.shandian.blacklight.model;

import java.util.List;
import java.util.ArrayList;

public class FavListModel
{
	private List<FavModel> favorites = new ArrayList<FavModel>();

	public MessageListModel toMsgList() {
		MessageListModel msg = new MessageListModel();
		msg.total_number = 0;
		msg.previous_cursor = "";
		msg.next_cursor = "";
		
		@SuppressWarnings("unchecked")
        List<MessageModel> msgs = (List<MessageModel>) msg.getList();
		
		for (FavModel fav : favorites) {
			msgs.add(fav.status);
		}
		
		return msg;
	}
}
