package com.test.scrubdemo.activities;

import android.app.Fragment;

import com.test.scrubdemo.activities.base.BaseActivity;
import com.test.scrubdemo.fragments.MainFragment;


public class MainActivity extends BaseActivity
{
    @Override
    protected void onStart()
    {
        super.onStart();
    }


    @Override
    protected Fragment createFragment()
    {
        return new MainFragment();
    }

    public void showTime(Long time)
    {
        setTitle(Long.toString(time));
    }
}
