package com.magicwindow.deeplink.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.magicwindow.deeplink.R;
import com.magicwindow.deeplink.app.BaseAppCompatActivity;

import cn.salesuite.saf.inject.annotation.OnClick;

public class ShopDoneActivity extends BaseAppCompatActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_done);
        initToolBar();
    }

    private void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setNavigationIcon(R.drawable.ic_navigation);

        setSupportActionBar(toolbar);
        setTitle(R.string.shop_detail);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }
    @OnClick(id = R.id.go_home)
    public void goHome() {
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.putExtra(MainActivity.PAGE,"dianshang");
        startActivity(intent);
    }
}