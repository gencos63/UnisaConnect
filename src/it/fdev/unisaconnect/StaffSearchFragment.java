package it.fdev.unisaconnect;

import it.fdev.unisaconnect.data.StaffDB;
import it.fdev.unisaconnect.data.StaffMemberSummary;
import it.fdev.utils.ListAdapter;
import it.fdev.utils.ListAdapter.ListItem;
import it.fdev.utils.MyListFragment;

import java.util.ArrayList;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

public class StaffSearchFragment extends MyListFragment {

	private EditText editTextStaffName;
	private ImageView imgClearName;
	private StaffDB db = null;
	private ListAdapter adapter;
	private ArrayList<StaffMemberSummary> listaRisultati = new ArrayList<StaffMemberSummary>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			db = new StaffDB(activity);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		adapter = new ListAdapter(activity, R.layout.row_big_img, new ArrayList<ListItem>());
		setListAdapter(adapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.staff_search_list, container, false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (db == null)
			return;
		
//		View lastUpdateViewContainer = view.findViewById(R.id.last_update_time_container);
//		TextView lastUpdateView = (TextView) view.findViewById(R.id.last_update_time);
//		try {
//			SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss", Locale.ITALY);
//			Date date = formatter.parse(StaffDB.DB_STAFF_DATE);
//			String dateFirstPart = new SimpleDateFormat("dd/MM/yy", Locale.ITALY).format(date);
//			String updateText = getString(R.string.aggiornato_il, dateFirstPart);
//			lastUpdateView.setText(updateText);
//			lastUpdateViewContainer.setVisibility(View.VISIBLE);
//		} catch (ParseException e) {
//			Log.e(Utils.TAG, "Error parsing date", e);
//			lastUpdateViewContainer.setVisibility(View.GONE);
//		}
		
		
		imgClearName = (ImageView) view.findViewById(R.id.staff_name_clear);
		imgClearName.setVisibility(View.INVISIBLE);
		imgClearName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editTextStaffName.setText("");
			}
		});
		
		editTextStaffName = (EditText) view.findViewById(R.id.staff_name);
		editTextStaffName.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				initList();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() > 0)
					imgClearName.setVisibility(View.VISIBLE);
				else
					imgClearName.setVisibility(View.INVISIBLE);
			}
		});
		initList();
	}

	@Override
	public void onListItemClick(ListView lv, View v, int position, long id) {
		StaffMemberSummary choosenMember = listaRisultati.get(position);
		StaffDetailsFragment staffDetailsFrag = new StaffDetailsFragment();
		staffDetailsFrag.setMemberDetails(db.getStaffMember(choosenMember.getMatricola()));
		activity.switchContent(staffDetailsFrag);
	}
	
	public void initList() {
		if (!isAdded()) {
			return;
		}
		String name = editTextStaffName.getText().toString();
		listaRisultati = db.getStaffSummaryByName(name);
		ArrayList<ListItem> itemList = new ArrayList<ListItem>(listaRisultati);
		adapter.clear();
		adapter.addAll(itemList);
		adapter.notifyDataSetChanged();
		this.setSelection(0);
	}

	@Override
	public void onDestroy() {
		db.close();
		super.onDestroy();
	}
}