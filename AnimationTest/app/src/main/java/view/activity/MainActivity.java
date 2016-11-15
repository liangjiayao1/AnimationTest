package view.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.lucas.animationtest.R;
import com.example.lucas.animationtest.databinding.ActivityMainBinding;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import view.adapter.ImagePagerAdapter;
import view.adapter.PictureAdapter;
import view.dto.ImageDTO;
import view.entity.Image;
import view.service.ApiService;
import view.transformer.ImagePageTransformer;
import view.util.AnimationUtil;
import view.util.ZoomInUtil;

public class MainActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener
        , View.OnClickListener, ViewPager.OnPageChangeListener, com.miguelcatalan.materialsearchview.utils.AnimationUtil.AnimationListener {
    public static final int REQUEST_CODE = 1;
    private ActivityMainBinding binding;
    private PictureAdapter mAdapter;
    private ImagePagerAdapter vpAdapter;

    private long lastTime = 0;
    private boolean isFullScreen = false;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        getData("风景", false);
    }

    private void initView() {
        initViewPager();
        initToolbar();
        initSearchBar();
        initRecyclerView();
        binding.srlLayout.setEnabled(false);
    }

    private void initViewPager() {
        vpAdapter = new ImagePagerAdapter();
        binding.vpImgs.setVisibility(View.GONE);
        binding.vpImgs.setAdapter(vpAdapter);
        binding.vpImgs.setOffscreenPageLimit(3);

        //init pager change's animator in ViewPager
        ImagePageTransformer transformer = new ImagePageTransformer();
        binding.vpImgs.setPageTransformer(true, transformer);

        binding.vpImgs.addOnPageChangeListener(this);
    }

    private void initToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_item);
        binding.toolbar.setOnMenuItemClickListener(this);
    }

    private void initSearchBar() {
        binding.svSearch.findViewById(R.id.action_up_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearchBar();
            }
        });

        binding.svSearch.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getData(query, true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void initRecyclerView() {
        GridLayoutManager manager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);
        manager.setSmoothScrollbarEnabled(true);
        manager.setAutoMeasureEnabled(true);
        binding.recyclerView.setLayoutManager(manager);

        mAdapter = new PictureAdapter(binding.getRoot().getContext(), this);
        binding.recyclerView.setAdapter(mAdapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void getData(String tag, final boolean needRefresh) {
        binding.srlLayout.setRefreshing(true);
        ApiService.getPictureService().getPicture(tag)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ImageDTO>() {
                    @Override
                    public void onCompleted() {
                        binding.srlLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", "onError: " + e.toString());
                    }

                    @Override
                    public void onNext(ImageDTO imageDTO) {
                        if (needRefresh) {
                            mAdapter.clear();
                            mAdapter.notifyDataSetChanged();
                            binding.vpImgs.removeAllViews();
                            vpAdapter.clear();
                            vpAdapter.notifyDataSetChanged();
                        }
                        List<Image> images = imageDTO.getImgs();
                        for (int i = 0; i < images.size() - 1; i++) {
                            mAdapter.add(images.get(i));
                            vpAdapter.add(images.get(i));
                        }
                        mAdapter.notifyDataSetChanged();
                        vpAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                startAnimation();
                break;
            case R.id.action_change:
                changeNightMode(item);
                break;
            case R.id.action_search:
                if (!binding.svSearch.isSearchOpen()) {
                    binding.svSearch.showSearch(true);
                }
                break;
        }
        return true;
    }

    private void startAnimation() {
        int height = getWindowManager().getDefaultDisplay().getHeight();
        int width = getWindowManager().getDefaultDisplay().getWidth();
        final int endRadius = (int) Math.sqrt(height * height + width * width);

        final ViewGroup container = binding.clMain;
        final View targetView = getLayoutInflater().inflate(R.layout.activity_notification, container, false);
        container.addView(targetView, container.getWidth(), container.getHeight());
        View next = binding.toolbar.getChildAt(0);
        final int centerX = (next.getRight() + next.getLeft()) / 2;
        final int centerY = (next.getBottom() + next.getTop()) / 2;
        AnimationUtil.startActivityCircleReveal(this, container, targetView, centerX, centerY, endRadius);
    }

    //change night mode
    public void changeNightMode(MenuItem item) {
        boolean isNight = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        if (isNight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        recreate();
    }

    @Override
    public void onClick(final View v) {
        closeSearchBar();

        int position = mAdapter.getClickPosition();
        binding.vpImgs.setCurrentItem(position, false);
        Image currentImage = mAdapter.get(position);
        DisplayMetrics display = getResources().getDisplayMetrics();

        ZoomInUtil.initZoomInAnimation(v, binding.clMain, binding.vpImgs, currentImage, display);
        pictureZoomIn();
    }

    private void pictureZoomIn() {
        backgroundAnim(true, 300);
    }

    private void backgroundAnim(final boolean needShow, long duration) {
        binding.viewBackground
                .animate()
                .alpha(needShow ? 1 : 0)
                .setDuration(duration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        if (needShow) {
                            binding.vpImgs.setVisibility(View.VISIBLE);
                            isFullScreen = true;
                        } else {
                            binding.actionBarLayout.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (needShow) {
                            binding.actionBarLayout.setVisibility(View.GONE);
                        }
                    }
                })
                .start();
    }

    @Override
    public void onBackPressed() {
        if (isAnimating) {
            return;
        }
        if (binding.svSearch.isSearchOpen()) {
            closeSearchBar();
            return;
        }
        if (isFullScreen) {
            outOfFullScreen();
        } else {
            if (System.currentTimeMillis() - lastTime > 1000) {
                Snackbar.make(binding.clMain, "Are you sure to quit?", Snackbar.LENGTH_SHORT).show();
            } else {
                super.onBackPressed();
            }
            lastTime = System.currentTimeMillis();
        }
    }

    private void closeSearchBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimationUtil.revealReturn(binding.svSearch, this);
        } else {
            binding.svSearch.closeSearch();
        }
    }

    private void outOfFullScreen() {
        int currentItem = binding.vpImgs.getCurrentItem();
        PhotoView current = getPhotoView(currentItem);
        resetScale(current);
        DisplayMetrics display = getResources().getDisplayMetrics();
        View toView = mAdapter.getViews().get(currentItem);

        AnimatorListenerAdapter listener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                binding.vpImgs.setVisibility(View.GONE);
                isFullScreen = false;
                isAnimating = false;
            }
        };

        ZoomInUtil.initZoomOutAnimation(mAdapter.get(currentItem), binding.vpImgs, toView,
                binding.clMain, display, listener);

        backgroundAnim(false, 300);
    }

    @Override
    public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
        getPhotoView(position).setOnScaleChangeListener(new PhotoViewAttacher.OnScaleChangeListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                if (scaleFactor > getPhotoView(position).getMinimumScale()) {
                    binding.vpImgs.setLockPage(true);
                } else {
                    binding.vpImgs.setLockPage(false);
                }
            }
        });
    }

    @Override
    public void onPageSelected(int position) {
        GridLayoutManager manager = (GridLayoutManager) binding.recyclerView.getLayoutManager();
        manager.scrollToPosition(position);
        if (position > 6) {
            binding.actionBarLayout.setExpanded(false);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private PhotoView getPhotoView(int position) {
        View currentView = binding.vpImgs.findViewWithTag("pic" + position);
        if (currentView == null) {
            return null;
        }
        PhotoView photoView = (PhotoView) currentView.findViewById(R.id.iv_picture);
        return photoView;
    }

    private void resetScale(final PhotoView photoView) {
        if (photoView != null) {
            if (photoView.getScale() != photoView.getMinimumScale()) {
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        photoView.setScale(photoView.getMinimumScale(), true);
                    }
                }, 100);
            }
        }
    }

    @Override
    public boolean onAnimationStart(View view) {
        return false;
    }

    @Override
    public boolean onAnimationEnd(View view) {
        binding.svSearch.closeSearch();
        return true;
    }

    @Override
    public boolean onAnimationCancel(View view) {
        return false;
    }
}
