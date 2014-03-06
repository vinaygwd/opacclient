/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.frontend;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.geeksfactory.opacclient.OpacClient;
import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.AccountDataSource;

public class NavigationFragment extends Fragment {

	protected long tolerance;
	protected SharedPreferences sp;
	protected AccountDataSource aData;

	public int getLayoutResource() {
		return R.layout.sliding_navigation;
	}

	protected void registerListeners(View v) {
		v.findViewById(R.id.llNavSearch).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								SearchActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavAccount).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								AccountActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavStarred).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								StarredActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavInfo).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								InfoActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavSettings).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								MainPreferenceActivity.class);
						startActivity(intent);
					}
				});

		v.findViewById(R.id.llNavAbout).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent intent = new Intent(getActivity(),
								AboutActivity.class);
						startActivity(intent);
					}
				});
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(getLayoutResource(), null);
		registerListeners(v);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		aData = new AccountDataSource(getActivity());
		reload();
	}

	public void reload() {
		sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		tolerance = Long.decode(sp.getString("notification_warning",
				"367200000"));

		Library lib = ((OpacClient) getActivity().getApplication())
				.getLibrary();

		if (lib != null) {
			TextView tvBn = (TextView) getView().findViewById(R.id.tvLibrary);
			if (lib.getTitle() != null && !lib.getTitle().equals("null"))
				tvBn.setText(lib.getCity() + " · " + lib.getTitle());
			else
				tvBn.setText(lib.getCity());

			try {
				if (lib.getData().getString("information") != null) {
					if (!lib.getData().getString("information").equals("null")) {
						getView().findViewById(R.id.llNavInfo).setVisibility(
								View.VISIBLE);
					} else {
						getView().findViewById(R.id.llNavInfo).setVisibility(
								View.GONE);
					}
				} else {
					getView().findViewById(R.id.llNavInfo).setVisibility(
							View.GONE);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		LinearLayout llAccountlist = (LinearLayout) getView().findViewById(
				R.id.llAccountlist);
		llAccountlist.removeAllViews();
		aData.open();
		List<Account> accounts = aData.getAllAccounts();
		if (accounts.size() > 1) {
			for (final Account account : accounts) {
				View v = getAccountItemView(account);
				llAccountlist.addView(v);
			}
			getView().findViewById(R.id.tvHlAccountlist).setVisibility(
					View.VISIBLE);
		} else {
			getView().findViewById(R.id.tvHlAccountlist).setVisibility(
					View.GONE);
		}
		aData.close();
	}

	protected View getAccountItemView(final Account account) {
		View v = getActivity().getLayoutInflater().inflate(
				R.layout.account_listitem_nav, null);
		((TextView) v.findViewById(R.id.tvLabel)).setText(account.getLabel());
		if (account.getId() == ((OpacClient) getActivity().getApplication())
				.getAccount().getId()) {
			((TextView) v.findViewById(R.id.tvLabel)).setTypeface(null,
					Typeface.BOLD);
			v.findViewById(R.id.llAccount).setClickable(false);
			v.findViewById(R.id.llAccount).setBackgroundResource(
					R.drawable.nav_active);
			v.findViewById(R.id.viewRedbar).setBackgroundColor(
					getResources().getColor(R.color.nav_highlighted_border));
		}

		Library library;
		try {
			library = ((OpacClient) getActivity().getApplication())
					.getLibrary(account.getLibrary());
			TextView tvCity = (TextView) v.findViewById(R.id.tvCity);
			if (library.getTitle() != null
					&& !library.getTitle().equals("null")) {
				tvCity.setText(library.getCity() + " · " + library.getTitle());
			} else {
				tvCity.setText(library.getCity());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		TextView tvNotifications = (TextView) v
				.findViewById(R.id.tvNotifications);
		int expiring = aData.getExpiring(account, tolerance);
		if (expiring > 0) {
			tvNotifications.setText("" + expiring);
		} else {
			tvNotifications.setText("");
		}
		v.findViewById(R.id.llAccount).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						selectaccount(account.getId());
					}
				});
		v.findViewById(R.id.llAccount).setOnLongClickListener(
				new OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						Intent i = new Intent(getActivity(),
								AccountEditActivity.class);
						i.putExtra("id", account.getId());
						startActivity(i);
						return true;
					}
				});
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		reload();
	}

	public void selectaccount(long id) {
		((OpacClient) getActivity().getApplication()).setAccount(id);
		if (getActivity() instanceof OpacActivity) {
			((OpacActivity) getActivity()).accountSelected();
			((OpacActivity) getActivity()).showContent();
			reload();
		}
	}
}