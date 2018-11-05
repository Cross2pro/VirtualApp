package io.virtualapp.home;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.client.stub.ChooseTypeAndAccountActivity;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.VDeviceInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.nestedadapter.SmartRecyclerAdapter;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.LaunchpadAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.location.LocationSettingsActivity;
import io.virtualapp.home.models.AddAppButton;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.EmptyAppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.widgets.TwoGearsView;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;
import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.END;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.START;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;

/**
 * @author Lody
 */
public class HomeActivity extends VActivity implements HomeContract.HomeView {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private HomeContract.HomePresenter mPresenter;
    private TwoGearsView mLoadingView;
    private RecyclerView mLauncherView;
    private View mMenuView;
    private PopupMenu mPopupMenu;
    private View mBottomArea;
    private View mCreateShortcutBox;
    private TextView mCreateShortcutTextView;
    private View mDeleteAppBox;
    private TextView mDeleteAppTextView;
    private LaunchpadAdapter mLaunchpadAdapter;
    private Handler mUiHandler;

    public static void goHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUiHandler = new Handler(Looper.getMainLooper());
        bindViews();
        initLaunchpad();
        initMenu();
        new HomePresenterImpl(this).start();
        initMagic();
//        IBinderTool.printAllService();
//        IBinderTool.printIBinder("android.content.IFlymePermissionService");
        //debug
        //testMockDevice();
        //end debug
    }

    /**
     * create default build.prop
     * cat /system/build.prop -> /virtual/data/0/system/build.prop
     * tip:Android7.0 + don't read /system/build.prop
     *
     * how use?
     * @see  com.lody.virtual.client.ipc.VDeviceManager#createBuildEditor
     * @see  com.lody.virtual.client.ipc.VDeviceManager.Editor
     */
    private void testMockDevice(){
        VDeviceInfo deviceInfo = VDeviceManager.get().getDeviceInfo(0);
        deviceInfo.setWifiMac("12:34:56:78:9a:bc");
        VDeviceManager.get().updateDeviceInfo(0, deviceInfo);

        VDeviceManager.Editor editor = VDeviceManager.get().createBuildEditor(0);
        editor.setDefault();
        editor.setBrand("1");
        editor.setBoard("2");
        editor.setDevice("3");
        editor.setDisplay("4");
        editor.setID("5");
        editor.setProduct("6");
        editor.setSerial("7");
        if (editor.save()) {
            Log.i("kk", "保存配置ok");
        } else {
            Log.e("kk", "保存配置失败");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_PACKAGE_ADDED);
        filter.addAction(Constants.ACTION_PACKAGE_REMOVED);
        filter.addAction(Constants.ACTION_PACKAGE_WILL_ADDED);
        filter.addDataScheme("package");
        registerReceiver(mReceiver, filter);
    }
    private void initMagic() {
        try {
            String filePath = getApplicationContext().getFilesDir().getAbsolutePath();
            String fileName = filePath + "/magic.mgc";
            File f = new File(fileName);
            if (!f.exists()) {
                new File(filePath).mkdirs();
                new Thread().sleep(500);
                copyBigDataToSD(fileName);
            }
        } catch (IOException e) {
            Log.e("lxf", "copy magicto sdcard is failed!");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = this.getAssets().open("magic.mgc");
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    ProgressDialog mDialog = null;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String pkg = intent.getData() == null ? null : intent.getData().getSchemeSpecificPart();
            if (Constants.ACTION_PACKAGE_WILL_ADDED.equals(intent.getAction())) {
                String msg = "wait update " + mPresenter.getLabel(pkg);
                if (mDialog != null) {
                    mDialog.setMessage(msg);
                } else {
                    mDialog = ProgressDialog.show(context, null, msg);
                }
            } else {
                if (mDialog != null && mDialog.isShowing()) {
                    mDialog.dismiss();
                    mPresenter.dataChanged();
                    mDialog = null;
                }
            }
        }
    };

    private void initMenu() {
        mPopupMenu = new PopupMenu(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light), mMenuView);
        Menu menu = mPopupMenu.getMenu();
        setIconEnable(menu, true);
        menu.add("Accounts").setIcon(R.drawable.ic_account).setOnMenuItemClickListener(item -> {
            List<VUserInfo> users = VUserManager.get().getUsers();
            List<String> names = new ArrayList<>(users.size());
            for (VUserInfo info : users) {
                names.add(info.name);
            }
            CharSequence[] items = new CharSequence[names.size()];
            for (int i = 0; i < names.size(); i++) {
                items[i] = names.get(i);
            }
            new AlertDialog.Builder(this)
                    .setTitle("Please select an user")
                    .setItems(items, (dialog, which) -> {
                        VUserInfo info = users.get(which);
                        Intent intent = new Intent(this, ChooseTypeAndAccountActivity.class);
                        intent.putExtra(ChooseTypeAndAccountActivity.KEY_USER_ID, info.id);
                        startActivity(intent);
                    }).show();
            return false;
        });
        menu.add("Virtual Storage").setIcon(R.drawable.ic_vs).setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "The coming", Toast.LENGTH_SHORT).show();
            return false;
        });
        menu.add("Notification").setIcon(R.drawable.ic_notification).setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "The coming", Toast.LENGTH_SHORT).show();
            return false;
        });
        menu.add("Virtual Location").setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, LocationSettingsActivity.class));
            return true;
        });
        menu.add("Install Google Support").setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            askInstallGms();
            return true;
        });
        menu.add("Settings").setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "The coming", Toast.LENGTH_SHORT).show();
            return false;
        });
        mMenuView.setOnClickListener(v -> mPopupMenu.show());
    }

    private static void setIconEnable(Menu menu, boolean enable) {
        try {
            @SuppressLint("PrivateApi")
            Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindViews() {
        mLoadingView = (TwoGearsView) findViewById(R.id.pb_loading_app);
        mLauncherView = (RecyclerView) findViewById(R.id.home_launcher);
        mMenuView = findViewById(R.id.home_menu);
        mBottomArea = findViewById(R.id.bottom_area);
        mCreateShortcutBox = findViewById(R.id.create_shortcut_area);
        mCreateShortcutTextView = (TextView) findViewById(R.id.create_shortcut_text);
        mDeleteAppBox = findViewById(R.id.delete_app_area);
        mDeleteAppTextView = (TextView) findViewById(R.id.delete_app_text);
    }

    private void initLaunchpad() {
        mLauncherView.setHasFixedSize(true);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL);
        mLauncherView.setLayoutManager(layoutManager);
        mLaunchpadAdapter = new LaunchpadAdapter(this);
        SmartRecyclerAdapter wrap = new SmartRecyclerAdapter(mLaunchpadAdapter);
        View footer = new View(this);
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
        ItemTouchHelper touchHelper = new ItemTouchHelper(new LauncherTouchCallback());
        touchHelper.attachToRecyclerView(mLauncherView);
        mLaunchpadAdapter.setAppClickListener((pos, data) -> {
            if (!data.isLoading()) {
                if (data instanceof AddAppButton) {
                    onAddAppButtonClick();
                }
                mLaunchpadAdapter.notifyItemChanged(pos);
                mPresenter.launchApp(data);
            }
        });
    }

    private void onAddAppButtonClick() {
        ListAppActivity.gotoListApp(this);
    }

    private void deleteApp(int position) {
        AppData data = mLaunchpadAdapter.getList().get(position);
        new AlertDialog.Builder(this)
                .setTitle("Delete app")
                .setMessage("Do you want to delete " + data.getName() + "?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mPresenter.deleteApp(data);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void createShortcut(int position) {
        AppData model = mLaunchpadAdapter.getList().get(position);
        if (model instanceof PackageAppData || model instanceof MultiplePackageAppData) {
            mPresenter.createShortcut(model);
        }
    }

    @Override
    public void setPresenter(HomeContract.HomePresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showBottomAction() {
        mBottomArea.setTranslationY(mBottomArea.getHeight());
        mBottomArea.setVisibility(View.VISIBLE);
        mBottomArea.animate().translationY(0).setDuration(500L).start();
    }

    @Override
    public void hideBottomAction() {
        mBottomArea.setTranslationY(0);
        ObjectAnimator transAnim = ObjectAnimator.ofFloat(mBottomArea, "translationY", 0, mBottomArea.getHeight());
        transAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        transAnim.setDuration(500L);
        transAnim.start();
    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.startAnim();
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
        mLoadingView.stopAnim();
    }

    @Override
    public void loadFinish(List<AppData> list) {
        list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);
        hideLoading();
    }

    @Override
    public void loadError(Throwable err) {
        err.printStackTrace();
        hideLoading();
    }

    @Override
    public void showGuide() {

    }

    @Override
    public void addAppToLauncher(AppData model) {
        List<AppData> dataList = mLaunchpadAdapter.getList();
        boolean replaced = false;
        for (int i = 0; i < dataList.size(); i++) {
            AppData data = dataList.get(i);
            if (data instanceof EmptyAppData) {
                mLaunchpadAdapter.replace(i, model);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            mLaunchpadAdapter.add(model);
            mLauncherView.smoothScrollToPosition(mLaunchpadAdapter.getItemCount() - 1);
        }
    }


    @Override
    public void removeAppToLauncher(AppData model) {
        mLaunchpadAdapter.remove(model);
    }

    @Override
    public void refreshLauncherItem(AppData model) {
        mLaunchpadAdapter.refresh(model);
    }

    @Override
    public void askInstallGms() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.tip_install_gms)
                .setMessage(R.string.text_install_gms)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    defer().when(() -> {
                        GmsSupport.installGApps(0);
                    }).done((res) -> {
                        mPresenter.dataChanged();
                    });
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        Toast.makeText(HomeActivity.this, "You can also find it in the Settings~", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
            if (appList != null) {
                for (AppInfoLite info : appList) {
                    mPresenter.addApp(info);
                }
            }
        }
    }

    private class LauncherTouchCallback extends ItemTouchHelper.SimpleCallback {

        int[] location = new int[2];
        boolean upAtDeleteAppArea;
        boolean upAtCreateShortcutArea;
        RecyclerView.ViewHolder dragHolder;

        LauncherTouchCallback() {
            super(UP | DOWN | LEFT | RIGHT | START | END, 0);
        }

        @Override
        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            return 0;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            try {
                AppData data = mLaunchpadAdapter.getList().get(viewHolder.getAdapterPosition());
                if (!data.canReorder()) {
                    return makeMovementFlags(0, 0);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int pos = viewHolder.getAdapterPosition();
            int targetPos = target.getAdapterPosition();
            mLaunchpadAdapter.moveItem(pos, targetPos);
            return true;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                if (actionState == ACTION_STATE_DRAG) {
                    if (dragHolder != viewHolder) {
                        dragHolder = viewHolder;
                        viewHolder.itemView.setScaleX(1.2f);
                        viewHolder.itemView.setScaleY(1.2f);
                        if (mBottomArea.getVisibility() == View.GONE) {
                            showBottomAction();
                        }
                    }
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            if (upAtCreateShortcutArea || upAtDeleteAppArea) {
                return false;
            }
            try {
                AppData data = mLaunchpadAdapter.getList().get(target.getAdapterPosition());
                return data.canReorder();
            } catch (IndexOutOfBoundsException e) {
                //ignore
            }
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                LaunchpadAdapter.ViewHolder holder = (LaunchpadAdapter.ViewHolder) viewHolder;
                viewHolder.itemView.setScaleX(1f);
                viewHolder.itemView.setScaleY(1f);
                viewHolder.itemView.setBackgroundColor(holder.color);
            }
            super.clearView(recyclerView, viewHolder);
            if (dragHolder == viewHolder) {
                if (mBottomArea.getVisibility() == View.VISIBLE) {
                    mUiHandler.postDelayed(HomeActivity.this::hideBottomAction, 200L);
                    if (upAtCreateShortcutArea) {
                        createShortcut(viewHolder.getAdapterPosition());
                    } else if (upAtDeleteAppArea) {
                        deleteApp(viewHolder.getAdapterPosition());
                    }
                }
                dragHolder = null;
            }
        }


        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if (actionState != ACTION_STATE_DRAG || !isCurrentlyActive) {
                return;
            }
            View itemView = viewHolder.itemView;
            itemView.getLocationInWindow(location);
            int x = (int) (location[0] + dX);
            int y = (int) (location[1] + dY);

            mBottomArea.getLocationInWindow(location);
            int baseLine = location[1] - mBottomArea.getHeight();
            if (y >= baseLine) {
                mDeleteAppBox.getLocationInWindow(location);
                int deleteAppAreaStartX = location[0];
                if (x < deleteAppAreaStartX) {
                    upAtCreateShortcutArea = true;
                    upAtDeleteAppArea = false;
                    mCreateShortcutTextView.setTextColor(Color.parseColor("#0099cc"));
                    mDeleteAppTextView.setTextColor(Color.WHITE);
                } else {
                    upAtDeleteAppArea = true;
                    upAtCreateShortcutArea = false;
                    mDeleteAppTextView.setTextColor(Color.parseColor("#0099cc"));
                    mCreateShortcutTextView.setTextColor(Color.WHITE);
                }
            } else {
                upAtCreateShortcutArea = false;
                upAtDeleteAppArea = false;
                mDeleteAppTextView.setTextColor(Color.WHITE);
                mCreateShortcutTextView.setTextColor(Color.WHITE);
            }
        }
    }
}
