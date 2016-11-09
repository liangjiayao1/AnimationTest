package view.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.lucas.animationtest.R;
import com.example.lucas.animationtest.databinding.ActivityMainBinding;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import view.adapter.ImagePagerAdapter;
import view.adapter.PictureAdapter;
import view.dto.ImageDTO;
import view.entity.Image;
import view.service.ApiService;
import view.transformer.ImagePageTransformer;
import view.util.AnimationUtil;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener
        , Toolbar.OnMenuItemClickListener, AppBarLayout.OnOffsetChangedListener, View.OnClickListener, ViewPager.OnPageChangeListener {
    public static final int REQUEST_CODE = 1;
    private ActivityMainBinding binding;
    private PictureAdapter mAdapter;
    private ImagePagerAdapter vpAdapter;

    private long lastTime = 0;
    private boolean isFullScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        initView();
        getData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.actionBarLayout.addOnOffsetChangedListener(this);
    }

    private void initView() {
        binding.viewBackground.setAlpha(0);
        initViewPager();
        initToolbar();
        initRecyclerView();
        binding.srlLayout.setEnabled(false);
        binding.srlLayout.setOnRefreshListener(this);
    }

    private void initViewPager() {
        vpAdapter = new ImagePagerAdapter();
        binding.vpImgs.setVisibility(View.GONE);
        binding.vpImgs.setAdapter(vpAdapter);
        binding.vpImgs.setOffscreenPageLimit(3);

        ImagePageTransformer transformer = new ImagePageTransformer();
        binding.vpImgs.setPageTransformer(true, transformer);

        binding.vpImgs.addOnPageChangeListener(this);
    }

    private void initToolbar() {
        binding.toolbarLayout.setTitle("Album");
        binding.toolbar.inflateMenu(R.menu.menu_item);
        binding.toolbar.setOnMenuItemClickListener(this);
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

    private void getData() {
        binding.srlLayout.setRefreshing(true);
        ApiService.getPictureService().getPicture()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ImageDTO>() {
                    @Override
                    public void onCompleted() {
                        binding.srlLayout.setRefreshing(false);
                        binding.srlLayout.setEnabled(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", "onError: " + e.toString());
                    }

                    @Override
                    public void onNext(ImageDTO imageDTO) {
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
    public void onRefresh() {
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.clear();
                vpAdapter.clear();
                binding.vpImgs.removeAllViews();
                vpAdapter.notifyDataSetChanged();
                getData();
            }
        }, 200);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                startAnimation();
                break;
            case R.id.action_change:
                Intent intent = new Intent(MainActivity.this, ThemeActivity.class);
                startActivityForResult(intent, REQUEST_CODE);
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

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        binding.srlLayout.setEnabled(verticalOffset == 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.actionBarLayout.removeOnOffsetChangedListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE:
                    recreate();
                    break;
            }
        }
    }

    @Override
    public void onBackPressed() {
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

    private void outOfFullScreen() {
        binding.viewBackground
                .animate()
                .alpha(0)
                .setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        binding.vpImgs.setVisibility(View.GONE);
                    }
                })
                .start();
        isFullScreen = false;
    }

    @Override
    public void onClick(final View v) {
        int position = mAdapter.getClickPosition();
        binding.vpImgs.setCurrentItem(position, false);

        Point fromPoint = new Point();
        Rect fromRect = new Rect();
        v.getGlobalVisibleRect(fromRect, fromPoint);

        Point toPoint = new Point();
        Rect toRect = new Rect();
        binding.getRoot().getGlobalVisibleRect(toRect, toPoint);
        fromRect.offset(-toPoint.x, -toPoint.y);
        toRect.offset(-toPoint.x, -toPoint.y);

        float ratio = initZoomInPosition(position, fromRect, toRect);
        binding.vpImgs.setPivotX(0);
        binding.vpImgs.setPivotY(0);

        initZoomInAnimation(fromRect, toRect, ratio);
    }

    private float initZoomInPosition(int position, Rect fromRect, Rect toRect) {
        float ratio;
        if ((float) toRect.width() / (float) toRect.height() > (float) fromRect.width() / (float) fromRect.height()) {
            ratio = (float) fromRect.height() / (float) toRect.height();
            int fromWidth = (int) (toRect.width() * ratio);
            int deltaWidth = (fromWidth - toRect.width()) / 2;
            fromRect.left -= deltaWidth;
            fromRect.right += deltaWidth;
        } else {
            ratio = (float) fromRect.width() / (float) toRect.width();
            int fromHeight = (int) (toRect.height() * ratio);
            int deltaHeight = (fromHeight - fromRect.height()) / 2;
            fromRect.top -= deltaHeight;
            fromRect.bottom += deltaHeight;
            int fromWidth = (int) (toRect.width() * ratio);
            int deltaWidth = (fromWidth - fromRect.width()) / 2;
            if ((position + 1) % 3 == 0) {
                fromRect.left -= fromWidth - fromRect.width();
            } else if ((position + 1) % 3 != 1) {
                fromRect.left -= deltaWidth;
                fromRect.right += deltaWidth;
            }
        }
        return ratio;
    }

    private void initZoomInAnimation(Rect fromRect, Rect toRect, float ratio) {
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(binding.vpImgs, View.X, fromRect.left, toRect.left))
                .with(ObjectAnimator.ofFloat(binding.vpImgs, View.Y, fromRect.top, toRect.top))
                .with(ObjectAnimator.ofFloat(binding.vpImgs, View.SCALE_X, ratio, 1))
                .with(ObjectAnimator.ofFloat(binding.vpImgs, View.SCALE_Y, ratio, 1));

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                pictureZoomIn();
            }
        });
        set.setDuration(300);
        set.start();
    }

    private void pictureZoomIn() {
        binding.viewBackground.setAlpha(1);
        binding.vpImgs.setVisibility(View.VISIBLE);
        binding.actionBarLayout.setExpanded(false);
        isFullScreen = true;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        GridLayoutManager manager = (GridLayoutManager) binding.recyclerView.getLayoutManager();
        manager.scrollToPosition(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
