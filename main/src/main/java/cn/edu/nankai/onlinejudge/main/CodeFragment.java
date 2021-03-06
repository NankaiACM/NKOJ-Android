package cn.edu.nankai.onlinejudge.main;


import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.*;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yydcdut.sdlv.Menu;
import com.yydcdut.sdlv.MenuItem;
import com.yydcdut.sdlv.SlideAndDragListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.Request;

import static cn.edu.nankai.onlinejudge.main.ProblemSubmitFragment.HTTPREQ_SUBMIT_CODE;


/**
 * A simple {@link Fragment} subclass.
 */
public class CodeFragment extends Fragment implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener,
        SlideAndDragListView.OnDragDropListener, SlideAndDragListView.OnSlideListener,
        SlideAndDragListView.OnMenuItemClickListener, SlideAndDragListView.OnItemDeleteListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private List<Menu> mMenuList;
    private List<ApplicationInfo> mAppList;
    private SlideAndDragListView mListView;
    private Toast mToast;
    private ApplicationInfo mDraggedEntity;
    private ArrayList<String> code = new ArrayList<>();
    private View view;
    private String pid;
    public String getAllCode(){
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < code.size(); i++){
            str.append(code.get(i));
            str.append('\n');
        }
        return str.toString();
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return code.size();
        }

        @Override
        public Object getItem(int position) {
            return mAppList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mAppList.get(position).hashCode();
        }

        @Override
        public int getItemViewType(int position) {
            return position % 3;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CustomViewHolder cvh;
            if (convertView == null) {
                cvh = new CustomViewHolder();
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_custom_btn, null);
                cvh.txtName = (TextView) convertView.findViewById(R.id.txt_item_edit);
                convertView.setTag(cvh);
            } else {
                cvh = (CustomViewHolder) convertView.getTag();
            }
            ApplicationInfo item = (ApplicationInfo) this.getItem(position);
            cvh.txtName.setText(code.get(position % code.size()));

            /*if (getItemViewType(position) == 2) {
                cvh.txtName.setText("No Menu");
            } else if (getItemViewType(position) == 0) {
                cvh.txtName.setText("right");
            } else {
                cvh.txtName.setText("left");
            }*/
            return convertView;
        }

        class CustomViewHolder {
            public TextView txtName;
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_code, container, false);
        assert getArguments() != null;
        String data = getArguments().getString("code");
        pid = getArguments().getString("pid");
        Button button = view.findViewById(R.id.magic_submit_btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ProblemSubmitFragment().magicCall((MainActivity) getActivity(), pid, getAllCode());
            }
        });
        initData(data);
        initMenu();
        initUiAndListener();
        mToast = Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT);
        mToast.show();
        return view;
    }

    public void initMenu() {
        mMenuList = new ArrayList<>();
        Menu menu0 = new Menu(true, 0);
        Menu menu1 = new Menu(false, 1);
        Menu menu2 = new Menu(false, 2);
        mMenuList.add(menu0);
        mMenuList.add(menu1);
        mMenuList.add(menu2);
    }

    public void initUiAndListener() {
        mListView = (SlideAndDragListView) view.findViewById(R.id.lv_edit);
        mListView.setMenu(mMenuList);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnDragDropListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setOnSlideListener(this);
        mListView.setOnMenuItemClickListener(this);
        mListView.setOnItemDeleteListener(this);
        mListView.setDivider(new ColorDrawable(Color.GRAY));
        mListView.setDividerHeight(1);
    }

    public void initData(String args) {

        code.addAll(Arrays.asList(args.split("\n")));
        Collections.shuffle(code);
        mAppList = new ArrayList<ApplicationInfo>(code.size());
        int temp = code.size();
        while (temp-- != 0) mAppList.add(new ApplicationInfo());
        //mAppList = getActivity().getPackageManager().getInstalledApplications(0);
    }

    @Override
    public void onDragViewStart(int beginPosition) {
        mDraggedEntity = mAppList.get(beginPosition);
        mToast.setText("onDragViewStart   position--->" + beginPosition);
        mToast.show();
        Log.i(TAG, "onDragViewStart   " + beginPosition);
    }

    @Override
    public void onDragDropViewMoved(int fromPosition, int toPosition) {
        ApplicationInfo applicationInfo = mAppList.remove(fromPosition);
        mAppList.add(toPosition, applicationInfo);
        Log.i(TAG, "onDragDropViewMoved  fromPosition--> " + fromPosition + "  toPosition-->" + toPosition);
        mToast.setText("onDragDropViewMoved  fromPosition--> " + fromPosition + "  toPosition-->" + toPosition);
        String temp = code.get(fromPosition);
        code.set(fromPosition, code.get(toPosition));
        code.set(toPosition, temp);
        mToast.show();
    }

    @Override
    public void onDragViewDown(int finalPosition) {
        mAppList.set(finalPosition, mDraggedEntity);
        mToast.setText("onDragViewDown   finalPosition--->" + finalPosition);
        mToast.show();

        Log.i(TAG, "onDragViewDown   " + finalPosition);
    }

    @Override
    public void onSlideOpen(View view, View parentView, int position, int direction) {
        mToast.setText("onSlideOpen   position--->" + position + "  direction--->" + direction);
        mToast.show();
        Log.i(TAG, "onSlideOpen   " + position);
    }

    @Override
    public void onSlideClose(View view, View parentView, int position, int direction) {
        mToast.setText("onSlideClose   position--->" + position + "  direction--->" + direction);
        mToast.show();
        Log.i(TAG, "onSlideClose   " + position);
    }

    @Override
    public int onMenuItemClick(View v, int itemPosition, int buttonPosition, int direction) {
        Log.i(TAG, "onMenuItemClick   " + itemPosition + "   " + buttonPosition + "   " + direction);
        int viewType = mAdapter.getItemViewType(itemPosition);
        switch (viewType) {
            case 0:
                return clickMenuBtn0(buttonPosition, direction);
            case 1:
                return clickMenuBtn1(buttonPosition, direction);
            default:
                return Menu.ITEM_NOTHING;
        }
    }

    private int clickMenuBtn0(int buttonPosition, int direction) {
        switch (direction) {
            case MenuItem.DIRECTION_RIGHT:
                switch (buttonPosition) {
                    case 0:
                        return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP;
                    case 1:
                        return Menu.ITEM_NOTHING;
                    case 2:
                        return Menu.ITEM_SCROLL_BACK;
                }
        }
        return Menu.ITEM_NOTHING;
    }

    private int clickMenuBtn1(int buttonPosition, int direction) {
        switch (direction) {
            case MenuItem.DIRECTION_LEFT:
                switch (buttonPosition) {
                    case 0:
                        return Menu.ITEM_SCROLL_BACK;
                    case 1:
                        return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP;
                }
                break;
        }
        return Menu.ITEM_NOTHING;
    }

    @Override
    public void onItemDeleteAnimationFinished(View view, int position) {
        mAppList.remove(position - mListView.getHeaderViewsCount());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "onItemClick   " + position);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "onItemLongClick   " + position);
        return false;
    }
}
