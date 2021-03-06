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

package us.shandian.blacklight.ui.common;

import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.os.Bundle;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import us.shandian.blacklight.R;

public class ColorPickerFragment extends Fragment implements View.OnClickListener {

	public static interface OnColorSelectedListener {
		void onSelected(String hex);
	}

	private ColorPicker mPicker;
	private Button mOkay;
	private OnColorSelectedListener mListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.color_picker, null);

		// Main picker
		mPicker = (ColorPicker) v.findViewById(R.id.picker);

		// Color picker views
		SVBar svb = (SVBar) v.findViewById(R.id.svbar);
		SaturationBar sb = (SaturationBar) v.findViewById(R.id.saturationbar);
		ValueBar vb = (ValueBar) v.findViewById(R.id.valuebar);

		// Okay button
		mOkay = (Button) v.findViewById(R.id.okay);
		mOkay.setOnClickListener(this);

		// Register
		mPicker.addSVBar(svb);
		mPicker.addSaturationBar(sb);
		mPicker.addValueBar(vb);

		// Default color
		int color = getResources().getColor(R.color.darker_gray);
		mPicker.setOldCenterColor(color);
		mPicker.setNewCenterColor(color);
		mPicker.setColor(color);
		
		return v;
	}

	@Override
	public void onClick(View v) {
		String hex = String.format("#%06X", (0xFFFFFF & mPicker.getColor()));
		mPicker.setOldCenterColor(mPicker.getColor());

		if (mListener != null) {
			mListener.onSelected(hex);
		}
	}

	public void setOnColorSelectedListener(OnColorSelectedListener listener) {
		mListener = listener;
	}
}
