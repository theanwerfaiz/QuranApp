package com.quranapp.android.activities;

import static com.quranapp.android.utils.app.AppActions.APP_ACTION_KEY;
import static com.quranapp.android.utils.app.AppActions.APP_ACTION_VICTIM_KEY;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.peacedesign.android.widget.sheet.PeaceBottomSheet;
import com.quranapp.android.R;
import com.quranapp.android.activities.account.ActivityAccount;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.utility.ViewPagerAdapter2;
import com.quranapp.android.databinding.ActivityMainBinding;
import com.quranapp.android.databinding.LytAccountReminderBinding;
import com.quranapp.android.frags.main.FragMain;
import com.quranapp.android.suppliments.IndexMenu;
import com.quranapp.android.utils.account.AccManager;
import com.quranapp.android.utils.app.AppActions;
import com.quranapp.android.utils.app.UpdateManager;
import com.quranapp.android.utils.sp.SPAppActions;
import com.quranapp.android.widgets.tablayout.BottomTab;
import com.quranapp.android.widgets.tablayout.BottomTabLayout;

import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    private ActivityMainBinding mBinding;
    private IndexMenu mIndexMenu;
    private UpdateManager mUpdateManager;

    @Override
    protected int getStatusBarBG() {
        return ContextCompat.getColor(this, R.color.colorBGHomePageItem);
    }

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_main;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIndexMenu != null) {
            mIndexMenu.close();
        }
    }

    @Override
    protected void onPause() {
        if (mUpdateManager != null) {
            mUpdateManager.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUpdateManager != null) {
            mUpdateManager.onResume();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActions(intent);
    }

    @Override
    protected void initCreate(Bundle savedInstanceState) {
        if (isOnboardingRequired()) {
            initOnboarding();
            return;
        }


        mUpdateManager = new UpdateManager(this, null);
        if (mUpdateManager.check4CriticalUpdate()) {
            return;
        }


        super.initCreate(savedInstanceState);
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityMainBinding.bind(activityView);

        if (isOnboardingRequired()) {
            return;
        }

        if (SPAppActions.getRequireAccReminder(this) && !AccManager.isLoggedIn()) {
            initAccountReminder();
        }

        init();
    }

    private void init() {
        initContent();
        initActions(getIntent());
    }

    private void initHeader() {
        initMenu();

        mBinding.header.indexMenu.setOnClickListener(v -> mIndexMenu.open());

        //        ViewUtils.addStrokedBGToHeader(mBinding.header.getRoot(), R.color.colorBGHomePageItem, R.color.colorDivider);
    }

    private void initActions(Intent intent) {
        AppActions.checkForResourcesVersions(this);
        AppActions.doPendingActions(this);

        Bundle data = intent.getExtras();
        if (data == null) {
            return;
        }

        String action = data.getString(APP_ACTION_KEY);
        if (!TextUtils.isEmpty(action)) {
            String victim = data.getString(APP_ACTION_VICTIM_KEY);
            AppActions.doAction(this, null, data, action, victim, true, false);
        }

        data.remove(APP_ACTION_KEY);
        data.remove(APP_ACTION_VICTIM_KEY);

        intent.putExtras(data);
        setIntent(intent);
    }

    private void initContent() {
        initHeader();
        initViewPager();
        initBottomNavigation();
    }

    private void initViewPager() {
        ViewPager2 viewPager = mBinding.viewPager;
        ViewPagerAdapter2 mViewPagerAdapter = new ViewPagerAdapter2(this);
        mViewPagerAdapter.addFragment(FragMain.newInstance(), str(R.string.strLabelNavHome));
        viewPager.setAdapter(mViewPagerAdapter);
        viewPager.setOffscreenPageLimit(mViewPagerAdapter.getItemCount());
        viewPager.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        viewPager.setUserInputEnabled(false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mBinding.header.getRoot().setExpanded(true);

                boolean notMainPage = position == 1;
                mBinding.header.getRoot().setElevation(notMainPage ? 0 : dp2px(4));
            }
        });
    }

    private void initBottomNavigation() {
        BottomTabLayout bottomTabLayout = mBinding.bottomTabLayout;
        bottomTabLayout.setTabs(getBottomTabs());
        bottomTabLayout.setKingTab(new BottomTab(R.drawable.quran_kareem),
                kingTab -> launchActivity(ActivityReaderIndexPage.class));

        bottomTabLayout.setSelectionChangeListener(tab -> {
            if (tab.getId() == R.id.navSearch) {
                launchActivity(ActivitySearch.class);
            }
        });
    }

    private ArrayList<BottomTab> getBottomTabs() {
        @DrawableRes int[] bottomTabsIcons = {R.drawable.dr_icon_home, R.drawable.dr_icon_search};
        @StringRes int[] bottomTabsLabels = {R.string.strLabelNavHome, R.string.strLabelNavSearch};
        @IdRes int[] ids = {R.id.navHome, R.id.navSearch};

        ArrayList<BottomTab> bottomTabs = new ArrayList<>();
        for (int i = 0; i < bottomTabsIcons.length; i++) {
            BottomTab bottomTab = new BottomTab(getString(bottomTabsLabels[i]), bottomTabsIcons[i]);
            bottomTab.setId(ids[i]);
            bottomTabs.add(bottomTab);
        }

        return bottomTabs;
    }

    private void initMenu() {
        mIndexMenu = new IndexMenu(this, mBinding.getRoot());
    }

    private boolean isOnboardingRequired() {
        return SPAppActions.getRequireOnboarding(this);
    }

    private void initOnboarding() {
        launchActivity(ActivityOnboarding.class);
        finish();
    }

    private void initAccountReminder() {
        LytAccountReminderBinding binding = LytAccountReminderBinding.inflate(getLayoutInflater(), null, false);
        PeaceBottomSheet dialog = new PeaceBottomSheet();
        PeaceBottomSheet.PeaceBottomSheetParams p = dialog.getDialogParams();
        p.disableDragging = true;
        p.headerShown = false;
        p.disableBackKey = true;
        p.setContentView(binding.getRoot());
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager());

        binding.btnLogin.setOnClickListener(v -> {
            ActivityAccount.launchLogin(this);
            dialog.dismiss();
        });
        binding.btnRegister.setOnClickListener(v -> {
            ActivityAccount.launchRegister(this);
            dialog.dismiss();
        });
        binding.btnSkip.setOnClickListener(v -> dialog.dismiss());

        SPAppActions.setRequireAccReminder(this, false);
    }
}