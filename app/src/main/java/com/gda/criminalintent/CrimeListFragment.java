package com.gda.criminalintent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;
import java.util.UUID;

import javax.security.auth.callback.Callback;

public class CrimeListFragment extends Fragment {
    private RecyclerView mCrimeRecyclerView;
    private TextView mNoCrimeText;
    private Button mNewCrimeButton;
    private CrimeAdapter mAdapter;
    private int mLastClickedCrimePos;
    private  boolean mSubtitleVisible;
    private Callbacks mCallbacks;

    private static final String SAVED_SUBTITLE_VISIBLE = "subtitle";
    public static final String CRIME_POS = "com.gda.criminalintent.crime_pos";
    public static final int CRIME_STATE = 0;
    public static final int CRIME_RESULT_REMOVED = 777;

    public interface Callbacks {
        void onCrimeSelected(Crime crime);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("CrimeLIstFragment","onCreateView");
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);

        mNoCrimeText = (TextView) view.findViewById(R.id.noCrimeTextView);
        mNewCrimeButton = (Button) view.findViewById(R.id.newCrimeButton);
        mNewCrimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewCrimeActivity();
            }
        });

        mCrimeRecyclerView = (RecyclerView) view.findViewById(R.id.crime_recycler_view);
        mCrimeRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mSubtitleVisible = false;

        if (savedInstanceState != null) {
            mSubtitleVisible = savedInstanceState.getBoolean(SAVED_SUBTITLE_VISIBLE);
        }

        updateUI();

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d("CrimeLIstFragment","onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SUBTITLE_VISIBLE, mSubtitleVisible);
    }

    @Override
    public void onResume() {
        Log.d("CrimeLIstFragment","onResume");
        super.onResume();
        updateUI();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_crime_list, menu);

        MenuItem menuItem = menu.findItem(R.id.menu_item_show_subtitle);
        if (mSubtitleVisible) {
            menuItem.setTitle(R.string.hide_subtitle);
        } else {
            menuItem.setTitle(R.string.show_subtitle);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_new_crime:
                startNewCrimeActivity();
                return true;
            case R.id.menu_item_show_subtitle:
                mSubtitleVisible = !mSubtitleVisible;
                updateSubtitle();
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void startNewCrimeActivity() {
        Crime crime = new Crime();
        CrimeLab.get(getActivity()).addCrime(crime);
//        Intent intent = CrimePagerActivity.newIntent(getActivity(), crime.getId());
//        startActivity(intent);
        mCallbacks.onCrimeSelected(crime);
        updateUI();
    }

    private void updateSubtitle() {
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        int crimeCount = crimeLab.getCrimeList().size();
//        String subtitle = getString(R.string.subtitle_format, crimeCount);
        String subtitle = getResources().getQuantityString(R.plurals.subtitle_plural, crimeCount, crimeCount);

        if (!mSubtitleVisible) {
            subtitle = null;
        }

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setSubtitle(subtitle);

    }

    public void updateUI() {
        Log.d("CrimeLIstFragment","updateUI");
        CrimeLab crimeLab = CrimeLab.get(getActivity());
        List<Crime> crimes = crimeLab.getCrimeList();

        if (crimes.size() == 0) {
            mNoCrimeText.setVisibility(View.VISIBLE);
            mNewCrimeButton.setVisibility(View.VISIBLE);
        } else {
            mNoCrimeText.setVisibility(View.INVISIBLE);
            mNewCrimeButton.setVisibility(View.INVISIBLE);
        }

        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyItemChanged(mLastClickedCrimePos);
        }

        mAdapter.setCrimes(crimes);
        updateSubtitle();

    }

    private void updateUIRemoved() {
        if (mAdapter != null) {
            Log.d("CrimeLIstFragment","updateUIRemoved");
            mAdapter.notifyItemRemoved(mLastClickedCrimePos);
        }
    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mTitleTextView;
        private TextView mDateTextView;
        private CheckBox mSolvedCheckBox;
        private Crime mCrime;

        public CrimeHolder(View itemView) {
            super(itemView);

            mTitleTextView = (TextView) itemView.findViewById(R.id.list_item_crime_title_text_view);
            mDateTextView = itemView.findViewById(R.id.list_item_crime_date_text_view);
            mSolvedCheckBox = itemView.findViewById(R.id.list_item_crime_solved_check_box);
            mSolvedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mCrime.setSolved(isChecked);
                }
            });
            itemView.setOnClickListener(this);
        }

        public void bindCrime(Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getTitle());
            mDateTextView.setText(mCrime.getDate().toString());
            mSolvedCheckBox.setChecked(mCrime.isSolved());
        }

        @Override
        public void onClick(View v) {
            mLastClickedCrimePos = getAdapterPosition();

            mCallbacks.onCrimeSelected(mCrime);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("CrimeListFragment", "onActivityResult");
        if (requestCode == CRIME_STATE) {
            if (resultCode == CRIME_RESULT_REMOVED) {
                Log.d("CrimeListFragment", "onActivityResult: ---> CRIME_RESULT_REMOVED");
                updateUIRemoved();
            }
            if (data != null) {
//                mLastClickedCrimePos = data.getIntExtra(CRIME_POS, -1);

            }
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes) {
            mCrimes = crimes;
        }

        @NonNull
        @Override
        public CrimeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);
            return new CrimeHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            holder.bindCrime(crime);
        }

        @Override
        public int getItemCount() {
            return mCrimes.size();
        }

        public void setCrimes(List<Crime> crimes) {
            mCrimes = crimes;
        }
    }
}
